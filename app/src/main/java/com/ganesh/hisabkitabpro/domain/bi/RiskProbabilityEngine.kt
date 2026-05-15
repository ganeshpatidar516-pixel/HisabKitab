package com.ganesh.hisabkitabpro.domain.bi

import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import kotlin.math.pow

object RiskProbabilityEngine {

    /**
     * ग्राहक के ट्रांजैक्शन इतिहास के आधार पर रिस्क स्कोर (0.0 to 1.0) कैलकुलेट करता है।
     * 1.0 मतलब बहुत हाई रिस्क (पैसे डूबने का खतरा)।
     */
    fun calculateRiskScore(customer: Customer, transactions: List<Transaction>): Double {
        if (transactions.isEmpty()) return 0.0

        val totalUdhar = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val totalJama = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        
        // 1. बकाया राशि का अनुपात (Balance Ratio)
        val balanceRatio = if (totalUdhar > 0) (totalUdhar - totalJama).toDouble() / totalUdhar.toDouble() else 0.0
        
        // 2. पेमेंट में देरी (Payment Delay Factor)
        val lastTransactionTime = transactions.maxOfOrNull { it.createdAt } ?: customer.createdAt
        val lastPaymentDays = (System.currentTimeMillis() - lastTransactionTime) / (1000 * 60 * 60 * 24)
        val delayFactor = if (lastPaymentDays > 30) 0.5 else 0.0

        val riskScore = (balanceRatio * 0.6) + (delayFactor * 0.4)
        
        return riskScore.coerceIn(0.0, 1.0)
    }
}
