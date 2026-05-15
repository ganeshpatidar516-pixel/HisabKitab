package com.ganesh.hisabkitabpro.domain.engine

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

enum class AssistantIntent {
    CHANGE_LANGUAGE,
    OPEN_SETTINGS,
    TOGGLE_REMINDER,
    SEND_WHATSAPP_BILL,
    BUSINESS_ANALYSIS,
    BALANCE_QUERY,
    UNKNOWN
}

data class AssistantCommand(
    val intent: AssistantIntent,
    val parameters: Map<String, String> = emptyMap()
)

data class AssistantResult(
    val message: String,
    val action: String? = null
)

/**
 * Splits one user line into multiple commands when they used explicit separators
 * or chained phrases ending with … karo / … bhejo (common in voice typing).
 */
object AssistantInputSplitter {
    fun splitForSequentialRun(raw: String): List<String> {
        val t = raw.trim()
        if (t.isEmpty()) return emptyList()
        listOf("|", "｜").forEach { d ->
            if (t.contains(d)) {
                return t.split(d).map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        if (t.contains('\n')) return t.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (t.contains(';')) return t.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        val boundary = Regex("""(?i)(?<=\bkaro|\bbhejo)\s+(?=[\p{L}\p{N}])""")
        val bits = boundary.split(t).map { it.trim() }.filter { it.isNotEmpty() }
        return if (bits.size > 1) bits else listOf(t)
    }
}

object AIKhataAssistant {

    fun interpretCommand(text: String): AssistantCommand {
        val input = text.lowercase()
        return when {
            input.contains("language") || input.contains("भाषा") -> AssistantCommand(AssistantIntent.CHANGE_LANGUAGE)
            input.contains("setting") -> AssistantCommand(AssistantIntent.OPEN_SETTINGS)
            input.contains("reminder") -> AssistantCommand(AssistantIntent.TOGGLE_REMINDER)
            input.contains("whatsapp") -> AssistantCommand(AssistantIntent.SEND_WHATSAPP_BILL)
            input.contains("balance") || input.contains("हिसाब") || input.contains("बकाया") -> AssistantCommand(AssistantIntent.BALANCE_QUERY)
            input.contains("analysis") || input.contains("business") || input.contains("रिपोर्ट") -> AssistantCommand(AssistantIntent.BUSINESS_ANALYSIS)
            else -> AssistantCommand(AssistantIntent.UNKNOWN)
        }
    }

    fun executeCommand(
        command: AssistantCommand,
        transactions: List<Transaction>
    ): AssistantResult {
        return when (command.intent) {
            AssistantIntent.CHANGE_LANGUAGE -> AssistantResult("भाषा सेटिंग खोली जा रही है...", "open_language_settings")
            AssistantIntent.OPEN_SETTINGS -> AssistantResult("सेटिंग्स खोली जा रही हैं...", "open_settings")
            AssistantIntent.TOGGLE_REMINDER -> AssistantResult("रिमाइंडर सेटिंग अपडेट कर दी गई है।", "toggle_reminder")
            AssistantIntent.SEND_WHATSAPP_BILL -> AssistantResult("व्हाट्सएप बिल तैयार किया जा रहा है...", "send_whatsapp_bill")
            
            AssistantIntent.BALANCE_QUERY -> {
                val totalCredit = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
                val totalDebit = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
                val balance = totalCredit - totalDebit
                AssistantResult("आपका कुल बकाया ₹${balance / 100.0} है (उधार: ₹${totalCredit / 100.0}, जमा: ₹${totalDebit / 100.0})।")
            }

            AssistantIntent.BUSINESS_ANALYSIS -> {
                val customerCount = transactions.map { it.customerId }.distinct().size
                val transactionCount = transactions.size
                AssistantResult("आपके पास कुल $customerCount सक्रिय ग्राहक हैं और अब तक $transactionCount लेन-देने हुए हैं।")
            }

            else -> AssistantResult("माफ करें, मैं इस कमांड को समझ नहीं पाया।")
        }
    }
}
