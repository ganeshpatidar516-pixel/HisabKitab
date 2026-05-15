package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.*
import com.ganesh.hisabkitabpro.data.local.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE isSent = 0 ORDER BY scheduledAt ASC")
    fun getPendingReminders(): Flow<List<ReminderEntity>>

    @Query(
        "SELECT * FROM reminders WHERE customerId = :customerId AND counterpartyKind = 'CUSTOMER'"
    )
    fun getRemindersByCustomer(customerId: Long): Flow<List<ReminderEntity>>

    @Query(
        "SELECT * FROM reminders WHERE isSent = 0 AND scheduledAt <= :nowMillis " +
            "ORDER BY scheduledAt ASC LIMIT 100"
    )
    suspend fun getDueRemindersNotSent(nowMillis: Long): List<ReminderEntity>

    @Query(
        "DELETE FROM reminders WHERE customerId = :customerId AND isSent = 0 AND counterpartyKind = 'CUSTOMER'"
    )
    suspend fun deletePendingUnsentForCustomer(customerId: Long)

    @Query(
        "DELETE FROM reminders WHERE partyId = :partyId AND isSent = 0 AND counterpartyKind = 'PARTY_SUPPLIER'"
    )
    suspend fun deletePendingUnsentForPartySupplier(partyId: Long)
}
