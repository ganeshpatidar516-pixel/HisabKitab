package com.ganesh.hisabkitabpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.data.repository.local.ProductDao
import com.ganesh.hisabkitabpro.data.local.ProductEntity
import com.ganesh.hisabkitabpro.domain.inventory.InventoryScanNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventorySummaryUiState(
    val productCount: Int = 0,
    val costValue: Double = 0.0,
    val saleValue: Double = 0.0,
    val lowStockCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productDao: ProductDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _scannedCode = MutableStateFlow<String?>(null)
    val scannedCode: StateFlow<String?> = _scannedCode.asStateFlow()

    val products: StateFlow<List<ProductEntity>> = _searchQuery
        .flatMapLatest { q ->
            val query = q.trim()
            when {
                query.isBlank() -> productDao.getAllProducts()
                query.equals("low stock", ignoreCase = true) -> productDao.getLowStockProducts()
                else -> productDao.searchProducts(query, limit = 100)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = productDao.getLowStockProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<InventorySummaryUiState> = combine(
        productDao.observeProductCount(),
        productDao.observeInventoryCostValue(),
        productDao.observeInventorySaleValue(),
        lowStockProducts
    ) { count, cost, sale, low ->
        InventorySummaryUiState(
            productCount = count,
            costValue = cost,
            saleValue = sale,
            lowStockCount = low.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventorySummaryUiState())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.insertProduct(product.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.softDeleteProduct(product.id)
        }
    }

    fun adjustStock(productId: String, delta: Double) {
        viewModelScope.launch {
            productDao.adjustStockById(productId, delta)
        }
    }

    fun handleScannedRawValue(rawValue: String, onResolved: (ProductEntity?) -> Unit) {
        viewModelScope.launch {
            val code = InventoryScanNormalizer.normalize(rawValue)
            _scannedCode.value = code.ifBlank { null }
            val resolved = if (code.isBlank()) {
                null
            } else {
                productDao.getProductByBarcode(code) ?: productDao.getProductBySku(code)
            }
            onResolved(resolved)
        }
    }

    fun consumeScannedCode() {
        _scannedCode.value = null
    }

    fun validateProductDraft(
        name: String,
        stock: String,
        sellingPrice: String,
        purchasePrice: String,
        minStock: String
    ): String? {
        if (name.trim().isBlank()) return "Product name is required."
        if ((stock.toDoubleOrNull() ?: -1.0) < 0.0) return "Enter valid stock value."
        if ((sellingPrice.toDoubleOrNull() ?: -1.0) < 0.0) return "Enter valid selling price."
        if ((purchasePrice.toDoubleOrNull() ?: -1.0) < 0.0) return "Enter valid purchase price."
        if ((minStock.toDoubleOrNull() ?: -1.0) < 0.0) return "Enter valid low-stock level."
        return null
    }
}
