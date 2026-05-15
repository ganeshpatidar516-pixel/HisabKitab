package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.*
import com.ganesh.hisabkitabpro.domain.model.Bill

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): Bill?

    /** Pending bills for bank SMS / auto-settle suggestion matching (read-only helper queries). */
    @Query("SELECT * FROM bills WHERE status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingBills(): List<Bill>
}
