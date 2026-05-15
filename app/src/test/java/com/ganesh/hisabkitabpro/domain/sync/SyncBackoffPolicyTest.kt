package com.ganesh.hisabkitabpro.domain.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SyncBackoffPolicyTest {

    @Test
    fun isEligible_firstRetry_afterBackoffWindow() {
        val updatedAt = 1_000L
        val now = updatedAt + SyncBackoffPolicy.BASE_DELAY_MS * 2
        assertTrue(SyncBackoffPolicy.isEligible(retryCount = 1, updatedAt = updatedAt, now = now))
    }

    @Test
    fun isEligible_notReady_beforeDelay() {
        val updatedAt = 10_000L
        val now = updatedAt + 1_000L
        assertFalse(SyncBackoffPolicy.isEligible(retryCount = 2, updatedAt = updatedAt, now = now))
    }

    @Test
    fun hasExhaustedAutoRetries_atCap() {
        assertTrue(SyncBackoffPolicy.hasExhaustedAutoRetries(SyncBackoffPolicy.MAX_AUTO_RETRIES))
        assertFalse(SyncBackoffPolicy.hasExhaustedAutoRetries(SyncBackoffPolicy.MAX_AUTO_RETRIES - 1))
    }

    @Test
    fun delayMs_growsWithRetryCount() {
        val random = Random(42)
        val first = SyncBackoffPolicy.delayMs(retryCount = 0, random = random)
        val later = SyncBackoffPolicy.delayMs(retryCount = 4, random = random)
        assertTrue(later >= first)
    }

    @Test
    fun conflictKind_isPermanent() {
        assertFalse(SyncFailureKind.Conflict.transient)
    }
}
