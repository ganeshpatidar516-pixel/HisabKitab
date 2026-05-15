package com.ganesh.hisabkitabpro.addon.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureScheduled() {
        val wm = WorkManager.getInstance(context)
        val req = PeriodicWorkRequestBuilder<ReminderEscalationWorker>(12, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }

    companion object {
        const val WORK_NAME = "HisabKitabReminderEngineV2"
    }
}
