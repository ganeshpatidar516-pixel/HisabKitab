package com.ganesh.hisabkitabpro.network.api

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.google.gson.annotations.SerializedName

/**
 * Payload for [TransactionApi.addTransaction] — matches FastAPI [TransactionBase] (snake_case, UDHAR/JAMA, rupees).
 */
data class TransactionSyncRequest(
    @SerializedName("id") val id: String,
    @SerializedName("customer_id") val customerId: String,
    @SerializedName("customer_name") val customerName: String?,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("note") val note: String?,
    @SerializedName("created_at") val createdAt: Long
)

fun Transaction.toSyncRequest(customerName: String? = null): TransactionSyncRequest {
    val stableId = txnRef.ifBlank { id.toString() }
    val rupees = amount / 100.0
    val apiType = when (type) {
        TransactionType.CREDIT, TransactionType.INVOICE -> "UDHAR"
        TransactionType.DEBIT, TransactionType.PAYMENT -> "JAMA"
        TransactionType.ADJUSTMENT -> "UDHAR"
    }
    return TransactionSyncRequest(
        id = stableId,
        customerId = customerId.toString(),
        customerName = customerName,
        amount = rupees,
        type = apiType,
        note = note,
        createdAt = createdAt
    )
}
