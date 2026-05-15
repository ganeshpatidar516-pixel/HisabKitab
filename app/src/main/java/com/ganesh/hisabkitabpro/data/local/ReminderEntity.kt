package com.ganesh.hisabkitabpro.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["customerId", "isSent", "scheduledAt"]), // Optimized for dispatcher lookup
        Index(value = ["scheduledAt"]),
        Index(value = ["counterpartyKind", "partyId", "isSent", "scheduledAt"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Long, // Customer.id when [counterpartyKind] is CUSTOMER; 0 for party-supplier rows
    /** [ReminderCounterpartyKind]: separates party supplier ids from customer ids (same numeric id could exist in both tables). */
    val counterpartyKind: String = ReminderCounterpartyKind.CUSTOMER,
    /** [com.ganesh.hisabkitabpro.domain.model.Party.id] when [counterpartyKind] is PARTY_SUPPLIER; otherwise 0. */
    val partyId: Long = 0L,
    val message: String,
    val scheduledAt: Long,
    val isSent: Boolean = false,
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH
    val type: String = "PAYMENT", // PAYMENT, FOLLOWUP
    val createdAt: Long = System.currentTimeMillis(),
    /** Add-on: 0=none, 1=T-0 notified, 2=T+3, 3=T+7 — avoids duplicate escalations. */
    val lastEscalationTier: Int = 0
)
