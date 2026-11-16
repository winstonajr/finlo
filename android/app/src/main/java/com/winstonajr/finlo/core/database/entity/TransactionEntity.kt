package com.seunome.finlo.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seunome.finlo.domain.model.SyncStatus
import com.seunome.finlo.domain.model.Transaction
import com.seunome.finlo.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("dateEpochMs")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val targetAccountId: String?,
    val categoryId: String,
    val type: String,
    val amountCents: Long,
    val dateEpochMs: Long,
    val description: String,
    val receiptAttachment: String?,
    val recurringRuleId: String?,
    val installmentNumber: Int?,
    val totalInstallments: Int?,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: String
) {
    fun toDomain() = Transaction(
        id = id,
        accountId = accountId,
        targetAccountId = targetAccountId,
        categoryId = categoryId,
        type = TransactionType.valueOf(type),
        amountCents = amountCents,
        dateEpochMs = dateEpochMs,
        description = description,
        receiptAttachment = receiptAttachment,
        recurringRuleId = recurringRuleId,
        installmentNumber = installmentNumber,
        totalInstallments = totalInstallments,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )

    companion object {
        fun fromDomain(tx: Transaction) = TransactionEntity(
            id = tx.id,
            accountId = tx.accountId,
            targetAccountId = tx.targetAccountId,
            categoryId = tx.categoryId,
            type = tx.type.name,
            amountCents = tx.amountCents,
            dateEpochMs = tx.dateEpochMs,
            description = tx.description,
            receiptAttachment = tx.receiptAttachment,
            recurringRuleId = tx.recurringRuleId,
            installmentNumber = tx.installmentNumber,
            totalInstallments = tx.totalInstallments,
            updatedAt = tx.updatedAt,
            deletedAt = tx.deletedAt,
            syncStatus = tx.syncStatus.name
        )
    }
}
