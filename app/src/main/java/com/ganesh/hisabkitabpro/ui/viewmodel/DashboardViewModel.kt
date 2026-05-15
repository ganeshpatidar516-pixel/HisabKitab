package com.ganesh.hisabkitabpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.domain.engine.CustomerBalanceEngine
import com.ganesh.hisabkitabpro.domain.engine.TransactionCalculator
import com.ganesh.hisabkitabpro.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Unified DashboardState moved to TransactionViewModel.kt to avoid redeclaration.
// Using TransactionViewModel.DashboardState instead.

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun updateDashboard(transactions: List<Transaction>) {
        viewModelScope.launch(Dispatchers.Default) {
            val totalCustomers = CustomerBalanceEngine.totalCustomers(transactions)
            val totalSuppliers = 0 

            val totalUdhaar = TransactionCalculator.totalDebit(transactions)
            val totalJama = TransactionCalculator.totalCredit(transactions)

            val balance = totalJama - totalUdhaar

            _state.value = DashboardState(
                totalCustomers = totalCustomers,
                totalSuppliers = totalSuppliers,
                totalUdhaar = totalUdhaar,
                totalJama = totalJama,
                balance = balance
            )
        }
    }
}
