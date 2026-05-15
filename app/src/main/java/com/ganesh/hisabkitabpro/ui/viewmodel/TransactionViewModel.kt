package com.ganesh.hisabkitabpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ganesh.hisabkitabpro.domain.model.BillItem
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.inventory.InventoryBillingSync
import com.ganesh.hisabkitabpro.domain.repository.CreateBillResult
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.PartyRepository
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class DashboardState(
    val totalCustomers: Int = 0,
    val totalSuppliers: Int = 0,
    val totalUdhaar: Double = 0.0,
    val totalJama: Double = 0.0,
    val totalSupplierPayable: Double = 0.0,
    val netExposure: Double = 0.0,
    val paymentPriorityHint: String = "",
    /** Customer net receivable in rupees (derived from [overallCustomerNetBalancePaise]). */
    val balance: Double = 0.0,
    /** Same source as Mere Grahak: SUM(customers.balanceCache) in paise — updates with every ledger change. */
    val overallCustomerNetBalancePaise: Long = 0L,
    val advice: List<String> = emptyList()
)

data class TransactionUiState(
    val balance: Long = 0,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/** OCR scan → add-entry screen (Wave 7: [lowConfidenceAmount] when amount heuristic was LOW). */
data class PendingOcrScanPrefill(
    val amountKeypadText: String,
    val note: String,
    val lowConfidenceAmount: Boolean = false,
    val billImageUri: String? = null,
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val customerRepository: CustomerRepository,
    private val partyRepository: PartyRepository,
    private val inventoryBillingSync: InventoryBillingSync
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    val recentTransactions: StateFlow<List<Transaction>> = repository.getRecentTransactions()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = recentTransactions

    val pagedTransactions: Flow<PagingData<Transaction>> = repository.getTransactionsPaged()
        .cachedIn(viewModelScope)

    /** OCR scan → add-entry screen. */
    private val _pendingScanPrefill = MutableStateFlow<PendingOcrScanPrefill?>(null)
    val pendingScanPrefill: StateFlow<PendingOcrScanPrefill?> = _pendingScanPrefill.asStateFlow()

    fun setPendingScanPrefill(
        amountKeypadText: String,
        note: String,
        lowConfidenceAmount: Boolean = false,
        billImageUri: String? = null,
    ) {
        _pendingScanPrefill.value = PendingOcrScanPrefill(
            amountKeypadText,
            note,
            lowConfidenceAmount,
            billImageUri,
        )
    }

    fun consumePendingScanPrefill() {
        _pendingScanPrefill.value = null
    }

    private val _customerPhoneForBill = MutableStateFlow("")
    val customerPhoneForBill: StateFlow<String> = _customerPhoneForBill.asStateFlow()

    fun refreshCustomerPhoneForBill(customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val c = customerRepository.getCustomerById(customerId)
            _customerPhoneForBill.value = c?.phone?.trim().orEmpty()
        }
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                combine(
                    combine(
                        repository.getGlobalTotalGiven().onStart { emit(0L) }.distinctUntilChanged(),
                        repository.getGlobalTotalReceived().onStart { emit(0L) }.distinctUntilChanged(),
                        customerRepository.getCustomerCount().onStart { emit(0) }.distinctUntilChanged(),
                    ) { given, received, customerCount ->
                        Triple(given ?: 0L, received ?: 0L, customerCount)
                    },
                    partyRepository.getParties(isSupplier = true).onStart { emit(emptyList()) }
                        .distinctUntilChanged(),
                    partyRepository.observeActiveSuppliersTotalBalancePaise().onStart { emit(0L) }
                        .distinctUntilChanged(),
                    customerRepository.getOverallNetBalancePaise().onStart { emit(0L) }.distinctUntilChanged(),
                ) { triple, suppliers, supplierPayablePaise, overallCustomerNetPaise ->
                    val (givenPaise, receivedPaise, customerCount) = triple
                    val totalG = givenPaise / 100.0
                    val totalR = receivedPaise / 100.0
                    val totalPayableRupees = supplierPayablePaise / 100.0
                    val topSupplier = suppliers.maxByOrNull { it.totalBalance }
                    val customerNetRupees = overallCustomerNetPaise / 100.0

                    DashboardState(
                        totalCustomers = customerCount,
                        totalSuppliers = suppliers.size,
                        totalUdhaar = totalG,
                        totalJama = totalR,
                        totalSupplierPayable = totalPayableRupees,
                        netExposure = totalG + totalPayableRupees - totalR,
                        paymentPriorityHint = buildString {
                            if (totalPayableRupees > 0 && topSupplier != null) {
                                append("Priority: ${topSupplier.name} (")
                                append(String.format("%.2f", topSupplier.totalBalance / 100.0))
                                append(") - pay early for possible 1% discount.")
                            } else {
                                append("No supplier payable pending today.")
                            }
                        },
                        balance = customerNetRupees,
                        overallCustomerNetBalancePaise = overallCustomerNetPaise,
                        advice = listOf("Business is stable.", "Keep tracking your daily entries.")
                    )
                }
                    .flowOn(Dispatchers.IO)
                    .conflate()
                    .collect { newState ->
                        _dashboardState.value = newState
                    }
            } catch (e: Throwable) {
                // Defense-in-depth: log only the exception type, not the throwable
                // (a downstream Flow operator may carry transaction values inside
                // its exception message; we never want them to reach logcat).
                Log.e("TransactionViewModel", "Dashboard state aggregation failed; keeping last safe state: ${e::class.java.simpleName}")
            }
        }
    }

    fun loadCustomerData(customerId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                repository.getCustomerFull(customerId, 50, 0)
            }
            result.onSuccess { data ->
                _uiState.update { it.copy(
                    balance = data.balance,
                    transactions = data.transactions,
                    isLoading = false
                ) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun getTransactionsByCustomerPaged(customerId: Long): Flow<PagingData<Transaction>> {
        // Repository already uses flowOn(IO). Extra flowOn after cachedIn breaks Paging caching semantics.
        return repository.getTransactionsByCustomerPaged(customerId).cachedIn(viewModelScope)
    }

    /** P1: bounded export for shared khata / reminders (not global recent-50). */
    suspend fun getCustomerTransactionsSnapshot(customerId: Long, limit: Int = 500): List<Transaction> =
        withContext(Dispatchers.IO) {
            repository.getCustomerFull(customerId, limit, 0).getOrNull()?.transactions.orEmpty()
        }

    fun getTransactionById(id: Long): Flow<Transaction?> {
        return repository.getTransactionById(id).flowOn(Dispatchers.IO)
    }

    private val addTransactionInFlight = AtomicBoolean(false)

    fun addTransaction(
        customerId: Long,
        amountPaise: Long,
        type: TransactionType,
        note: String? = null,
        onComplete: (transactionId: Long) -> Unit = {},
        onError: () -> Unit = {},
    ) {
        if (!addTransactionInFlight.compareAndSet(false, true)) {
            onError()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transaction = Transaction(
                    customerId = customerId,
                    amount = amountPaise,
                    type = type,
                    note = note,
                    txnRef = UUID.randomUUID().toString(),
                )
                val result = repository.addTransaction(transaction)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        onComplete(result.getOrDefault(0L))
                    } else {
                        onError()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onError() }
            } finally {
                addTransactionInFlight.set(false)
            }
        }
    }

    /**
     * Creates a professional bill (INVOICE) in the ledger, generates PDF on disk, returns ids for share/reminder.
     */
    fun markBillSentViaWhatsApp(transactionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markBillSentViaWhatsApp(transactionId)
        }
    }

    fun createBill(
        customerId: Long,
        totalAmountPaise: Long,
        note: String?,
        onResult: (Result<CreateBillResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.createBill(customerId, totalAmountPaise, note)
            }
            onResult(result)
        }
    }

    fun createBillWithLineItems(
        customerId: Long,
        items: List<BillItem>,
        businessProfile: BusinessProfile?,
        pdfTemplateId: String,
        extraNote: String?,
        settingsGstEnabled: Boolean,
        settingsGstRatePercent: Double,
        onResult: (Result<CreateBillResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.createBillWithLineItems(
                    customerId,
                    items,
                    businessProfile,
                    pdfTemplateId,
                    extraNote,
                    settingsGstEnabled,
                    settingsGstRatePercent
                )
            }
            if (result.isSuccess) {
                viewModelScope.launch(Dispatchers.IO) {
                    inventoryBillingSync.syncSoldBillItemsByName(items)
                }
            }
            onResult(result)
        }
    }

    fun deleteTransaction(customerId: Long, transactionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.softDeleteTransaction(transactionId)
        }
    }

    /** Add-on: metadata only; balance math unchanged. */
    fun setSettlementKind(transactionId: Long, kind: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val t = repository.getTransactionById(transactionId).first() ?: return@launch
            repository.updateTransaction(t.copy(settlementKind = kind))
        }
    }

    fun restoreTransaction(transactionId: Long, customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreTransaction(transactionId)
        }
    }
}
