package com.ganesh.hisabkitabpro.engine

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class BusinessAdvice(
    val message: String,
    val priority: Int
)

object BusinessAdvisorEngine {

    fun analyzeBusiness(
        transactions: List<Transaction>
    ): List<BusinessAdvice> {

        val adviceList = mutableListOf<BusinessAdvice>()

        val totalCredit = transactions
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

        val totalDebit = transactions
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        val balance = totalCredit - totalDebit

        if (balance > 10000) {
            adviceList.add(
                BusinessAdvice(
                    message = "Customers have high outstanding balance. Consider collecting payments.",
                    priority = 1
                )
            )
        }

        if (totalDebit > totalCredit) {
            adviceList.add(
                BusinessAdvice(
                    message = "Expenses are higher than credits. Monitor spending carefully.",
                    priority = 2
                )
            )
        }

        if (transactions.size > 50) {
            adviceList.add(
                BusinessAdvice(
                    message = "Your business is growing. Consider reviewing top customers.",
                    priority = 3
                )
            )
        }

        if (adviceList.isEmpty()) {
            adviceList.add(
                BusinessAdvice(
                    message = "Business activity looks stable.",
                    priority = 4
                )
            )
        }

        return adviceList
    }
}