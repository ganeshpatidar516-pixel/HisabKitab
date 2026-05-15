package com.ganesh.hisabkitabpro.addon.bulk

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ganesh.hisabkitabpro.addon.reminder.ReminderEscalationWorker

/**
 * Entry point for bulk reminder processing — schedules the same escalation worker once.
 * UI can call this without touching existing per-reminder flows.
 */
object BulkReminderCoordinator {
    fun enqueueProcessAll(context: Context) {
        val req = OneTimeWorkRequestBuilder<ReminderEscalationWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "bulk_reminder_pass",
            ExistingWorkPolicy.KEEP,
            req
        )
    }
}
