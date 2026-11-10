package com.seunome.finlo.core.network.dto

import com.google.gson.JsonElement

data class SyncPushItemDto(
    val entity: String,
    val id: String,
    val op: String,
    val payload: JsonElement?,
    val updatedAt: Long,
    val deviceId: String,
    val deletedAt: Long?
)

data class SyncPushRequestDto(
    val changes: List<SyncPushItemDto>
)

data class SyncPushResponseDto(
    val appliedCount: Int,
    val serverVersion: Long
)

data class SyncChangeItemDto(
    val id: String,
    val userId: String,
    val entity: String,
    val entityId: String,
    val op: String,
    val payload: JsonElement?,
    val updatedAt: Long,
    val serverVersion: Long,
    val deviceId: String,
    val deletedAt: Long?
)

data class SyncPullResponseDto(
    val changes: List<SyncChangeItemDto>,
    val nextCursor: Long,
    val hasMore: Boolean
)
