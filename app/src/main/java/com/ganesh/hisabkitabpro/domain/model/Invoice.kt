package com.ganesh.hisabkitabpro.domain.model

/**
 * [Block 6: Data Layer] - Main Invoice Model.
 * Updated with conditional GST logic based on Blueprint.
 */
data class Invoice(
    val invoiceId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val items: List<InvoiceItem>,
    val subtotal: Double,
    
    // GST Blueprint Implementation
    val gstEnabled: Boolean = false,
    val gstPercent: Double = 0.0,
    val gstAmount: Double = 0.0,

    /** When set (e.g. GST / VAT / Sales tax), PDF uses this instead of a hardcoded "GST" label. */
    val taxLineLabel: String? = null,

    val discount: Double = 0.0,
    val finalAmount: Double,
    val paymentStatus: String,
    val paymentMethod: String,
    val date: Long,
    val notes: String?
)
