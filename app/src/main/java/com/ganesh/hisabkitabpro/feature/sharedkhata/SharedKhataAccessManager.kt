package com.ganesh.hisabkitabpro.feature.sharedkhata

import android.content.SharedPreferences

/**
 * Local guardrails for Shared Khata publish flow.
 * Keeps rollout safe by enforcing cooldown, tracking latest publish, and honoring local revoke.
 */
class SharedKhataAccessManager(
    private val prefs: SharedPreferences
) {
    data class LocalAccessState(
        val isRevoked: Boolean,
        val lastPublishedAt: Long,
        val expiresAt: Long
    ) {
        fun isActive(now: Long = System.currentTimeMillis()): Boolean {
            return !isRevoked && expiresAt > now
        }
    }

    fun canPublishNow(
        now: Long = System.currentTimeMillis(),
        cooldownMs: Long = DEFAULT_COOLDOWN_MS
    ): Boolean {
        if (isRevoked()) return false
        val last = prefs.getLong(KEY_LAST_PUBLISHED_AT, 0L)
        return last <= 0L || now - last >= cooldownMs
    }

    fun remainingCooldownMs(
        now: Long = System.currentTimeMillis(),
        cooldownMs: Long = DEFAULT_COOLDOWN_MS
    ): Long {
        val last = prefs.getLong(KEY_LAST_PUBLISHED_AT, 0L)
        if (last <= 0L) return 0L
        return (cooldownMs - (now - last)).coerceAtLeast(0L)
    }

    fun onPublished(expiresAt: Long, now: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putBoolean(KEY_REVOKED, false)
            .putLong(KEY_LAST_PUBLISHED_AT, now)
            .putLong(KEY_LAST_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun revoke() {
        prefs.edit().putBoolean(KEY_REVOKED, true).apply()
    }

    fun clearRevoke() {
        prefs.edit().putBoolean(KEY_REVOKED, false).apply()
    }

    fun isRevoked(): Boolean = prefs.getBoolean(KEY_REVOKED, false)

    fun getLocalState(): LocalAccessState {
        return LocalAccessState(
            isRevoked = isRevoked(),
            lastPublishedAt = prefs.getLong(KEY_LAST_PUBLISHED_AT, 0L),
            expiresAt = prefs.getLong(KEY_LAST_EXPIRES_AT, 0L)
        )
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS: Long = 2L * 60L * 1000L
        private const val KEY_LAST_PUBLISHED_AT = "shared_khata_last_published_at"
        private const val KEY_LAST_EXPIRES_AT = "shared_khata_last_expires_at"
        private const val KEY_REVOKED = "shared_khata_revoked_local"
    }
}
