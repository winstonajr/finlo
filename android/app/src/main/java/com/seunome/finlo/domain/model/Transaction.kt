package com.seunome.finlo.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

data class Transaction(
    val id: String,
    val accountId: String,
    val targetAccountId: String? = null,
    val categoryId: String,
    val type: TransactionType,
    val amountCents: Long,
    val dateEpochMs: Long,
    val description: String = "",
    val receiptAttachment: String? = null,
    val recurringRuleId: String? = null,
    val installmentNumber: Int? = null,
    val totalInstallments: Int? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
