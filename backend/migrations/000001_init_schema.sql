-- Finlo Schema Migration 000001
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    google_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key VARCHAR(255) NOT NULL,
    response BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, key)
);

CREATE TABLE IF NOT EXISTS user_version_counters (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sync_changes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity VARCHAR(64) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    operation VARCHAR(32) NOT NULL,
    payload JSONB,
    client_updated BIGINT NOT NULL,
    server_version BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    deleted_at BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sync_changes_pull ON sync_changes(user_id, server_version ASC);
CREATE INDEX IF NOT EXISTS idx_sync_changes_entity ON sync_changes(user_id, entity, entity_id);
