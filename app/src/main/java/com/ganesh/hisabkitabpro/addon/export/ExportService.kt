package com.ganesh.hisabkitabpro.addon.export

import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction

/**
 * Isolated export helpers — fetch data from callers; no side effects on ledger.
 */
object ExportService {

    fun transactionsToCsv(transactions: List<Transaction>): String = buildString {
        appendLine("id,customerId,amountPaise,type,note,createdAt")
        transactions.forEach { t ->
            append(t.id).append(',')
                .append(t.customerId).append(',')
                .append(t.amount).append(',')
                .append(t.type.name).append(',')
                .append(t.note?.replace(",", ";") ?: "").append(',')
                .append(t.createdAt)
                .appendLine()
        }
    }

    fun customersBalanceCsv(customers: List<Customer>): String = buildString {
        appendLine("id,name,phone,balancePaise")
        customers.forEach { c ->
            append(c.id).append(',').append(c.name.replace(",", ";")).append(',')
                .append(c.phone.replace(",", ";")).append(',').append(c.balanceCache).appendLine()
        }
    }
}
