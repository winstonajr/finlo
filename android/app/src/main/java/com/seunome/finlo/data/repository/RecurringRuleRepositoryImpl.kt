package com.seunome.finlo.data.repository

import com.google.gson.Gson
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.database.entity.RecurringRuleEntity
import com.seunome.finlo.core.database.entity.SyncChangeEntity
import com.seunome.finlo.core.database.entity.TransactionEntity
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.core.util.UuidV7
import com.seunome.finlo.domain.model.Frequency
import com.seunome.finlo.domain.model.RecurringRule
import com.seunome.finlo.domain.repository.RecurringRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class RecurringRuleRepositoryImpl(
    private val database: AppDatabase,
    private val secureStorage: SecureStorage,
    private val gson: Gson = Gson()
) : RecurringRuleRepository {

    override fun getActiveRulesFlow(): Flow<List<RecurringRule>> {
        return database.recurringRuleDao().getActiveRulesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRuleById(id: String): RecurringRule? {
        return database.recurringRuleDao().getRuleById(id)?.toDomain()
    }

    override suspend fun createRule(rule: RecurringRule): String {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val ruleId = if (rule.id.isBlank()) UuidV7.generate(now) else rule.id
        val entity = RecurringRuleEntity.fromDomain(rule.copy(id = ruleId, updatedAt = now))

        database.recurringRuleDao().upsertRule(entity)
        enqueueSync("recurring_rule", entity.id, "upsert", entity, now, deviceId, null)
        return ruleId
    }

    override suspend fun updateRule(rule: RecurringRule) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val entity = RecurringRuleEntity.fromDomain(rule.copy(updatedAt = now))

        database.recurringRuleDao().upsertRule(entity)
        enqueueSync("recurring_rule", entity.id, "upsert", entity, now, deviceId, null)
    }

    override suspend fun deleteRule(id: String) {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()

        database.recurringRuleDao().softDeleteRule(id, deletedAt = now, updatedAt = now)
        enqueueSync("recurring_rule", id, "delete", null, now, deviceId, now)
    }

    override suspend fun processDueRecurringRules(): Int {
        val now = System.currentTimeMillis()
        val deviceId = secureStorage.getDeviceId()
        val dueRules = database.recurringRuleDao().getDueRules(now)
        var generatedCount = 0

        for (ruleEntity in dueRules) {
            val rule = ruleEntity.toDomain()
            var nextDue = rule.nextDueDateEpochMs

            while (nextDue <= now) {
                val txId = UuidV7.generate(nextDue)
                val description = "Recorrente: " + when (rule.frequency) {
                    Frequency.DAILY -> "Diária"
                    Frequency.WEEKLY -> "Semanal"
                    Frequency.MONTHLY -> "Mensal"
                    Frequency.YEARLY -> "Anual"
                }

                val txEntity = TransactionEntity(
                    id = txId,
                    accountId = rule.accountId,
                    targetAccountId = null,
                    categoryId = rule.categoryId,
                    type = rule.type.name,
                    amountCents = rule.amountCents,
                    dateEpochMs = nextDue,
                    description = description,
                    receiptAttachment = null,
                    recurringRuleId = rule.id,
                    installmentNumber = null,
                    totalInstallments = null,
                    updatedAt = now,
                    deletedAt = null,
                    syncStatus = "PENDING"
                )

                database.transactionDao().upsertTransaction(txEntity)
                enqueueSync("transaction", txId, "upsert", txEntity, now, deviceId, null)
                generatedCount++

                nextDue = calculateNextDueDate(nextDue, rule.frequency, rule.dayOfMonth)
            }

            val updatedRule = rule.copy(nextDueDateEpochMs = nextDue, updatedAt = now)
            val updatedRuleEntity = RecurringRuleEntity.fromDomain(updatedRule)
            database.recurringRuleDao().upsertRule(updatedRuleEntity)
            enqueueSync("recurring_rule", rule.id, "upsert", updatedRuleEntity, now, deviceId, null)
        }

        return generatedCount
    }

    companion object {
        fun calculateNextDueDate(currentDueDateMs: Long, frequency: Frequency, dayOfMonth: Int): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = currentDueDateMs

            when (frequency) {
                Frequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                Frequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                Frequency.MONTHLY -> {
                    cal.add(Calendar.MONTH, 1)
                    if (dayOfMonth in 1..31) {
                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(maxDay))
                    }
                }
                Frequency.YEARLY -> cal.add(Calendar.YEAR, 1)
            }
            return cal.timeInMillis
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
