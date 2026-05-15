package com.ganesh.hisabkitabpro.domain.service

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

object LedgerService {

    /**
     * [Block 4: Business Logic] - लेजर बैलेंस की सटीक गणना।
     */
    fun calculateNetBalance(transactions: List<Transaction>): Double {
        val credit = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val debit = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        // Blueprint Formula: (Credit - Debit) converted to Rupees
        return (credit - debit) / 100.0
    }

    /**
     * [Block 5: Service Engine] - ग्राहक के लिए सारांश (Summary) जनरेट करना।
     */
    fun getLedgerSummary(transactions: List<Transaction>): Map<String, Double> {
        val totalCredit = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val totalDebit = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        
        return mapOf(
            "total_credit" to totalCredit / 100.0,
            "total_debit" to totalDebit / 100.0,
            "net_balance" to (totalCredit - totalDebit) / 100.0
        )
    }
}
