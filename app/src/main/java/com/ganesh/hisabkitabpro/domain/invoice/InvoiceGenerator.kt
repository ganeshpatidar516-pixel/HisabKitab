package com.ganesh.hisabkitabpro.domain.invoice

import com.ganesh.hisabkitabpro.domain.model.InvoiceItem
import java.text.SimpleDateFormat
import java.util.*

object InvoiceGenerator {

    data class Invoice(
        val invoiceNumber: String,
        val customerName: String,
        val date: String,
        val items: List<InvoiceItem>,
        val totalAmount: Double
    )

    fun generateInvoice(
        customerName: String,
        items: List<InvoiceItem>
    ): Invoice {

        val total = items.sumOf { it.total }

        val invoiceNumber = generateInvoiceNumber()

        val date = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        ).format(Date())

        return Invoice(
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            date = date,
            items = items,
            totalAmount = total
        )
    }

    private fun generateInvoiceNumber(): String {

        val random = (1000..9999).random()

        return "INV-$random"
    }
}