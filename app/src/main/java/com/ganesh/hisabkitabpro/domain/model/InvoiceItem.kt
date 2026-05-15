package com.ganesh.hisabkitabpro.domain.model

/**
 * [Block 6: Data Layer] - Advanced Invoice Item Model.
 * Matches UI requirements (GST, HSN, Unit, MRP).
 */
data class InvoiceItem(
    val itemName: String,
    val quantity: Double,
    val unit: String = "Nos",
    val rate: Double,
    val mrp: Double? = null,
    val gstPercentage: Double = 0.0,
    val cess: Double = 0.0,
    val hsnCode: String? = null,
    val total: Double,
    val notes: String? = null
)
