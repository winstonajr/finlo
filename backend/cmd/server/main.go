package main

import (
	"context"
	"database/sql"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/seunome/finlo-api/internal/auth"
	"github.com/seunome/finlo-api/internal/config"
	"github.com/seunome/finlo-api/internal/handler"
	"github.com/seunome/finlo-api/internal/repository"
	"github.com/seunome/finlo-api/internal/storage"
	"github.com/seunome/finlo-api/internal/sync"
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))
	slog.SetDefault(logger)

	cfg := config.Load()
	logger.Info("starting finlo server", "port", cfg.Port, "mode", cfg.StorageMode)

	var userRepo repository.UserRepository
	var syncRepo repository.SyncRepository

	if cfg.StorageMode == "memory" {
		memRepo := repository.NewMemoryRepository()
		userRepo = memRepo
		syncRepo = memRepo
		logger.Info("initialized in-memory repository")
	} else {
		db, err := sql.Open("postgres", cfg.DatabaseURL)
		if err != nil {
			logger.Error("failed to open database, falling back to memory repository", "error", err)
			memRepo := repository.NewMemoryRepository()
			userRepo = memRepo
			syncRepo = memRepo
		} else {
			pingCtx, pingCancel := context.WithTimeout(context.Background(), 2*time.Second)
			pingErr := db.PingContext(pingCtx)
			pingCancel()

			if pingErr != nil {
				logger.Error("database ping failed, falling back to memory repository", "error", pingErr)
				_ = db.Close()
				memRepo := repository.NewMemoryRepository()
				userRepo = memRepo
				syncRepo = memRepo
			} else {
				pgRepo := repository.NewPostgresRepository(db)
				userRepo = pgRepo
				syncRepo = pgRepo
				logger.Info("connected to database successfully")
				defer db.Close()
			}
		}
	}

	tokenManager := auth.NewTokenManager(cfg.JWTSecret, cfg.AccessExpiry, cfg.RefreshExpiry)
	authService := auth.NewService(userRepo, tokenManager)
	syncService := sync.NewService(syncRepo)
	attachmentService := storage.NewAttachmentService(storage.S3Config{
		Endpoint:        cfg.S3Endpoint,
		Bucket:          cfg.S3Bucket,
		AccessKeyID:     cfg.S3AccessKey,
		SecretAccessKey: cfg.S3SecretKey,
		Region:          cfg.S3Region,
	})

	router := handler.SetupRouter(handler.RouterDependencies{
		AuthService:       authService,
		TokenManager:      tokenManager,
		SyncService:       syncService,
		UserRepo:          userRepo,
		AttachmentService: attachmentService,
		Logger:            logger,
	})

	server := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	shutdownChan := make(chan os.Signal, 1)
	signal.Notify(shutdownChan, os.Interrupt, syscall.SIGTERM)

	go func() {
		logger.Info("http server listening", "addr", server.Addr)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.Error("server failed to listen and serve", "error", err)
			os.Exit(1)
		}
	}()

	<-shutdownChan
	logger.Info("shutting down gracefully...")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		logger.Error("server forced to shutdown", "error", err)
	}

	logger.Info("server exited cleanly")
}
