package repository

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/seunome/finlo-api/internal/domain"
)

type PostgresRepository struct {
	db *sql.DB
}

func NewPostgresRepository(db *sql.DB) *PostgresRepository {
	return &PostgresRepository{db: db}
}

func (p *PostgresRepository) CreateUser(ctx context.Context, user *domain.User) error {
	query := `
		INSERT INTO users (id, email, password_hash, google_id, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6)
	`
	_, err := p.db.ExecContext(ctx, query, user.ID, user.Email, user.PasswordHash, user.GoogleID, user.CreatedAt, user.UpdatedAt)
	if err != nil {
		return fmt.Errorf("failed to create user: %w", err)
	}

	initCounterQuery := `
		INSERT INTO user_version_counters (user_id, current_version)
		VALUES ($1, 0)
		ON CONFLICT (user_id) DO NOTHING
	`
	_, _ = p.db.ExecContext(ctx, initCounterQuery, user.ID)

	return nil
}

func (p *PostgresRepository) GetUserByEmail(ctx context.Context, email string) (*domain.User, error) {
	query := `
		SELECT id, email, password_hash, google_id, created_at, updated_at
		FROM users
		WHERE email = $1
	`
	row := p.db.QueryRowContext(ctx, query, email)
	var u domain.User
	err := row.Scan(&u.ID, &u.Email, &u.PasswordHash, &u.GoogleID, &u.CreatedAt, &u.UpdatedAt)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, domain.ErrUserNotFound
		}
		return nil, fmt.Errorf("failed to get user by email: %w", err)
	}
	return &u, nil
}

func (p *PostgresRepository) GetUserByID(ctx context.Context, id string) (*domain.User, error) {
	query := `
		SELECT id, email, password_hash, google_id, created_at, updated_at
		FROM users
		WHERE id = $1
	`
	row := p.db.QueryRowContext(ctx, query, id)
	var u domain.User
	err := row.Scan(&u.ID, &u.Email, &u.PasswordHash, &u.GoogleID, &u.CreatedAt, &u.UpdatedAt)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, domain.ErrUserNotFound
		}
		return nil, fmt.Errorf("failed to get user by id: %w", err)
	}
	return &u, nil
}

func (p *PostgresRepository) GetUserByGoogleID(ctx context.Context, googleID string) (*domain.User, error) {
	query := `
		SELECT id, email, password_hash, google_id, created_at, updated_at
		FROM users
		WHERE google_id = $1
	`
	row := p.db.QueryRowContext(ctx, query, googleID)
	var u domain.User
	err := row.Scan(&u.ID, &u.Email, &u.PasswordHash, &u.GoogleID, &u.CreatedAt, &u.UpdatedAt)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, domain.ErrUserNotFound
		}
		return nil, fmt.Errorf("failed to get user by google id: %w", err)
	}
	return &u, nil
}

func (p *PostgresRepository) SaveRefreshToken(ctx context.Context, token *domain.RefreshToken) error {
	query := `
		INSERT INTO refresh_tokens (token_hash, user_id, expires_at, revoked_at)
		VALUES ($1, $2, $3, $4)
		ON CONFLICT (token_hash) DO UPDATE
		SET expires_at = EXCLUDED.expires_at, revoked_at = EXCLUDED.revoked_at
	`
	_, err := p.db.ExecContext(ctx, query, token.TokenHash, token.UserID, token.ExpiresAt, token.RevokedAt)
	if err != nil {
		return fmt.Errorf("failed to save refresh token: %w", err)
	}
	return nil
}

func (p *PostgresRepository) GetRefreshToken(ctx context.Context, tokenHash string) (*domain.RefreshToken, error) {
	query := `
		SELECT token_hash, user_id, expires_at, revoked_at
		FROM refresh_tokens
		WHERE token_hash = $1
	`
	row := p.db.QueryRowContext(ctx, query, tokenHash)
	var t domain.RefreshToken
	err := row.Scan(&t.TokenHash, &t.UserID, &t.ExpiresAt, &t.RevokedAt)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, domain.ErrInvalidToken
		}
		return nil, fmt.Errorf("failed to get refresh token: %w", err)
	}
	return &t, nil
}

func (p *PostgresRepository) RevokeRefreshToken(ctx context.Context, tokenHash string) error {
	query := `
		UPDATE refresh_tokens
		SET revoked_at = $1
		WHERE token_hash = $2
	`
	_, err := p.db.ExecContext(ctx, query, time.Now(), tokenHash)
	if err != nil {
		return fmt.Errorf("failed to revoke refresh token: %w", err)
	}
	return nil
}

func (p *PostgresRepository) RevokeAllUserTokens(ctx context.Context, userID string) error {
	query := `
		UPDATE refresh_tokens
		SET revoked_at = $1
		WHERE user_id = $2
	`
	_, err := p.db.ExecContext(ctx, query, time.Now(), userID)
	if err != nil {
		return fmt.Errorf("failed to revoke user tokens: %w", err)
	}
	return nil
}

func (p *PostgresRepository) DeleteUserData(ctx context.Context, userID string) error {
	query := `DELETE FROM users WHERE id = $1`
	_, err := p.db.ExecContext(ctx, query, userID)
	if err != nil {
		return fmt.Errorf("failed to delete user: %w", err)
	}
	return nil
}

func (p *PostgresRepository) CheckIdempotencyKey(ctx context.Context, userID, key string) (bool, error) {
	query := `SELECT 1 FROM idempotency_keys WHERE user_id = $1 AND key = $2`
	var exists int
	err := p.db.QueryRowContext(ctx, query, userID, key).Scan(&exists)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return false, nil
		}
		return false, fmt.Errorf("failed to check idempotency key: %w", err)
	}
	return true, nil
}

func (p *PostgresRepository) RecordIdempotencyKey(ctx context.Context, userID, key string, response []byte) error {
	query := `
		INSERT INTO idempotency_keys (user_id, key, response, created_at)
		VALUES ($1, $2, $3, $4)
		ON CONFLICT (user_id, key) DO UPDATE
		SET response = EXCLUDED.response
	`
	_, err := p.db.ExecContext(ctx, query, userID, key, response, time.Now())
	if err != nil {
		return fmt.Errorf("failed to record idempotency key: %w", err)
	}
	return nil
}

func (p *PostgresRepository) GetIdempotencyResponse(ctx context.Context, userID, key string) ([]byte, bool, error) {
	query := `SELECT response FROM idempotency_keys WHERE user_id = $1 AND key = $2`
	var resp []byte
	err := p.db.QueryRowContext(ctx, query, userID, key).Scan(&resp)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, false, nil
		}
		return nil, false, fmt.Errorf("failed to get idempotency response: %w", err)
	}
	return resp, true, nil
}

func (p *PostgresRepository) ApplyChanges(ctx context.Context, userID string, changes []domain.SyncChange) (int64, error) {
	tx, err := p.db.BeginTx(ctx, nil)
	if err != nil {
		return 0, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer func() {
		_ = tx.Rollback()
	}()

	var currentVersion int64
	versionQuery := `
		SELECT current_version FROM user_version_counters
		WHERE user_id = $1
		FOR UPDATE
	`
	err = tx.QueryRowContext(ctx, versionQuery, userID).Scan(&currentVersion)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			currentVersion = 0
			_, err = tx.ExecContext(ctx, `INSERT INTO user_version_counters (user_id, current_version) VALUES ($1, 0)`, userID)
			if err != nil {
				return 0, fmt.Errorf("failed to init user version counter: %w", err)
			}
		} else {
			return 0, fmt.Errorf("failed to fetch user version: %w", err)
		}
	}

	for _, item := range changes {
		latestQuery := `
			SELECT client_updated, device_id FROM sync_changes
			WHERE user_id = $1 AND entity = $2 AND entity_id = $3
			ORDER BY server_version DESC LIMIT 1
		`
		var existingUpdated int64
		var existingDevice string
		err = tx.QueryRowContext(ctx, latestQuery, userID, item.Entity, item.EntityID).Scan(&existingUpdated, &existingDevice)
		if err == nil {
			if item.ClientUpdated < existingUpdated {
				continue
			}
			if item.ClientUpdated == existingUpdated && item.DeviceID <= existingDevice {
				continue
			}
		} else if !errors.Is(err, sql.ErrNoRows) {
			return 0, fmt.Errorf("failed to query latest change: %w", err)
		}

		currentVersion++
		changeID := uuid.New().String()
		insertQuery := `
			INSERT INTO sync_changes (id, user_id, entity, entity_id, operation, payload, client_updated, server_version, device_id, deleted_at, created_at)
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
		`
		_, err = tx.ExecContext(ctx, insertQuery,
			changeID,
			userID,
			item.Entity,
			item.EntityID,
			item.Operation,
			item.Payload,
			item.ClientUpdated,
			currentVersion,
			item.DeviceID,
			item.DeletedAt,
			time.Now(),
		)
		if err != nil {
			return 0, fmt.Errorf("failed to insert sync change: %w", err)
		}
	}

	updateCounterQuery := `
		UPDATE user_version_counters
		SET current_version = $1
		WHERE user_id = $2
	`
	_, err = tx.ExecContext(ctx, updateCounterQuery, currentVersion, userID)
	if err != nil {
		return 0, fmt.Errorf("failed to update user version counter: %w", err)
	}

	if err = tx.Commit(); err != nil {
		return 0, fmt.Errorf("failed to commit sync transaction: %w", err)
	}

	return currentVersion, nil
}

func (p *PostgresRepository) GetChangesAfterVersion(ctx context.Context, userID string, cursor int64, limit int) ([]domain.SyncChange, int64, bool, error) {
	if limit <= 0 {
		limit = 100
	}

	query := `
		SELECT id, user_id, entity, entity_id, operation, payload, client_updated, server_version, device_id, deleted_at
		FROM sync_changes
		WHERE user_id = $1 AND server_version > $2
		ORDER BY server_version ASC
		LIMIT $3
	`
	rows, err := p.db.QueryContext(ctx, query, userID, cursor, limit+1)
	if err != nil {
		return nil, cursor, false, fmt.Errorf("failed to query changes: %w", err)
	}
	defer rows.Close()

	var changes []domain.SyncChange
	for rows.Next() {
		var ch domain.SyncChange
		err := rows.Scan(
			&ch.ID,
			&ch.UserID,
			&ch.Entity,
			&ch.EntityID,
			&ch.Operation,
			&ch.Payload,
			&ch.ClientUpdated,
			&ch.ServerVersion,
			&ch.DeviceID,
			&ch.DeletedAt,
		)
		if err != nil {
			return nil, cursor, false, fmt.Errorf("failed to scan sync change: %w", err)
		}
		changes = append(changes, ch)
	}
	if err = rows.Err(); err != nil {
		return nil, cursor, false, fmt.Errorf("rows error: %w", err)
	}

	hasMore := len(changes) > limit
	if hasMore {
		changes = changes[:limit]
	}

	nextCursor := cursor
	if len(changes) > 0 {
		nextCursor = changes[len(changes)-1].ServerVersion
	}

	return changes, nextCursor, hasMore, nil
}

func (p *PostgresRepository) GetAllChangesForExport(ctx context.Context, userID string) ([]domain.SyncChange, error) {
	query := `
		SELECT id, user_id, entity, entity_id, operation, payload, client_updated, server_version, device_id, deleted_at
		FROM sync_changes
		WHERE user_id = $1
		ORDER BY server_version ASC
	`
	rows, err := p.db.QueryContext(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("failed to query export changes: %w", err)
	}
	defer rows.Close()

	var changes []domain.SyncChange
	for rows.Next() {
		var ch domain.SyncChange
		err := rows.Scan(
			&ch.ID,
			&ch.UserID,
			&ch.Entity,
			&ch.EntityID,
			&ch.Operation,
			&ch.Payload,
			&ch.ClientUpdated,
			&ch.ServerVersion,
			&ch.DeviceID,
			&ch.DeletedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan sync change: %w", err)
		}
		changes = append(changes, ch)
	}
	if err = rows.Err(); err != nil {
		return nil, fmt.Errorf("rows error: %w", err)
	}

	return changes, nil
}
