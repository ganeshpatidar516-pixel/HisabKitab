package com.ganesh.hisabkitabpro.domain.cloud

import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stable local BusinessID source for the hybrid cloud layer.
 *
 * The value is stored in SharedPreferences, not Room, so it can scope cloud
 * documents without changing any customer / transaction schema or ledger math.
 */
@Singleton
class CloudBusinessIdentity @Inject constructor(
    private val prefs: SharedPreferences
) {

    fun currentBusinessId(): String = ensureBusinessId(prefs)

    fun setCurrentBusinessId(businessId: String) {
        val normalized = businessId.trim()
        require(normalized.isNotBlank()) { "businessId cannot be blank" }
        prefs.edit { putString(PREF_BUSINESS_ID, normalized) }
    }

    companion object {
        const val PREF_BUSINESS_ID = "cloud_shared_khata.business_id"

        fun ensureBusinessId(prefs: SharedPreferences): String {
            prefs.getString(PREF_BUSINESS_ID, null)
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }

            val generated = "biz_${UUID.randomUUID().toString().replace("-", "")}"
            prefs.edit { putString(PREF_BUSINESS_ID, generated) }
            return generated
        }
    }
}
