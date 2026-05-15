package com.ganesh.hisabkitabpro.core.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ganesh.hisabkitabpro.core.background.TaskManager
import com.ganesh.hisabkitabpro.core.storage.StorageSpaceGuard
import com.ganesh.hisabkitabpro.di.BackupWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val context: Context,
    private val taskManager: TaskManager,
) {
    fun triggerBackup() {
        taskManager.runBackgroundTask(
            BackupWorker::class.java,
            "cloud_backup",
        )
    }
}

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!StorageSpaceGuard.hasMinFreeSpace(applicationContext, minFreeMb = 64L)) {
            Log.w(TAG, "Backup skipped — insufficient free storage")
            return Result.failure()
        }
        return try {
            val manager = EntryPointAccessors.fromApplication(
                applicationContext,
                BackupWorkerEntryPoint::class.java,
            ).cloudBackupManager()
            val outcome = manager.backupDatabaseToDrive()
            if (outcome.isSuccess) {
                Result.success()
            } else {
                val msg = outcome.exceptionOrNull()?.message.orEmpty()
                Log.w(TAG, "Drive backup failed: $msg")
                if (runAttemptCount < 2 && msg.contains("signed in", ignoreCase = true).not()) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "BackupWorker error", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "BackupWorker"
    }
}
