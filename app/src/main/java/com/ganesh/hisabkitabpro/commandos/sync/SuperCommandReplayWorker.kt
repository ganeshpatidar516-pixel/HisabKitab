package com.ganesh.hisabkitabpro.commandos.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ganesh.hisabkitabpro.di.SuperCommandSyncEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

class SuperCommandReplayWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SuperCommandSyncEntryPoint::class.java
        )
        val service = entryPoint.superCommandService()
        val journal = entryPoint.offlineCommandJournal()
        val metrics = entryPoint.queueHealthMetrics()

        return try {
            val pendingBefore = journal.size()
            if (pendingBefore == 0) {
                metrics.record(pendingCount = 0, processedCount = 0, status = "IDLE")
                return Result.success()
            }

            val processed = service.replayPending(limit = 25)
            val pendingAfter = journal.size()
            val status = when {
                pendingAfter == 0 -> "DRAINED"
                processed > 0 -> "PARTIAL"
                else -> "STALLED"
            }
            metrics.record(pendingCount = pendingAfter, processedCount = processed, status = status)

            if (status == "STALLED") {
                if (runAttemptCount >= 5) Result.failure() else Result.retry()
            } else {
                Result.success()
            }
        } catch (_: Exception) {
            metrics.record(
                pendingCount = journal.size(),
                processedCount = 0,
                status = "ERROR"
            )
            if (runAttemptCount >= 5) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "super_command_replay_worker"

        fun ensureScheduled(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SuperCommandReplayWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
