package com.ganesh.hisabkitabpro.domain.repository

import com.ganesh.hisabkitabpro.domain.model.AppSettings
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings?>
    suspend fun saveSettings(settings: AppSettings)
    suspend fun syncSettings(): Result<Unit>

    fun getBusinessProfile(): Flow<BusinessProfile?>
    suspend fun saveBusinessProfile(profile: BusinessProfile)
    suspend fun syncBusinessProfile(): Result<Unit>
    suspend fun uploadLogo(logoPath: String): Result<String>
}
