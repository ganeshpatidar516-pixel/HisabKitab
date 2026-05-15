package com.ganesh.hisabkitabpro.domain.model

/**
 * Domain model for Reminders.
 * Table structure is handled by ReminderEntity.
 */
data class Reminder(
    val id: String,
    val customerId: Long,
    val customerName: String,
    val amount: Double,
    val dueDate: Long,
    val message: String,
    val status: String, // PENDING, SENT, PAID
    val createdAt: Long = System.currentTimeMillis()
)
