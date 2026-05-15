package com.ganesh.hisabkitabpro.data.repository

import com.ganesh.hisabkitabpro.core.database.safeDatabaseCall
import com.ganesh.hisabkitabpro.core.network.safeApiCall
import com.ganesh.hisabkitabpro.data.repository.local.SettingsDao
import com.ganesh.hisabkitabpro.data.repository.local.BusinessProfileDao
import com.ganesh.hisabkitabpro.domain.cloud.SelectiveCloudMirror
import com.ganesh.hisabkitabpro.domain.model.AppSettings
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import com.ganesh.hisabkitabpro.network.api.SettingsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

/**
 * HISABKITAB PRO - SETTINGS REPOSITORY (ZERO-ANR PROTOCOL)
 * All database and network calls are strictly off-loaded from the Main Thread.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val businessProfileDao: BusinessProfileDao,
    private val settingsApi: SettingsApi,
    private val selectiveCloudMirror: SelectiveCloudMirror
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings?> = settingsDao.getSettings()

    override suspend fun saveSettings(settings: AppSettings) {
        withContext(Dispatchers.IO) {
            safeDatabaseCall("SaveSettings") {
                settingsDao.saveSettings(settings)
            }
            
            // Background network update without blocking current scope
            CoroutineScope(Dispatchers.IO).launch {
                safeApiCall("update_settings") {
                    settingsApi.updateSettings(settings)
                }
            }
        }
    }

    override suspend fun syncSettings(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val result = safeApiCall("get_settings") {
                settingsApi.getSettings()
            }
            
            when (result) {
                is com.ganesh.hisabkitabpro.core.network.NetworkResult.Success -> {
                    val response = result.data
                    if (response.isSuccessful) {
                        response.body()?.let { remote ->
                            safeDatabaseCall("SyncSettings_Save") {
                                val local = settingsDao.getSettingsOnce()
                                val merged = remote.copy(
                                    ocrLiveAutoSaveEnabled = local?.ocrLiveAutoSaveEnabled
                                        ?: remote.ocrLiveAutoSaveEnabled,
                                )
                                settingsDao.saveSettings(merged)
                            }
                        }
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to fetch settings"))
                    }
                }
                else -> Result.failure(Exception("Network error during settings sync"))
            }
        }
    }

    override fun getBusinessProfile(): Flow<BusinessProfile?> = businessProfileDao.getProfile()

    override suspend fun saveBusinessProfile(profile: BusinessProfile) {
        withContext(Dispatchers.IO) {
            safeDatabaseCall("SaveBusinessProfile") {
                businessProfileDao.saveProfile(profile)
            }
            selectiveCloudMirror.mirrorBusinessProfile(profile)
            
            CoroutineScope(Dispatchers.IO).launch {
                safeApiCall("update_profile") {
                    settingsApi.updateBusinessProfile(profile)
                }
            }
        }
    }

    override suspend fun syncBusinessProfile(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val result = safeApiCall("get_profile") {
                settingsApi.getBusinessProfile()
            }
            
            when (result) {
                is com.ganesh.hisabkitabpro.core.network.NetworkResult.Success -> {
                    val response = result.data
                    if (response.isSuccessful) {
                        response.body()?.let { remote ->
                            val normalized = remote.copy(
                                tagline = remote.tagline?.ifBlank { null },
                                servicesDescription = remote.servicesDescription?.ifBlank { null },
                                cardCtaText = remote.cardCtaText?.ifBlank { null },
                            )
                            safeDatabaseCall("SyncProfile_Save") {
                                businessProfileDao.saveProfile(normalized)
                            }
                        }
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to fetch profile"))
                    }
                }
                else -> Result.failure(Exception("Network error during profile sync"))
            }
        }
    }

    override suspend fun uploadLogo(logoPath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(logoPath)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("logo", file.name, requestFile)
                
                val result = safeApiCall("upload_logo") {
                    settingsApi.uploadLogo(body)
                }
                
                when (result) {
                    is com.ganesh.hisabkitabpro.core.network.NetworkResult.Success -> {
                        val response = result.data
                        if (response.isSuccessful) {
                            val url = response.body()?.get("logo_url") ?: ""
                            Result.success(url)
                        } else {
                            Result.failure(Exception("Upload failed"))
                        }
                    }
                    else -> Result.failure(Exception("Network error during logo upload"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
