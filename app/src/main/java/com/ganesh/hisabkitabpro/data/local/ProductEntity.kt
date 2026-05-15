package com.ganesh.hisabkitabpro.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"]),
        Index(value = ["barcode"], unique = true),
        Index(value = ["sku"], unique = true),
        Index(value = ["isDeleted", "name"]),
        Index(value = ["isDeleted", "stockQuantity", "minStockLevel"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Double,
    val unit: String = "Pcs", // Pcs, Kg, Ltr, etc.
    val minStockLevel: Double = 5.0,
    val barcode: String? = null,
    val sku: String? = null,
    val taxRatePercent: Double = 0.0,
    val isActive: Boolean = true,
    val isDeleted: Int = 0,
    val lastStockSyncAt: Long = 0L,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
