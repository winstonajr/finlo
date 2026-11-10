package com.seunome.finlo.domain.model

enum class AccountType {
    WALLET,
    CHECKING,
    SAVINGS,
    CREDIT_CARD
}

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: String = "BRL",
    val colorHex: String = "#2196F3",
    val isArchived: Boolean = false,
    val balanceCents: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
