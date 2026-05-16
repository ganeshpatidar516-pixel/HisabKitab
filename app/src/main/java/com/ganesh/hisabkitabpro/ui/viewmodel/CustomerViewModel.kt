package com.ganesh.hisabkitabpro.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ganesh.hisabkitabpro.domain.customers.CustomerListMenuTab
import com.ganesh.hisabkitabpro.domain.customers.CustomerListReminderSegment
import com.ganesh.hisabkitabpro.domain.customers.CustomerListSortOption
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.repository.CUSTOMER_AI_SNAPSHOT_LIMIT
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.ui.customers.CustomerListFilterPrefs
import com.ganesh.hisabkitabpro.ui.customers.CustomerListFilterSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ganesh.hisabkitabpro.addon.reminder.ReminderAutomationPrefs
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * HISABKITAB PRO - 🚀 ULTRA SCALABLE CUSTOMER VIEWMODEL
 * Optimized for millions of users. Zero-lag architecture.
 */
@HiltViewModel
class CustomerViewModel @Inject constructor(
    val repository: CustomerRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class ReminderOverviewCustomer(
        val id: Long,
        val name: String,
        val phone: String,
        val balanceCache: Long,
        val autoReminderEnabled: Boolean
    )

    data class CustomerListOverview(
        val totalCustomers: Int = 0,
        val overallNetBalancePaise: Long = 0L,
        val remindedCustomers: Int = 0,
        val notRemindedCustomers: Int = 0,
        val remindersDueNow: Int = 0
    )

    private val _searchQuery = MutableStateFlow("")
    private val _menuTab = MutableStateFlow(CustomerListMenuTab.SORT_BY)
    private val _sortOption = MutableStateFlow(CustomerListSortOption.DEFAULT)
    private val _reminderSegments = MutableStateFlow<Set<CustomerListReminderSegment>>(emptySet())

    init {
        val s = CustomerListFilterPrefs.load(appContext)
        _menuTab.value = s.menuTab
        _sortOption.value = s.sortOption
        _reminderSegments.value = s.reminderSegments
    }

    val menuTab: StateFlow<CustomerListMenuTab> = _menuTab.asStateFlow()
    val sortOption: StateFlow<CustomerListSortOption> = _sortOption.asStateFlow()
    val reminderSegments: StateFlow<Set<CustomerListReminderSegment>> = _reminderSegments.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedCustomers: Flow<PagingData<Customer>> = combine(
        combine(_searchQuery, _menuTab) { q, tab -> q to tab },
        combine(_sortOption, _reminderSegments) { sort, segs -> sort to segs }
    ) { queryTab, sortSegs ->
        val (q, tab) = queryTab
        val (sort, segs) = sortSegs
        repository.getCustomersPaged(
            q.ifBlank { null },
            tab,
            sort,
            segs
        )
    }
        .flatMapLatest { it }
        .cachedIn(viewModelScope)

    val recentCustomers: StateFlow<List<Customer>> = repository.getRecentCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getCustomerByIdFlow(id: Long): Flow<Customer?> = repository.getCustomerByIdFlow(id)

    val customers: StateFlow<List<Customer>> = recentCustomers

  // Phase-10: removed hot allCustomers StateFlow (full-table subscription). Use pagedCustomers / searchCustomers.

    val remindedCustomerIds: StateFlow<Set<Long>> = repository.getDistinctRemindedCustomerIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val dueReminderCustomerIds: StateFlow<Set<Long>> = repository.getDueReminderCustomerIdsNow()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val customerListOverview: StateFlow<CustomerListOverview> = combine(
        repository.getCustomerCount(),
        repository.getOverallNetBalancePaise(),
        repository.getDistinctCustomersRemindedCount(),
        repository.getCustomerDueReminderCountNow()
    ) { totalCustomers, netBalance, reminded, dueNow ->
        val safeReminded = reminded.coerceAtMost(totalCustomers)
        CustomerListOverview(
            totalCustomers = totalCustomers,
            overallNetBalancePaise = netBalance,
            remindedCustomers = safeReminded,
            notRemindedCustomers = (totalCustomers - safeReminded).coerceAtLeast(0),
            remindersDueNow = dueNow.coerceAtLeast(0)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerListOverview())

    private fun toReminderOverview(customers: List<Customer>): List<ReminderOverviewCustomer> {
        val autoPilot = ReminderAutomationPrefs.isAutoPilotEnabled(appContext)
        return customers.map { c ->
            ReminderOverviewCustomer(
                id = c.id,
                name = c.name,
                phone = c.phone,
                balanceCache = c.balanceCache,
                autoReminderEnabled = autoPilot &&
                    ReminderAutomationPrefs.isCustomerReminderEnabled(appContext, c.id),
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val remindedCustomersOverview: StateFlow<List<ReminderOverviewCustomer>> =
        remindedCustomerIds.flatMapLatest { ids ->
            flow {
                val customers = withContext(Dispatchers.IO) {
                    repository.getCustomersByIds(ids.toList())
                }
                emit(toReminderOverview(customers))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notRemindedCustomersOverview: StateFlow<List<ReminderOverviewCustomer>> =
        remindedCustomerIds.flatMapLatest { reminded ->
            flow {
                val customers = withContext(Dispatchers.IO) {
                    val allIds = repository.getAllCustomerIds()
                    val notRemindedIds = allIds.filterNot { reminded.contains(it) }
                    repository.getCustomersByIds(notRemindedIds)
                }
                emit(toReminderOverview(customers))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val dueNowCustomersOverview: StateFlow<List<ReminderOverviewCustomer>> =
        dueReminderCustomerIds.flatMapLatest { ids ->
            flow {
                val customers = withContext(Dispatchers.IO) {
                    repository.getCustomersByIds(ids.toList())
                }
                emit(toReminderOverview(customers))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bulkReminderDebtors: StateFlow<List<Customer>> = flow {
        emit(
            withContext(Dispatchers.IO) {
                repository.getDebtors().sortedWith(
                    compareByDescending<Customer> { it.balanceCache }
                        .thenBy { it.name.lowercase() },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assistantCustomerNames: StateFlow<List<String>> = flow {
        emit(
            withContext(Dispatchers.IO) {
                repository.getCustomerNamesLimited(CUSTOMER_AI_SNAPSHOT_LIMIT)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val autoOffCustomerCount: StateFlow<Int> = combine(
        remindedCustomerIds,
        flow {
            emit(withContext(Dispatchers.IO) { repository.getAllCustomerIds() })
        },
    ) { reminded, allIds ->
        val autoPilot = ReminderAutomationPrefs.isAutoPilotEnabled(appContext)
        if (!autoPilot) {
            allIds.size
        } else {
            allIds.count { id ->
                !ReminderAutomationPrefs.isCustomerReminderEnabled(appContext, id)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markCustomerReminderSent(customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markReminderSent(customerId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun applyListSort(
        menuTab: CustomerListMenuTab,
        sort: CustomerListSortOption,
        reminderSegments: Set<CustomerListReminderSegment>
    ) {
        _menuTab.value = menuTab
        _sortOption.value = sort
        _reminderSegments.value = reminderSegments
        CustomerListFilterPrefs.save(
            appContext,
            CustomerListFilterSettings(menuTab, sort, reminderSegments)
        )
    }

    fun addCustomer(name: String, phone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = Customer(name = name, phone = phone)
            repository.addCustomer(customer)
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomer(customerId)
        }
    }
}
