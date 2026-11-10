package auth

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/seunome/finlo-api/internal/domain"
	"github.com/seunome/finlo-api/internal/repository"
)

type Service struct {
	userRepo     repository.UserRepository
	tokenManager *TokenManager
	argonParams  Argon2Params
}

func NewService(userRepo repository.UserRepository, tokenManager *TokenManager) *Service {
	return &Service{
		userRepo:     userRepo,
		tokenManager: tokenManager,
		argonParams:  DefaultArgon2Params,
	}
}

func (s *Service) Register(ctx context.Context, email, password string) (*domain.AuthResponse, error) {
	email = strings.TrimSpace(strings.ToLower(email))
	if email == "" || !strings.Contains(email, "@") {
		return nil, domain.ErrInvalidPayload
	}
	if len(password) < 8 {
		return nil, errors.New("password must be at least 8 characters")
	}

	existing, _ := s.userRepo.GetUserByEmail(ctx, email)
	if existing != nil {
		return nil, domain.ErrUserAlreadyExists
	}

	hash, err := HashPassword(password, s.argonParams)
	if err != nil {
		return nil, fmt.Errorf("failed to hash password: %w", err)
	}

	now := time.Now()
	user := &domain.User{
		ID:           uuid.New().String(),
		Email:        email,
		PasswordHash: hash,
		CreatedAt:    now,
		UpdatedAt:    now,
	}

	if err := s.userRepo.CreateUser(ctx, user); err != nil {
		return nil, err
	}

	return s.createAuthResponse(ctx, user)
}

func (s *Service) Login(ctx context.Context, email, password string) (*domain.AuthResponse, error) {
	email = strings.TrimSpace(strings.ToLower(email))
	user, err := s.userRepo.GetUserByEmail(ctx, email)
	if err != nil {
		return nil, domain.ErrInvalidCredentials
	}

	valid, err := VerifyPassword(password, user.PasswordHash)
	if err != nil || !valid {
		return nil, domain.ErrInvalidCredentials
	}

	return s.createAuthResponse(ctx, user)
}

func (s *Service) RefreshToken(ctx context.Context, rawRefreshToken string) (*domain.AuthResponse, error) {
	tokenHash := HashToken(rawRefreshToken)
	tokenRecord, err := s.userRepo.GetRefreshToken(ctx, tokenHash)
	if err != nil {
		return nil, domain.ErrInvalidToken
	}

	if tokenRecord.RevokedAt != nil {
		return nil, domain.ErrTokenRevoked
	}

	if time.Now().After(tokenRecord.ExpiresAt) {
		return nil, domain.ErrInvalidToken
	}

	if err := s.userRepo.RevokeRefreshToken(ctx, tokenHash); err != nil {
		return nil, fmt.Errorf("failed to revoke old token: %w", err)
	}

	user, err := s.userRepo.GetUserByID(ctx, tokenRecord.UserID)
	if err != nil {
		return nil, domain.ErrUserNotFound
	}

	return s.createAuthResponse(ctx, user)
}

func (s *Service) GoogleLogin(ctx context.Context, idToken string) (*domain.AuthResponse, error) {
	if strings.TrimSpace(idToken) == "" {
		return nil, domain.ErrInvalidPayload
	}

	googleSub, email, err := parseMockOrRealGoogleToken(idToken)
	if err != nil {
		return nil, fmt.Errorf("invalid google token: %w", err)
	}

	user, err := s.userRepo.GetUserByGoogleID(ctx, googleSub)
	if errors.Is(err, domain.ErrUserNotFound) {
		user, err = s.userRepo.GetUserByEmail(ctx, email)
		if errors.Is(err, domain.ErrUserNotFound) {
			now := time.Now()
			user = &domain.User{
				ID:           uuid.New().String(),
				Email:        email,
				PasswordHash: "",
				GoogleID:     &googleSub,
				CreatedAt:    now,
				UpdatedAt:    now,
			}
			if err := s.userRepo.CreateUser(ctx, user); err != nil {
				return nil, err
			}
		} else if err != nil {
			return nil, err
		}
	} else if err != nil {
		return nil, err
	}

	return s.createAuthResponse(ctx, user)
}

func (s *Service) createAuthResponse(ctx context.Context, user *domain.User) (*domain.AuthResponse, error) {
	accessToken, expiresIn, err := s.tokenManager.GenerateAccessToken(user.ID, user.Email)
	if err != nil {
		return nil, err
	}

	rawRefresh, refreshHash, expiresAt, err := s.tokenManager.GenerateRefreshToken()
	if err != nil {
		return nil, err
	}

	tokenRecord := &domain.RefreshToken{
		TokenHash: refreshHash,
		UserID:    user.ID,
		ExpiresAt: expiresAt,
	}

	if err := s.userRepo.SaveRefreshToken(ctx, tokenRecord); err != nil {
		return nil, err
	}

	return &domain.AuthResponse{
		AccessToken:  accessToken,
		RefreshToken: rawRefresh,
		UserID:       user.ID,
		Email:        user.Email,
		ExpiresIn:    expiresIn,
	}, nil
}

func parseMockOrRealGoogleToken(token string) (sub, email string, err error) {
	parts := strings.Split(token, ".")
	if len(parts) >= 2 {
		payloadBytes, err := base64.RawURLEncoding.DecodeString(parts[1])
		if err == nil {
			var claims struct {
				Sub   string `json:"sub"`
				Email string `json:"email"`
			}
			if err := json.Unmarshal(payloadBytes, &claims); err == nil && claims.Sub != "" {
				if claims.Email == "" {
					claims.Email = claims.Sub + "@gmail.com"
				}
				return claims.Sub, strings.ToLower(claims.Email), nil
			}
		}
	}

	return "google-" + token, strings.ToLower(token) + "@finlo.app", nil
}
