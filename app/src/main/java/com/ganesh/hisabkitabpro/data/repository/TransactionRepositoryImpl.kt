package com.ganesh.hisabkitabpro.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.repository.local.TransactionDao
import com.ganesh.hisabkitabpro.data.repository.local.BillDao
import com.ganesh.hisabkitabpro.data.repository.local.CustomerDao
import com.ganesh.hisabkitabpro.domain.invoice.InvoiceTemplateEngine
import com.ganesh.hisabkitabpro.domain.model.BillItem
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.model.Bill
import com.ganesh.hisabkitabpro.domain.model.TaxType
import com.ganesh.hisabkitabpro.domain.model.UniversalInvoice
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import com.ganesh.hisabkitabpro.domain.repository.CustomerFullData
import com.ganesh.hisabkitabpro.domain.repository.CreateBillResult
import com.ganesh.hisabkitabpro.domain.backup.CloudBackupManager
import com.ganesh.hisabkitabpro.domain.cloud.SelectiveCloudMirror
import com.ganesh.hisabkitabpro.domain.ledger.InvoicePdfGenerator
import com.ganesh.hisabkitabpro.domain.sync.SyncEngine
import com.ganesh.hisabkitabpro.addon.audit.AuditLogRecorder
import com.ganesh.hisabkitabpro.addon.reminder.CustomerPaymentReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong
import javax.inject.Inject

/**
 * HISABKITAB PRO - 🚀 ULTRA STABLE TRANSACTION REPOSITORY
 * Fixed "Freeze on Click" by deferring DAO and Database access to IO dispatcher.
 * Even with Lazy injection, calling DAO methods that return Flow on the Main thread
 * can trigger heavy SQLCipher/Room initialization synchronously.
 */
class TransactionRepositoryImpl @Inject constructor(
    private val databaseLazy: dagger.Lazy<AppDatabase>,
    private val transactionDaoLazy: dagger.Lazy<TransactionDao>,
    private val billDaoLazy: dagger.Lazy<BillDao>,
    private val customerDaoLazy: dagger.Lazy<CustomerDao>,
    private val cloudBackupManager: CloudBackupManager,
    private val auditLogRecorder: AuditLogRecorder,
    private val selectiveCloudMirror: SelectiveCloudMirror,
    @ApplicationContext private val context: Context
) : TransactionRepository {

    private val mutex = Mutex()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Helper to get DAOs lazily - Ensure these are only called within IO context
    private val transactionDao get() = transactionDaoLazy.get()
    private val database get() = databaseLazy.get()
    private val billDao get() = billDaoLazy.get()
    private val customerDao get() = customerDaoLazy.get()

    private suspend fun syncPaymentReminderAfterBalanceChange(customerId: Long) {
        CustomerPaymentReminderScheduler.syncAfterCustomerBalanceChange(context, database, customerId)
    }

    /**
     * ✅ PERFORMANCE FIX: Wrap DAO Flow calls in flow { emitAll(...) } 
     * This ensures Lazy initialization and DAO method execution happen on Dispatchers.IO.
     */
    override fun getRecentTransactions(): Flow<List<Transaction>> = flow {
        emitAll(transactionDao.getRecentActiveTransactions())
    }.flowOn(Dispatchers.IO)

    override fun getTransactionsPaged(): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = true),
            pagingSourceFactory = { transactionDao.getAllTransactionsPaging() }
        ).flow.flowOn(Dispatchers.IO)
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = flow {
        emitAll(transactionDao.getRecentActiveTransactions())
    }.flowOn(Dispatchers.IO)

    override fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>> = flow {
        emitAll(transactionDao.getTransactionsByCustomerId(customerId))
    }.flowOn(Dispatchers.IO)

    override fun getTransactionsByCustomerPaged(customerId: Long): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { transactionDao.getTransactionsPagingSource(customerId) }
        ).flow.flowOn(Dispatchers.IO)
    }

    override fun getTransactionById(id: Long): Flow<Transaction?> = flow {
        emitAll(transactionDao.getTransactionByIdFlow(id))
    }.flowOn(Dispatchers.IO)

    override fun getDeletedTransactions(): Flow<List<Transaction>> = flow {
        emitAll(transactionDao.getDeletedTransactions())
    }.flowOn(Dispatchers.IO)

    override fun getGlobalTotalGiven(): Flow<Long?> = flow {
        emitAll(transactionDao.getGlobalTotalGiven())
    }.flowOn(Dispatchers.IO)
    
    override fun getGlobalTotalReceived(): Flow<Long?> = flow {
        emitAll(transactionDao.getGlobalTotalReceived())
    }.flowOn(Dispatchers.IO)

    override suspend fun addTransaction(transaction: Transaction): Result<Long> {
        if (transaction.amount <= 0) return Result.failure(Exception("Invalid amount"))
        return try {
            mutex.withLock {
                val transactionId = withContext(Dispatchers.IO) {
                    val pendingTx = transaction.copy(syncStatus = "PENDING")
                    val id = database.withTransaction {
                        transactionDao.insertTransactionWithBalanceUpdate(pendingTx)
                    }
                    syncPaymentReminderAfterBalanceChange(transaction.customerId)
                    val persisted = pendingTx.copy(id = id)
                    SyncEngine.enqueueTransaction(persisted)
                    selectiveCloudMirror.mirrorTransaction(persisted)
                    id
                }
                auditLogRecorder.recordAsync("TRANSACTION", transactionId, "CREATE", null)
                repositoryScope.launch {
                    if (transaction.type == TransactionType.CREDIT || transaction.type == TransactionType.INVOICE) {
                        generatePdfForTransaction(transactionId)
                    }
                } // fire-and-forget for normal entries; createBill awaits PDF below
                Result.success(transactionId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generatePdfForTransaction(transactionId: Long): File? {
        return withContext(Dispatchers.IO) {
            val fullTransaction = transactionDao.getTransactionById(transactionId)
            if (fullTransaction != null) {
                val customer = customerDao.getCustomerById(fullTransaction.customerId)
                if (customer != null) {
                    return@withContext InvoicePdfGenerator.generateInvoicePDF(context, customer, fullTransaction)
                }
            }
            null
        }
    }

    private suspend fun generateProfessionalItemizedPdf(
        transactionId: Long,
        customerId: Long,
        items: List<BillItem>,
        businessProfile: BusinessProfile?,
        templateId: String,
        settingsGstEnabled: Boolean,
        settingsGstRatePercent: Double
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val customer = customerDao.getCustomerById(customerId) ?: return@withContext false
            val txn = transactionDao.getTransactionById(transactionId) ?: return@withContext false
            val invNo = txn.invoiceNo ?: "BILL-${txn.billId}"
            val applyGst = settingsGstEnabled && settingsGstRatePercent > 0.0
            val universal = UniversalInvoice(
                invoiceNumber = invNo,
                businessName = businessProfile?.businessName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "HisabKitab Pro",
                customerName = customer.name,
                items = items,
                timestamp = txn.createdAt,
                taxType = if (applyGst) TaxType.GST else TaxType.NONE,
                taxRate = if (applyGst) settingsGstRatePercent else 0.0
            )
            val legacy = InvoiceTemplateEngine.toLegacyInvoice(universal).copy(
                customerPhone = customer.phone.ifBlank { "—" },
                invoiceId = invNo
            )
            val path = try {
                InvoiceTemplateEngine.generatePdf(context, templateId, legacy, businessProfile)
            } catch (e: com.ganesh.hisabkitabpro.domain.invoice.PdfInvoiceGenerator.InvoicePdfLayoutException) {
                android.util.Log.w(
                    "TransactionRepository",
                    "Itemized PDF too large (${e.renderedItems}/${e.totalItems} items); falling back to receipt PDF",
                )
                return@withContext false
            }
            val src = File(path)
            if (!src.exists()) return@withContext false
            val dest = InvoicePdfGenerator.getInvoicePdfFile(context, transactionId)
            src.copyTo(dest, overwrite = true)
            src.delete()
            true
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        return try {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    val pendingTx = transaction.copy(syncStatus = "PENDING", updatedAt = System.currentTimeMillis())
                    database.withTransaction {
                        transactionDao.updateTransactionWithBalanceUpdate(pendingTx)
                    }
                    syncPaymentReminderAfterBalanceChange(transaction.customerId)
                    SyncEngine.enqueueTransaction(pendingTx)
                    selectiveCloudMirror.mirrorTransaction(pendingTx)
                }
                auditLogRecorder.recordAsync("TRANSACTION", transaction.id, "UPDATE", null)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction): Result<Unit> {
        return softDeleteTransaction(transaction.id)
    }

    override suspend fun softDeleteTransaction(transactionId: Long): Result<Unit> {
        return try {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    val customerId = database.withTransaction {
                        val tx = transactionDao.getTransactionById(transactionId) ?: return@withTransaction null
                        transactionDao.softDeleteWithBalanceUpdate(transactionId)
                        tx.customerId
                    }
                    InvoicePdfGenerator.getInvoicePdfFile(context, transactionId)
                        .takeIf { it.exists() }
                        ?.delete()
                    File(context.getExternalFilesDir(null), "INV_${transactionId}.pdf")
                        .takeIf { it.exists() }
                        ?.delete()
                    customerId?.let { syncPaymentReminderAfterBalanceChange(it) }
                    transactionDao.getTransactionById(transactionId)?.let {
                        SyncEngine.enqueueTransaction(it)
                        selectiveCloudMirror.mirrorTransaction(it)
                    }
                }
                auditLogRecorder.recordAsync("TRANSACTION", transactionId, "SOFT_DELETE", null)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreTransaction(transactionId: Long): Result<Unit> {
        return try {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    val customerId = database.withTransaction {
                        val tx = transactionDao.getTransactionById(transactionId)
                        transactionDao.restoreWithBalanceUpdate(transactionId)
                        tx?.customerId
                    }
                    customerId?.let { syncPaymentReminderAfterBalanceChange(it) }
                    transactionDao.getTransactionById(transactionId)?.let {
                        SyncEngine.enqueueTransaction(it)
                        selectiveCloudMirror.mirrorTransaction(it)
                    }
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markBillSentViaWhatsApp(transactionId: Long): Result<Unit> {
        return try {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    transactionDao.markWhatsAppBillSent(transactionId, System.currentTimeMillis())
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createBill(customerId: Long, totalAmount: Long, note: String?): Result<CreateBillResult> {
        if (totalAmount <= 0) return Result.failure(IllegalArgumentException("Invalid amount"))
        return try {
            val result = mutex.withLock {
                withContext(Dispatchers.IO) {
                    val pair = database.withTransaction {
                        val bill = Bill(customerId = customerId, totalAmount = totalAmount, status = "GENERATED")
                        val id = billDao.insertBill(bill)
                        val trimmed = note?.trim().orEmpty()
                        val noteText = buildString {
                            append("Bill #").append(id)
                            if (trimmed.isNotEmpty()) append(" — ").append(trimmed)
                        }
                        val txn = Transaction(
                            customerId = customerId,
                            amount = totalAmount,
                            type = TransactionType.INVOICE,
                            note = noteText,
                            billId = id,
                            invoiceNo = "BILL-$id",
                            txnRef = UUID.randomUUID().toString(),
                            syncStatus = "PENDING"
                        )
                        val txnId = transactionDao.insertTransactionWithBalanceUpdate(txn)
                        Pair(id, txnId)
                    }
                    syncPaymentReminderAfterBalanceChange(customerId)
                    transactionDao.getTransactionById(pair.second)?.let {
                        SyncEngine.enqueueTransaction(it)
                        selectiveCloudMirror.mirrorTransaction(it)
                    }
                    pair
                }
            }
            withContext(Dispatchers.IO) {
                generatePdfForTransaction(result.second)
            }
            Result.success(CreateBillResult(billId = result.first, transactionId = result.second))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createBillWithLineItems(
        customerId: Long,
        items: List<BillItem>,
        businessProfile: BusinessProfile?,
        pdfTemplateId: String,
        extraNote: String?,
        settingsGstEnabled: Boolean,
        settingsGstRatePercent: Double
    ): Result<CreateBillResult> {
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("Add at least one line item"))
        val subtotalRupee = items.sumOf { it.totalPrice }
        if (subtotalRupee <= 0) return Result.failure(IllegalArgumentException("Total must be positive"))
        val applyGst = settingsGstEnabled && settingsGstRatePercent > 0.0
        val taxRupee = if (applyGst) subtotalRupee * (settingsGstRatePercent / 100.0) else 0.0
        val grandTotalRupee = subtotalRupee + taxRupee
        if (grandTotalRupee <= 0) return Result.failure(IllegalArgumentException("Total must be positive"))
        val totalPaise = (grandTotalRupee * 100.0).roundToLong().coerceAtLeast(1L)
        val linesNote = items.joinToString("\n") { bi ->
            "${bi.name}\t${bi.quantity}\t${bi.price}\t${bi.totalPrice}"
        }
        val noteText = buildString {
            append("Items (name × qty × rate = total):\n").append(linesNote)
            if (applyGst) {
                append("\n\nSubtotal (taxable): ₹").append(String.format(Locale.US, "%.2f", subtotalRupee))
                append("\nGST @").append(String.format(Locale.US, "%.2f", settingsGstRatePercent)).append("%: ₹")
                    .append(String.format(Locale.US, "%.2f", taxRupee))
                append("\nGrand total (incl. GST): ₹").append(String.format(Locale.US, "%.2f", grandTotalRupee))
            }
            val extra = extraNote?.trim().orEmpty()
            if (extra.isNotEmpty()) append("\n\n").append(extra)
        }
        return try {
            val result = mutex.withLock {
                withContext(Dispatchers.IO) {
                    val pair = database.withTransaction {
                        val bill = Bill(customerId = customerId, totalAmount = totalPaise, status = "GENERATED")
                        val id = billDao.insertBill(bill)
                        val txn = Transaction(
                            customerId = customerId,
                            amount = totalPaise,
                            type = TransactionType.INVOICE,
                            note = "Bill #$id\n$noteText",
                            billId = id,
                            invoiceNo = "BILL-$id",
                            txnRef = UUID.randomUUID().toString(),
                            syncStatus = "PENDING"
                        )
                        val txnId = transactionDao.insertTransactionWithBalanceUpdate(txn)
                        Pair(id, txnId)
                    }
                    syncPaymentReminderAfterBalanceChange(customerId)
                    transactionDao.getTransactionById(pair.second)?.let {
                        SyncEngine.enqueueTransaction(it)
                        selectiveCloudMirror.mirrorTransaction(it)
                    }
                    pair
                }
            }
            withContext(Dispatchers.IO) {
                val pdfOk = generateProfessionalItemizedPdf(
                    transactionId = result.second,
                    customerId = customerId,
                    items = items,
                    businessProfile = businessProfile,
                    templateId = pdfTemplateId,
                    settingsGstEnabled = settingsGstEnabled,
                    settingsGstRatePercent = settingsGstRatePercent,
                )
                if (!pdfOk) {
                    generatePdfForTransaction(result.second)
                }
            }
            Result.success(CreateBillResult(billId = result.first, transactionId = result.second))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCalculateBalance(customerId: Long): Long {
        return withContext(Dispatchers.IO) { transactionDao.calculateBalance(customerId) }
    }

    override suspend fun getCustomerFull(customerId: Long, limit: Int, offset: Int): Result<CustomerFullData> {
        return try {
            withContext(Dispatchers.IO) {
                val transactions = transactionDao.getTransactionsPaged(customerId, limit, offset)
                val balance = transactionDao.calculateBalance(customerId)
                Result.success(CustomerFullData(customerId, balance, transactions))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generatePdf(transactionId: Long): Result<Unit> {
        repositoryScope.launch { generatePdfForTransaction(transactionId) }
        return Result.success(Unit)
    }

    override suspend fun shareTransaction(transactionId: Long): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val tx = transactionDao.getTransactionById(transactionId)
                    ?: return@withContext Result.failure(Exception("Transaction not found"))
                var pdfFile = InvoicePdfGenerator.resolveInvoicePdfFile(context, transactionId)
                if (pdfFile == null || !pdfFile.exists()) {
                    generatePdfForTransaction(transactionId)
                    pdfFile = InvoicePdfGenerator.resolveInvoicePdfFile(context, transactionId)
                }
                if (pdfFile == null || !pdfFile.exists()) {
                    return@withContext Result.failure(Exception("Unable to prepare PDF for sharing"))
                }
                auditLogRecorder.recordAsync("TRANSACTION", tx.id, "SHARE_READY", null)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setReminder(transactionId: Long): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val tx = transactionDao.getTransactionById(transactionId)
                    ?: return@withContext Result.failure(Exception("Transaction not found"))
                syncPaymentReminderAfterBalanceChange(tx.customerId)
                auditLogRecorder.recordAsync("TRANSACTION", tx.id, "REMINDER_REFRESHED", null)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun backupToCloud(): Result<Unit> {
        return withContext(Dispatchers.IO) { cloudBackupManager.backupDatabaseToDrive() }
    }
}
