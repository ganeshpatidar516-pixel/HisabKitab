package com.ganesh.hisabkitabpro.domain.billing

import com.ganesh.hisabkitabpro.domain.ai.ParsedCommand

data class VoiceTransactionResult(
    val success: Boolean,
    val message: String
)

class VoiceTransactionEngine {

    fun processVoiceTransaction(
        command: ParsedCommand
    ): VoiceTransactionResult {

        val customer = command.customerName
        val amount = command.amount

        if (customer == null) {

            return VoiceTransactionResult(
                success = false,
                message = "Customer name not detected"
            )
        }

        if (amount == null) {

            return VoiceTransactionResult(
                success = false,
                message = "Amount not detected"
            )
        }

        // Future: connect with repository / database

        return VoiceTransactionResult(
            success = true,
            message = "Transaction created: ₹$amount for $customer"
        )
    }
}