package com.ganesh.hisabkitabpro.domain.ledger

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class BalanceSummary(
    val totalCredit: Double,
    val totalDebit: Double,
    val netBalance: Double
)

object CustomerBalanceEngine {

    fun calculateBalance(transactions: List<Transaction>): Double {
        var totalCreditPaise = 0L
        var totalDebitPaise = 0L

        transactions.forEach {
            when (it.type) {
                TransactionType.CREDIT, TransactionType.INVOICE -> {
                    totalCreditPaise += it.amount
                }
                TransactionType.DEBIT, TransactionType.PAYMENT -> {
                    totalDebitPaise += it.amount
                }
                TransactionType.ADJUSTMENT -> {
                    // Adjustments could be handled differently, usually subtracted if negative
                }
            }
        }

        // ✅ IMPORTANT: Net Balance = Debt (Credit) - Payment (Debit)
        return (totalCreditPaise - totalDebitPaise) / 100.0
    }

    fun getSummary(transactions: List<Transaction>): BalanceSummary {
        val totalCreditPaise = transactions.filter { it.type == TransactionType.CREDIT || it.type == TransactionType.INVOICE }.sumOf { it.amount }
        val totalDebitPaise = transactions.filter { it.type == TransactionType.DEBIT || it.type == TransactionType.PAYMENT }.sumOf { it.amount }
        return BalanceSummary(
            totalCredit = totalCreditPaise / 100.0,
            totalDebit = totalDebitPaise / 100.0,
            netBalance = (totalCreditPaise - totalDebitPaise) / 100.0
        )
    }
}
