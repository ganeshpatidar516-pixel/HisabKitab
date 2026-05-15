package com.ganesh.hisabkitabpro.domain.repository

import androidx.paging.PagingData
import com.ganesh.hisabkitabpro.domain.model.Supplier
import kotlinx.coroutines.flow.Flow

/**
 * HISABKITAB PRO - SUPPLIER REPOSITORY INTERFACE
 */
interface SupplierRepository {
    fun getSuppliersPaged(): Flow<PagingData<Supplier>>
    fun searchSuppliers(query: String): Flow<PagingData<Supplier>>
    fun getSupplierById(id: Long): Flow<Supplier?>
    fun getGlobalTotalPayable(): Flow<Long?>
    
    suspend fun addSupplier(supplier: Supplier): Result<Long>
    suspend fun updateSupplier(supplier: Supplier): Result<Unit>
    suspend fun deleteSupplier(supplierId: Long): Result<Unit>
    suspend fun updateBalance(supplierId: Long, newBalance: Long): Result<Unit>
}
