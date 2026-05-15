package com.ganesh.hisabkitabpro.domain.ai

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class CustomerRiskReport(
    val customerId: Long,
    val totalCredit: Long,
    val totalDebit: Long,
    val balance: Long,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

object CustomerRiskEngine {

    fun analyzeCustomer(
        customerId: Long,
        transactions: List<Transaction>
    ): CustomerRiskReport {

        val credit =
            transactions
                .filter {
                    it.type == TransactionType.CREDIT
                            && it.customerId == customerId
                }
                .sumOf { it.amount }

        val debit =
            transactions
                .filter {
                    it.type == TransactionType.DEBIT
                            && it.customerId == customerId
                }
                .sumOf { it.amount }

        val balance = credit - debit

        val riskLevel =
            when {
                balance > 2000000L -> RiskLevel.HIGH
                balance > 500000L -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }

        return CustomerRiskReport(
            customerId = customerId,
            totalCredit = credit,
            totalDebit = debit,
            balance = balance,
            riskLevel = riskLevel
        )
    }
}
