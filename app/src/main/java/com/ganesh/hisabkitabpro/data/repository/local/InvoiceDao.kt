package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.*
import com.ganesh.hisabkitabpro.data.local.InvoiceEntity
import com.ganesh.hisabkitabpro.data.local.InvoiceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoices WHERE invoiceId = :invoiceId AND isDeleted = 0")
    suspend fun getInvoiceById(invoiceId: String): InvoiceEntity?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId AND isDeleted = 0")
    suspend fun getItemsForInvoice(invoiceId: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoices WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("UPDATE invoices SET isDeleted = 1, updatedAt = :timestamp WHERE invoiceId = :invoiceId")
    suspend fun softDeleteInvoice(invoiceId: String, timestamp: Long = System.currentTimeMillis())
}
