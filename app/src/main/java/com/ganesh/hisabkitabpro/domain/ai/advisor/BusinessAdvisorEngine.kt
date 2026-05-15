package com.ganesh.hisabkitabpro.domain.ai.advisor

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class BusinessAdvice(
    val title: String,
    val message: String,
    val priority: String = "NORMAL" // HIGH, NORMAL, LOW
)

object BusinessAdvisorEngine {

    fun analyzeBusiness(transactions: List<Transaction>): List<BusinessAdvice> {
        val advices = mutableListOf<BusinessAdvice>()
        
        val totalCredit = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val totalDebit = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }

        if (totalCredit > totalDebit * 2) {
            advices.add(BusinessAdvice(
                "High Credit Warning",
                "Your credit (udhaar) is twice your collections. Focus on recovery to maintain cash flow.",
                "HIGH"
            ))
        }

        if (transactions.size > 10) {
            advices.add(BusinessAdvice(
                "Growth Insight",
                "Your business activity is increasing. Consider adding more stock for your top-selling products.",
                "NORMAL"
            ))
        }

        return advices
    }
}
