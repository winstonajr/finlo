package auth

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestTokenManager(t *testing.T) {
	secret := "test-secret-key-at-least-32-bytes-long"
	tm := NewTokenManager(secret, 2*time.Second, 1*time.Hour)

	userID := "user-12345"
	email := "test@example.com"

	tokenStr, expiresIn, err := tm.GenerateAccessToken(userID, email)
	require.NoError(t, err)
	assert.NotEmpty(t, tokenStr)
	assert.Equal(t, int64(2), expiresIn)

	claims, err := tm.ValidateAccessToken(tokenStr)
	require.NoError(t, err)
	assert.Equal(t, userID, claims.UserID)
	assert.Equal(t, email, claims.Email)

	// Tampered token
	tampered := tokenStr + "extra"
	_, err = tm.ValidateAccessToken(tampered)
	assert.Error(t, err)

	// Expired token test
	shortTM := NewTokenManager(secret, -1*time.Second, 1*time.Hour)
	expiredToken, _, err := shortTM.GenerateAccessToken(userID, email)
	require.NoError(t, err)

	_, err = tm.ValidateAccessToken(expiredToken)
	assert.Error(t, err)

	// Refresh token generation
	rawRefresh, hash, expiresAt, err := tm.GenerateRefreshToken()
	require.NoError(t, err)
	assert.NotEmpty(t, rawRefresh)
	assert.NotEmpty(t, hash)
	assert.True(t, expiresAt.After(time.Now()))
	assert.Equal(t, HashToken(rawRefresh), hash)
}
