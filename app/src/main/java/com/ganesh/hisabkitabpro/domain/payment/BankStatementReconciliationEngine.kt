package com.ganesh.hisabkitabpro.domain.payment

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Lightweight reconciliation matcher for imported statement rows.
 * It only proposes candidates; caller still confirms before applying settlement changes.
 */
object BankStatementReconciliationEngine {

    data class MatchCandidate(
        val statement: BankStatementCsvParser.StatementRow,
        val transaction: Transaction?,
        val confidence: Int,
        val reason: String
    )

    fun proposeMatches(
        statementRows: List<BankStatementCsvParser.StatementRow>,
        ledgerTransactions: List<Transaction>,
        maxTimeDeltaMs: Long = 3L * 24L * 60L * 60L * 1000L
    ): List<MatchCandidate> {
        if (statementRows.isEmpty()) return emptyList()
        if (ledgerTransactions.isEmpty()) {
            return statementRows.map {
                MatchCandidate(it, null, 0, "No ledger transactions available")
            }
        }

        val receivableCandidates = ledgerTransactions
            .filter { !it.isDeleted && (it.type == TransactionType.DEBIT || it.type == TransactionType.PAYMENT) }

        return statementRows.map { row ->
            val exactAmount = receivableCandidates.filter { it.amount == kotlin.math.abs(row.amountPaise) }
            val amountPool = if (exactAmount.isNotEmpty()) exactAmount else receivableCandidates

            val best = amountPool.minByOrNull { tx ->
                kotlin.math.abs(tx.createdAt - estimateEpoch(row.postedAtRaw))
            }

            if (best == null) {
                MatchCandidate(row, null, 0, "No candidate found")
            } else {
                val delta = kotlin.math.abs(best.createdAt - estimateEpoch(row.postedAtRaw))
                val amountMatched = best.amount == kotlin.math.abs(row.amountPaise)
                val confidence = when {
                    amountMatched && delta <= maxTimeDeltaMs -> 90
                    amountMatched -> 75
                    delta <= maxTimeDeltaMs -> 55
                    else -> 35
                }
                val reason = when {
                    amountMatched && delta <= maxTimeDeltaMs -> "Exact amount + near date match"
                    amountMatched -> "Exact amount match"
                    delta <= maxTimeDeltaMs -> "Near date match"
                    else -> "Weak heuristic match"
                }
                MatchCandidate(row, best, confidence, reason)
            }
        }
    }

    private fun estimateEpoch(rawDate: String): Long {
        val cleaned = rawDate.trim()
        val now = System.currentTimeMillis()
        if (cleaned.isBlank()) return now
        val formats = listOf(
            "dd/MM/yyyy",
            "d/M/yyyy",
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "dd MMM yyyy"
        )
        for (pattern in formats) {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.isLenient = true
            val parsed = runCatching { sdf.parse(cleaned) }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return now
    }
}
