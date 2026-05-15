package com.ganesh.hisabkitabpro.domain.repository

import com.ganesh.hisabkitabpro.data.remote.api.*
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 PRODUCTION REPOSITORY: Handles all advanced business actions.
 */
interface ActionRepository {
    // Help Logic
    suspend fun getFaqs(): Result<List<FAQResponse>>
    suspend fun contactSupport(message: String): Result<Unit>

    // Report Logic
    suspend fun getCustomerReport(customerId: String): Result<ReportResponse>

    // Billing Logic
    suspend fun applyDiscount(customerId: String, type: String, value: Double): Result<Double>

    // Reminder Logic
    suspend fun createReminder(customerId: String, date: String, frequency: String): Result<Unit>

    // Communication Logic
    suspend fun sendSms(phone: String, message: String): Result<Unit>
    suspend fun sendWhatsApp(phone: String, message: String): Result<Unit>

    // Payment Logic
    suspend fun createPayment(customerId: String, amount: Double): Result<PaymentResponse>
}
