package com.seunome.finlo.core.sync

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.database.entity.AccountEntity
import com.seunome.finlo.core.database.entity.CategoryEntity
import com.seunome.finlo.core.database.entity.GoalEntity
import com.seunome.finlo.core.database.entity.RecurringRuleEntity
import com.seunome.finlo.core.database.entity.TransactionEntity
import com.seunome.finlo.core.network.ApiService
import com.seunome.finlo.core.network.dto.SyncChangeItemDto
import com.seunome.finlo.core.network.dto.SyncPushItemDto
import com.seunome.finlo.core.network.dto.SyncPushRequestDto
import com.seunome.finlo.core.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

class SyncManager(
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val secureStorage: SecureStorage,
    private val gson: Gson = Gson()
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun sync(): Boolean {
        val token = secureStorage.getAccessToken()
        if (token == null) {
            _syncState.value = SyncState.Idle
            return true
        }

        _syncState.value = SyncState.Syncing

        try {
            val deviceId = secureStorage.getDeviceId()

            // 1. Push Phase
            pushPendingChanges(deviceId)

            // 2. Pull Phase
            pullRemoteChanges(deviceId)

            _syncState.value = SyncState.Success
            return true
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            return false
        }
    }

    private suspend fun pushPendingChanges(deviceId: String) {
        val pendingChanges = database.syncChangeDao().getPendingChanges(100)
        if (pendingChanges.isEmpty()) return

        val items = pendingChanges.map { entity ->
            val payloadElement = try {
                gson.fromJson(entity.payloadJson, JsonElement::class.java)
            } catch (e: Exception) {
                null
            }

            SyncPushItemDto(
                entity = entity.entityType,
                id = entity.entityId,
                op = entity.operation,
                payload = payloadElement,
                updatedAt = entity.clientUpdated,
                deviceId = deviceId,
                deletedAt = entity.deletedAt
            )
        }

        val idempotencyKey = UUID.randomUUID().toString()
        val response = apiService.pushChanges(idempotencyKey, SyncPushRequestDto(items))

        if (response.isSuccessful) {
            database.syncChangeDao().removeChanges(pendingChanges.map { it.id })
        } else {
            throw Exception("Push failed: HTTP ${response.code()}")
        }
    }

    private suspend fun pullRemoteChanges(localDeviceId: String) {
        var cursor = secureStorage.getServerCursor()
        var hasMore = true

        while (hasMore) {
            val response = apiService.pullChanges(cursor = cursor, limit = 100)
            if (!response.isSuccessful) {
                throw Exception("Pull failed: HTTP ${response.code()}")
            }

            val body = response.body() ?: break
            for (change in body.changes) {
                applyRemoteChange(change, localDeviceId)
            }

            cursor = body.nextCursor
            secureStorage.setServerCursor(cursor)
            hasMore = body.hasMore
        }
    }

    private suspend fun applyRemoteChange(change: SyncChangeItemDto, localDeviceId: String) {
        when (change.entity) {
            "account" -> {
                val existing = database.accountDao().getAccountById(change.entityId)
                val shouldApply = existing == null || ConflictResolver.shouldApplyRemote(
                    localUpdatedAt = existing.updatedAt,
                    localDeviceId = localDeviceId,
                    remoteUpdatedAt = change.updatedAt,
                    remoteDeviceId = change.deviceId
                )
                if (shouldApply && change.payload != null) {
                    val acc = gson.fromJson(change.payload, AccountEntity::class.java)
                    database.accountDao().upsertAccount(acc)
                }
            }
            "category" -> {
                val existing = database.categoryDao().getCategoryById(change.entityId)
                val shouldApply = existing == null || ConflictResolver.shouldApplyRemote(
                    localUpdatedAt = existing.updatedAt,
                    localDeviceId = localDeviceId,
                    remoteUpdatedAt = change.updatedAt,
                    remoteDeviceId = change.deviceId
                )
                if (shouldApply && change.payload != null) {
                    val cat = gson.fromJson(change.payload, CategoryEntity::class.java)
                    database.categoryDao().upsertCategory(cat)
                }
            }
            "transaction" -> {
                val existing = database.transactionDao().getTransactionById(change.entityId)
                val shouldApply = existing == null || ConflictResolver.shouldApplyRemote(
                    localUpdatedAt = existing.updatedAt,
                    localDeviceId = localDeviceId,
                    remoteUpdatedAt = change.updatedAt,
                    remoteDeviceId = change.deviceId
                )
                if (shouldApply && change.payload != null) {
                    val tx = gson.fromJson(change.payload, TransactionEntity::class.java)
                    database.transactionDao().upsertTransaction(tx)
                }
            }
            "goal" -> {
                val existing = database.goalDao().getGoalById(change.entityId)
                val shouldApply = existing == null || ConflictResolver.shouldApplyRemote(
                    localUpdatedAt = existing.updatedAt,
                    localDeviceId = localDeviceId,
                    remoteUpdatedAt = change.updatedAt,
                    remoteDeviceId = change.deviceId
                )
                if (shouldApply && change.payload != null) {
                    val goal = gson.fromJson(change.payload, GoalEntity::class.java)
                    database.goalDao().upsertGoal(goal)
                }
            }
        }
    }
}
