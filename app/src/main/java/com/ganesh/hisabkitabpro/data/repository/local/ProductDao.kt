package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.*
import com.ganesh.hisabkitabpro.data.local.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET isDeleted = 1, isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteProduct(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query(
        "SELECT * FROM products " +
            "WHERE isDeleted = 0 AND (" +
            "name LIKE '%' || :query || '%' OR " +
            "category LIKE '%' || :query || '%' OR " +
            "barcode LIKE '%' || :query || '%' OR " +
            "sku LIKE '%' || :query || '%')" +
            "ORDER BY name COLLATE NOCASE ASC LIMIT :limit"
    )
    fun searchProducts(query: String, limit: Int = 50): Flow<List<ProductEntity>>

    @Query(
        "SELECT * FROM products " +
            "WHERE isDeleted = 0 AND (" +
            "name LIKE '%' || :query || '%' OR " +
            "category LIKE '%' || :query || '%' OR " +
            "barcode LIKE '%' || :query || '%' OR " +
            "sku LIKE '%' || :query || '%')" +
            "ORDER BY name COLLATE NOCASE ASC LIMIT :limit"
    )
    suspend fun searchProductsOnce(query: String, limit: Int = 50): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isDeleted = 0 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku AND isDeleted = 0 LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE name = :name COLLATE NOCASE AND isDeleted = 0 LIMIT 1")
    suspend fun getProductByExactName(name: String): ProductEntity?

    @Query(
        "SELECT * FROM products " +
            "WHERE isDeleted = 0 AND stockQuantity <= minStockLevel " +
            "ORDER BY (stockQuantity - minStockLevel) ASC, name COLLATE NOCASE ASC"
    )
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query(
        "SELECT * FROM products " +
            "WHERE isDeleted = 0 AND stockQuantity <= minStockLevel " +
            "ORDER BY (stockQuantity - minStockLevel) ASC, name COLLATE NOCASE ASC LIMIT :limit"
    )
    suspend fun getLowStockProductsOnce(limit: Int = 10): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0")
    fun observeProductCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(stockQuantity * purchasePrice), 0.0) FROM products WHERE isDeleted = 0")
    fun observeInventoryCostValue(): Flow<Double>

    @Query("SELECT COALESCE(SUM(stockQuantity * sellingPrice), 0.0) FROM products WHERE isDeleted = 0")
    fun observeInventorySaleValue(): Flow<Double>

    @Query(
        "UPDATE products SET stockQuantity = MAX(stockQuantity + :delta, 0), " +
            "updatedAt = :timestamp, lastStockSyncAt = :timestamp " +
            "WHERE id = :productId AND isDeleted = 0"
    )
    suspend fun adjustStockById(
        productId: String,
        delta: Double,
        timestamp: Long = System.currentTimeMillis()
    ): Int
}
