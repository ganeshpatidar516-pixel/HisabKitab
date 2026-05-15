package com.ganesh.hisabkitabpro.commandos.policy

import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.commandos.model.IntentName
import com.ganesh.hisabkitabpro.commandos.model.ParsedCommand
import com.ganesh.hisabkitabpro.commandos.model.RiskLevel

class PolicyGuard(
    private val autoExecuteThreshold: Double = 0.86,
    private val clarificationThreshold: Double = 0.70
) {
    fun evaluate(command: ParsedCommand, userConfirmed: Boolean = false): CommandResult? {
        val confidence = command.intent.confidence
        if (confidence < clarificationThreshold) {
            return CommandResult.Rejected("Command confidence too low for safe execution.")
        }
        if (command.policy.requiresConfirmation && !userConfirmed) {
            return CommandResult.ClarificationRequired(question = buildClarifyQuestion(command))
        }
        if (command.policy.riskLevel == RiskLevel.HIGH && !userConfirmed) {
            return CommandResult.ClarificationRequired(
                question = "High-risk action. Confirm before proceeding.\n${buildClarifyQuestion(command)}"
            )
        }
        val ledgerAddOneShot = command.intent.name == IntentName.LEDGER_ADD &&
            command.policy.riskLevel != RiskLevel.HIGH &&
            !command.entities.customerName.isNullOrBlank() &&
            command.entities.amount != null &&
            confidence >= clarificationThreshold

        val reminderSendOneShot = command.intent.name == IntentName.REMINDER_SEND &&
            command.policy.riskLevel != RiskLevel.HIGH &&
            !command.entities.customerName.isNullOrBlank() &&
            confidence >= clarificationThreshold

        if (!ledgerAddOneShot && !reminderSendOneShot && confidence < autoExecuteThreshold && !userConfirmed) {
            return CommandResult.ClarificationRequired(question = buildClarifyQuestion(command))
        }
        return null
    }

    private fun buildClarifyQuestion(command: ParsedCommand): String {
        val name = command.entities.customerName?.trim().orEmpty().ifBlank { "customer" }
        val amt = command.entities.amount
        return when (command.intent.name) {
            IntentName.LEDGER_ADD ->
                "Ledger +₹${amt ?: "?"}: $name? Type CONFIRM."
            IntentName.BILL_CLEAR ->
                "Bill clear (HIGH RISK): $name? Type CONFIRM."
            IntentName.REMINDER_SEND ->
                "Reminder: $name? Type CONFIRM."
            IntentName.CUSTOMER_ADD -> {
                val ph = command.entities.customerPhone?.trim().orEmpty()
                if (ph.isBlank()) "Naya customer: $name — phone likh kar bhejein. Phir CONFIRM."
                else "Naya customer: $name, phone $ph? Type CONFIRM."
            }
            IntentName.OPEN_SCREEN ->
                "Screen: ${command.entities.targetRoute ?: "?"}? Type CONFIRM."
            IntentName.SETTING_UPDATE ->
                "Setting: ${command.entities.settingKey ?: "?"} = ${command.entities.settingValue ?: "?"}? Type CONFIRM."
            IntentName.LEDGER_QUERY ->
                "डेटा सारांश: ${command.normalizedInput}? Type CONFIRM."
            IntentName.UNKNOWN ->
                "Please confirm: ${command.normalizedInput}"
        }
    }
}
