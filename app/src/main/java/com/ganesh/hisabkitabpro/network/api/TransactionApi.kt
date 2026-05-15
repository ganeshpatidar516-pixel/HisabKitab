package com.ganesh.hisabkitabpro.network.api

import com.ganesh.hisabkitabpro.domain.model.Transaction
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class TransactionResponse(
    val transactions: List<Transaction>,
    val total_balance: Double
)

interface TransactionApi {

    @GET("api/v1/transactions/all")
    suspend fun getAllTransactions(): TransactionResponse

    @GET("api/v1/transactions/{customerId}")
    suspend fun getTransactions(
        @Path("customerId") customerId: String
    ): List<Transaction>

    @POST("api/v1/transactions/add")
    suspend fun addTransaction(
        @Body body: TransactionSyncRequest
    ): Response<ResponseBody>

    @PUT("api/v1/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long, // Changed to Long
        @Body transaction: Transaction
    ): Transaction

    @DELETE("api/v1/transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: Long // Changed to Long
    ): Response<Unit>

    @POST("api/v1/transactions/{id}/pdf")
    suspend fun generatePdf(
        @Path("id") id: String
    ): Response<ResponseBody>

    @POST("api/v1/transactions/{id}/share")
    suspend fun shareTransaction(
        @Path("id") id: String
    ): Response<ResponseBody>

    @POST("api/v1/transactions/{id}/reminder")
    suspend fun setReminder(
        @Path("id") id: String
    ): Response<ResponseBody>
}
