package com.seunome.finlo

import com.seunome.finlo.core.sync.ConflictResolver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictResolverTest {

    @Test
    fun testRN10_remoteWithHigherUpdatedAtWins() {
        val result = ConflictResolver.shouldApplyRemote(
            localUpdatedAt = 1000L,
            localDeviceId = "device-A",
            remoteUpdatedAt = 2000L,
            remoteDeviceId = "device-B"
        )
        assertTrue(result)
    }

    @Test
    fun testRN10_remoteWithOlderUpdatedAtLoses() {
        val result = ConflictResolver.shouldApplyRemote(
            localUpdatedAt = 2000L,
            localDeviceId = "device-A",
            remoteUpdatedAt = 1000L,
            remoteDeviceId = "device-B"
        )
        assertFalse(result)
    }

    @Test
    fun testRN10_tieBreakerUsesHigherDeviceId() {
        val remoteWins = ConflictResolver.shouldApplyRemote(
            localUpdatedAt = 1500L,
            localDeviceId = "device-A",
            remoteUpdatedAt = 1500L,
            remoteDeviceId = "device-B"
        )
        assertTrue(remoteWins)

        val localWins = ConflictResolver.shouldApplyRemote(
            localUpdatedAt = 1500L,
            localDeviceId = "device-Z",
            remoteUpdatedAt = 1500L,
            remoteDeviceId = "device-B"
        )
        assertFalse(localWins)
    }
}
