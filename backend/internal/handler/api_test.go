package handler

import (
	"bytes"
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/seunome/finlo-api/internal/auth"
	"github.com/seunome/finlo-api/internal/domain"
	"github.com/seunome/finlo-api/internal/repository"
	"github.com/seunome/finlo-api/internal/storage"
	"github.com/seunome/finlo-api/internal/sync"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func setupTestServer() (http.Handler, *auth.TokenManager) {
	memRepo := repository.NewMemoryRepository()
	tm := auth.NewTokenManager("test-secret-at-least-32-bytes-finlo-api", 15*time.Minute, 30*24*time.Hour)
	authService := auth.NewService(memRepo, tm)
	syncService := sync.NewService(memRepo)
	attachmentService := storage.NewAttachmentService(storage.S3Config{
		Endpoint:        "http://localhost:9000",
		Bucket:          "finlo-test",
		AccessKeyID:     "minio",
		SecretAccessKey: "miniosecret",
	})
	logger := slog.New(slog.NewTextHandler(io.Discard, nil))

	router := SetupRouter(RouterDependencies{
		AuthService:       authService,
		TokenManager:      tm,
		SyncService:       syncService,
		UserRepo:          memRepo,
		AttachmentService: attachmentService,
		Logger:            logger,
	})

	return router, tm
}

func TestHealthEndpoints(t *testing.T) {
	router, _ := setupTestServer()

	reqLive := httptest.NewRequest(http.MethodGet, "/health/live", nil)
	wLive := httptest.NewRecorder()
	router.ServeHTTP(wLive, reqLive)
	assert.Equal(t, http.StatusOK, wLive.Code)
	assert.Contains(t, wLive.Body.String(), `"status":"ok"`)

	reqReady := httptest.NewRequest(http.MethodGet, "/health/ready", nil)
	wReady := httptest.NewRecorder()
	router.ServeHTTP(wReady, reqReady)
	assert.Equal(t, http.StatusOK, wReady.Code)
	assert.Contains(t, wReady.Body.String(), `"status":"ready"`)
}

func TestAuthAndSyncFlow(t *testing.T) {
	router, _ := setupTestServer()

	// 1. Register
	regBody, _ := json.Marshal(map[string]string{
		"email":    "user@finlo.app",
		"password": "Password123!",
	})
	reqReg := httptest.NewRequest(http.MethodPost, "/v1/auth/register", bytes.NewReader(regBody))
	wReg := httptest.NewRecorder()
	router.ServeHTTP(wReg, reqReg)
	require.Equal(t, http.StatusCreated, wReg.Code)

	var authResp domain.AuthResponse
	err := json.Unmarshal(wReg.Body.Bytes(), &authResp)
	require.NoError(t, err)
	assert.NotEmpty(t, authResp.AccessToken)
	assert.NotEmpty(t, authResp.RefreshToken)

	// 2. Login
	loginBody, _ := json.Marshal(map[string]string{
		"email":    "user@finlo.app",
		"password": "Password123!",
	})
	reqLogin := httptest.NewRequest(http.MethodPost, "/v1/auth/login", bytes.NewReader(loginBody))
	wLogin := httptest.NewRecorder()
	router.ServeHTTP(wLogin, reqLogin)
	require.Equal(t, http.StatusOK, wLogin.Code)

	// 3. Refresh token
	refBody, _ := json.Marshal(map[string]string{
		"refreshToken": authResp.RefreshToken,
	})
	reqRef := httptest.NewRequest(http.MethodPost, "/v1/auth/refresh", bytes.NewReader(refBody))
	wRef := httptest.NewRecorder()
	router.ServeHTTP(wRef, reqRef)
	require.Equal(t, http.StatusOK, wRef.Code)

	var refResp domain.AuthResponse
	_ = json.Unmarshal(wRef.Body.Bytes(), &refResp)
	assert.NotEmpty(t, refResp.AccessToken)
	accessToken := refResp.AccessToken

	// 4. Sync Push
	pushPayload, _ := json.Marshal(domain.SyncPushRequest{
		Changes: []domain.SyncPushItem{
			{
				Entity:    domain.EntityAccount,
				ID:        "acc-100",
				Operation: domain.OpUpsert,
				Payload:   json.RawMessage(`{"name":"Main Wallet","cents":15000}`),
				UpdatedAt: time.Now().UnixMilli(),
				DeviceID:  "dev-android",
			},
		},
	})
	reqPush := httptest.NewRequest(http.MethodPost, "/v1/sync/push", bytes.NewReader(pushPayload))
	reqPush.Header.Set("Authorization", "Bearer "+accessToken)
	reqPush.Header.Set("Idempotency-Key", "push-batch-1")
	wPush := httptest.NewRecorder()
	router.ServeHTTP(wPush, reqPush)
	require.Equal(t, http.StatusOK, wPush.Code)

	var pushResp domain.SyncPushResponse
	_ = json.Unmarshal(wPush.Body.Bytes(), &pushResp)
	assert.Equal(t, 1, pushResp.AppliedCount)

	// 5. Sync Pull
	reqPull := httptest.NewRequest(http.MethodGet, "/v1/sync/pull?cursor=0&limit=50", nil)
	reqPull.Header.Set("Authorization", "Bearer "+accessToken)
	wPull := httptest.NewRecorder()
	router.ServeHTTP(wPull, reqPull)
	require.Equal(t, http.StatusOK, wPull.Code)

	var pullResp domain.SyncPullResponse
	_ = json.Unmarshal(wPull.Body.Bytes(), &pullResp)
	assert.Len(t, pullResp.Changes, 1)
	assert.Equal(t, "acc-100", pullResp.Changes[0].EntityID)

	// 6. Presign Upload & Download
	uploadReqBody, _ := json.Marshal(domain.PresignUploadRequest{
		FileName:    "receipt.png",
		ContentType: "image/png",
		Size:        1024,
	})
	reqUpload := httptest.NewRequest(http.MethodPost, "/v1/attachments/presign-upload", bytes.NewReader(uploadReqBody))
	reqUpload.Header.Set("Authorization", "Bearer "+accessToken)
	wUpload := httptest.NewRecorder()
	router.ServeHTTP(wUpload, reqUpload)
	require.Equal(t, http.StatusOK, wUpload.Code)

	var uploadResp domain.PresignUploadResponse
	_ = json.Unmarshal(wUpload.Body.Bytes(), &uploadResp)
	assert.NotEmpty(t, uploadResp.UploadURL)
	assert.NotEmpty(t, uploadResp.FileKey)

	reqDownload := httptest.NewRequest(http.MethodGet, "/v1/attachments/presign-download?fileKey="+uploadResp.FileKey, nil)
	reqDownload.Header.Set("Authorization", "Bearer "+accessToken)
	wDownload := httptest.NewRecorder()
	router.ServeHTTP(wDownload, reqDownload)
	require.Equal(t, http.StatusOK, wDownload.Code)

	// 7. LGPD Export
	reqExport := httptest.NewRequest(http.MethodGet, "/v1/user/export", nil)
	reqExport.Header.Set("Authorization", "Bearer "+accessToken)
	wExport := httptest.NewRecorder()
	router.ServeHTTP(wExport, reqExport)
	require.Equal(t, http.StatusOK, wExport.Code)

	var exportResp domain.ExportData
	_ = json.Unmarshal(wExport.Body.Bytes(), &exportResp)
	assert.Equal(t, "user@finlo.app", exportResp.User.Email)
	assert.Len(t, exportResp.Changes, 1)

	// 8. LGPD Delete Account
	reqDelete := httptest.NewRequest(http.MethodDelete, "/v1/user/account", nil)
	reqDelete.Header.Set("Authorization", "Bearer "+accessToken)
	wDelete := httptest.NewRecorder()
	router.ServeHTTP(wDelete, reqDelete)
	require.Equal(t, http.StatusOK, wDelete.Code)
}
