package com.ganesh.hisabkitabpro.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.commandos.SuperCommandFeatureToggle
import com.ganesh.hisabkitabpro.core.firebase.FirebaseTelemetryBootstrap
import com.ganesh.hisabkitabpro.feature.banksettle.BankAutoSettleFeatureToggle
import com.ganesh.hisabkitabpro.feature.sharedkhata.SharedKhataAccessManager
import com.ganesh.hisabkitabpro.feature.sharedkhata.SharedKhataFeatureToggle
import com.ganesh.hisabkitabpro.feature.telemetry.TelemetryFeatureToggle
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.domain.model.AppSettings
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import com.ganesh.hisabkitabpro.domain.sync.SyncEngine
import com.ganesh.hisabkitabpro.domain.sync.SyncHealthMonitor
import com.ganesh.hisabkitabpro.integrity.PlayIntegrityRepository
import com.ganesh.hisabkitabpro.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * HISABKITAB PRO - 🚀 ATOMIC STABILITY BLUEPRINT (FORCE RESTORE)
 * Thread Isolation & ANR Protection.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val repository: SettingsRepository,
    private val securityManager: SecurityManager,
    private val superCommandFeatureToggle: SuperCommandFeatureToggle,
    private val sharedKhataFeatureToggle: SharedKhataFeatureToggle,
    private val bankAutoSettleFeatureToggle: BankAutoSettleFeatureToggle,
    private val telemetryFeatureToggle: TelemetryFeatureToggle,
    private val playIntegrityRepository: PlayIntegrityRepository,
) : ViewModel() {

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()
    val syncHealth: StateFlow<SyncHealthMonitor.SyncHealth> = SyncHealthMonitor.state
    private val _isBiometricEnabled = MutableStateFlow(securityManager.isAppLockEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _screenPrivacySecure = MutableStateFlow(securityManager.isScreenPrivacySecureEnabled())
    val screenPrivacySecure: StateFlow<Boolean> = _screenPrivacySecure.asStateFlow()

    private val _sharedKhataEnabled = MutableStateFlow(sharedKhataFeatureToggle.isEnabled())
    val sharedKhataEnabled: StateFlow<Boolean> = _sharedKhataEnabled.asStateFlow()

    private val _bankAutoSettleEnabled = MutableStateFlow(bankAutoSettleFeatureToggle.isEnabled())
    val bankAutoSettleEnabled: StateFlow<Boolean> = _bankAutoSettleEnabled.asStateFlow()

    private val _superCommandEnabled = MutableStateFlow(superCommandFeatureToggle.isEnabled())
    val superCommandEnabled: StateFlow<Boolean> = _superCommandEnabled.asStateFlow()

    private val _crashReportingEnabled = MutableStateFlow(telemetryFeatureToggle.isCrashReportingEnabled())
    val crashReportingEnabled: StateFlow<Boolean> = _crashReportingEnabled.asStateFlow()

    private val _analyticsEnabled = MutableStateFlow(telemetryFeatureToggle.isAnalyticsEnabled())
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    val settings: StateFlow<AppSettings?> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val businessProfile: StateFlow<BusinessProfile?> = repository.getBusinessProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Keep persisted state intact; do not hard-reset command layer at startup.
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch(Dispatchers.IO) { // Force IO
            try {
                val current = settings.value ?: AppSettings()
                if (current.theme != theme) {
                    repository.saveSettings(current.copy(theme = theme, updatedAt = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to update theme", e)
            }
        }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = settings.value ?: AppSettings()
                repository.saveSettings(current.copy(currency = currency, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to update currency", e)
            }
        }
    }

    fun updateTemplate(templateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = settings.value ?: AppSettings()
                repository.saveSettings(current.copy(invoiceTemplateId = templateId, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to update template", e)
            }
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val normalized = AppLocaleManager.normalizeLanguageCode(languageCode)
                val current = settings.value ?: AppSettings()
                repository.saveSettings(current.copy(languageCode = normalized, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to update language", e)
            }
        }
    }

    fun setOcrLiveAutoSaveEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = settings.value ?: AppSettings()
                repository.saveSettings(
                    current.copy(
                        ocrLiveAutoSaveEnabled = enabled,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to set OCR live auto-save", e)
            }
        }
    }

    fun toggleVoiceAssistant(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = settings.value ?: AppSettings()
                repository.saveSettings(current.copy(voiceAssistantEnabled = enabled, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to toggle voice assistant", e)
            }
        }
    }

    fun toggleSmartSuggestions(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = settings.value ?: AppSettings()
                repository.saveSettings(current.copy(smartSuggestionsEnabled = enabled, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to toggle smart suggestions", e)
            }
        }
    }

    fun toggleGst(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = settings.value ?: AppSettings()
                repository.saveSettings(current.copy(gstEnabled = enabled, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to toggle GST", e)
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                securityManager.setAppLockEnabled(enabled)
                _isBiometricEnabled.value = enabled
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to set biometric", e)
            }
        }
    }

    fun setScreenPrivacySecureEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                securityManager.setScreenPrivacySecureEnabled(enabled)
                _screenPrivacySecure.value = enabled
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to set screen privacy", e)
            }
        }
    }

    fun canUseBiometricLock(): Boolean = securityManager.canAuthenticate()

    fun updatePin(pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hashedPin = securityManager.hashPin(pin)
                val current = settings.value ?: AppSettings()
                repository.saveSettings(current.copy(securityPinHash = hashedPin, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                // Defense-in-depth: log only the exception type, never the throwable
                // itself, so a future change in SecurityManager that echoes the raw
                // PIN inside an exception message can never reach logcat.
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to update PIN: ${e::class.java.simpleName}")
            }
        }
    }

    fun updateBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveBusinessProfile(profile.copy(updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "STABILITY_FAIL: Failed to update business profile", e)
            }
        }
    }

    /**
     * ✅ BACKGROUND ISOLATION (FORCE IO)
     */
    fun syncData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                playIntegrityRepository.requestAndSubmitToBackend()
                    .onFailure {
                        Log.w("SettingsViewModel", "Play Integrity skipped: ${it::class.java.simpleName}")
                    }
                val before = SyncEngine.getHealthSnapshot()
                _syncStatus.value =
                    "Cloud Sync Initiated... Pending: ${before.pendingInDb}, Failed: ${before.failedInDb}, Memory: ${before.queuedInMemory}"
                val result = repository.syncSettings()
                val profileResult = repository.syncBusinessProfile()
                val after = SyncEngine.getHealthSnapshot()
                withContext(Dispatchers.Main) {
                    if (result.isSuccess && profileResult.isSuccess) {
                        _syncStatus.value =
                            "Sync successful! Pending: ${after.pendingInDb}, Failed: ${after.failedInDb}, Memory: ${after.queuedInMemory}"
                    } else {
                        _syncStatus.value =
                            "Sync failed. Pending: ${after.pendingInDb}, Failed: ${after.failedInDb}, Memory: ${after.queuedInMemory}"
                    }
                }
            } catch (e: Exception) {
                _syncStatus.value = "Sync Exception: ${e.message}"
            }
        }
    }

    /**
     * Drives the "Retry Failed Sync Items" button.
     *
     * Internally calls [SyncHealthMonitor.deepRetryAll] which:
     *  1. Resets every FAILED row back to PENDING with `retryCount=0` and a
     *     backdated `updatedAt` so the exponential-backoff gate lets it
     *     through immediately.
     *  2. Schedules an *expedited* OneTimeWorkRequest with WorkManager
     *     exponential backoff so the OS will keep retrying through transient
     *     network blips.
     *
     * The UI shows "Syncing…" right away. Once the cycle completes, the
     * monitor transitions to `Healthy` ("All items secured") or `Degraded`
     * with a clear reason string, and we mirror that into [syncStatus].
     */
    fun retryFailedSyncItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _syncStatus.value = "Syncing…"
                val moved = SyncHealthMonitor.deepRetryAll(appContext)
                // Wait briefly for the worker to start executing so the user
                // sees real progress instead of a frozen "Syncing…" string.
                kotlinx.coroutines.delay(750)
                val health = SyncHealthMonitor.state.value
                val message = when {
                    health.totalOutstanding == 0 && health.workerPauseReason == SyncHealthMonitor.WorkerPauseReason.NONE ->
                        "All items secured"
                    moved > 0 ->
                        "Retry queued: $moved item(s). ${health.message}"
                    else -> health.message
                }
                _syncStatus.value = message
            } catch (e: Exception) {
                _syncStatus.value = "Retry failed: ${e.message}"
            }
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun setSharedKhataEnabled(enabled: Boolean): Boolean {
        return runCatching {
            sharedKhataFeatureToggle.setEnabled(enabled)
            val prefs = appContext.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
            val accessManager = SharedKhataAccessManager(prefs)
            if (enabled) accessManager.clearRevoke() else accessManager.revoke()
            _sharedKhataEnabled.value = sharedKhataFeatureToggle.isEnabled()
            true
        }.getOrElse {
            Log.e("SettingsViewModel", "Failed to persist shared khata toggle", it)
            _sharedKhataEnabled.value = sharedKhataFeatureToggle.isEnabled()
            false
        }
    }

    fun setBankAutoSettleEnabled(enabled: Boolean): Boolean {
        return runCatching {
            bankAutoSettleFeatureToggle.setEnabled(enabled)
            _bankAutoSettleEnabled.value = bankAutoSettleFeatureToggle.isEnabled()
            true
        }.getOrElse {
            Log.e("SettingsViewModel", "Failed to persist bank auto-settle toggle", it)
            _bankAutoSettleEnabled.value = bankAutoSettleFeatureToggle.isEnabled()
            false
        }
    }

    fun setSuperCommandEnabled(enabled: Boolean): Boolean {
        return runCatching {
            superCommandFeatureToggle.setEnabled(enabled)
            _superCommandEnabled.value = superCommandFeatureToggle.isEnabled()
            true
        }.getOrElse {
            Log.e("SettingsViewModel", "Failed to persist super command toggle", it)
            _superCommandEnabled.value = superCommandFeatureToggle.isEnabled()
            false
        }
    }

    fun setCrashReportingEnabled(enabled: Boolean): Boolean {
        return runCatching {
            telemetryFeatureToggle.setCrashReportingEnabled(enabled)
            _crashReportingEnabled.value = telemetryFeatureToggle.isCrashReportingEnabled()
            FirebaseTelemetryBootstrap.applyRuntimeChange(
                appContext,
                crashReportingEnabled = telemetryFeatureToggle.isCrashReportingEnabled(),
                analyticsEnabled = telemetryFeatureToggle.isAnalyticsEnabled()
            )
            true
        }.getOrElse {
            Log.e("SettingsViewModel", "Failed to persist crash reporting toggle: ${it::class.java.simpleName}")
            _crashReportingEnabled.value = telemetryFeatureToggle.isCrashReportingEnabled()
            false
        }
    }

    fun setAnalyticsEnabled(enabled: Boolean): Boolean {
        return runCatching {
            telemetryFeatureToggle.setAnalyticsEnabled(enabled)
            _analyticsEnabled.value = telemetryFeatureToggle.isAnalyticsEnabled()
            FirebaseTelemetryBootstrap.applyRuntimeChange(
                appContext,
                crashReportingEnabled = telemetryFeatureToggle.isCrashReportingEnabled(),
                analyticsEnabled = telemetryFeatureToggle.isAnalyticsEnabled()
            )
            true
        }.getOrElse {
            Log.e("SettingsViewModel", "Failed to persist analytics toggle: ${it::class.java.simpleName}")
            _analyticsEnabled.value = telemetryFeatureToggle.isAnalyticsEnabled()
            false
        }
    }
}
