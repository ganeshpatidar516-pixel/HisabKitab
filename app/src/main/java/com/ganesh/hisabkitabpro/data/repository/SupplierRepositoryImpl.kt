package com.ganesh.hisabkitabpro.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ganesh.hisabkitabpro.data.repository.local.SupplierDao
import com.ganesh.hisabkitabpro.domain.model.Supplier
import com.ganesh.hisabkitabpro.domain.repository.SupplierRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * HISABKITAB PRO - 🚀 ULTRA STABLE SUPPLIER REPOSITORY
 * Offloaded all DAO Flow calls to IO dispatcher to prevent UI freeze.
 */
class SupplierRepositoryImpl @Inject constructor(
    private val supplierDaoLazy: dagger.Lazy<SupplierDao>
) : SupplierRepository {

    private val supplierDao get() = supplierDaoLazy.get()

    override fun getSuppliersPaged(): Flow<PagingData<Supplier>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { supplierDao.getSuppliersPagingSource() }
        ).flow.flowOn(Dispatchers.IO)
    }

    override fun searchSuppliers(query: String): Flow<PagingData<Supplier>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { supplierDao.searchSuppliers("%$query%") }
        ).flow.flowOn(Dispatchers.IO)
    }

    override fun getSupplierById(id: Long): Flow<Supplier?> = flow {
        // Implementation: Using getAllActiveSuppliers for flow context, or a custom flow
        emitAll(supplierDao.getAllActiveSuppliers().map { list -> list.find { it.id == id } })
    }.flowOn(Dispatchers.IO)
    
    override fun getGlobalTotalPayable(): Flow<Long?> = flow {
        emitAll(supplierDao.getGlobalTotalPayable())
    }.flowOn(Dispatchers.IO)

    override suspend fun addSupplier(supplier: Supplier): Result<Long> {
        return try {
            val id = supplierDao.insertSupplier(supplier)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSupplier(supplier: Supplier): Result<Unit> {
        return try {
            supplierDao.updateSupplier(supplier)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSupplier(supplierId: Long): Result<Unit> {
        return try {
            supplierDao.softDelete(supplierId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBalance(supplierId: Long, newBalance: Long): Result<Unit> {
        return try {
            supplierDao.updateBalanceCache(supplierId, newBalance)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
