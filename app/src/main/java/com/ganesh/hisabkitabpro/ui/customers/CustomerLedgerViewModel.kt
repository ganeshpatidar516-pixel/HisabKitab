package com.ganesh.hisabkitabpro.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.data.repository.local.BusinessProfileDao
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Tier-A: moves direct [AppDatabase] access off [CustomerLedgerScreen] for profile + audit writes.
 * Transaction list/paging remains on [com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel].
 */
@HiltViewModel
class CustomerLedgerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val businessProfileDao: BusinessProfileDao,
    private val auditLogDao: com.ganesh.hisabkitabpro.addon.audit.AuditLogDao,
) : ViewModel() {

    private val _businessProfile = MutableStateFlow<BusinessProfile?>(null)
    val businessProfile: StateFlow<BusinessProfile?> = _businessProfile.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getBusinessProfile().collect { profile ->
                if (profile != null) {
                    _businessProfile.value = profile
                }
            }
        }
        refreshBusinessProfileOnce()
    }

    fun refreshBusinessProfileOnce() {
        viewModelScope.launch {
            _businessProfile.value = loadBusinessProfileOnce()
        }
    }

    suspend fun loadBusinessProfileOnce(): BusinessProfile? = withContext(Dispatchers.IO) {
        businessProfileDao.getBusinessProfileOnce()
    }

    fun insertAuditLog(entry: AuditLogEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            auditLogDao.insert(entry)
        }
    }
}
