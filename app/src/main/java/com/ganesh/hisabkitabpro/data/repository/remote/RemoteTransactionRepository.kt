package com.ganesh.hisabkitabpro.data.repository.remote

import androidx.paging.PagingData
import com.ganesh.hisabkitabpro.domain.model.BillItem
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import com.ganesh.hisabkitabpro.domain.repository.CustomerFullData
import com.ganesh.hisabkitabpro.domain.repository.CreateBillResult
import com.ganesh.hisabkitabpro.network.api.TransactionApi
import com.ganesh.hisabkitabpro.network.api.toSyncRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.max
import javax.inject.Inject

class RemoteTransactionRepository @Inject constructor(
    private val api: TransactionApi
) : TransactionRepository {

    data class CapabilityFlags(
        val supportsRemoteRestore: Boolean,
        val supportsRemoteBackup: Boolean,
        val supportsServerGeneratedBillLifecycle: Boolean
    )

    companion object {
        val CAPABILITIES = CapabilityFlags(
            supportsRemoteRestore = false,
            supportsRemoteBackup = false,
            supportsServerGeneratedBillLifecycle = true
        )
    }

    private fun <T> unsupported(operation: String): Result<T> {
        return Result.failure(
            UnsupportedOperationException(
                "$operation is not supported by current remote backend capability."
            )
        )
    }

    private suspend fun fetchAllTransactionsSafe(): List<Transaction> {
        val fromTypedEndpoint = runCatching { api.getAllTransactions().transactions }.getOrNull()
        if (fromTypedEndpoint != null) return fromTypedEndpoint
        return runCatching { api.getTransactions("all") }.getOrDefault(emptyList())
    }

    override fun getRecentTransactions(): Flow<List<Transaction>> = flow {
        val data = fetchAllTransactionsSafe()
            .sortedByDescending { it.createdAt }
            .take(50)
        emit(data)
    }

    override fun getTransactionsPaged(): Flow<PagingData<Transaction>> = flow {
        val data = fetchAllTransactionsSafe().sortedByDescending { it.createdAt }
        emit(PagingData.from(data))
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = flow {
        val data = fetchAllTransactionsSafe()
        emit(data)
    }

    override fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>> = flow {
        val data = api.getTransactions(customerId.toString())
        emit(data)
    }

    override fun getTransactionsByCustomerPaged(customerId: Long): Flow<PagingData<Transaction>> = flow {
        val data = runCatching { api.getTransactions(customerId.toString()) }.getOrDefault(emptyList())
        emit(PagingData.from(data.sortedByDescending { it.createdAt }))
    }

    override fun getTransactionById(id: Long): Flow<Transaction?> = flow {
        val item = fetchAllTransactionsSafe().firstOrNull { it.id == id }
        emit(item)
    }

    override fun getDeletedTransactions(): Flow<List<Transaction>> = flow {
        emit(fetchAllTransactionsSafe().filter { it.isDeleted })
    }

    override fun getGlobalTotalGiven(): Flow<Long?> = flow {
        val given = fetchAllTransactionsSafe()
            .filter { !it.isDeleted && (it.type == TransactionType.CREDIT || it.type == TransactionType.INVOICE) }
            .sumOf { it.amount }
        emit(given)
    }

    override fun getGlobalTotalReceived(): Flow<Long?> = flow {
        val received = fetchAllTransactionsSafe()
            .filter { !it.isDeleted && (it.type == TransactionType.DEBIT || it.type == TransactionType.PAYMENT) }
            .sumOf { it.amount }
        emit(received)
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Long> {
        return try {
            val response = api.addTransaction(transaction.toSyncRequest())
            if (response.isSuccessful) {
                Result.success(transaction.id)
            } else {
                Result.failure(Exception("Add transaction failed with code ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        return try {
            api.updateTransaction(transaction.id, transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction): Result<Unit> {
        return softDeleteTransaction(transaction.id)
    }

    override suspend fun softDeleteTransaction(transactionId: Long): Result<Unit> {
        return try {
            val response = api.deleteTransaction(transactionId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete transaction failed with code ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreTransaction(transactionId: Long): Result<Unit> {
        return unsupported("restoreTransaction")
    }

    override suspend fun createBill(customerId: Long, totalAmount: Long, note: String?): Result<CreateBillResult> {
        val tx = Transaction(
            customerId = customerId,
            amount = totalAmount,
            type = TransactionType.INVOICE,
            note = note
        )
        return addTransaction(tx).map { txId ->
            CreateBillResult(
                billId = max(1L, txId),
                transactionId = txId
            )
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
        val subtotalPaise = items.sumOf { (it.totalPrice * 100.0).toLong() }
        val gstPaise = if (settingsGstEnabled && settingsGstRatePercent > 0.0) {
            ((subtotalPaise * settingsGstRatePercent) / 100.0).toLong()
        } else {
            0L
        }
        val finalAmount = subtotalPaise + gstPaise
        return createBill(
            customerId = customerId,
            totalAmount = finalAmount,
            note = extraNote
        )
    }

    override suspend fun markBillSentViaWhatsApp(transactionId: Long): Result<Unit> {
        return runCatching {
            val tx = fetchAllTransactionsSafe().firstOrNull { it.id == transactionId }
                ?: error("Transaction not found")
            api.updateTransaction(
                id = transactionId,
                transaction = tx.copy(whatsappSentAt = System.currentTimeMillis())
            )
            Unit
        }
    }

    override suspend fun getCalculateBalance(customerId: Long): Long {
        val txs = runCatching { api.getTransactions(customerId.toString()) }.getOrDefault(emptyList())
        return txs.filter { !it.isDeleted }.fold(0L) { acc, tx ->
            when (tx.type) {
                TransactionType.CREDIT, TransactionType.INVOICE -> acc + tx.amount
                TransactionType.DEBIT, TransactionType.PAYMENT -> acc - tx.amount
                TransactionType.ADJUSTMENT -> acc
            }
        }
    }

    override suspend fun getCustomerFull(customerId: Long, limit: Int, offset: Int): Result<CustomerFullData> {
        return runCatching {
            val all = runCatching { api.getTransactions(customerId.toString()) }.getOrDefault(emptyList())
            val slice = all
                .sortedByDescending { it.createdAt }
                .drop(offset)
                .take(limit)
            CustomerFullData(
                customerId = customerId,
                balance = getCalculateBalance(customerId),
                transactions = slice
            )
        }
    }

    override suspend fun generatePdf(transactionId: Long): Result<Unit> {
        return try {
            val response = api.generatePdf(transactionId.toString())
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("PDF Error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun shareTransaction(transactionId: Long): Result<Unit> {
        return try {
            val response = api.shareTransaction(transactionId.toString())
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Share Error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setReminder(transactionId: Long): Result<Unit> {
        return try {
            val response = api.setReminder(transactionId.toString())
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Reminder Error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun backupToCloud(): Result<Unit> =
        unsupported("backupToCloud")
}
