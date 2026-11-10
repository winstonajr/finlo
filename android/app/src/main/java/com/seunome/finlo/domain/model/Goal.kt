package com.seunome.finlo.domain.model

data class Goal(
    val id: String,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long = 0L,
    val targetDateEpochMs: Long,
    val colorHex: String = "#FF9800",
    val icon: String = "flag",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
