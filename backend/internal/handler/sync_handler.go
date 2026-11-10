package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"strconv"

	"github.com/seunome/finlo-api/internal/domain"
	"github.com/seunome/finlo-api/internal/sync"
)

type SyncHandler struct {
	syncService *sync.Service
}

func NewSyncHandler(syncService *sync.Service) *SyncHandler {
	return &SyncHandler{syncService: syncService}
}

func (h *SyncHandler) Push(w http.ResponseWriter, r *http.Request) {
	userID := GetUserID(r.Context())
	if userID == "" {
		writeJSONError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	idempotencyKey := r.Header.Get("Idempotency-Key")
	if idempotencyKey == "" {
		writeJSONError(w, http.StatusBadRequest, "header Idempotency-Key is required")
		return
	}

	var req domain.SyncPushRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSONError(w, http.StatusBadRequest, "invalid push payload")
		return
	}

	resp, err := h.syncService.Push(r.Context(), userID, idempotencyKey, req)
	if err != nil {
		if errors.Is(err, domain.ErrFutureTimestamp) {
			writeJSONError(w, http.StatusBadRequest, "client timestamp too far in the future")
			return
		}
		writeJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, resp)
}

func (h *SyncHandler) Pull(w http.ResponseWriter, r *http.Request) {
	userID := GetUserID(r.Context())
	if userID == "" {
		writeJSONError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	var cursor int64
	var limit int = 100

	if cursorStr := r.URL.Query().Get("cursor"); cursorStr != "" {
		if parsed, err := strconv.ParseInt(cursorStr, 10, 64); err == nil {
			cursor = parsed
		}
	}

	if limitStr := r.URL.Query().Get("limit"); limitStr != "" {
		if parsed, err := strconv.Atoi(limitStr); err == nil {
			limit = parsed
		}
	}

	resp, err := h.syncService.Pull(r.Context(), userID, cursor, limit)
	if err != nil {
		writeJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, resp)
}
