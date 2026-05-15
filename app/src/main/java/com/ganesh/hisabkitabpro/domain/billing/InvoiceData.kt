package com.ganesh.hisabkitabpro.domain.billing

import com.ganesh.hisabkitabpro.domain.model.InvoiceItem

data class InvoiceData(

    val invoiceNumber: String,

    val businessName: String,
    val businessAddress: String,

    val customerName: String,
    val customerPhone: String? = null,

    val items: List<InvoiceItem>,

    val subtotal: Double,
    val tax: Double,
    val total: Double,

    val createdAt: String,

    val gstNumber: String? = null
)