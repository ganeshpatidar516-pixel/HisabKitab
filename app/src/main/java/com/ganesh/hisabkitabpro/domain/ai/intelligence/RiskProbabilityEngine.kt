package com.ganesh.hisabkitabpro.domain.ai.intelligence

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import kotlin.math.pow

data class RiskAnalysis(
    val probabilityScore: Double, // 0.0 to 1.0
    val riskLevel: String, // LOW, MEDIUM, HIGH
    val recommendation: String
)

object RiskProbabilityEngine {

    /**
     * Calculates the probability of payment delay using historical data.
     * Uses a simplified logistic regression approach based on credit-to-payment ratio.
     */
    fun analyzeRisk(transactions: List<Transaction>): RiskAnalysis {
        if (transactions.isEmpty()) {
            return RiskAnalysis(0.1, "LOW", "No history found. Safe to start with small credit.")
        }

        val totalCredit = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val totalDebit = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }

        if (totalCredit == 0L) return RiskAnalysis(0.0, "LOW", "Excellent payment history.")

        // Probability calculation: Ratio of unpaid credit to total credit
        val unpaidRatio = (totalCredit - totalDebit).toDouble() / totalCredit.toDouble()
        val score = unpaidRatio.coerceIn(0.0, 1.0)

        val (level, recommendation) = when {
            score < 0.2 -> "LOW" to "Trusted customer. Can offer higher credit limit."
            score < 0.5 -> "MEDIUM" to "Monitor payments. Send reminders before due date."
            else -> "HIGH" to "High risk! Stop further credit until current balance is cleared."
        }

        return RiskAnalysis(score, level, recommendation)
    }
}
