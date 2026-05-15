package com.ganesh.hisabkitabpro.core.backup

import android.content.Context
import androidx.work.*
import com.ganesh.hisabkitabpro.core.background.TaskManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val context: Context,
    private val taskManager: TaskManager
) {
    fun triggerBackup() {
        taskManager.runBackgroundTask(
            BackupWorker::class.java,
            "cloud_backup"
        )
    }
}

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            // Perform backup logic
            // 1. Create local backup file
            // 2. Attempt upload
            // 3. If fails, retry (WorkManager handles this with BackoffPolicy)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                // Save locally and fail gracefully
                Result.failure()
            }
        }
    }
}
