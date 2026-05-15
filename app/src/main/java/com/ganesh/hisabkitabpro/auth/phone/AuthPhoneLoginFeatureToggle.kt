package com.ganesh.hisabkitabpro.auth.phone

import android.content.SharedPreferences
import javax.inject.Inject

/**
 * Rollout gate for **Firebase phone (OTP) on the sign-in sheet** only (“Continue with mobile number”).
 *
 * - Default **OFF**: sign-in sheet stays Google + email only; **Settings → Link mobile number** can
 *   still be used when signed in (see [PhoneAuthRepository.linkCurrentUserPhone]).
 * - When ON: phone sign-in appears on the auth sheet and uses the same SMS pipeline as linking.
 *
 * Uses the same [SharedPreferences] file as other feature toggles (`hisabkitab_prefs`).
 */
class AuthPhoneLoginFeatureToggle @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_AUTH_PHONE_LOGIN_V1, DEFAULT_ENABLED)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_PHONE_LOGIN_V1, enabled).apply()
    }

    companion object {
        const val KEY_AUTH_PHONE_LOGIN_V1 = "feature_auth_phone_login_v1"
        private const val DEFAULT_ENABLED = false
    }
}
