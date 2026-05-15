package com.ganesh.hisabkitabpro.commandos.observability

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

data class CommandSloSnapshot(
    val total: Int,
    val success: Int,
    val rejected: Int,
    val clarification: Int,
    val failureRate: Double
)

@Singleton
class CommandSloTracker @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun recordSuccess() = mutate { total, success, rejected, clarification ->
        State(total + 1, success + 1, rejected, clarification)
    }

    fun recordRejected() = mutate { total, success, rejected, clarification ->
        State(total + 1, success, rejected + 1, clarification)
    }

    fun recordClarification() = mutate { total, success, rejected, clarification ->
        State(total + 1, success, rejected, clarification + 1)
    }

    fun snapshot(): CommandSloSnapshot {
        val total = prefs.getInt(KEY_TOTAL, 0)
        val success = prefs.getInt(KEY_SUCCESS, 0)
        val rejected = prefs.getInt(KEY_REJECTED, 0)
        val clarification = prefs.getInt(KEY_CLARIFICATION, 0)
        val failureRate = if (total == 0) 0.0 else rejected.toDouble() / total.toDouble()
        return CommandSloSnapshot(total, success, rejected, clarification, failureRate)
    }

    private data class State(val total: Int, val success: Int, val rejected: Int, val clarification: Int)

    private fun mutate(update: (Int, Int, Int, Int) -> State) {
        val current = State(
            prefs.getInt(KEY_TOTAL, 0),
            prefs.getInt(KEY_SUCCESS, 0),
            prefs.getInt(KEY_REJECTED, 0),
            prefs.getInt(KEY_CLARIFICATION, 0)
        )
        val next = update(current.total, current.success, current.rejected, current.clarification)
        prefs.edit()
            .putInt(KEY_TOTAL, next.total)
            .putInt(KEY_SUCCESS, next.success)
            .putInt(KEY_REJECTED, next.rejected)
            .putInt(KEY_CLARIFICATION, next.clarification)
            .apply()
    }

    companion object {
        private const val KEY_TOTAL = "super_command_slo_total"
        private const val KEY_SUCCESS = "super_command_slo_success"
        private const val KEY_REJECTED = "super_command_slo_rejected"
        private const val KEY_CLARIFICATION = "super_command_slo_clarification"
    }
}
