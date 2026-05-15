package com.ganesh.hisabkitabpro.domain.ai

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class BusinessInsight(
    val totalCredit: Long,
    val totalDebit: Long,
    val balance: Long,
    val mostActiveCustomer: Long?,
    val riskCustomers: List<Long>
)

object AiLearningEngine {

    fun analyzeBusiness(
        transactions: List<Transaction>
    ): BusinessInsight {

        val totalCredit =
            transactions
                .filter { it.type == TransactionType.CREDIT }
                .sumOf { it.amount }

        val totalDebit =
            transactions
                .filter { it.type == TransactionType.DEBIT }
                .sumOf { it.amount }

        val balance = totalCredit - totalDebit

        val riskCustomers =
            transactions
                .filter { it.amount > 1000000L } // Using Paise (10,000 Rupees)
                .map { it.customerId }
                .distinct()

        return BusinessInsight(
            totalCredit = totalCredit,
            totalDebit = totalDebit,
            balance = balance,
            mostActiveCustomer = transactions
                .groupBy { it.customerId }
                .maxByOrNull { it.value.size }
                ?.key,
            riskCustomers = riskCustomers
        )
    }
}
