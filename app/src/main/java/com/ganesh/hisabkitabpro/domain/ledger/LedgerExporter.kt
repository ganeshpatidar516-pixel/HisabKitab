package com.ganesh.hisabkitabpro.domain.ledger

import com.ganesh.hisabkitabpro.domain.model.Transaction

object LedgerExporter {

    fun exportLedger(
        customerName: String,
        transactions: List<Transaction>
    ): String {

        val builder = StringBuilder()

        builder.appendLine("Ledger for $customerName")
        builder.appendLine("----------------")

        transactions.forEach {
            builder.appendLine(
                "Transaction ID: ${it.id} : ₹${it.amount / 100.0} (${it.type})"
            )
        }

        return builder.toString()
    }
}
