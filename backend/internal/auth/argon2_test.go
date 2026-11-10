package auth

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestArgon2PasswordHashing(t *testing.T) {
	fastParams := Argon2Params{
		Memory:      16 * 1024,
		Iterations:  1,
		Parallelism: 1,
		SaltLength:  16,
		KeyLength:   32,
	}

	password := "SecretPassword123!"

	hash, err := HashPassword(password, fastParams)
	require.NoError(t, err)
	assert.NotEmpty(t, hash)

	valid, err := VerifyPassword(password, hash)
	require.NoError(t, err)
	assert.True(t, valid)

	invalid, err := VerifyPassword("WrongPassword", hash)
	require.NoError(t, err)
	assert.False(t, invalid)

	_, err = VerifyPassword(password, "invalid-hash-string")
	assert.Error(t, err)
}
