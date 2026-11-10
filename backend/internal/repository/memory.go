package repository

import (
	"context"
	"sort"
	"sync"
	"time"

	"github.com/seunome/finlo-api/internal/domain"
)

type MemoryRepository struct {
	mu             sync.RWMutex
	users          map[string]*domain.User
	usersByEmail   map[string]*domain.User
	usersByGoogle  map[string]*domain.User
	refreshTokens  map[string]*domain.RefreshToken
	userVersions   map[string]int64
	userChanges    map[string]map[string]*domain.SyncChange
	allUserChanges map[string][]*domain.SyncChange
	idempotency    map[string][]byte
}

func NewMemoryRepository() *MemoryRepository {
	return &MemoryRepository{
		users:          make(map[string]*domain.User),
		usersByEmail:   make(map[string]*domain.User),
		usersByGoogle:  make(map[string]*domain.User),
		refreshTokens:  make(map[string]*domain.RefreshToken),
		userVersions:   make(map[string]int64),
		userChanges:    make(map[string]map[string]*domain.SyncChange),
		allUserChanges: make(map[string][]*domain.SyncChange),
		idempotency:    make(map[string][]byte),
	}
}

func (m *MemoryRepository) CreateUser(_ context.Context, user *domain.User) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	if _, exists := m.usersByEmail[user.Email]; exists {
		return domain.ErrUserAlreadyExists
	}

	copied := *user
	m.users[user.ID] = &copied
	m.usersByEmail[user.Email] = &copied
	if user.GoogleID != nil {
		m.usersByGoogle[*user.GoogleID] = &copied
	}
	m.userVersions[user.ID] = 0
	m.userChanges[user.ID] = make(map[string]*domain.SyncChange)
	return nil
}

func (m *MemoryRepository) GetUserByEmail(_ context.Context, email string) (*domain.User, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	user, exists := m.usersByEmail[email]
	if !exists {
		return nil, domain.ErrUserNotFound
	}
	copied := *user
	return &copied, nil
}

func (m *MemoryRepository) GetUserByID(_ context.Context, id string) (*domain.User, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	user, exists := m.users[id]
	if !exists {
		return nil, domain.ErrUserNotFound
	}
	copied := *user
	return &copied, nil
}

func (m *MemoryRepository) GetUserByGoogleID(_ context.Context, googleID string) (*domain.User, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	user, exists := m.usersByGoogle[googleID]
	if !exists {
		return nil, domain.ErrUserNotFound
	}
	copied := *user
	return &copied, nil
}

func (m *MemoryRepository) SaveRefreshToken(_ context.Context, token *domain.RefreshToken) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	copied := *token
	m.refreshTokens[token.TokenHash] = &copied
	return nil
}

func (m *MemoryRepository) GetRefreshToken(_ context.Context, tokenHash string) (*domain.RefreshToken, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	token, exists := m.refreshTokens[tokenHash]
	if !exists {
		return nil, domain.ErrInvalidToken
	}
	copied := *token
	return &copied, nil
}

func (m *MemoryRepository) RevokeRefreshToken(_ context.Context, tokenHash string) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	token, exists := m.refreshTokens[tokenHash]
	if !exists {
		return domain.ErrInvalidToken
	}
	now := time.Now()
	token.RevokedAt = &now
	return nil
}

func (m *MemoryRepository) RevokeAllUserTokens(_ context.Context, userID string) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	now := time.Now()
	for _, token := range m.refreshTokens {
		if token.UserID == userID {
			token.RevokedAt = &now
		}
	}
	return nil
}

func (m *MemoryRepository) DeleteUserData(_ context.Context, userID string) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	user, exists := m.users[userID]
	if exists {
		delete(m.usersByEmail, user.Email)
		if user.GoogleID != nil {
			delete(m.usersByGoogle, *user.GoogleID)
		}
		delete(m.users, userID)
	}

	for hash, token := range m.refreshTokens {
		if token.UserID == userID {
			delete(m.refreshTokens, hash)
		}
	}

	delete(m.userVersions, userID)
	delete(m.userChanges, userID)
	delete(m.allUserChanges, userID)

	for k := range m.idempotency {
		if len(k) > len(userID) && k[:len(userID)] == userID {
			delete(m.idempotency, k)
		}
	}

	return nil
}

func (m *MemoryRepository) CheckIdempotencyKey(_ context.Context, userID, key string) (bool, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	compositeKey := userID + ":" + key
	_, exists := m.idempotency[compositeKey]
	return exists, nil
}

func (m *MemoryRepository) RecordIdempotencyKey(_ context.Context, userID, key string, response []byte) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	compositeKey := userID + ":" + key
	m.idempotency[compositeKey] = response
	return nil
}

func (m *MemoryRepository) GetIdempotencyResponse(_ context.Context, userID, key string) ([]byte, bool, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	compositeKey := userID + ":" + key
	resp, exists := m.idempotency[compositeKey]
	return resp, exists, nil
}

func (m *MemoryRepository) ApplyChanges(_ context.Context, userID string, changes []domain.SyncChange) (int64, int, error) {
	m.mu.Lock()
	defer m.mu.Unlock()

	currentVersion := m.userVersions[userID]
	activeEntities, ok := m.userChanges[userID]
	if !ok {
		activeEntities = make(map[string]*domain.SyncChange)
		m.userChanges[userID] = activeEntities
	}

	var appliedCount int
	for _, item := range changes {
		entityKey := string(item.Entity) + ":" + item.EntityID
		existing, hasExisting := activeEntities[entityKey]

		if hasExisting {
			if item.ClientUpdated < existing.ClientUpdated {
				continue
			}
			if item.ClientUpdated == existing.ClientUpdated && item.DeviceID <= existing.DeviceID {
				continue
			}
		}

		currentVersion++
		appliedCount++
		changeRecord := item
		changeRecord.UserID = userID
		changeRecord.ServerVersion = currentVersion

		activeEntities[entityKey] = &changeRecord
		m.allUserChanges[userID] = append(m.allUserChanges[userID], &changeRecord)
	}

	m.userVersions[userID] = currentVersion
	return currentVersion, appliedCount, nil
}

func (m *MemoryRepository) GetChangesAfterVersion(_ context.Context, userID string, cursor int64, limit int) ([]domain.SyncChange, int64, bool, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	if limit <= 0 {
		limit = 100
	}

	all := m.allUserChanges[userID]
	var filtered []domain.SyncChange
	for _, ch := range all {
		if ch.ServerVersion > cursor {
			filtered = append(filtered, *ch)
		}
	}

	sort.Slice(filtered, func(i, j int) bool {
		return filtered[i].ServerVersion < filtered[j].ServerVersion
	})

	hasMore := len(filtered) > limit
	if hasMore {
		filtered = filtered[:limit]
	}

	var nextCursor = cursor
	if len(filtered) > 0 {
		nextCursor = filtered[len(filtered)-1].ServerVersion
	}

	return filtered, nextCursor, hasMore, nil
}

func (m *MemoryRepository) GetAllChangesForExport(_ context.Context, userID string) ([]domain.SyncChange, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	all := m.allUserChanges[userID]
	result := make([]domain.SyncChange, len(all))
	for i, ch := range all {
		result[i] = *ch
	}
	return result, nil
}
