package auth

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"
)

type JWTClaims struct {
	UserID    string `json:"userId"`
	Email     string `json:"email"`
	Subject   string `json:"sub"`
	IssuedAt  int64  `json:"iat"`
	ExpiresAt int64  `json:"exp"`
}

type TokenManager struct {
	secretKey     []byte
	accessExpiry  time.Duration
	refreshExpiry time.Duration
}

func NewTokenManager(secret string, accessExpiry, refreshExpiry time.Duration) *TokenManager {
	return &TokenManager{
		secretKey:     []byte(secret),
		accessExpiry:  accessExpiry,
		refreshExpiry: refreshExpiry,
	}
}

func (tm *TokenManager) GenerateAccessToken(userID, email string) (string, int64, error) {
	now := time.Now().Unix()
	expiresAt := now + int64(tm.accessExpiry.Seconds())

	headerJSON := `{"alg":"HS256","typ":"JWT"}`
	encodedHeader := base64.RawURLEncoding.EncodeToString([]byte(headerJSON))

	claims := JWTClaims{
		UserID:    userID,
		Email:     email,
		Subject:   userID,
		IssuedAt:  now,
		ExpiresAt: expiresAt,
	}

	claimsBytes, err := json.Marshal(claims)
	if err != nil {
		return "", 0, fmt.Errorf("failed to marshal token claims: %w", err)
	}
	encodedPayload := base64.RawURLEncoding.EncodeToString(claimsBytes)

	signingInput := encodedHeader + "." + encodedPayload
	signature := tm.sign(signingInput)
	encodedSignature := base64.RawURLEncoding.EncodeToString(signature)

	token := signingInput + "." + encodedSignature
	return token, int64(tm.accessExpiry.Seconds()), nil
}

func (tm *TokenManager) ValidateAccessToken(tokenStr string) (*JWTClaims, error) {
	parts := strings.Split(tokenStr, ".")
	if len(parts) != 3 {
		return nil, errors.New("invalid jwt token format")
	}

	signingInput := parts[0] + "." + parts[1]
	expectedSig := tm.sign(signingInput)
	actualSig, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil {
		return nil, errors.New("invalid token signature encoding")
	}

	if subtle.ConstantTimeCompare(expectedSig, actualSig) != 1 {
		return nil, errors.New("invalid token signature")
	}

	payloadBytes, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return nil, errors.New("invalid token payload encoding")
	}

	var claims JWTClaims
	if err := json.Unmarshal(payloadBytes, &claims); err != nil {
		return nil, errors.New("invalid token claims")
	}

	if time.Now().Unix() > claims.ExpiresAt {
		return nil, errors.New("token has expired")
	}

	return &claims, nil
}

func (tm *TokenManager) GenerateRefreshToken() (rawToken string, tokenHash string, expiresAt time.Time, err error) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", "", time.Time{}, fmt.Errorf("failed to generate random token: %w", err)
	}

	rawToken = hex.EncodeToString(b)
	tokenHash = HashToken(rawToken)
	expiresAt = time.Now().Add(tm.refreshExpiry)
	return rawToken, tokenHash, expiresAt, nil
}

func (tm *TokenManager) sign(input string) []byte {
	h := hmac.New(sha256.New, tm.secretKey)
	h.Write([]byte(input))
	return h.Sum(nil)
}

func HashToken(token string) string {
	h := sha256.Sum256([]byte(token))
	return hex.EncodeToString(h[:])
}
