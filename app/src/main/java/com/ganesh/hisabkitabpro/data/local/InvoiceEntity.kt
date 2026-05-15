package com.ganesh.hisabkitabpro.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ganesh.hisabkitabpro.domain.model.Customer

/**
 * HISABKITAB PRO - ULTRA SCALE INVOICE ENGINE
 * Optimized with Composite Indexes and ACID-Compliant Foreign Keys.
 */
@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["customerId", "isDeleted"]),
        Index(value = ["invoiceId"], unique = true),
        Index(value = ["date"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey
    val invoiceId: String,
    val customerId: String, // Keep as String if that's the legacy, but mapping usually requires Long for speed
    val customerName: String,
    val date: Long = System.currentTimeMillis(),
    
    val subtotal: Double,
    
    // GST Implementation fields
    val gstEnabled: Boolean = false,
    val gstRate: Double = 0.0,
    val gstAmount: Double = 0.0,
    
    val totalAmount: Double,
    val discountAmount: Double = 0.0,

    val status: String = "PENDING", // PENDING, PAID, CANCELLED
    val pdfPath: String? = null,
    val qrData: String? = null,
    val isDeleted: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["invoiceId"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["invoiceId"])
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val invoiceId: String,
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val isDeleted: Int = 0
)
