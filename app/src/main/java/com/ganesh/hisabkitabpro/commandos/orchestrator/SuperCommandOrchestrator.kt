package com.ganesh.hisabkitabpro.commandos.orchestrator

import com.ganesh.hisabkitabpro.commandos.adapters.contracts.DomainAdapter
import com.ganesh.hisabkitabpro.commandos.dialect.DialectRegistry
import com.ganesh.hisabkitabpro.commandos.intent.DeterministicIntentParser
import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.commandos.model.IntentName
import com.ganesh.hisabkitabpro.commandos.model.TransactionClass
import com.ganesh.hisabkitabpro.commandos.normalize.InputNormalizer
import com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaExecutionRequest
import com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaExecutor
import com.ganesh.hisabkitabpro.commandos.policy.PolicyGuard

class SuperCommandOrchestrator(
    private val normalizer: InputNormalizer,
    private val dialectRegistry: DialectRegistry,
    private val parser: DeterministicIntentParser,
    private val policyGuard: PolicyGuard,
    private val adapter: DomainAdapter
) {
    private val sagaExecutor = SagaExecutor(adapter)

    suspend fun handle(
        rawInput: String,
        locale: String = "hinglish-hi",
        userConfirmed: Boolean = false
    ): CommandResult {
        val normalized = normalizer.normalize(rawInput)
        val dialectAdjusted = dialectRegistry.apply(normalized, locale)
        if (looksInventoryRelated(dialectAdjusted) && !looksOpenScreenCommand(dialectAdjusted)) {
            adapter.handleInventoryCommand(dialectAdjusted)?.let { return CommandResult.Success(it) }
        }
        val parsed = parser.parse(rawInput, dialectAdjusted, locale)

        val policyDecision = policyGuard.evaluate(parsed, userConfirmed)
        if (policyDecision != null) {
            if (policyDecision is CommandResult.Rejected &&
                policyDecision.reason.contains("confidence too low", ignoreCase = true)
            ) {
                val insight = adapter.answerLedgerInsightQuery(dialectAdjusted, parsed.entities.customerName)
                if (insight != null) {
                    return CommandResult.Success(insight)
                }
            }
            return policyDecision
        }

        val executionResult = when (parsed.intent.name) {
            IntentName.LEDGER_ADD -> executeLedgerAdd(parsed.entities.customerName, parsed.entities.amount)
            IntentName.BILL_CLEAR -> executeBillClear(parsed.entities.customerName)
            IntentName.REMINDER_SEND -> executeReminderSend(parsed.entities.customerName)
            IntentName.CUSTOMER_ADD -> executeCustomerAdd(parsed.entities.customerName, parsed.entities.customerPhone)
            IntentName.OPEN_SCREEN -> executeOpenScreen(parsed.entities.targetRoute)
            IntentName.SETTING_UPDATE -> executeSettingUpdate(
                parsed.entities.settingKey,
                parsed.entities.settingValue
            )
            IntentName.LEDGER_QUERY -> executeLedgerQuery(dialectAdjusted, parsed.entities.customerName)
            IntentName.UNKNOWN -> CommandResult.Rejected("Unknown command. Please try again.")
        }
        return enrichWithCanonicalHintIfNeeded(parsed, executionResult)
    }

    private fun looksInventoryRelated(input: String): Boolean {
        val q = input.lowercase()
        return listOf("inventory", "stock", "product", "item", "barcode", "sku", "maal", "samaan")
            .any { q.contains(it) }
    }

    private fun looksOpenScreenCommand(input: String): Boolean {
        val q = input.trim().lowercase()
        return q.startsWith("open ") || q.startsWith("khol ") || q.startsWith("go ") || q.startsWith("jao ")
    }

    private fun enrichWithCanonicalHintIfNeeded(parsed: com.ganesh.hisabkitabpro.commandos.model.ParsedCommand, result: CommandResult): CommandResult {
        if (result !is CommandResult.Success) return result
        if (!parsed.intent.templateId.contains("_fuzzy_")) return result
        val canonical = when (parsed.intent.name) {
            IntentName.LEDGER_ADD -> "${parsed.entities.customerName ?: "customer"} ko ${parsed.entities.amount ?: 0} add karo"
            IntentName.BILL_CLEAR -> "${parsed.entities.customerName ?: "customer"} ka bill clear karo"
            IntentName.REMINDER_SEND -> "${parsed.entities.customerName ?: "customer"} ko reminder bhejo"
            IntentName.CUSTOMER_ADD -> {
                val phone = parsed.entities.customerPhone ?: "<phone>"
                "${parsed.entities.customerName ?: "customer"} customer add phone $phone"
            }
            IntentName.OPEN_SCREEN -> "open ${parsed.entities.targetRoute ?: "settings"}"
            IntentName.LEDGER_QUERY -> parsed.normalizedInput
            IntentName.SETTING_UPDATE, IntentName.UNKNOWN -> parsed.normalizedInput
        }
        return CommandResult.Success("${result.message}\nSamjha gaya: $canonical")
    }

    private suspend fun executeLedgerQuery(normalized: String, customerHint: String?): CommandResult {
        val msg = adapter.answerLedgerInsightQuery(normalized, customerHint)
        return if (msg != null) {
            CommandResult.Success(msg)
        } else {
            CommandResult.Rejected("Unknown command. Please try again.")
        }
    }

    private suspend fun executeLedgerAdd(customerName: String?, amount: Long?): CommandResult {
        if (customerName.isNullOrBlank() || amount == null) {
            return CommandResult.Rejected("Ledger command missing customer or amount.")
        }
        val customerExists = adapter.searchCustomer(customerName)
        if (!customerExists) {
            return customerNotResolvedSoftMessage(customerName, "लेजर एंट्री")
        }
        val success = adapter.addLedgerEntry(customerName, amount)
        return if (success) CommandResult.Success("Ledger updated for $customerName.")
        else CommandResult.Rejected("Failed to add ledger entry.")
    }

    private suspend fun executeBillClear(customerName: String?): CommandResult {
        if (customerName.isNullOrBlank()) {
            return CommandResult.Rejected("Bill clear command missing customer.")
        }
        val customerExists = adapter.searchCustomer(customerName)
        if (!customerExists) {
            return customerNotResolvedSoftMessage(customerName, "बिल क्लियर")
        }
        val success = adapter.clearBill(customerName)
        return if (success) CommandResult.Success("Bill cleared for $customerName.")
        else CommandResult.Rejected("Failed to clear bill.")
    }

    private suspend fun executeReminderSend(customerName: String?): CommandResult {
        if (customerName.isNullOrBlank()) {
            return CommandResult.Rejected("Reminder command missing customer.")
        }
        val customerExists = adapter.searchCustomer(customerName)
        if (!customerExists) {
            return customerNotResolvedSoftMessage(customerName, "रिमाइंडर")
        }
        val report = adapter.sendReminderWithReport(customerName)
        return if (report.success) {
            val ch = report.channel ?: "UNKNOWN"
            CommandResult.Success("रिमाइंडर ट्रिगर सफल: ग्राहक=${report.customerName}, channel=$ch")
        } else {
            val reasonText = when (report.reason) {
                "phone_missing" -> "फोन नंबर सेव नहीं है"
                "channel_unavailable" -> "WhatsApp/SMS app उपलब्ध नहीं/खुला नहीं"
                "customer_not_found", "customer_missing" -> "ग्राहक नहीं मिला"
                else -> "अज्ञात कारण"
            }
            CommandResult.Success(
                "रिमाइंडर ट्रिगर असफल: ग्राहक=${report.customerName}, कारण=$reasonText, code=${report.reason ?: "unknown"}"
            )
        }
    }

    private suspend fun executeSettingUpdate(key: String?, value: String?): CommandResult {
        if (key.isNullOrBlank() || value.isNullOrBlank()) {
            return CommandResult.Rejected("Setting update command missing key/value.")
        }
        val success = adapter.updateSetting(key, value)
        return if (success) CommandResult.Success("Setting updated: $key")
        else CommandResult.Rejected("Failed to update setting.")
    }

    private suspend fun executeCustomerAdd(name: String?, phone: String?): CommandResult {
        if (name.isNullOrBlank()) {
            return CommandResult.Rejected("Customer add command missing name.")
        }
        if (phone.isNullOrBlank()) {
            return CommandResult.ClarificationRequired(
                "Customer phone missing. Send like: $name customer add phone 9876543210"
            )
        }
        val success = adapter.addCustomer(name, phone)
        return if (success) CommandResult.Success("Customer added: $name")
        else CommandResult.Rejected("Failed to add customer.")
    }

    private fun executeOpenScreen(targetRoute: String?): CommandResult {
        if (targetRoute.isNullOrBlank()) {
            return CommandResult.Rejected("Open screen command missing target.")
        }
        return CommandResult.Success("OPEN_SCREEN::$targetRoute")
    }

    private suspend fun customerNotResolvedSoftMessage(customerName: String, actionLabel: String): CommandResult {
        val sug = adapter.suggestCustomerNames(customerName, 5)
        val msg = if (sug.isEmpty()) {
            "कोई बदलाव नहीं किया ($actionLabel)। ग्राहक '$customerName' मिला नहीं। ग्राहक सूची में जो नाम सेव है वही लिखें।"
        } else {
            "कोई बदलाव नहीं किया ($actionLabel)। ग्राहक '$customerName' मिला नहीं। क्या इनमें से कोई: ${sug.joinToString(", ")}? सही नाम से फिर से भेजें।"
        }
        return CommandResult.Success(msg)
    }

    suspend fun executeSaga(request: SagaExecutionRequest): CommandResult {
        val result = sagaExecutor.execute(request)
        if (result.success) {
            return CommandResult.Success("Saga executed successfully with ${result.results.size} step(s).")
        }
        val failed = result.results.lastOrNull { !it.success }?.message ?: "Unknown failure"
        val manual = if (result.requiresManualReview) " Manual review required." else ""
        return CommandResult.Rejected("Saga failed: $failed.$manual")
    }

    suspend fun executeFinancialCompound(
        customerName: String,
        amount: Long,
        includeBillClear: Boolean = true,
        includeReminder: Boolean = true
    ): CommandResult {
        val planner = com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaPlanner()
        val request = planner.planCompoundCommand(customerName, amount, includeBillClear, includeReminder)
        if (request.transactionClass != TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL) {
            return CommandResult.Rejected("Unsupported transaction class for compound flow.")
        }
        return executeSaga(request)
    }
}
