package com.ganesh.hisabkitabpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ganesh.hisabkitabpro.domain.inventory.InventoryBillingSync
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.PartyRepository

class TransactionViewModelFactory(
    private val repository: TransactionRepository,
    private val customerRepository: CustomerRepository,
    private val partyRepository: PartyRepository,
    private val inventoryBillingSync: InventoryBillingSync
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(
                repository,
                customerRepository,
                partyRepository,
                inventoryBillingSync
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
