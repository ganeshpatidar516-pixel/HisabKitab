package com.ganesh.hisabkitabpro.domain.model

/**
 * [Block 6: Data Layer] - लेजर एंट्री का मुख्य मॉडल।
 */
data class LedgerEntry(
    val id: String,
    val customerId: String,
    val transactionId: Int,
    val amount: Double,
    val type: TransactionType,
    val balanceAfter: Double,
    val timestamp: Long = System.currentTimeMillis()
)
