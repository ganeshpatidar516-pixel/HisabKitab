package com.ganesh.hisabkitabpro.domain.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.ganesh.hisabkitabpro.network.RetrofitClient
import com.ganesh.hisabkitabpro.network.api.CustomerApi
import com.ganesh.hisabkitabpro.network.api.TransactionApi
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Periodic + on-demand sync worker.
 *
 * **Phase 14 — scheduling:** first periodic run uses a **random initial delay** (0–13 min) so
 * installs do not align on the same minute boundary (thundering herd mitigation). Existing
 * scheduled work is left unchanged ([ExistingPeriodicWorkPolicy.KEEP]).
 *
 * **Phase 14 — retry cap:** transient `Result.retry()` is capped per rolling hour to reduce
 * retry storms on persistent backend outages; after the cap, the worker yields success until the
 * next window. A **clean** sync clears the guard.
 */
class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "hisabkitab_sync_worker"
        private const val TAG = "SyncWorker"

        private const val RETRY_GUARD_PREFS = "sync_worker_retry_guard"
        private const val KEY_WINDOW_START_MS = "window_start_ms"
        private const val KEY_RETRY_COUNT = "retry_count"
        private const val RETRY_WINDOW_MS = 60L * 60L * 1000L
        private const val MAX_RETRIES_PER_WINDOW = 16

        fun ensureScheduled(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .build()

            val jitterMinutes = Random.nextLong(0L, 14L)
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(jitterMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30L,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        private fun clearRetryGuard(ctx: Context) {
            ctx.applicationContext
                .getSharedPreferences(RETRY_GUARD_PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }

        /**
         * Returns [Result.retry] if under cap, else [Result.success] to stop WM retry escalation.
         */
        private fun guardedRetry(ctx: Context): Result {
            val app = ctx.applicationContext
            val prefs = app.getSharedPreferences(RETRY_GUARD_PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            var windowStart = prefs.getLong(KEY_WINDOW_START_MS, 0L)
            var count = prefs.getInt(KEY_RETRY_COUNT, 0)
            if (windowStart == 0L || now - windowStart > RETRY_WINDOW_MS) {
                windowStart = now
                count = 0
            }
            if (count >= MAX_RETRIES_PER_WINDOW) {
                Log.w(
                    TAG,
                    "sync_retry_cap: $MAX_RETRIES_PER_WINDOW transient retries/hour reached — yielding"
                )
                SyncHealthMonitor.onWorkerPaused(SyncHealthMonitor.WorkerPauseReason.RETRY_CAP)
                return Result.success()
            }
            prefs.edit()
                .putLong(KEY_WINDOW_START_MS, windowStart)
                .putInt(KEY_RETRY_COUNT, count + 1)
                .apply()
            return Result.retry()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val customerApi = RetrofitClient.retrofit.create(CustomerApi::class.java)
            val transactionApi = RetrofitClient.retrofit.create(TransactionApi::class.java)

            SyncEngine.performFullSync(customerApi, transactionApi)

            val health = SyncHealthMonitor.state.value
            val report = health.lastReport

            when {
                report == null -> {
                    clearRetryGuard(applicationContext)
                    Result.success()
                }
                report.isFullyClean -> {
                    clearRetryGuard(applicationContext)
                    Result.success()
                }
                report.authExpired -> {
                    Log.w(TAG, "Auth expired during sync — pausing automatic retries.")
                    SyncHealthMonitor.onWorkerPaused(SyncHealthMonitor.WorkerPauseReason.AUTH_EXPIRED)
                    Result.success()
                }
                report.transientFailures > 0 -> guardedRetry(applicationContext)
                report.permanentFailures > 0 -> {
                    Result.success()
                }
                else -> {
                    clearRetryGuard(applicationContext)
                    Result.success()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Sync worker threw — scheduling retry with exponential backoff", t)
            guardedRetry(applicationContext)
        }
    }
}
