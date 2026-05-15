package com.ganesh.hisabkitabpro.commandos.sync

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

data class QueueHealthSnapshot(
    val pendingCount: Int,
    val lastReplayAt: Long,
    val lastProcessedCount: Int,
    val lastStatus: String
)

@Singleton
class QueueHealthMetrics @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun record(pendingCount: Int, processedCount: Int, status: String) {
        prefs.edit()
            .putInt(KEY_PENDING_COUNT, pendingCount)
            .putLong(KEY_LAST_REPLAY_AT, System.currentTimeMillis())
            .putInt(KEY_LAST_PROCESSED_COUNT, processedCount)
            .putString(KEY_LAST_STATUS, status)
            .apply()
    }

    fun snapshot(): QueueHealthSnapshot {
        return QueueHealthSnapshot(
            pendingCount = prefs.getInt(KEY_PENDING_COUNT, 0),
            lastReplayAt = prefs.getLong(KEY_LAST_REPLAY_AT, 0L),
            lastProcessedCount = prefs.getInt(KEY_LAST_PROCESSED_COUNT, 0),
            lastStatus = prefs.getString(KEY_LAST_STATUS, "IDLE") ?: "IDLE"
        )
    }

    companion object {
        private const val KEY_PENDING_COUNT = "super_command_queue_pending_count"
        private const val KEY_LAST_REPLAY_AT = "super_command_queue_last_replay_at"
        private const val KEY_LAST_PROCESSED_COUNT = "super_command_queue_last_processed_count"
        private const val KEY_LAST_STATUS = "super_command_queue_last_status"
    }
}
