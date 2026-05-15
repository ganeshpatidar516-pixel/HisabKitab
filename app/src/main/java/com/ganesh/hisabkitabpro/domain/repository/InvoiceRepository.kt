package com.ganesh.hisabkitabpro.domain.repository

import com.ganesh.hisabkitabpro.domain.model.Invoice
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    suspend fun saveInvoice(invoice: Invoice, customerId: String)
    fun getAllInvoices(): Flow<List<Invoice>>
    fun getInvoicesByCustomer(customerId: String): Flow<List<Invoice>>
    suspend fun getInvoiceById(invoiceId: String): Invoice?
}
