package com.seunome.finlo.domain.model

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class RecurringRule(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val type: TransactionType,
    val amountCents: Long,
    val frequency: Frequency,
    val dayOfMonth: Int,
    val nextDueDateEpochMs: Long,
    val active: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
