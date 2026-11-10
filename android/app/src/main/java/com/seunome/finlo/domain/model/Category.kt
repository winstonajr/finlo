package com.seunome.finlo.domain.model

data class Category(
    val id: String,
    val name: String,
    val icon: String = "category",
    val colorHex: String = "#4CAF50",
    val budgetCents: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
