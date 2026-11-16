package com.seunome.finlo.core.sync

object ConflictResolver {
    fun shouldApplyRemote(
        localUpdatedAt: Long,
        localDeviceId: String,
        remoteUpdatedAt: Long,
        remoteDeviceId: String
    ): Boolean {
        if (remoteUpdatedAt > localUpdatedAt) {
            return true
        }
        if (remoteUpdatedAt == localUpdatedAt) {
            return remoteDeviceId > localDeviceId
        }
        return false
    }
}
