package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.model.CommandIntent
import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.commandos.model.ExecutionPolicy
import com.ganesh.hisabkitabpro.commandos.model.IntentName
import com.ganesh.hisabkitabpro.commandos.model.ParsedCommand
import com.ganesh.hisabkitabpro.commandos.model.ResolvedEntities
import com.ganesh.hisabkitabpro.commandos.model.RiskLevel
import com.ganesh.hisabkitabpro.commandos.model.TransactionClass
import com.ganesh.hisabkitabpro.commandos.policy.PolicyGuard
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyGuardTest {
    private val guard = PolicyGuard()

    @Test
    fun evaluate_rejectsLowConfidenceCommands() {
        val result = guard.evaluate(sampleParsed(0.5))
        assertTrue(result is CommandResult.Rejected)
    }

    @Test
    fun evaluate_requestsConfirmationForMediumConfidence() {
        val parsed = ParsedCommand(
            rawInput = "open settings",
            normalizedInput = "open settings",
            locale = "hinglish-hi",
            intent = CommandIntent(IntentName.OPEN_SCREEN, 0.8, "open_screen_fuzzy_v1"),
            entities = ResolvedEntities(targetRoute = "settings"),
            policy = ExecutionPolicy(
                transactionClass = TransactionClass.HARD_ATOMIC,
                riskLevel = RiskLevel.LOW,
                requiresConfirmation = false
            )
        )
        val result = guard.evaluate(parsed)
        assertTrue(result is CommandResult.ClarificationRequired)
    }

    @Test
    fun evaluate_allowsLedgerAddWhenNameAndAmountParsed_evenIfBelowAutoExecuteThreshold() {
        val result = guard.evaluate(sampleParsed(0.83))
        assertTrue(result == null)
    }

    @Test
    fun evaluate_allowsReminderSendWhenNameParsed_evenIfBelowAutoExecuteThreshold() {
        val parsed = ParsedCommand(
            rawInput = "ramesh ko reminder bhejo",
            normalizedInput = "ramesh ko reminder bhejo",
            locale = "hinglish-hi",
            intent = CommandIntent(IntentName.REMINDER_SEND, 0.82, "reminder_fuzzy_v1"),
            entities = ResolvedEntities(customerName = "ramesh"),
            policy = ExecutionPolicy(
                transactionClass = TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL,
                riskLevel = RiskLevel.LOW,
                requiresConfirmation = false
            )
        )
        val result = guard.evaluate(parsed)
        assertTrue(result == null)
    }

    @Test
    fun evaluate_allowsHighConfidenceLowRisk() {
        val parsed = sampleParsed(0.95).copy(
            policy = ExecutionPolicy(
                transactionClass = TransactionClass.HARD_ATOMIC,
                riskLevel = RiskLevel.LOW,
                requiresConfirmation = false
            )
        )
        val result = guard.evaluate(parsed)
        assertTrue(result == null)
    }

    private fun sampleParsed(confidence: Double): ParsedCommand {
        return ParsedCommand(
            rawInput = "ramesh ko 500 add karo",
            normalizedInput = "ramesh ko 500 add karo",
            locale = "hinglish-hi",
            intent = CommandIntent(IntentName.LEDGER_ADD, confidence, "ledger_add_v1"),
            entities = ResolvedEntities(customerName = "ramesh", amount = 500L),
            policy = ExecutionPolicy(
                transactionClass = TransactionClass.HARD_ATOMIC,
                riskLevel = RiskLevel.MEDIUM,
                requiresConfirmation = false
            )
        )
    }
}
