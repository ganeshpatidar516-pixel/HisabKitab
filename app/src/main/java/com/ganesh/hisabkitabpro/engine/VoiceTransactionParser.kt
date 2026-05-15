package com.ganesh.hisabkitabpro.domain.engine

import com.ganesh.hisabkitabpro.domain.model.TransactionInput
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class VoiceParseResult(
    val success: Boolean,
    val transaction: TransactionInput?,
    val error: String? = null
)

object VoiceTransactionParser {

    fun parseVoiceInput(
        text: String
    ): VoiceParseResult {

        try {

            val words = text.lowercase().split(" ")

            val amount =
                words.firstOrNull { it.toDoubleOrNull() != null }
                    ?.toDoubleOrNull()

            if (amount == null) {
                return VoiceParseResult(
                    success = false,
                    transaction = null,
                    error = "Amount not detected"
                )
            }

            val customer =
                words.firstOrNull {
                    it.first().isLetter()
                } ?: "Customer"

            val type =
                if (text.contains("udhar") || text.contains("de diya"))
                    TransactionType.CREDIT
                else
                    TransactionType.DEBIT

            val transaction = TransactionInput(
                customerName = customer.replaceFirstChar { it.uppercase() },
                amount = amount,
                type = type
            )

            return VoiceParseResult(
                success = true,
                transaction = transaction
            )

        } catch (e: Exception) {

            return VoiceParseResult(
                success = false,
                transaction = null,
                error = "Could not understand voice command"
            )
        }
    }
}