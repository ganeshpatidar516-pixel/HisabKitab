package com.ganesh.hisabkitabpro.network.api

import com.ganesh.hisabkitabpro.domain.model.Invoice
import retrofit2.Response
import retrofit2.http.*

interface InvoiceApi {

    @GET("invoices")
    suspend fun getAllInvoices(): Response<List<Invoice>>

    @GET("invoices/{invoice_id}")
    suspend fun getInvoiceById(@Path("invoice_id") invoiceId: String): Response<Invoice>

    @POST("invoices")
    suspend fun createInvoice(@Body invoice: Invoice): Response<Invoice>

    @DELETE("invoices/{invoice_id}")
    suspend fun deleteInvoice(@Path("invoice_id") invoiceId: String): Response<Unit>
}