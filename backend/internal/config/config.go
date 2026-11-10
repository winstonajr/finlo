package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	Port            string
	DatabaseURL     string
	JWTSecret       string
	AccessExpiry    time.Duration
	RefreshExpiry   time.Duration
	S3Endpoint      string
	S3Bucket        string
	S3AccessKey     string
	S3SecretKey     string
	S3Region        string
	StorageMode     string
}

func Load() *Config {
	port := getEnv("PORT", "8080")
	dbURL := getEnv("DATABASE_URL", "postgres://finlo:finlo_secret@localhost:5432/finlo?sslmode=disable")
	jwtSecret := getEnv("JWT_SECRET", "super-secret-finlo-jwt-key-minimum-32-chars-long")

	accessMin, _ := strconv.Atoi(getEnv("ACCESS_EXPIRY_MINUTES", "15"))
	if accessMin <= 0 {
		accessMin = 15
	}

	refreshDays, _ := strconv.Atoi(getEnv("REFRESH_EXPIRY_DAYS", "30"))
	if refreshDays <= 0 {
		refreshDays = 30
	}

	return &Config{
		Port:          port,
		DatabaseURL:   dbURL,
		JWTSecret:     jwtSecret,
		AccessExpiry:  time.Duration(accessMin) * time.Minute,
		RefreshExpiry: time.Duration(refreshDays) * 24 * time.Hour,
		S3Endpoint:    getEnv("S3_ENDPOINT", "http://localhost:9000"),
		S3Bucket:      getEnv("S3_BUCKET", "finlo-attachments"),
		S3AccessKey:   getEnv("S3_ACCESS_KEY", "minioadmin"),
		S3SecretKey:   getEnv("S3_SECRET_KEY", "minioadmin"),
		S3Region:      getEnv("S3_REGION", "us-east-1"),
		StorageMode:   getEnv("STORAGE_MODE", "postgres"),
	}
}

func getEnv(key, defaultVal string) string {
	if val := os.Getenv(key); val != "" {
		return val
	}
	return defaultVal
}
