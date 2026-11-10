package com.seunome.finlo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seunome.finlo.domain.model.Goal
import com.seunome.finlo.domain.model.SyncStatus

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long,
    val targetDateEpochMs: Long,
    val colorHex: String,
    val icon: String,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: String
) {
    fun toDomain() = Goal(
        id = id,
        name = name,
        targetAmountCents = targetAmountCents,
        currentAmountCents = currentAmountCents,
        targetDateEpochMs = targetDateEpochMs,
        colorHex = colorHex,
        icon = icon,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )

    companion object {
        fun fromDomain(goal: Goal) = GoalEntity(
            id = goal.id,
            name = goal.name,
            targetAmountCents = goal.targetAmountCents,
            currentAmountCents = goal.currentAmountCents,
            targetDateEpochMs = goal.targetDateEpochMs,
            colorHex = goal.colorHex,
            icon = goal.icon,
            updatedAt = goal.updatedAt,
            deletedAt = goal.deletedAt,
            syncStatus = goal.syncStatus.name
        )
    }
}
