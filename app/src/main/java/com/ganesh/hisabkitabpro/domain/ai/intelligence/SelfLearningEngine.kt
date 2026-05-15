package com.ganesh.hisabkitabpro.domain.ai.intelligence

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.model.Customer

data class BusinessPattern(
    val isGrowing: Boolean,
    val avgTransactionAmount: Double,
    val learningConfidence: Double
)

class SelfLearningEngine {

    /**
     * Learns from historical transaction patterns.
     */
    fun analyzePatterns(transactions: List<Transaction>): BusinessPattern {
        if (transactions.isEmpty()) return BusinessPattern(false, 0.0, 0.0)

        val recentTransactions = transactions.take(100)
        val creditCount = recentTransactions.count { it.type == TransactionType.CREDIT }
        val debitCount = recentTransactions.count { it.type == TransactionType.DEBIT }

        val confidence = if (transactions.size > 50) 0.9 else 0.6
        
        return BusinessPattern(
            isGrowing = debitCount > creditCount,
            avgTransactionAmount = transactions.map { it.amount }.average() / 100.0,
            learningConfidence = confidence
        )
    }

    /**
     * Predicts customer risk score based on payment behavior.
     */
    fun predictCustomerRisk(customer: Customer, transactions: List<Transaction>): Double {
        val customerTransactions = transactions.filter { it.customerId == customer.id }
        if (customerTransactions.isEmpty()) return 0.5 // Default neutral risk

        val totalAmount = customerTransactions.sumOf { it.amount }
        val creditAmount = customerTransactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        
        // Logic: If credit is much higher than payments, risk is high
        if (totalAmount == 0L) return 0.5
        return (creditAmount.toDouble() / totalAmount.toDouble()).coerceIn(0.0, 1.0)
    }
}
