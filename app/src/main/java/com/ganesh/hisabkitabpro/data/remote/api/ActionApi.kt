package com.ganesh.hisabkitabpro.data.remote.api

import retrofit2.Response
import retrofit2.http.*

// 🔷 Data Transfer Objects (DTOs) for Production Consistency
data class GenericResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String
)

data class FAQResponse(val question: String, val answer: String)

data class DiscountRequest(
    val invoice_id: String?,
    val customer_id: String,
    val discount_type: String, // "percent" or "fixed"
    val value: Double
)

data class ReminderRequest(
    val customer_id: String,
    val date: String, // YYYY-MM-DD
    val frequency: String // "once", "daily", "weekly"
)

data class SmsRequest(val phone: String, val message: String)
data class WhatsAppRequest(val phone: String, val message: String)

data class PaymentResponse(
    val upi_link: String,
    val qr_code_url: String
)

data class ReportResponse(
    val total_credit: Double,
    val total_debit: Double,
    val balance: Double,
    val pdf_url: String
)

/**
 * 🎯 ACTION API: Single point of truth for all ledger actions.
 * Connected to FastAPI Backend.
 */
interface ActionApi {

    // 1. HELP SYSTEM
    @GET("help/faqs")
    suspend fun getFaqs(): Response<List<FAQResponse>>

    @POST("help/contact")
    suspend fun contactSupport(@Body body: Map<String, String>): Response<GenericResponse<Unit>>

    // 2. REPORT SYSTEM
    @GET("reports/customer/{id}")
    suspend fun getCustomerReport(@Path("id") customerId: String): Response<GenericResponse<ReportResponse>>

    // 3. GIVE DISCOUNT
    @POST("billing/apply-discount")
    suspend fun applyDiscount(@Body request: DiscountRequest): Response<GenericResponse<Double>>

    // 4. AUTO REMINDER
    @POST("reminder/create")
    suspend fun createReminder(@Body request: ReminderRequest): Response<GenericResponse<Unit>>

    // 5. SMS SYSTEM
    @POST("sms/send")
    suspend fun sendSms(@Body request: SmsRequest): Response<GenericResponse<Unit>>

    // 6. WHATSAPP SYSTEM
    @POST("whatsapp/send")
    suspend fun sendWhatsApp(@Body request: WhatsAppRequest): Response<GenericResponse<Unit>>

    // 10. COLLECT PAYMENT
    @POST("payment/create")
    suspend fun createPayment(@Body body: Map<String, String>): Response<GenericResponse<PaymentResponse>>
}
