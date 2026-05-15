package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.*
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE userId = :userId LIMIT 1")
    fun getProfile(userId: String = "default_user"): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE userId = :userId LIMIT 1")
    suspend fun getBusinessProfileOnce(userId: String = "default_user"): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: BusinessProfile)

    @Update
    suspend fun updateProfile(profile: BusinessProfile)
}
