package com.ganesh.hisabkitabpro.domain.repository

import androidx.paging.PagingData
import com.ganesh.hisabkitabpro.domain.model.BillItem
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/** Result of creating a ledger bill (invoice) entry plus linked transaction id for PDF/share. */
data class CreateBillResult(
    val billId: Long,
    val transactionId: Long,
    /** False when ledger saved but receipt/itemized PDF could not be written. */
    val pdfReady: Boolean = true,
)

interface TransactionRepository {
    // ✅ ZERO-LAG: Get only recent for quick UI preview
    fun getRecentTransactions(): Flow<List<Transaction>>
    
    // ✅ PAGINATION: For large lists without freezing
    fun getTransactionsPaged(): Flow<PagingData<Transaction>>

    fun getAllTransactions(): Flow<List<Transaction>>

    fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>>
    
    fun getTransactionsByCustomerPaged(customerId: Long): Flow<PagingData<Transaction>>
    
    fun getTransactionById(id: Long): Flow<Transaction?>

    fun getDeletedTransactions(): Flow<List<Transaction>>

    fun getGlobalTotalGiven(): Flow<Long?>
    fun getGlobalTotalReceived(): Flow<Long?>

    suspend fun addTransaction(transaction: Transaction): Result<Long>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(transaction: Transaction): Result<Unit>
    suspend fun softDeleteTransaction(transactionId: Long): Result<Unit>
    suspend fun restoreTransaction(transactionId: Long): Result<Unit>
    suspend fun createBill(customerId: Long, totalAmount: Long, note: String? = null): Result<CreateBillResult>
    /** Ledger bill with line items; PDF uses [pdfTemplateId] (TemplateRegistry, e.g. TEMP_ROYAL). */
    suspend fun createBillWithLineItems(
        customerId: Long,
        items: List<BillItem>,
        businessProfile: BusinessProfile?,
        pdfTemplateId: String,
        extraNote: String?,
        /** From [com.ganesh.hisabkitabpro.domain.model.AppSettings.gstEnabled]: add GST on subtotal + PDF. */
        settingsGstEnabled: Boolean = false,
        /** From [com.ganesh.hisabkitabpro.domain.model.AppSettings.gstRate] (percent, e.g. 18.0). */
        settingsGstRatePercent: Double = 0.0
    ): Result<CreateBillResult>
    /** Records that the user sent this bill PDF via WhatsApp (local ledger / timeline). */
    suspend fun markBillSentViaWhatsApp(transactionId: Long): Result<Unit>
    suspend fun getCalculateBalance(customerId: Long): Long
    suspend fun getCustomerFull(customerId: Long, limit: Int, offset: Int): Result<CustomerFullData>
    suspend fun generatePdf(transactionId: Long): Result<Unit>
    suspend fun shareTransaction(transactionId: Long): Result<Unit>
    suspend fun setReminder(transactionId: Long): Result<Unit>
    suspend fun backupToCloud(): Result<Unit>
}

data class CustomerFullData(
    val customerId: Long,
    val balance: Long,
    val transactions: List<Transaction>
)
