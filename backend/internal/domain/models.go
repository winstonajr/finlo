package domain

import (
	"encoding/json"
	"time"
)

type User struct {
	ID           string    `json:"id"`
	Email        string    `json:"email"`
	PasswordHash string    `json:"-"`
	GoogleID     *string   `json:"googleId,omitempty"`
	CreatedAt    time.Time `json:"createdAt"`
	UpdatedAt    time.Time `json:"updatedAt"`
}

type RefreshToken struct {
	TokenHash string    `json:"-"`
	UserID    string    `json:"userId"`
	ExpiresAt time.Time `json:"expiresAt"`
	RevokedAt *time.Time `json:"revokedAt,omitempty"`
}

type EntityType string

const (
	EntityAccount       EntityType = "account"
	EntityCategory      EntityType = "category"
	EntityTransaction   EntityType = "transaction"
	EntityGoal          EntityType = "goal"
	EntityRecurringRule EntityType = "recurring_rule"
)

type SyncOperation string

const (
	OpUpsert SyncOperation = "upsert"
	OpDelete SyncOperation = "delete"
)

type SyncChange struct {
	ID            string          `json:"id"`
	UserID        string          `json:"userId"`
	Entity        EntityType      `json:"entity"`
	EntityID      string          `json:"entityId"`
	Operation     SyncOperation   `json:"op"`
	Payload       json.RawMessage `json:"payload,omitempty"`
	ClientUpdated int64           `json:"updatedAt"`
	ServerVersion int64           `json:"serverVersion"`
	DeviceID      string          `json:"deviceId"`
	DeletedAt     *int64          `json:"deletedAt,omitempty"`
}

type SyncPushItem struct {
	Entity    EntityType      `json:"entity"`
	ID        string          `json:"id"`
	Operation SyncOperation   `json:"op"`
	Payload   json.RawMessage `json:"payload,omitempty"`
	UpdatedAt int64           `json:"updatedAt"`
	DeviceID  string          `json:"deviceId"`
	DeletedAt *int64          `json:"deletedAt,omitempty"`
}

type SyncPushRequest struct {
	Changes []SyncPushItem `json:"changes"`
}

type SyncPushResponse struct {
	AppliedCount  int   `json:"appliedCount"`
	ServerVersion int64 `json:"serverVersion"`
}

type SyncPullResponse struct {
	Changes    []SyncChange `json:"changes"`
	NextCursor int64        `json:"nextCursor"`
	HasMore    bool         `json:"hasMore"`
}

type RegisterRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type LoginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type GoogleLoginRequest struct {
	IDToken string `json:"idToken"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refreshToken"`
}

type AuthResponse struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
	UserID       string `json:"userId"`
	Email        string `json:"email"`
	ExpiresIn    int64  `json:"expiresIn"`
}

type PresignUploadRequest struct {
	FileName    string `json:"fileName"`
	ContentType string `json:"contentType"`
	Size        int64  `json:"size"`
}

type PresignUploadResponse struct {
	UploadURL string `json:"uploadUrl"`
	FileKey   string `json:"fileKey"`
	ExpiresIn int64  `json:"expiresIn"`
}

type PresignDownloadResponse struct {
	DownloadURL string `json:"downloadUrl"`
	ExpiresIn   int64  `json:"expiresIn"`
}

type ExportData struct {
	User      User         `json:"user"`
	Changes   []SyncChange `json:"changes"`
	ExportDate time.Time   `json:"exportDate"`
}
