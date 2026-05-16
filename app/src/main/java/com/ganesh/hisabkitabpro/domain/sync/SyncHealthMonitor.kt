package com.ganesh.hisabkitabpro.domain.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.ganesh.hisabkitabpro.core.firebase.OpsTelemetryHub
import com.ganesh.hisabkitabpro.data.repository.local.SyncDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

/**
 * SYNC HEALTH MONITOR — the central observability + auto-recovery pane of glass.
 *
 * Solves all three failure modes that produced the "Failed: 5" sticky state:
 *   1. Lack of an observable signal — the UI now reads [state] and renders
 *      "Syncing…" / "All items secured" / detailed failure copy in real time.
 *   2. No auto-recovery — [observeOutcome] folds engine outcomes into a
 *      [SyncHealth] aggregate so the UI knows when the queue is clean.
 *   3. Spam-retry of permanently failing items — [deepRetryAll] is the only
 *      path that resets retryCount + updatedAt and triggers an *expedited*
 *      OneTimeWorkRequest with WorkManager exponential backoff.
 *
 * Lifecycle: a process-singleton initialized from [com.ganesh.hisabkitabpro.HisabKitabApp].
 * It does NOT touch Customer/Bill/Ledger logic. It does NOT mutate any column
 * outside `sync_queue`. SyncEngine + WorkManager keep their public contracts.
 */
object SyncHealthMonitor {

    private const val TAG = "SyncHealthMonitor"
    private const val EXPEDITED_RETRY_WORK = "hisabkitab_sync_retry_oneshot"

    /** Public observable surface. */
    enum class Phase { Idle, Syncing, Healthy, Degraded, Stuck }

    enum class WorkerPauseReason {
        NONE,
        RETRY_CAP,
        AUTH_EXPIRED,
    }

    data class SyncHealth(
        val phase: Phase = Phase.Idle,
        val pending: Int = 0,
        val failed: Int = 0,
        val queuedInMemory: Int = 0,
        val lastAttemptAt: Long? = null,
        val lastSuccessAt: Long? = null,
        val lastReport: SyncCycleReport? = null,
        val lastFailureKind: SyncFailureKind? = null,
        val workerPauseReason: WorkerPauseReason = WorkerPauseReason.NONE,
        val message: String = "Idle"
    ) {
        val totalOutstanding: Int get() = pending + failed + queuedInMemory
        val needsUserAttention: Boolean
            get() = workerPauseReason != WorkerPauseReason.NONE ||
                phase == Phase.Stuck ||
                (phase == Phase.Degraded && totalOutstanding > 0)
    }

    private val _state = MutableStateFlow(SyncHealth())
    val state: StateFlow<SyncHealth> = _state.asStateFlow()

    @Volatile
    private var syncDao: SyncDao? = null

    @Volatile
    private var appContext: Context? = null

    fun initialize(syncDao: SyncDao, context: Context? = null) {
        this.syncDao = syncDao
        if (context != null) {
            appContext = context.applicationContext
        }
    }

    /** Called by [SyncEngine] before a cycle starts. */
    fun onCycleStart() {
        _state.update { it.copy(phase = Phase.Syncing, message = "Syncing…") }
    }

    /** Called by [SyncEngine] for each item outcome. */
    fun onItemOutcome(outcome: SyncItemOutcome) {
        when (outcome) {
            is SyncItemOutcome.Success -> Log.d(TAG, "Synced ${outcome.type}#${outcome.itemId}")
            is SyncItemOutcome.ResolvedByLww -> Log.d(TAG, "LWW resolved ${outcome.type}#${outcome.itemId}")
            is SyncItemOutcome.Failure -> {
                Log.w(
                    TAG,
                    "Failed ${outcome.type}#${outcome.itemId} (${outcome.kind}): ${outcome.rawReason}"
                )
            }
        }
    }

    /** Called by [SyncEngine] at the end of every cycle. */
    suspend fun onCycleComplete(report: SyncCycleReport) {
        val dao = syncDao
        val pending = dao?.getPendingCountOnce() ?: 0
        val failed = dao?.getFailedCountOnce() ?: 0
        val snap = _state.value
        val pause = snap.workerPauseReason
        val phase = when {
            pause == WorkerPauseReason.AUTH_EXPIRED -> Phase.Degraded
            pause == WorkerPauseReason.RETRY_CAP -> Phase.Degraded
            report.authExpired -> Phase.Degraded
            report.noNetwork -> Phase.Degraded
            failed > 0 && report.transientFailures == 0 -> Phase.Stuck
            failed > 0 || report.transientFailures > 0 -> Phase.Degraded
            pending == 0 && failed == 0 -> Phase.Healthy
            else -> Phase.Idle
        }
        val message = when {
            pause == WorkerPauseReason.AUTH_EXPIRED ->
                "Sync paused — sign in again, then tap Retry Failed Sync Items"
            pause == WorkerPauseReason.RETRY_CAP ->
                "Sync paused after many retries — open Cloud settings and tap Retry"
            phase == Phase.Healthy -> "All items secured"
            phase == Phase.Degraded ->
                "Sync degraded · ${report.permanentFailures + report.transientFailures} issue(s) · Pending: $pending"
            phase == Phase.Stuck -> "Stuck items: $failed (tap Retry Failed Sync Items)"
            phase == Phase.Syncing -> "Syncing…"
            else -> "Idle"
        }
        val clearPause = phase == Phase.Healthy && pause != WorkerPauseReason.NONE
        _state.update {
            it.copy(
                phase = phase,
                pending = pending,
                failed = failed,
                queuedInMemory = it.queuedInMemory, // SyncEngine updates this separately
                lastAttemptAt = report.finishedAt,
                lastSuccessAt = if (report.effectivelySucceeded > 0) report.finishedAt else it.lastSuccessAt,
                lastReport = report,
                lastFailureKind = report.toLastKind(it.lastFailureKind),
                workerPauseReason = if (clearPause) WorkerPauseReason.NONE else it.workerPauseReason,
                message = message
            )
        }
        appContext?.let { ctx ->
            val syncPhase = when (phase) {
                Phase.Healthy -> "cycle_healthy"
                Phase.Degraded, Phase.Stuck -> "cycle_degraded"
                Phase.Syncing -> "cycle_syncing"
                Phase.Idle -> "cycle_idle"
            }
            OpsTelemetryHub.logSyncPhase(
                ctx,
                syncPhase,
                mapOf(
                    "pending" to pending.toString(),
                    "failed" to failed.toString(),
                    "permanent_failures" to report.permanentFailures.toString(),
                ),
            )
        }
    }

    /** WorkManager returned success but automatic sync is intentionally paused. */
    fun onWorkerPaused(reason: WorkerPauseReason) {
        if (reason == WorkerPauseReason.NONE) return
        val message = when (reason) {
            WorkerPauseReason.AUTH_EXPIRED ->
                "Sync paused — sign in again, then tap Retry Failed Sync Items"
            WorkerPauseReason.RETRY_CAP ->
                "Sync paused after many retries — tap Retry Failed Sync Items when online"
            WorkerPauseReason.NONE -> return
        }
        _state.update {
            it.copy(
                phase = Phase.Degraded,
                workerPauseReason = reason,
                lastFailureKind = when (reason) {
                    WorkerPauseReason.AUTH_EXPIRED -> SyncFailureKind.AuthExpired
                    WorkerPauseReason.RETRY_CAP -> SyncFailureKind.Network
                    WorkerPauseReason.NONE -> it.lastFailureKind
                },
                message = message,
            )
        }
        Log.w(TAG, "sync_worker_paused: $reason")
    }

    fun clearWorkerPause() {
        _state.update {
            if (it.workerPauseReason == WorkerPauseReason.NONE) it
            else it.copy(workerPauseReason = WorkerPauseReason.NONE)
        }
    }

    /** Called by [SyncEngine] whenever the in-memory parking lot size changes. */
    fun onInMemoryQueueChanged(size: Int) {
        _state.update { it.copy(queuedInMemory = size) }
    }

    /**
     * The "deep retry" entry point used by the UI's
     * "Retry Failed Sync Items" button.
     *
     * Steps:
     *  1. Reset all `FAILED` rows back to `PENDING` with `retryCount=0` and
     *     `updatedAt` set far in the past so they are immediately eligible.
     *  2. Enqueue a *one-shot* WorkRequest with WorkManager exponential
     *     backoff so the OS retries even if the network blips.
     *
     * Returns the number of rows that were unparked. The actual sync result
     * arrives later via [state].
     */
    suspend fun deepRetryAll(context: Context): Int {
        val dao = syncDao ?: return 0
        // Reset rows: backoff policy uses updatedAt to compute eligibility, so we
        // backdate them to "long ago" so isEligible() returns true immediately.
        val movedFromFailed = dao.requeueFailedItems(updatedAt = 0L)
        // Also ping any PENDING rows so they aren't gated by their last delay.
        runCatching { dao.touchAllPending(updatedAt = 0L) }

        _state.update {
            it.copy(
                phase = Phase.Syncing,
                workerPauseReason = WorkerPauseReason.NONE,
                message = "Syncing…"
            )
        }

        scheduleExpeditedRetry(context)
        return movedFromFailed
    }

    /** Same as [deepRetryAll] but only schedules the OneTime worker. */
    fun scheduleExpeditedRetry(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30L,
                TimeUnit.SECONDS
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                EXPEDITED_RETRY_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }
}

private fun SyncCycleReport.toLastKind(prev: SyncFailureKind?): SyncFailureKind? = when {
    authExpired -> SyncFailureKind.AuthExpired
    noNetwork -> SyncFailureKind.Network
    transientFailures > 0 -> SyncFailureKind.Network
    permanentFailures > 0 -> SyncFailureKind.ClientError
    isFullyClean -> null
    else -> prev
}
