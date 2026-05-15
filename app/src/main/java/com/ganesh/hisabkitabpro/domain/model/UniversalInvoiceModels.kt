package com.ganesh.hisabkitabpro.domain.model

import java.util.UUID

enum class TaxType { GST, VAT, SALES_TAX, NONE }

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Double,
    val price: Double,
    val discountPercent: Double = 0.0
) {
    val totalPrice: Double get() = (quantity * price) * (1 - discountPercent / 100)
}

data class UniversalInvoice(
    val invoiceNumber: String,
    val businessName: String,
    val customerName: String,
    val items: List<BillItem>,
    val currencySymbol: String = "₹",
    val taxType: TaxType = TaxType.NONE,
    val taxRate: Double = 0.0,
    val signatureUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val subTotal: Double get() = items.sumOf { it.totalPrice }
    val taxAmount: Double get() = subTotal * (taxRate / 100)
    val grandTotal: Double get() = subTotal + taxAmount
}
