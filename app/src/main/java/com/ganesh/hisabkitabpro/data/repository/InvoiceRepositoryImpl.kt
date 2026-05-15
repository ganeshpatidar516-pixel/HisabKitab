package com.ganesh.hisabkitabpro.data.repository

import android.content.Context
import com.ganesh.hisabkitabpro.addon.reminder.CustomerPaymentReminderScheduler
import com.ganesh.hisabkitabpro.core.database.safeDatabaseCall
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.local.InvoiceEntity
import com.ganesh.hisabkitabpro.data.local.InvoiceItemEntity
import com.ganesh.hisabkitabpro.data.repository.local.InvoiceDao
import com.ganesh.hisabkitabpro.data.repository.local.TransactionDao
import com.ganesh.hisabkitabpro.domain.model.Invoice
import com.ganesh.hisabkitabpro.domain.model.InvoiceItem
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.repository.InvoiceRepository
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class InvoiceRepositoryImpl @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val transactionDao: TransactionDao,
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository,
    @ApplicationContext private val context: Context,
    private val appDatabase: Lazy<AppDatabase>
) : InvoiceRepository {

    override suspend fun saveInvoice(invoice: Invoice, customerId: String) {
        val custIdLong = customerId.toLongOrNull() ?: 0L
        safeDatabaseCall("SaveInvoice") {
            val entity = InvoiceEntity(
                invoiceId = invoice.invoiceId,
                customerId = customerId,
                customerName = invoice.customerName,
                date = invoice.date,
                subtotal = invoice.subtotal,
                gstEnabled = invoice.gstEnabled,
                gstRate = invoice.gstPercent,
                gstAmount = invoice.gstAmount,
                totalAmount = invoice.finalAmount,
                discountAmount = invoice.discount,
                status = invoice.paymentStatus,
                createdAt = System.currentTimeMillis()
            )

            val items = invoice.items.map {
                InvoiceItemEntity(
                    invoiceId = invoice.invoiceId,
                    itemName = it.itemName,
                    quantity = it.quantity,
                    unitPrice = it.rate,
                    totalPrice = it.total
                )
            }

            // 🔥 Fix: Create a transaction and use the Atomic balance update
            val transaction = Transaction(
                customerId = custIdLong,
                amount = (invoice.finalAmount * 100).toLong(), // Convert to Paise (Blueprint Rule 9)
                type = TransactionType.INVOICE,
                note = "Invoice #${invoice.invoiceId} generated",
                txnRef = UUID.randomUUID().toString(),
                uniqueHash = "INV_${invoice.invoiceId}", // Use invoice ID as hash to prevent duplicates
                createdAt = invoice.date
            )

            invoiceDao.insertInvoice(entity)
            invoiceDao.insertInvoiceItems(items)
            
            // This method automatically updates the customer's balance cache
            transactionDao.insertTransactionWithBalanceUpdate(transaction)
            if (custIdLong > 0L) {
                CustomerPaymentReminderScheduler.syncAfterCustomerBalanceChange(
                    context,
                    appDatabase.get(),
                    custIdLong
                )
            }
        }
    }

    override fun getAllInvoices(): Flow<List<Invoice>> {
        return invoiceDao.getAllInvoices().map { entities ->
            entities.map { it.toDomain(emptyList()) }
        }
    }

    override fun getInvoicesByCustomer(customerId: String): Flow<List<Invoice>> {
        return invoiceDao.getAllInvoices().map { entities -> 
            entities.filter { it.customerId == customerId }.map { it.toDomain(emptyList()) }
        }
    }

    override suspend fun getInvoiceById(invoiceId: String): Invoice? {
        return safeDatabaseCall("GetInvoiceById") {
            invoiceDao.getInvoiceById(invoiceId)?.toDomain(emptyList())
        }
    }

    private fun InvoiceEntity.toDomain(items: List<InvoiceItem>): Invoice {
        return Invoice(
            invoiceId = invoiceId,
            customerName = customerName,
            customerPhone = "",
            customerAddress = "",
            items = items,
            subtotal = subtotal,
            gstEnabled = gstEnabled,
            gstPercent = gstRate,
            gstAmount = gstAmount,
            taxLineLabel = null,
            discount = discountAmount,
            finalAmount = totalAmount,
            paymentStatus = status,
            paymentMethod = "CASH",
            date = date,
            notes = null
        )
    }
}
