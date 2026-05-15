package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.addon.audit.AuditLogRecorder
import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.commandos.observability.CanaryGuard
import com.ganesh.hisabkitabpro.commandos.observability.CommandSloSnapshot
import com.ganesh.hisabkitabpro.commandos.observability.CommandSloTracker
import com.ganesh.hisabkitabpro.commandos.agent.AgentToolContext
import com.ganesh.hisabkitabpro.commandos.agent.AgentToolRegistry
import com.ganesh.hisabkitabpro.commandos.sync.OfflineCommandJournal
import com.ganesh.hisabkitabpro.commandos.sync.OfflineReplayEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuperCommandService @Inject constructor(
    private val toggle: SuperCommandFeatureToggle,
    private val agentToolRegistry: AgentToolRegistry,
    private val auditLogRecorder: AuditLogRecorder? = null,
    private val offlineJournal: OfflineCommandJournal? = null,
    private val sloTracker: CommandSloTracker? = null,
    private val canaryGuard: CanaryGuard? = null
) {
    /** Same source as Settings toggle; UI can skip calling [run] when false to avoid extra work. */
    fun isRuntimeEnabled(): Boolean = toggle.isEnabled()

    suspend fun run(
        rawInput: String,
        locale: String = "hinglish-hi",
        userConfirmed: Boolean = false
    ): CommandResult {
        if (!toggle.isEnabled()) {
            return CommandResult.Rejected("Super Command is disabled.")
        }
        val result = agentToolRegistry.executeSuperCommand(
            AgentToolContext(rawInput = rawInput, locale = locale, userConfirmed = userConfirmed)
        )
        val action = when (result) {
            is CommandResult.Success -> "SUPER_COMMAND_SUCCESS"
            is CommandResult.ClarificationRequired -> "SUPER_COMMAND_CONFIRM_REQUIRED"
            is CommandResult.Rejected -> "SUPER_COMMAND_REJECTED"
        }
        when (result) {
            is CommandResult.Success -> sloTracker?.recordSuccess()
            is CommandResult.ClarificationRequired -> sloTracker?.recordClarification()
            is CommandResult.Rejected -> sloTracker?.recordRejected()
        }
        val snapshot = sloTracker?.snapshot()
        if (snapshot != null && canaryGuard?.shouldAutoDisable(snapshot) == true) {
            toggle.setEnabled(false)
            auditLogRecorder?.recordAsync(
                entityType = "SUPER_COMMAND",
                entityId = 0L,
                action = "SUPER_COMMAND_AUTO_DISABLED",
                detail = "failure_rate=${snapshot.failureRate}"
            )
        }
        auditLogRecorder?.recordAsync(
            entityType = "SUPER_COMMAND",
            entityId = 0L,
            action = action,
            detail = rawInput
        )
        return result
    }

    suspend fun replayPending(limit: Int = 20): Int {
        if (!toggle.isEnabled()) return 0
        val journal = offlineJournal ?: return 0
        val replayEngine = OfflineReplayEngine(journal)
        return replayEngine.replayBatch(limit) { entry ->
            val result = agentToolRegistry.executeSuperCommand(
                AgentToolContext(
                    rawInput = entry.rawCommand,
                    locale = entry.locale,
                    userConfirmed = true
                )
            )
            result is CommandResult.Success
        }
    }

    fun sloSnapshot(): CommandSloSnapshot {
        return sloTracker?.snapshot() ?: CommandSloSnapshot(0, 0, 0, 0, 0.0)
    }
}
