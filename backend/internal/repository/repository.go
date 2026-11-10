package repository

import (
	"context"

	"github.com/seunome/finlo-api/internal/domain"
)

type UserRepository interface {
	CreateUser(ctx context.Context, user *domain.User) error
	GetUserByEmail(ctx context.Context, email string) (*domain.User, error)
	GetUserByID(ctx context.Context, id string) (*domain.User, error)
	GetUserByGoogleID(ctx context.Context, googleID string) (*domain.User, error)
	SaveRefreshToken(ctx context.Context, token *domain.RefreshToken) error
	GetRefreshToken(ctx context.Context, tokenHash string) (*domain.RefreshToken, error)
	RevokeRefreshToken(ctx context.Context, tokenHash string) error
	RevokeAllUserTokens(ctx context.Context, userID string) error
	DeleteUserData(ctx context.Context, userID string) error
}

type SyncRepository interface {
	CheckIdempotencyKey(ctx context.Context, userID, key string) (bool, error)
	RecordIdempotencyKey(ctx context.Context, userID, key string, response []byte) error
	GetIdempotencyResponse(ctx context.Context, userID, key string) ([]byte, bool, error)
	ApplyChanges(ctx context.Context, userID string, changes []domain.SyncChange) (int64, int, error)
	GetChangesAfterVersion(ctx context.Context, userID string, cursor int64, limit int) ([]domain.SyncChange, int64, bool, error)
	GetAllChangesForExport(ctx context.Context, userID string) ([]domain.SyncChange, error)
}
