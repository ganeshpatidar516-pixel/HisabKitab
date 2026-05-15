package com.ganesh.hisabkitabpro.domain.export

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.Customer
import java.io.File

@Deprecated(
    message = "Legacy placeholder exporter. Use com.ganesh.hisabkitabpro.domain.ledger.LedgerPdfGenerator for customer statements."
)
class LedgerPDFExporter {

    /**
     * Legacy non-writing placeholder kept for binary/source compatibility.
     */
    fun exportCustomerLedger(
        customer: Customer,
        transactions: List<Transaction>
    ): File {
        val fileName = "Ledger_${customer.safeFileName()}_${System.currentTimeMillis()}_${transactions.size}.pdf"
        val tempRoot = System.getProperty("java.io.tmpdir")?.takeIf { it.isNotBlank() } ?: "."
        return File(tempRoot, fileName)
    }

    fun generateShareText(customer: Customer): String {
        return """
            Hello ${customer.name},

            Your current balance on HisabKitab Pro is ₹${customer.balanceCache / 100.0}.
            Please see the attached ledger report for details.

            Thank you!
        """.trimIndent()
    }

    private fun Customer.safeFileName(): String =
        name
            .trim()
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^A-Za-z0-9_-]"), "")
            .ifBlank { "Customer" }
}
