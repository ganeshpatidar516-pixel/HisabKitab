package com.ganesh.hisabkitabpro.domain.engine

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class CustomerBalance(
    val customerId: Long,
    val balance: Double,
    val totalCredit: Double,
    val totalDebit: Double
)

data class DashboardAnalytics(
    val totalCredit: Double,
    val totalDebit: Double,
    val totalBalance: Double,
    val totalCustomers: Int,
    val totalTransactions: Int
)

object CustomerAnalyticsEngine {

    fun calculateCustomerBalances(
        transactions: List<Transaction>
    ): List<CustomerBalance> {

        val grouped = transactions.groupBy { it.customerId }

        return grouped.map { (customerId, list) ->

            val creditPaise = list
                .filter { it.type == TransactionType.CREDIT }
                .sumOf { it.amount }

            val debitPaise = list
                .filter { it.type == TransactionType.DEBIT }
                .sumOf { it.amount }

            CustomerBalance(
                customerId = customerId,
                balance = (creditPaise - debitPaise) / 100.0,
                totalCredit = creditPaise / 100.0,
                totalDebit = debitPaise / 100.0
            )
        }
    }

    fun calculateDashboard(
        transactions: List<Transaction>
    ): DashboardAnalytics {

        val creditPaise = transactions
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

        val debitPaise = transactions
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        val customers = transactions
            .map { it.customerId }
            .distinct()

        return DashboardAnalytics(
            totalCredit = creditPaise / 100.0,
            totalDebit = debitPaise / 100.0,
            totalBalance = (creditPaise - debitPaise) / 100.0,
            totalCustomers = customers.size,
            totalTransactions = transactions.size
        )
    }
}
