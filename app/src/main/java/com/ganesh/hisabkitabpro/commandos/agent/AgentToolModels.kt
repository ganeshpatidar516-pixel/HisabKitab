package com.ganesh.hisabkitabpro.commandos.agent

import com.ganesh.hisabkitabpro.commandos.model.CommandResult

/**
 * Single payload passed into the agent tool layer (Phase-1 boundary).
 * Keeps locale / confirmation in one place for future multi-step tools.
 */
data class AgentToolContext(
    val rawInput: String,
    val locale: String = "hinglish-hi",
    val userConfirmed: Boolean = false
)

/**
 * Structured outcome for UI, analytics, and future cloud sync — without changing [CommandResult] semantics.
 */
data class AgentToolExecutionReport(
    val outcome: String,
    val userVisibleMessage: String,
    val reasonCode: String? = null
)

fun CommandResult.toAgentToolExecutionReport(): AgentToolExecutionReport = when (this) {
    is CommandResult.Success -> AgentToolExecutionReport(
        outcome = "SUCCESS",
        userVisibleMessage = message
    )
    is CommandResult.ClarificationRequired -> AgentToolExecutionReport(
        outcome = "CLARIFY",
        userVisibleMessage = question,
        reasonCode = "confirm_required"
    )
    is CommandResult.Rejected -> AgentToolExecutionReport(
        outcome = "REJECTED",
        userVisibleMessage = reason,
        reasonCode = "rejected"
    )
}
