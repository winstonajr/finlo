package handler

import (
	"encoding/json"
	"net/http"

	"github.com/seunome/finlo-api/internal/domain"
	"github.com/seunome/finlo-api/internal/storage"
)

type StorageHandler struct {
	attachmentService *storage.AttachmentService
}

func NewStorageHandler(attachmentService *storage.AttachmentService) *StorageHandler {
	return &StorageHandler{attachmentService: attachmentService}
}

func (h *StorageHandler) PresignUpload(w http.ResponseWriter, r *http.Request) {
	userID := GetUserID(r.Context())
	if userID == "" {
		writeJSONError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var req domain.PresignUploadRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSONError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	resp, err := h.attachmentService.GeneratePresignedUpload(userID, req)
	if err != nil {
		writeJSONError(w, http.StatusBadRequest, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, resp)
}

func (h *StorageHandler) PresignDownload(w http.ResponseWriter, r *http.Request) {
	userID := GetUserID(r.Context())
	if userID == "" {
		writeJSONError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	fileKey := r.URL.Query().Get("fileKey")
	if fileKey == "" {
		writeJSONError(w, http.StatusBadRequest, "parameter fileKey is required")
		return
	}

	resp, err := h.attachmentService.GeneratePresignedDownload(userID, fileKey)
	if err != nil {
		writeJSONError(w, http.StatusForbidden, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, resp)
}
