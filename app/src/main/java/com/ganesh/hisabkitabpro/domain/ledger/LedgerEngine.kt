package com.ganesh.hisabkitabpro.domain.ledger

import android.util.Log
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class LedgerEntry(
    val transaction: Transaction,
    /** Running balance in paise (same unit as [Transaction.amount]). */
    val runningBalancePaise: Long,
)

/**
 * Legacy running-balance helper — **not used** by production customer/supplier ledger screens
 * (they use [LedgerPdfGenerator] / DAO balance). Kept for tooling; fixed in P3 for safe future use.
 */
@Deprecated(
    message = "Not used by live ledger UI. Prefer customer_ledger balance + statement exporters.",
    level = DeprecationLevel.WARNING,
)
object LedgerEngine {

    private const val TAG = "LedgerEngine"

    fun calculateRunningBalance(transactions: List<Transaction>): List<LedgerEntry> {
        return try {
            var balancePaise = 0L
            val active = transactions.filter { !it.isDeleted && it.status == "SUCCESS" }
            val entries = active.sortedBy { it.createdAt }.map { transaction ->
                when (transaction.type) {
                    TransactionType.CREDIT, TransactionType.INVOICE ->
                        balancePaise += transaction.amount
                    TransactionType.DEBIT, TransactionType.PAYMENT ->
                        balancePaise -= transaction.amount
                    else -> Unit
                }
                LedgerEntry(transaction, balancePaise)
            }.reversed()
            entries
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate running balance: ${e::class.java.simpleName}")
            emptyList()
        }
    }
}
