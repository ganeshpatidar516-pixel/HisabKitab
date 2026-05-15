package com.ganesh.hisabkitabpro.commandos.agent

import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.commandos.orchestrator.SuperCommandOrchestrator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase-1 agent boundary: all Super Command execution goes through one entry that can later
 * add routing, extra logging, or tool-specific policies without touching ledger/customer screens.
 *
 * Today this delegates to [SuperCommandOrchestrator] unchanged (zero behaviour change).
 *
 * For read-only aggregated data (names, balances) use [BusinessContextBuilder] alongside this class.
 */
@Singleton
class AgentToolRegistry @Inject constructor(
    private val orchestrator: SuperCommandOrchestrator
) {
    suspend fun executeSuperCommand(context: AgentToolContext): CommandResult {
        return orchestrator.handle(
            rawInput = context.rawInput,
            locale = context.locale,
            userConfirmed = context.userConfirmed
        )
    }

    suspend fun executeSuperCommandWithReport(context: AgentToolContext): AgentToolExecutionReport {
        return executeSuperCommand(context).toAgentToolExecutionReport()
    }
}
