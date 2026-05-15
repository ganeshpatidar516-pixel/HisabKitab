package com.ganesh.hisabkitabpro.data.repository

import com.ganesh.hisabkitabpro.data.remote.api.*
import com.ganesh.hisabkitabpro.domain.repository.ActionRepository
import javax.inject.Inject

class ActionRepositoryImpl @Inject constructor(
    private val api: ActionApi
) : ActionRepository {

    override suspend fun getFaqs(): Result<List<FAQResponse>> = runCatching {
        val response = api.getFaqs()
        if (response.isSuccessful) response.body() ?: emptyList()
        else throw Exception(response.message())
    }

    override suspend fun contactSupport(message: String): Result<Unit> = runCatching {
        val response = api.contactSupport(mapOf("message" to message))
        if (!response.isSuccessful) throw Exception(response.message())
    }

    override suspend fun getCustomerReport(customerId: String): Result<ReportResponse> = runCatching {
        val response = api.getCustomerReport(customerId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Report failed")
    }

    override suspend fun applyDiscount(customerId: String, type: String, value: Double): Result<Double> = runCatching {
        val response = api.applyDiscount(DiscountRequest(null, customerId, type, value))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Discount failed")
    }

    override suspend fun createReminder(customerId: String, date: String, frequency: String): Result<Unit> = runCatching {
        val response = api.createReminder(ReminderRequest(customerId, date, frequency))
        if (!response.isSuccessful) throw Exception(response.message())
    }

    override suspend fun sendSms(phone: String, message: String): Result<Unit> = runCatching {
        val response = api.sendSms(SmsRequest(phone, message))
        if (!response.isSuccessful) throw Exception(response.message())
    }

    override suspend fun sendWhatsApp(phone: String, message: String): Result<Unit> = runCatching {
        val response = api.sendWhatsApp(WhatsAppRequest(phone, message))
        if (!response.isSuccessful) throw Exception(response.message())
    }

    override suspend fun createPayment(customerId: String, amount: Double): Result<PaymentResponse> = runCatching {
        val response = api.createPayment(mapOf("customer_id" to customerId, "amount" to amount.toString()))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Payment failed")
    }
}
