package com.ganesh.hisabkitabpro.domain.engine

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

object TransactionCalculator {

    fun totalCredit(transactions: List<Transaction>): Double {
        return transactions
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }.toDouble() / 100.0
    }

    fun totalDebit(transactions: List<Transaction>): Double {
        return transactions
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }.toDouble() / 100.0
    }

    fun balance(transactions: List<Transaction>): Double {
        return totalCredit(transactions) - totalDebit(transactions)
    }

    fun customerTransactions(
        transactions: List<Transaction>,
        customerId: Long
    ): List<Transaction> {
        return transactions.filter {
            it.customerId == customerId
        }
    }

    fun customerBalance(
        transactions: List<Transaction>,
        customerId: Long
    ): Double {
        val list = customerTransactions(transactions, customerId)
        return balance(list)
    }

    fun totalCustomers(transactions: List<Transaction>): Int {
        return transactions
            .map { it.customerId }
            .distinct()
            .size
    }

    fun recentTransactions(
        transactions: List<Transaction>,
        limit: Int = 10
    ): List<Transaction> {
        return transactions.sortedByDescending { it.createdAt }.take(limit)
    }

    fun topDebtors(
        transactions: List<Transaction>,
        limit: Int = 5
    ): List<Pair<Long, Double>> {

        val customerIds = transactions
            .map { it.customerId }
            .distinct()

        return customerIds.map { id ->
            id to customerBalance(transactions, id)
        }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun topPayers(
        transactions: List<Transaction>,
        limit: Int = 5
    ): List<Pair<Long, Double>> {

        val customerIds = transactions
            .map { it.customerId }
            .distinct()

        return customerIds.map { id ->
            id to customerBalance(transactions, id)
        }
            .sortedBy { it.second }
            .take(limit)
    }

    fun totalSystemCredit(transactions: List<Transaction>): Double {
        return totalCredit(transactions)
    }

    fun totalSystemDebit(transactions: List<Transaction>): Double {
        return totalDebit(transactions)
    }

    fun systemBalance(transactions: List<Transaction>): Double {
        return balance(transactions)
    }
}
