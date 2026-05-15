package com.ganesh.hisabkitabpro.data.local

import androidx.room.*
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

    /** Clears pending auto-pilot rows before inserting a fresh follow-up for this customer. */
    @Query(
        "DELETE FROM reminders WHERE customerId = :customerId AND isSent = 0 AND counterpartyKind = 'CUSTOMER'"
    )
    suspend fun deletePendingUnsentForCustomer(customerId: Long)

    /** Pending supplier-party (unified [com.ganesh.hisabkitabpro.domain.model.Party]) follow-ups. */
    @Query(
        "DELETE FROM reminders WHERE partyId = :partyId AND isSent = 0 AND counterpartyKind = 'PARTY_SUPPLIER'"
    )
    suspend fun deletePendingUnsentForPartySupplier(partyId: Long)
}
