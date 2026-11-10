package com.seunome.finlo.data.repository

import com.google.gson.Gson
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.database.entity.GoalEntity
import com.seunome.finlo.core.database.entity.SyncChangeEntity
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.domain.model.Goal
import com.seunome.finlo.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GoalRepositoryImpl(
    private val database: AppDatabase,
    private val secureStorage: SecureStorage,
    private val gson: Gson = Gson()
) : GoalRepository {

    override fun getGoalsFlow(): Flow<List<Goal>> {
        return database.goalDao().getActiveGoalsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getGoalById(id: String): Goal? {
        return database.goalDao().getGoalById(id)?.toDomain()
    }

    override suspend fun createGoal(goal: Goal) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val entity = GoalEntity.fromDomain(goal.copy(updatedAt = now))

        database.goalDao().upsertGoal(entity)
        enqueueSync("goal", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun updateGoal(goal: Goal) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val entity = GoalEntity.fromDomain(goal.copy(updatedAt = now))

        database.goalDao().upsertGoal(entity)
        enqueueSync("goal", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun updateGoalProgress(id: String, newAmountCents: Long) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()

        database.goalDao().updateGoalProgress(id, newAmountCents, now)
        val updated = database.goalDao().getGoalById(id)
        if (updated != null) {
            enqueueSync("goal", id, "upsert", updated, now, deviceId, null)
        }
    }

    override suspend fun deleteGoal(id: String) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()

        database.goalDao().softDeleteGoal(id, deletedAt = now, updatedAt = now)
        enqueueSync("goal", id, "delete", null, now, deviceId, now)
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
