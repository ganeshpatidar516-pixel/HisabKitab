package com.ganesh.hisabkitabpro.data.remote.api

import com.ganesh.hisabkitabpro.domain.model.Transaction
import retrofit2.Response
import retrofit2.http.*

interface TransactionApi {

    @GET("entries")
    suspend fun getTransactions(): Response<List<Transaction>>

    @POST("entries")
    suspend fun addTransaction(
        @Body transaction: Transaction
    ): Response<Transaction>

    @DELETE("entries/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: Int
    ): Response<Unit>
}