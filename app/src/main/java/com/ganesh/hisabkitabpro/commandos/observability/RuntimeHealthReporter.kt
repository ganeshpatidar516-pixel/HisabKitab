package com.ganesh.hisabkitabpro.commandos.observability

import com.ganesh.hisabkitabpro.commandos.SuperCommandFeatureToggle
import com.ganesh.hisabkitabpro.commandos.sync.QueueHealthMetrics
import javax.inject.Inject
import javax.inject.Singleton

data class RuntimeHealthReport(
    val superCommandEnabled: Boolean,
    val slo: CommandSloSnapshot,
    val queuePending: Int,
    val queueStatus: String,
    val releaseReady: Boolean
)

@Singleton
class RuntimeHealthReporter @Inject constructor(
    private val toggle: SuperCommandFeatureToggle,
    private val sloTracker: CommandSloTracker,
    private val queueHealthMetrics: QueueHealthMetrics
) {
    fun snapshot(): RuntimeHealthReport {
        val slo = sloTracker.snapshot()
        val queue = queueHealthMetrics.snapshot()
        val enabled = toggle.isEnabled()
        val releaseReady = if (!enabled) {
            queue.pendingCount <= 100
        } else {
            (slo.total == 0 || slo.failureRate < 0.10) && queue.pendingCount <= 100
        }
        return RuntimeHealthReport(
            superCommandEnabled = enabled,
            slo = slo,
            queuePending = queue.pendingCount,
            queueStatus = queue.lastStatus,
            releaseReady = releaseReady
        )
    }
}
