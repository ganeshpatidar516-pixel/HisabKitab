package com.ganesh.hisabkitabpro.domain.report

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class DailyReport(
    val date: Long,
    val totalTransactions: Int,
    val totalCredit: Double,
    val totalDebit: Double,
    val netCashFlow: Double,
    val topCustomerId: Long? = null
)

object DailyReportGenerator {

    fun generateDailyReport(
        date: Long,
        transactions: List<Transaction>
    ): DailyReport {
        val dailyTransactions = transactions.filter {
            it.createdAt >= date && it.createdAt < (date + 86400000)
        }

        val totalCreditPaise = dailyTransactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val totalDebitPaise = dailyTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }

        val topCustomerId = dailyTransactions
            .groupBy { it.customerId }
            .maxByOrNull { it.value.sumOf { t -> t.amount } }?.key

        return DailyReport(
            date = date,
            totalTransactions = dailyTransactions.size,
            totalCredit = totalCreditPaise / 100.0,
            totalDebit = totalDebitPaise / 100.0,
            netCashFlow = (totalDebitPaise - totalCreditPaise) / 100.0,
            topCustomerId = topCustomerId
        )
    }
}
