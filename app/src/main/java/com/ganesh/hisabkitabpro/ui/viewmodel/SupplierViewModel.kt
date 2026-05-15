package com.ganesh.hisabkitabpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ganesh.hisabkitabpro.domain.model.Supplier
import com.ganesh.hisabkitabpro.domain.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierUiState(
    val totalPayable: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val repository: SupplierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierUiState())
    val uiState: StateFlow<SupplierUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val suppliers: Flow<PagingData<Supplier>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) repository.getSuppliersPaged()
            else repository.searchSuppliers(query)
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            repository.getGlobalTotalPayable().collect { total ->
                _uiState.update { it.copy(totalPayable = (total ?: 0L) / 100.0) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addSupplier(name: String, phone: String, address: String? = null) {
        viewModelScope.launch {
            try {
                val supplier = Supplier(name = name, phone = phone, address = address)
                repository.addSupplier(supplier)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add supplier: ${e.message}") }
            }
        }
    }

    fun deleteSupplier(supplierId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteSupplier(supplierId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete supplier: ${e.message}") }
            }
        }
    }
}
