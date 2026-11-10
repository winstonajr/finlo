package handler

import (
	"net/http"

	"github.com/seunome/finlo-api/internal/repository"
	"github.com/seunome/finlo-api/internal/sync"
)

type UserHandler struct {
	userRepo    repository.UserRepository
	syncService *sync.Service
}

func NewUserHandler(userRepo repository.UserRepository, syncService *sync.Service) *UserHandler {
	return &UserHandler{
		userRepo:    userRepo,
		syncService: syncService,
	}
}

func (h *UserHandler) Export(w http.ResponseWriter, r *http.Request) {
	userID := GetUserID(r.Context())
	if userID == "" {
		writeJSONError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	user, err := h.userRepo.GetUserByID(r.Context(), userID)
	if err != nil {
		writeJSONError(w, http.StatusNotFound, "user not found")
		return
	}

	exportData, err := h.syncService.ExportUserData(r.Context(), user)
	if err != nil {
		writeJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, exportData)
}

func (h *UserHandler) DeleteAccount(w http.ResponseWriter, r *http.Request) {
	userID := GetUserID(r.Context())
	if userID == "" {
		writeJSONError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	if err := h.userRepo.DeleteUserData(r.Context(), userID); err != nil {
		writeJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{"message": "account and all associated data deleted successfully"})
}
