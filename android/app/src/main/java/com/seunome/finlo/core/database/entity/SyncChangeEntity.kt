package com.seunome.finlo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncChangeEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val clientUpdated: Long,
    val deviceId: String,
    val deletedAt: Long?,
    val attempts: Int = 0
)
