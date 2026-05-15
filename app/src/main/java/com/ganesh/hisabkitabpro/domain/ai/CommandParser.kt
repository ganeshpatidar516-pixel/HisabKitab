package com.ganesh.hisabkitabpro.domain.ai

import android.util.Log

data class ParsedCommand(
    val intent: CommandIntent,
    val customerName: String? = null,
    val amount: Double? = null
)

object CommandParser {

    /**
     * ✅ CRASH-PROOF Parser logic with Try-Catch Encapsulation
     */
    fun parseCommand(
        command: String
    ): ParsedCommand {
        return try {
            val text = command.lowercase()

            val intent = when {
                text.contains("ledger") || text.contains("खाता") -> CommandIntent.SEND_LEDGER
                text.contains("invoice") || text.contains("bill") || text.contains("बिल") -> CommandIntent.SEND_INVOICE
                text.contains("add") || text.contains("जोड़") || text.contains("उधार") -> CommandIntent.ADD_TRANSACTION
                text.contains("language") || text.contains("भाषा") -> CommandIntent.CHANGE_LANGUAGE
                text.contains("customer") || text.contains("ग्राहक") -> CommandIntent.OPEN_CUSTOMER
                text.contains("analytics") || text.contains("रिपोर्ट") -> CommandIntent.SHOW_ANALYTICS
                else -> CommandIntent.UNKNOWN
            }

            val amount = extractAmount(text)
            val customer = extractCustomer(text)

            ParsedCommand(
                intent = intent,
                customerName = customer,
                amount = amount
            )
        } catch (e: Exception) {
            Log.e("CommandParser", "CRASH_PROOF_FALLBACK: Failed to parse command", e)
            // Resource Fallback: Return UNKNOWN intent instead of crashing
            ParsedCommand(intent = CommandIntent.UNKNOWN)
        }
    }

    private fun extractAmount(text: String): Double? {
        return try {
            val regex = Regex("""\d+(\.\d+)?""")
            val match = regex.find(text)
            match?.value?.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractCustomer(text: String): String? {
        return try {
            val words = text.trim().split(Regex("\\s+"))
            if (words.size < 1 || words[0].isEmpty()) null else words[0]
        } catch (e: Exception) {
            null
        }
    }
}
