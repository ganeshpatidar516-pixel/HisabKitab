package com.ganesh.hisabkitabpro.network.api

import com.ganesh.hisabkitabpro.domain.model.Customer
import retrofit2.Response
import retrofit2.http.*

interface CustomerApi {

    @GET("api/v1/customers")
    suspend fun getCustomersV1(): Response<List<Customer>>

    @GET("customers")
    suspend fun getCustomers(): Response<List<Customer>>

    @POST("api/v1/customers")
    suspend fun addCustomerV1(
        @Body customer: Customer
    ): Response<Customer>

    @POST("customers")
    suspend fun addCustomer(
        @Body customer: Customer
    ): Response<Customer>

    @DELETE("api/v1/customers/{customer_id}")
    suspend fun deleteCustomerV1(
        @Path("customer_id") customerId: Int
    ): Response<Unit>

    @DELETE("customers/{customer_id}")
    suspend fun deleteCustomer(
        @Path("customer_id") customerId: Int
    ): Response<Unit>
}