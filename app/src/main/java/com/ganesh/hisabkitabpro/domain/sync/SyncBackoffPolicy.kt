package com.ganesh.hisabkitabpro.domain.sync

import kotlin.math.min
import kotlin.random.Random

/**
 * Pure, deterministic exponential-backoff policy used by [SyncEngine] and
 * [SyncHealthMonitor].
 *
 * For each failed item we compute:
 *
 * `delay = min(baseMs * 2^retry, capMs) ± jitter`
 *
 * Defaults are tuned for Google Drive / FastAPI sync on Indian mobile networks:
 * - First retry after ~30s
 * - Cap at 30 minutes
 * - ±20% jitter to avoid thundering-herd on reconnect
 *
 * The policy is *passive*: it never sleeps. It just answers two questions —
 *  - "How long should I wait before the next attempt?" → [delayMs]
 *  - "Is this item eligible for the next attempt right now?" → [isEligible]
 *
 * That keeps timing decisions data-driven: a row's `updatedAt` plus the
 * policy's [delayMs] is its earliest next attempt timestamp.
 *
 * NO DB SCHEMA CHANGES NEEDED — backoff is computed at runtime from columns
 * that already exist on `sync_queue` (`retryCount`, `updatedAt`).
 */
object SyncBackoffPolicy {

    /** First retry waits this long. */
    const val BASE_DELAY_MS = 30_000L            // 30s

    /** Hard cap so a poisoned row doesn't sit idle for hours. */
    const val MAX_DELAY_MS = 30L * 60_000L       // 30 min

    /** ±20% jitter window. */
    const val JITTER_FRACTION = 0.20

    /**
     * Total attempts before an item is parked for human review. Items that
     * exceed this are kept as `FAILED` and excluded from auto retries; only
     * an explicit user-driven "Retry Failed Sync Items" can revive them.
     */
    const val MAX_AUTO_RETRIES = 8

    /**
     * Returns the delay (ms) to wait between the [retryCount]-th attempt and
     * the next one.
     *
     * `retryCount = 0` → BASE_DELAY_MS (after the first failure)
     * `retryCount = 1` → 2 × BASE_DELAY_MS
     * `retryCount = 2` → 4 × BASE_DELAY_MS, …
     */
    fun delayMs(
        retryCount: Int,
        baseMs: Long = BASE_DELAY_MS,
        capMs: Long = MAX_DELAY_MS,
        jitterFraction: Double = JITTER_FRACTION,
        random: Random = Random.Default
    ): Long {
        val r = retryCount.coerceAtLeast(0).coerceAtMost(20) // guard 2^N overflow
        val raw = baseMs.toDouble() * (1L shl r)
        val capped = min(raw, capMs.toDouble())
        val jitter = capped * jitterFraction
        val delta = random.nextDouble(-jitter, jitter)
        return (capped + delta).toLong().coerceAtLeast(0L)
    }

    /**
     * True iff [updatedAt] + [delayMs] for [retryCount] is on/before [now].
     *
     * @param now Current epoch millis. Pass [System.currentTimeMillis] in
     *   production code, a fixed value in tests for determinism.
     */
    fun isEligible(
        retryCount: Int,
        updatedAt: Long,
        now: Long,
        baseMs: Long = BASE_DELAY_MS,
        capMs: Long = MAX_DELAY_MS
    ): Boolean {
        if (retryCount <= 0) return true
        // For eligibility checks we must be deterministic — no jitter.
        val deterministicDelay = min(baseMs.toDouble() * (1L shl retryCount.coerceAtMost(20)), capMs.toDouble())
        return updatedAt + deterministicDelay.toLong() <= now
    }

    /**
     * Whether an item with this retryCount has exceeded the auto-retry budget.
     * Such items are parked as `FAILED` and only revived by user action.
     */
    fun hasExhaustedAutoRetries(retryCount: Int): Boolean = retryCount >= MAX_AUTO_RETRIES
}
