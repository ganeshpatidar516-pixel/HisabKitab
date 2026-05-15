package com.ganesh.hisabkitabpro.domain.engine

import com.ganesh.hisabkitabpro.domain.model.Transaction

object CustomerBalanceEngine {

    data class CustomerSummary(
        val customerId: Long,
        val totalCredit: Double,
        val totalDebit: Double,
        val balance: Double,
        val transactionCount: Int
    )

    fun calculateCustomerSummary(
        transactions: List<Transaction>,
        customerId: Long
    ): CustomerSummary {

        val list = transactions.filter {
            it.customerId == customerId
        }

        val credit = TransactionCalculator.totalCredit(list)
        val debit = TransactionCalculator.totalDebit(list)
        val balance = credit - debit

        return CustomerSummary(
            customerId = customerId,
            totalCredit = credit,
            totalDebit = debit,
            balance = balance,
            transactionCount = list.size
        )
    }

    fun allCustomersSummary(
        transactions: List<Transaction>
    ): List<CustomerSummary> {

        val customers = transactions
            .map { it.customerId }
            .distinct()

        return customers.map { id ->
            calculateCustomerSummary(transactions, id)
        }
    }

    fun topDebtors(
        transactions: List<Transaction>,
        limit: Int = 5
    ): List<CustomerSummary> {

        return allCustomersSummary(transactions)
            .sortedByDescending { it.balance }
            .take(limit)
    }

    fun totalCustomers(transactions: List<Transaction>): Int {
        return transactions
            .map { it.customerId }
            .distinct()
            .size
    }
}
