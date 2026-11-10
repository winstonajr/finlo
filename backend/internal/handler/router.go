package handler

import (
	"log/slog"
	"net/http"

	"github.com/seunome/finlo-api/internal/auth"
	"github.com/seunome/finlo-api/internal/repository"
	"github.com/seunome/finlo-api/internal/storage"
	"github.com/seunome/finlo-api/internal/sync"
)

type RouterDependencies struct {
	AuthService       *auth.Service
	TokenManager      *auth.TokenManager
	SyncService       *sync.Service
	UserRepo          repository.UserRepository
	AttachmentService *storage.AttachmentService
	Logger            *slog.Logger
}

func SetupRouter(deps RouterDependencies) http.Handler {
	mux := http.NewServeMux()

	healthHandler := NewHealthHandler()
	mux.HandleFunc("GET /health/live", healthHandler.Live)
	mux.HandleFunc("GET /health/ready", healthHandler.Ready)

	authHandler := NewAuthHandler(deps.AuthService)
	mux.HandleFunc("POST /v1/auth/register", authHandler.Register)
	mux.HandleFunc("POST /v1/auth/login", authHandler.Login)
	mux.HandleFunc("POST /v1/auth/refresh", authHandler.Refresh)
	mux.HandleFunc("POST /v1/auth/google", authHandler.Google)

	syncHandler := NewSyncHandler(deps.SyncService)
	storageHandler := NewStorageHandler(deps.AttachmentService)
	userHandler := NewUserHandler(deps.UserRepo, deps.SyncService)

	authMW := AuthMiddleware(deps.TokenManager)

	mux.Handle("POST /v1/sync/push", authMW(http.HandlerFunc(syncHandler.Push)))
	mux.Handle("GET /v1/sync/pull", authMW(http.HandlerFunc(syncHandler.Pull)))

	mux.Handle("POST /v1/attachments/presign-upload", authMW(http.HandlerFunc(storageHandler.PresignUpload)))
	mux.Handle("GET /v1/attachments/presign-download", authMW(http.HandlerFunc(storageHandler.PresignDownload)))

	mux.Handle("GET /v1/user/export", authMW(http.HandlerFunc(userHandler.Export)))
	mux.Handle("DELETE /v1/user/account", authMW(http.HandlerFunc(userHandler.DeleteAccount)))

	handler := CorsMiddleware(LoggingMiddleware(deps.Logger)(mux))
	return handler
}
