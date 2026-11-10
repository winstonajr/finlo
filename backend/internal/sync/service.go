package sync

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/seunome/finlo-api/internal/domain"
	"github.com/seunome/finlo-api/internal/repository"
)

type Service struct {
	syncRepo repository.SyncRepository
}

func NewService(syncRepo repository.SyncRepository) *Service {
	return &Service{syncRepo: syncRepo}
}

func (s *Service) Push(ctx context.Context, userID, idempotencyKey string, req domain.SyncPushRequest) (*domain.SyncPushResponse, error) {
	if idempotencyKey == "" {
		return nil, domain.ErrIdempotencyKeyMissing
	}

	cachedResp, exists, err := s.syncRepo.GetIdempotencyResponse(ctx, userID, idempotencyKey)
	if err != nil {
		return nil, fmt.Errorf("failed to check idempotency: %w", err)
	}
	if exists {
		var resp domain.SyncPushResponse
		if err := json.Unmarshal(cachedResp, &resp); err == nil {
			return &resp, nil
		}
	}

	maxAllowedFuture := time.Now().Add(5 * time.Minute).UnixMilli()

	var changes []domain.SyncChange
	for _, item := range req.Changes {
		if item.UpdatedAt > maxAllowedFuture {
			return nil, domain.ErrFutureTimestamp
		}

		changeID := uuid.New().String()
		changes = append(changes, domain.SyncChange{
			ID:            changeID,
			UserID:        userID,
			Entity:        item.Entity,
			EntityID:      item.ID,
			Operation:     item.Operation,
			Payload:       item.Payload,
			ClientUpdated: item.UpdatedAt,
			DeviceID:      item.DeviceID,
			DeletedAt:     item.DeletedAt,
		})
	}

	latestVersion, appliedCount, err := s.syncRepo.ApplyChanges(ctx, userID, changes)
	if err != nil {
		return nil, fmt.Errorf("failed to apply changes: %w", err)
	}

	response := &domain.SyncPushResponse{
		AppliedCount:  appliedCount,
		ServerVersion: latestVersion,
	}

	respBytes, err := json.Marshal(response)
	if err == nil {
		_ = s.syncRepo.RecordIdempotencyKey(ctx, userID, idempotencyKey, respBytes)
	}

	return response, nil
}

func (s *Service) Pull(ctx context.Context, userID string, cursor int64, limit int) (*domain.SyncPullResponse, error) {
	if limit <= 0 || limit > 500 {
		limit = 100
	}

	changes, nextCursor, hasMore, err := s.syncRepo.GetChangesAfterVersion(ctx, userID, cursor, limit)
	if err != nil {
		return nil, fmt.Errorf("failed to pull changes: %w", err)
	}

	if changes == nil {
		changes = []domain.SyncChange{}
	}

	return &domain.SyncPullResponse{
		Changes:    changes,
		NextCursor: nextCursor,
		HasMore:    hasMore,
	}, nil
}

func (s *Service) ExportUserData(ctx context.Context, user *domain.User) (*domain.ExportData, error) {
	changes, err := s.syncRepo.GetAllChangesForExport(ctx, user.ID)
	if err != nil {
		return nil, fmt.Errorf("failed to export data: %w", err)
	}

	if changes == nil {
		changes = []domain.SyncChange{}
	}

	return &domain.ExportData{
		User:       *user,
		Changes:    changes,
		ExportDate: time.Now().UTC(),
	}, nil
}
