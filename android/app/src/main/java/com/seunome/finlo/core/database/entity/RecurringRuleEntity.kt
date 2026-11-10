package com.seunome.finlo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seunome.finlo.domain.model.Frequency
import com.seunome.finlo.domain.model.RecurringRule
import com.seunome.finlo.domain.model.SyncStatus
import com.seunome.finlo.domain.model.TransactionType

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val categoryId: String,
    val type: String,
    val amountCents: Long,
    val frequency: String,
    val dayOfMonth: Int,
    val nextDueDateEpochMs: Long,
    val active: Boolean,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: String
) {
    fun toDomain() = RecurringRule(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        type = TransactionType.valueOf(type),
        amountCents = amountCents,
        frequency = Frequency.valueOf(frequency),
        dayOfMonth = dayOfMonth,
        nextDueDateEpochMs = nextDueDateEpochMs,
        active = active,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )

    companion object {
        fun fromDomain(rule: RecurringRule) = RecurringRuleEntity(
            id = rule.id,
            accountId = rule.accountId,
            categoryId = rule.categoryId,
            type = rule.type.name,
            amountCents = rule.amountCents,
            frequency = rule.frequency.name,
            dayOfMonth = rule.dayOfMonth,
            nextDueDateEpochMs = rule.nextDueDateEpochMs,
            active = rule.active,
            updatedAt = rule.updatedAt,
            deletedAt = rule.deletedAt,
            syncStatus = rule.syncStatus.name
        )
    }
}
