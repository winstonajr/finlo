package com.seunome.finlo.data.repository

import com.google.gson.Gson
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.database.entity.AccountEntity
import com.seunome.finlo.core.database.entity.SyncChangeEntity
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.domain.model.Account
import com.seunome.finlo.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AccountRepositoryImpl(
    private val database: AppDatabase,
    private val secureStorage: SecureStorage,
    private val gson: Gson = Gson()
) : AccountRepository {

    override fun getAccountsFlow(): Flow<List<Account>> {
        return database.accountDao().getActiveAccountsFlow().map { entities ->
            entities.map { entity ->
                val balance = database.accountDao().getAccountBalance(entity.id)
                entity.toDomain(balance)
            }
        }
    }

    override fun getAccountBalanceFlow(accountId: String): Flow<Long> {
        return database.accountDao().getAccountBalanceFlow(accountId)
    }

    override suspend fun getAccountById(id: String): Account? {
        val entity = database.accountDao().getAccountById(id) ?: return null
        val balance = database.accountDao().getAccountBalance(id)
        return entity.toDomain(balance)
    }

    override suspend fun createAccount(account: Account) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val entity = AccountEntity.fromDomain(account.copy(updatedAt = now))

        database.accountDao().upsertAccount(entity)
        enqueueSync("account", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun updateAccount(account: Account) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val entity = AccountEntity.fromDomain(account.copy(updatedAt = now))

        database.accountDao().updateAccount(entity)
        enqueueSync("account", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun archiveAccount(id: String, archived: Boolean) {
        val existing = database.accountDao().getAccountById(id) ?: return
        val updated = existing.copy(isArchived = archived, updatedAt = System.currentTimeMillis())
        val deviceId = secureStorage.getDeviceId()

        database.accountDao().updateAccount(updated)
        enqueueSync("account", id, "upsert", updated, updated.updatedAt, deviceId, null)
    }

    override suspend fun deleteAccount(id: String) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()

        database.accountDao().softDeleteAccount(id, deletedAt = now, updatedAt = now)
        enqueueSync("account", id, "delete", null, now, deviceId, now)
    }

    private suspend fun enqueueSync(
        entityType: String,
        entityId: String,
        op: String,
        payload: Any?,
        updatedAt: Long,
        deviceId: String,
        deletedAt: Long?
    ) {
        val payloadJson = if (payload != null) gson.toJson(payload) else "{}"
        database.syncChangeDao().enqueue(
            SyncChangeEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operation = op,
                payloadJson = payloadJson,
                clientUpdated = updatedAt,
                deviceId = deviceId,
                deletedAt = deletedAt
            )
        )
    }
}
