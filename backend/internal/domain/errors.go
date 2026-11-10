package domain

import "errors"

var (
	ErrUserNotFound         = errors.New("user not found")
	ErrUserAlreadyExists     = errors.New("user already exists")
	ErrInvalidCredentials    = errors.New("invalid email or password")
	ErrInvalidToken          = errors.New("invalid or expired token")
	ErrTokenRevoked          = errors.New("token has been revoked")
	ErrUnauthorized          = errors.New("unauthorized")
	ErrIdempotencyKeyMissing = errors.New("idempotency-key header is required")
	ErrInvalidPayload        = errors.New("invalid request payload")
	ErrFutureTimestamp       = errors.New("timestamp is too far in the future")
)
