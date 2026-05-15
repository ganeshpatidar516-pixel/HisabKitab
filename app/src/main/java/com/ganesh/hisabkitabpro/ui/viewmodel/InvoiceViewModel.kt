package com.ganesh.hisabkitabpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.domain.model.Invoice
import com.ganesh.hisabkitabpro.domain.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    fun saveInvoice(invoice: Invoice, customerId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            invoiceRepository.saveInvoice(invoice, customerId)
            onComplete()
        }
    }
}
