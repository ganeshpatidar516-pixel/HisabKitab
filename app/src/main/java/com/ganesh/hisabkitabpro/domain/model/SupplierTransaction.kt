package com.ganesh.hisabkitabpro.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * HISABKITAB PRO - SUPPLIER TRANSACTION (BULLETPROOF)
 * Modularized to avoid conflict with Customer transactions.
 */
@Entity(
    tableName = "supplier_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["supplierId", "isDeleted", "createdAt"]),
        Index(value = ["uniqueHash"], unique = true)
    ]
)
data class SupplierTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: Long,
    val amount: Long, // In Paise
    val type: SupplierTransactionType,
    val note: String? = null,
    val uniqueHash: String = UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SupplierTransactionType {
    PURCHASE, // Mal Liya (Red) - Increases Payable
    PAYMENT,  // Paisa Chukaya (Green) - Decreases Payable
    ADJUSTMENT
}
