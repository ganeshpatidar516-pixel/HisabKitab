package com.ganesh.hisabkitabpro.domain.ledger

import android.util.Log
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class LedgerSummary(
    val totalLeneHai: Long = 0L,
    val totalDeneHai: Long = 0L,
    val netBalance: Long = 0L,
    val transactionCount: Int = 0
)

object LedgerSummaryGenerator {

    /**
     * ✅ CRASH-PROOF Summary Generator with Try-Catch Encapsulation
     */
    fun generateSummary(transactions: List<Transaction>): LedgerSummary {
        return try {
            var leneHai = 0L
            var deneHai = 0L

            transactions.filter { !it.isDeleted && it.status == "SUCCESS" }.forEach { tx ->
                when (tx.type) {
                    TransactionType.CREDIT, TransactionType.INVOICE -> leneHai += tx.amount
                    TransactionType.DEBIT, TransactionType.PAYMENT -> deneHai += tx.amount
                    else -> {}
                }
            }

            LedgerSummary(
                totalLeneHai = leneHai,
                totalDeneHai = deneHai,
                netBalance = leneHai - deneHai,
                transactionCount = transactions.size
            )
        } catch (e: Exception) {
            Log.e("LedgerSummaryGenerator", "CRASH_PROOF_FALLBACK: Failed to generate summary", e)
            // Resource Fallback: Return empty summary instead of crashing
            LedgerSummary()
        }
    }
}
