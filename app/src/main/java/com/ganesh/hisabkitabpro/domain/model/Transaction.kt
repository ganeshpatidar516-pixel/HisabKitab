package com.ganesh.hisabkitabpro.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId", "isDeleted", "createdAt"]),
        Index(value = ["type", "isDeleted", "status"]), // ✅ SPEED INDEX for Dashboard Totals
        Index(value = ["txnRef"], unique = true),
        Index(value = ["uniqueHash"], unique = true)
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val amount: Long,
    val type: TransactionType,
    val note: String? = null,
    val billId: Long? = null,
    val paymentMethod: String? = null,
    val txnRef: String = UUID.randomUUID().toString(),
    val uniqueHash: String = UUID.randomUUID().toString(),
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val oldAmount: Long = 0L,
    val invoiceNo: String? = null,
    /** Set when user sends the bill PDF via in-app WhatsApp action (ledger timeline). */
    val whatsappSentAt: Long? = null,
    val syncStatus: String = "PENDING",
    val dateString: String? = null,
    val status: String = "SUCCESS",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Add-on metadata only — does not change principal/balance formulas. */
    val settlementKind: String? = null
)
