package storage

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"net/url"
	"path/filepath"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/seunome/finlo-api/internal/domain"
)

type S3Config struct {
	Endpoint        string
	Bucket          string
	AccessKeyID     string
	SecretAccessKey string
	Region          string
}

type AttachmentService struct {
	config S3Config
}

func NewAttachmentService(config S3Config) *AttachmentService {
	return &AttachmentService{config: config}
}

var allowedMimeTypes = map[string]bool{
	"image/jpeg":      true,
	"image/jpg":       true,
	"image/png":       true,
	"image/webp":      true,
	"application/pdf": true,
}

func (s *AttachmentService) GeneratePresignedUpload(userID string, req domain.PresignUploadRequest) (*domain.PresignUploadResponse, error) {
	if !allowedMimeTypes[strings.ToLower(req.ContentType)] {
		return nil, errors.New("unsupported content type; only images and PDFs are allowed")
	}

	const maxSizeBytes = 10 * 1024 * 1024
	if req.Size > maxSizeBytes {
		return nil, errors.New("file size exceeds maximum allowed limit (10MB)")
	}

	ext := filepath.Ext(req.FileName)
	if ext == "" {
		ext = ".bin"
	}
	fileKey := fmt.Sprintf("receipts/%s/%s%s", userID, uuid.New().String(), ext)

	expiresIn := int64(900)
	expiresAt := time.Now().Add(time.Duration(expiresIn) * time.Second).Unix()

	signature := s.computeHMAC(fmt.Sprintf("PUT\n%s\n%d", fileKey, expiresAt))

	uploadURL := fmt.Sprintf("%s/%s/%s?expires=%d&signature=%s",
		strings.TrimRight(s.config.Endpoint, "/"),
		s.config.Bucket,
		url.PathEscape(fileKey),
		expiresAt,
		signature,
	)

	return &domain.PresignUploadResponse{
		UploadURL: uploadURL,
		FileKey:   fileKey,
		ExpiresIn: expiresIn,
	}, nil
}

func (s *AttachmentService) GeneratePresignedDownload(userID, fileKey string) (*domain.PresignDownloadResponse, error) {
	expectedPrefix := fmt.Sprintf("receipts/%s/", userID)
	if !strings.HasPrefix(fileKey, expectedPrefix) {
		return nil, errors.New("unauthorized attachment access")
	}

	expiresIn := int64(1800)
	expiresAt := time.Now().Add(time.Duration(expiresIn) * time.Second).Unix()

	signature := s.computeHMAC(fmt.Sprintf("GET\n%s\n%d", fileKey, expiresAt))

	downloadURL := fmt.Sprintf("%s/%s/%s?expires=%d&signature=%s",
		strings.TrimRight(s.config.Endpoint, "/"),
		s.config.Bucket,
		url.PathEscape(fileKey),
		expiresAt,
		signature,
	)

	return &domain.PresignDownloadResponse{
		DownloadURL: downloadURL,
		ExpiresIn:   expiresIn,
	}, nil
}

func (s *AttachmentService) computeHMAC(payload string) string {
	mac := hmac.New(sha256.New, []byte(s.config.SecretAccessKey))
	mac.Write([]byte(payload))
	return hex.EncodeToString(mac.Sum(nil))
}
