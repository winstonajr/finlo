package sync

import (
	"context"
	"encoding/json"
	"testing"
	"time"

	"github.com/seunome/finlo-api/internal/domain"
	"github.com/seunome/finlo-api/internal/repository"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSyncPushAndPullIdempotency(t *testing.T) {
	ctx := context.Background()
	repo := repository.NewMemoryRepository()
	service := NewService(repo)

	userID := "user-abc"
	idempotencyKey := "idem-key-1"

	req := domain.SyncPushRequest{
		Changes: []domain.SyncPushItem{
			{
				Entity:    domain.EntityAccount,
				ID:        "acc-1",
				Operation: domain.OpUpsert,
				Payload:   json.RawMessage(`{"name":"Checking","balance":50000}`),
				UpdatedAt: time.Now().UnixMilli(),
				DeviceID:  "device-1",
			},
		},
	}

	resp1, err := service.Push(ctx, userID, idempotencyKey, req)
	require.NoError(t, err)
	assert.Equal(t, 1, resp1.AppliedCount)
	assert.Equal(t, int64(1), resp1.ServerVersion)

	// Repeated push with same idempotency key must return cached result
	resp2, err := service.Push(ctx, userID, idempotencyKey, req)
	require.NoError(t, err)
	assert.Equal(t, resp1.ServerVersion, resp2.ServerVersion)
	assert.Equal(t, resp1.AppliedCount, resp2.AppliedCount)

	// Pull changes
	pullResp, err := service.Pull(ctx, userID, 0, 10)
	require.NoError(t, err)
	assert.Len(t, pullResp.Changes, 1)
	assert.Equal(t, "acc-1", pullResp.Changes[0].EntityID)
	assert.Equal(t, int64(1), pullResp.NextCursor)
	assert.False(t, pullResp.HasMore)
}

func TestSyncLWWConflictResolution(t *testing.T) {
	ctx := context.Background()
	repo := repository.NewMemoryRepository()
	service := NewService(repo)

	userID := "user-conflict"

	// 1. Initial change at t=1000
	req1 := domain.SyncPushRequest{
		Changes: []domain.SyncPushItem{
			{
				Entity:    domain.EntityTransaction,
				ID:        "tx-1",
				Operation: domain.OpUpsert,
				Payload:   json.RawMessage(`{"amount":1000}`),
				UpdatedAt: 1000,
				DeviceID:  "device-A",
			},
		},
	}
	_, err := service.Push(ctx, userID, "key-1", req1)
	require.NoError(t, err)

	// 2. Incoming change with older timestamp t=500 -> must lose (ignored)
	req2 := domain.SyncPushRequest{
		Changes: []domain.SyncPushItem{
			{
				Entity:    domain.EntityTransaction,
				ID:        "tx-1",
				Operation: domain.OpUpsert,
				Payload:   json.RawMessage(`{"amount":500}`),
				UpdatedAt: 500,
				DeviceID:  "device-B",
			},
		},
	}
	resp2, err := service.Push(ctx, userID, "key-2", req2)
	require.NoError(t, err)
	assert.Equal(t, int64(1), resp2.ServerVersion)

	// 3. Incoming change with equal timestamp t=1000, higher deviceId "device-Z" -> wins
	req3 := domain.SyncPushRequest{
		Changes: []domain.SyncPushItem{
			{
				Entity:    domain.EntityTransaction,
				ID:        "tx-1",
				Operation: domain.OpUpsert,
				Payload:   json.RawMessage(`{"amount":3000}`),
				UpdatedAt: 1000,
				DeviceID:  "device-Z",
			},
		},
	}
	resp3, err := service.Push(ctx, userID, "key-3", req3)
	require.NoError(t, err)
	assert.Equal(t, int64(2), resp3.ServerVersion)

	// 4. Incoming change with newer timestamp t=2000 -> wins
	req4 := domain.SyncPushRequest{
		Changes: []domain.SyncPushItem{
			{
				Entity:    domain.EntityTransaction,
				ID:        "tx-1",
				Operation: domain.OpUpsert,
				Payload:   json.RawMessage(`{"amount":4000}`),
				UpdatedAt: 2000,
				DeviceID:  "device-A",
			},
		},
	}
	resp4, err := service.Push(ctx, userID, "key-4", req4)
	require.NoError(t, err)
	assert.Equal(t, int64(3), resp4.ServerVersion)

	pullResp, err := service.Pull(ctx, userID, 0, 100)
	require.NoError(t, err)
	assert.Len(t, pullResp.Changes, 3)
}

func TestSyncFutureTimestampRejection(t *testing.T) {
	ctx := context.Background()
	repo := repository.NewMemoryRepository()
	service := NewService(repo)

	userID := "user-future"
	farFuture := time.Now().Add(1 * time.Hour).UnixMilli()

	req := domain.SyncPushRequest{
		Changes: []domain.SyncPushItem{
			{
				Entity:    domain.EntityCategory,
				ID:        "cat-1",
				Operation: domain.OpUpsert,
				Payload:   json.RawMessage(`{"name":"Food"}`),
				UpdatedAt: farFuture,
				DeviceID:  "device-1",
			},
		},
	}

	_, err := service.Push(ctx, userID, "key-future", req)
	assert.ErrorIs(t, err, domain.ErrFutureTimestamp)
}
