package com.ganesh.hisabkitabpro.domain.inventory

import android.util.Log
import com.ganesh.hisabkitabpro.data.local.ProductDao
import com.ganesh.hisabkitabpro.domain.model.BillItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Best-effort stock decrement after a bill is created successfully.
 *
 * This class is intentionally isolated from transaction creation. It never
 * changes bill/ledger results and never throws back into the billing flow.
 */
@Singleton
class InventoryBillingSync @Inject constructor(
    private val productDao: ProductDao
) {
    suspend fun syncSoldBillItemsByName(items: List<BillItem>): InventoryBillingSyncReport {
        if (items.isEmpty()) return InventoryBillingSyncReport()

        var matched = 0
        var decremented = 0
        items.forEach { item ->
            val product = productDao.getProductByExactName(item.name.trim()) ?: return@forEach
            matched += 1
            val rows = runCatching {
                productDao.adjustStockById(product.id, -item.quantity.coerceAtLeast(0.0))
            }.onFailure { e ->
                Log.w(TAG, "Inventory bill sync failed: ${e::class.java.simpleName}")
            }.getOrDefault(0)
            if (rows > 0) decremented += 1
        }

        return InventoryBillingSyncReport(
            matchedItems = matched,
            decrementedItems = decremented
        )
    }

    companion object {
        private const val TAG = "InventoryBillingSync"
    }
}

data class InventoryBillingSyncReport(
    val matchedItems: Int = 0,
    val decrementedItems: Int = 0
)
