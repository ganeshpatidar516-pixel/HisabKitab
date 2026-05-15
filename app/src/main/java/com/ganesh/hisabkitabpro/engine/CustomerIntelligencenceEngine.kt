package com.ganesh.hisabkitabpro.domain.engine

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class CustomerInsight(
    val customerId: Long,
    val totalCredit: Double,
    val totalDebit: Double,
    val balance: Double,
    val transactionCount: Int
)

object CustomerIntelligenceEngine {

    fun generateInsights(
        transactions: List<Transaction>
    ): List<CustomerInsight> {

        val grouped = transactions.groupBy { it.customerId }

        return grouped.map { (customerId, list) ->

            val creditPaise = list
                .filter { it.type == TransactionType.CREDIT }
                .sumOf { it.amount }

            val debitPaise = list
                .filter { it.type == TransactionType.DEBIT }
                .sumOf { it.amount }

            CustomerInsight(
                customerId = customerId,
                totalCredit = creditPaise / 100.0,
                totalDebit = debitPaise / 100.0,
                balance = (creditPaise - debitPaise) / 100.0,
                transactionCount = list.size
            )
        }
    }

    fun topCustomers(
        insights: List<CustomerInsight>,
        limit: Int = 5
    ): List<CustomerInsight> {

        return insights
            .sortedByDescending { it.balance }
            .take(limit)
    }

    fun riskCustomers(
        insights: List<CustomerInsight>,
        thresholdRupees: Double = 1000.0
    ): List<CustomerInsight> {

        return insights
            .filter { it.balance > thresholdRupees }
            .sortedByDescending { it.balance }
    }
}
