package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.*
import com.ganesh.hisabkitabpro.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE userId = :userId LIMIT 1")
    fun getSettings(userId: String = "default_user"): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettingsOnce(userId: String = "default_user"): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    @Update
    suspend fun updateSettings(settings: AppSettings)
}
