package com.seunome.finlo.data.repository

import com.google.gson.Gson
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.database.entity.CategoryEntity
import com.seunome.finlo.core.database.entity.SyncChangeEntity
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.core.util.UuidV7
import com.seunome.finlo.domain.model.Category
import com.seunome.finlo.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val database: AppDatabase,
    private val secureStorage: SecureStorage,
    private val gson: Gson = Gson()
) : CategoryRepository {

    override fun getCategoriesFlow(): Flow<List<Category>> {
        return database.categoryDao().getActiveCategoriesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return database.categoryDao().getCategoryById(id)?.toDomain()
    }

    override suspend fun createCategory(category: Category) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val catId = if (category.id.isBlank()) UuidV7.generate(now) else category.id
        val entity = CategoryEntity.fromDomain(category.copy(id = catId, updatedAt = now))

        database.categoryDao().upsertCategory(entity)
        enqueueSync("category", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun updateCategory(category: Category) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val entity = CategoryEntity.fromDomain(category.copy(updatedAt = now))

        database.categoryDao().upsertCategory(entity)
        enqueueSync("category", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun deleteCategory(id: String, fallbackCategoryId: String) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()

        val affectedTxIds = database.categoryDao().getTransactionIdsByCategoryId(id)

        database.categoryDao().reassignAndSoftDelete(
            oldCategoryId = id,
            newCategoryId = fallbackCategoryId,
            timestamp = now
        )

        enqueueSync("category", id, "delete", null, now, deviceId, now)

        for (txId in affectedTxIds) {
            val updatedTx = database.transactionDao().getTransactionById(txId)
            if (updatedTx != null) {
                enqueueSync("transaction", txId, "upsert", updatedTx, now, deviceId, null)
            }
        }
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
                id = UuidV7.generate(),
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
