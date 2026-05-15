package com.ganesh.hisabkitabpro.domain.inventory

import com.ganesh.hisabkitabpro.data.local.ProductEntity
import com.ganesh.hisabkitabpro.data.local.ProductDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryManager @Inject constructor(
    private val productDao: ProductDao
) {
    fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()

    fun getLowStockProducts(): Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    suspend fun addProduct(product: ProductEntity) {
        productDao.insertProduct(product.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateStock(productId: String, quantityDelta: Double) {
        productDao.adjustStockById(productId, quantityDelta)
    }

    suspend fun findByBarcodeOrSku(rawCode: String): ProductEntity? {
        val code = rawCode.trim()
        if (code.isBlank()) return null
        return productDao.getProductByBarcode(code) ?: productDao.getProductBySku(code)
    }

    suspend fun findByName(query: String): List<ProductEntity> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        return productDao.searchProductsOnce(q, limit = 10)
    }
}
