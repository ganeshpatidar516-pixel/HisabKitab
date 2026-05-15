package com.ganesh.hisabkitabpro.core.background

import android.content.Context
import androidx.work.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskManager @Inject constructor(
    private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun runBackgroundTask(
        workerClass: Class<out ListenableWorker>,
        tag: String,
        inputData: Data = Data.EMPTY
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequest.Builder(workerClass)
            .setConstraints(constraints)
            .addTag(tag)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            tag,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelTask(tag: String) {
        workManager.cancelUniqueWork(tag)
    }
}
