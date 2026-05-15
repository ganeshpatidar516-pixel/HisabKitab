package com.ganesh.hisabkitabpro.domain.ledger

import com.ganesh.hisabkitabpro.domain.model.Transaction
import java.util.*

object TransactionFilterEngine {

    fun filterToday(transactions: List<Transaction>): List<Transaction> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return transactions.filter { it.createdAt >= today }
    }

    fun filterThisWeek(transactions: List<Transaction>): List<Transaction> {
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        return transactions.filter { it.createdAt >= weekStart }
    }

    fun filterThisMonth(transactions: List<Transaction>): List<Transaction> {
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
        return transactions.filter { it.createdAt >= monthStart }
    }

    fun filterByCustomer(transactions: List<Transaction>, customerId: Long): List<Transaction> {
        return transactions.filter { it.customerId == customerId }
    }
}
