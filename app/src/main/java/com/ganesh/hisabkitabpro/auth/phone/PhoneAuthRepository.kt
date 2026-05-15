package com.ganesh.hisabkitabpro.auth.phone

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import com.ganesh.hisabkitabpro.auth.AuthDataMerger
import com.ganesh.hisabkitabpro.auth.AuthOpResult
import com.ganesh.hisabkitabpro.network.FirebaseRetrofitAuthBridge
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Firebase **phone (SMS OTP)** sign-in / link.
 *
 * - **Sign-in with phone** on the auth sheet is gated by [AuthPhoneLoginFeatureToggle] (default off).
 * - **Link phone** from Settings is allowed for any signed-in user (same Firebase Phone API); does
 *   not require the sign-in-sheet toggle, so merchants can link a number without exposing mobile
 *   sign-in on the public sheet.
 *
 * - Does **not** touch Room; on success calls [AuthDataMerger.onSignInSuccess] like Google/email.
 * - [Activity] is required for Firebase device verification / reCAPTCHA flows.
 */
@Singleton
class PhoneAuthRepository @Inject constructor(
    private val toggle: AuthPhoneLoginFeatureToggle,
    private val firebaseAuth: FirebaseAuth,
    private val authDataMerger: AuthDataMerger,
    private val prefs: SharedPreferences,
) {

    @Volatile
    private var storedVerificationId: String? = null

    @Volatile
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    @Volatile
    private var pendingE164: String? = null

    @Volatile
    private var awaitingLink: Boolean = false

    @Volatile
    private var lastOtpRequestAtMs: Long = 0L

    @Volatile
    private var otpSubmitFailures: Int = 0

    @Volatile
    private var otpLockUntilMs: Long = 0L

    private val phoneSlotLock = Any()

    fun isFeatureEnabled(): Boolean = toggle.isEnabled()

    /** Persists the rollout flag in `hisabkitab_prefs` (same as other feature toggles). */
    fun setPhoneLoginFeatureEnabled(enabled: Boolean) {
        toggle.setEnabled(enabled)
    }

    fun clearPhoneVerificationSession() {
        storedVerificationId = null
        resendToken = null
        pendingE164 = null
        awaitingLink = false
        lastOtpRequestAtMs = 0L
        otpSubmitFailures = 0
        otpLockUntilMs = 0L
    }

    suspend fun beginPhoneSignIn(
        activity: Activity,
        e164Phone: String,
        forceResend: Boolean = false
    ): PhoneVerificationStartResult {
        if (!toggle.isEnabled()) {
            return PhoneVerificationStartResult.Failed(MSG_FEATURE_DISABLED)
        }
        awaitingLink = false
        return requestVerification(activity, e164Phone.trim(), forceResend)
    }

    suspend fun linkCurrentUserPhone(
        activity: Activity,
        e164Phone: String,
        forceResend: Boolean = false,
    ): PhoneVerificationStartResult {
        if (firebaseAuth.currentUser == null) {
            return PhoneVerificationStartResult.Failed("Sign in first to link a phone number.")
        }
        awaitingLink = true
        return requestVerification(activity, e164Phone.trim(), forceResend)
    }

    suspend fun confirmPhoneOtp(smsCode: String): AuthOpResult {
        if (!toggle.isEnabled() && !awaitingLink) {
            return AuthOpResult.Failure(MSG_FEATURE_DISABLED)
        }
        val now = System.currentTimeMillis()
        if (now < otpLockUntilMs) {
            val sec = (otpLockUntilMs - now + 999L) / 1000L
            return AuthOpResult.Failure(
                "Too many incorrect codes. Wait ${sec}s or tap Resend for a new code."
            )
        }
        val code = smsCode.trim()
        if (code.length < 4) {
            return AuthOpResult.Failure("Enter the verification code from SMS.")
        }
        val verificationId = storedVerificationId
        if (verificationId.isNullOrBlank()) {
            return AuthOpResult.Failure("No active verification. Request a new code.")
        }
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            completeWithCredential(credential)
        } catch (t: Throwable) {
            Log.w(TAG, "confirm_failed type=${t::class.java.simpleName}")
            val msg = humanizePhone(t)
            noteOtpVerifyFailure(msg)
            AuthOpResult.Failure(msg)
        }
    }

    private suspend fun requestVerification(
        activity: Activity,
        e164: String,
        forceResend: Boolean
    ): PhoneVerificationStartResult {
        if (e164.isBlank()) {
            return PhoneVerificationStartResult.Failed("Enter your mobile number with country code (e.g. +91…).")
        }
        if (!e164.startsWith("+") || e164.length < 10) {
            return PhoneVerificationStartResult.Failed("Use international format starting with + and country code.")
        }
        if (forceResend && resendToken == null) {
            return PhoneVerificationStartResult.Failed("Send the first code before resending.")
        }
        if (!forceResend && pendingE164 != null && pendingE164 != e164) {
            storedVerificationId = null
            resendToken = null
        }
        val now = System.currentTimeMillis()
        if (lastOtpRequestAtMs > 0L && now - lastOtpRequestAtMs < RESEND_COOLDOWN_MS) {
            val waitSec = (RESEND_COOLDOWN_MS - (now - lastOtpRequestAtMs) + 999L) / 1000L
            return PhoneVerificationStartResult.Failed("Please wait ${waitSec}s before requesting another code.")
        }

        acquirePhoneVerificationSlot()?.let { msg ->
            return PhoneVerificationStartResult.Failed(msg)
        }

        return suspendCancellableCoroutine { cont ->
            var resumed = false
            fun resumeOnce(result: PhoneVerificationStartResult) {
                if (resumed) return
                resumed = true
                cont.resume(result)
            }

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.i(TAG, "event=auto_verification")
                    val current = firebaseAuth.currentUser
                    val task = if (current != null && awaitingLink) {
                        current.linkWithCredential(credential)
                    } else {
                        firebaseAuth.signInWithCredential(credential)
                    }
                    task.addOnCompleteListener { snap ->
                        if (!snap.isSuccessful) {
                            awaitingLink = false
                            resumeOnce(PhoneVerificationStartResult.Failed(humanizePhone(snap.exception)))
                            return@addOnCompleteListener
                        }
                        val uid = snap.result?.user?.uid
                        if (uid.isNullOrBlank()) {
                            awaitingLink = false
                            resumeOnce(
                                PhoneVerificationStartResult.Failed("Sign-in completed but user id was missing.")
                            )
                        } else {
                            resetOtpGuards()
                            authDataMerger.onSignInSuccess(uid)
                            FirebaseRetrofitAuthBridge.enqueueBearerFromFirebase(firebaseAuth)
                            storedVerificationId = null
                            resendToken = null
                            pendingE164 = null
                            awaitingLink = false
                            lastOtpRequestAtMs = 0L
                            resumeOnce(PhoneVerificationStartResult.CompletedWithoutSmsUi)
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.w(TAG, "event=verification_failed type=${e::class.java.simpleName}")
                    awaitingLink = false
                    resumeOnce(PhoneVerificationStartResult.Failed(humanizePhone(e)))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.i(TAG, "event=code_sent")
                    resetOtpGuards()
                    storedVerificationId = verificationId
                    resendToken = token
                    pendingE164 = e164
                    lastOtpRequestAtMs = System.currentTimeMillis()
                    resumeOnce(PhoneVerificationStartResult.AwaitingSmsCode)
                }
            }

            val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(e164)
                .setTimeout(OTP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (forceResend) {
                val token = resendToken
                if (token != null) {
                    optionsBuilder.setForceResendingToken(token)
                }
            }

            cont.invokeOnCancellation {
                Log.i(TAG, "event=request_cancelled")
            }

            runCatching {
                PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
            }.onFailure { t ->
                Log.w(TAG, "event=verify_phone_invoke_failed", t)
                awaitingLink = false
                resumeOnce(PhoneVerificationStartResult.Failed(humanizePhone(t)))
            }
        }
    }

    private suspend fun completeWithCredential(credential: PhoneAuthCredential): AuthOpResult =
        suspendCancellableCoroutine { cont ->
            val current = firebaseAuth.currentUser
            val task = if (current != null && awaitingLink) {
                current.linkWithCredential(credential)
            } else {
                firebaseAuth.signInWithCredential(credential)
            }
            task.addOnCompleteListener { t ->
                awaitingLink = false
                if (!t.isSuccessful) {
                    val msg = humanizePhone(t.exception)
                    noteOtpVerifyFailure(msg)
                    cont.resume(AuthOpResult.Failure(msg))
                    return@addOnCompleteListener
                }
                val uid = t.result?.user?.uid
                if (uid.isNullOrBlank()) {
                    cont.resume(AuthOpResult.Failure("Sign-in failed."))
                } else {
                    resetOtpGuards()
                    authDataMerger.onSignInSuccess(uid)
                    FirebaseRetrofitAuthBridge.enqueueBearerFromFirebase(firebaseAuth)
                    storedVerificationId = null
                    resendToken = null
                    pendingE164 = null
                    lastOtpRequestAtMs = 0L
                    cont.resume(AuthOpResult.Success)
                }
            }
        }

    private fun resetOtpGuards() {
        otpSubmitFailures = 0
        otpLockUntilMs = 0L
    }

    private fun noteOtpVerifyFailure(userMessage: String) {
        if (!isLikelyWrongOtp(userMessage)) return
        otpSubmitFailures++
        if (otpSubmitFailures >= MAX_OTP_FAILURES_BEFORE_LOCK) {
            otpLockUntilMs = System.currentTimeMillis() + OTP_LOCK_MS
        }
    }

    private fun isLikelyWrongOtp(userMessage: String): Boolean =
        userMessage.contains("invalid", ignoreCase = true) &&
            (userMessage.contains("code", ignoreCase = true) ||
                userMessage.contains("verification", ignoreCase = true))

    private fun acquirePhoneVerificationSlot(): String? = synchronized(phoneSlotLock) {
        val now = System.currentTimeMillis()
        var windowStart = prefs.getLong(PREF_PHONE_VERIFY_WINDOW_START_MS, 0L)
        var count = prefs.getInt(PREF_PHONE_VERIFY_WINDOW_COUNT, 0)
        if (windowStart == 0L || now - windowStart > PHONE_VERIFY_WINDOW_MS) {
            windowStart = now
            count = 0
        }
        if (count >= MAX_PHONE_VERIFIES_PER_WINDOW) {
            val waitMin =
                ((PHONE_VERIFY_WINDOW_MS - (now - windowStart)) / 60_000L).coerceAtLeast(1L)
            return@synchronized "Too many SMS requests this hour. Try again in about ${waitMin} min."
        }
        prefs.edit()
            .putLong(PREF_PHONE_VERIFY_WINDOW_START_MS, windowStart)
            .putInt(PREF_PHONE_VERIFY_WINDOW_COUNT, count + 1)
            .apply()
        null
    }

    companion object {
        private const val TAG = "HK_PhoneAuth"
        private const val OTP_TIMEOUT_SEC = 60L
        private const val RESEND_COOLDOWN_MS = 45_000L
        private const val PHONE_VERIFY_WINDOW_MS = 3_600_000L
        private const val MAX_PHONE_VERIFIES_PER_WINDOW = 8
        private const val PREF_PHONE_VERIFY_WINDOW_START_MS = "auth.phone_verify_window_start_ms"
        private const val PREF_PHONE_VERIFY_WINDOW_COUNT = "auth.phone_verify_window_count"
        private const val MAX_OTP_FAILURES_BEFORE_LOCK = 5
        private const val OTP_LOCK_MS = 600_000L

        const val MSG_FEATURE_DISABLED =
            "Phone sign-in is not enabled on this build. Use Google or email."

        private fun humanizePhone(t: Throwable?): String {
            runCatching {
                when (val ex = t) {
                    is FirebaseException -> {
                        val code = (ex as? FirebaseAuthException)?.errorCode
                        Log.w(TAG, "phone_verify_diag simple=${ex.javaClass.simpleName} errorCode=${code ?: "n/a"}")
                    }
                    null -> Unit
                    else -> Log.w(TAG, "phone_verify_diag simple=${ex.javaClass.simpleName}")
                }
            }
            val raw = t?.message.orEmpty()
            val low = raw.lowercase()
            return when {
                t is FirebaseAuthInvalidCredentialsException ||
                    low.contains("invalid-verification") ||
                    low.contains("invalid verification") ->
                    "Invalid verification code. Try again."
                low.contains("session-expired") ->
                    "This code expired. Request a new one."
                low.contains("quota") ||
                    low.contains("too-many") ||
                    low.contains("blocked all requests") ||
                    low.contains("we have blocked") ->
                    "Too many attempts. Try again later."
                low.contains("network") || low.contains("unavailable") ->
                    "Network error. Check your connection."
                low.contains("credential-already-in-use") ->
                    "This phone is already linked to another account."
                low.contains("provider-already-linked") ->
                    "This sign-in method is already linked."
                low.contains("requires-recent-login") ->
                    "Please sign in again, then try linking your phone."
                low.contains("invalid-app-credential") ||
                    low.contains("app_not_authorized") ||
                    low.contains("missing client identifier") ||
                    low.contains("missing_client_identifier") ||
                    low.contains("api_key_invalid") ||
                    low.contains("invalid api key") ->
                    "Firebase app check failed: add this app's SHA-1 (and SHA-256) in Firebase Console " +
                        "→ Project settings → Your apps, enable Phone sign-in, and use a valid google-services.json."
                low.contains("billing_not_enabled") ||
                    low.contains("billing not enabled") ->
                    "Firebase project billing may be required for phone auth. Check Firebase Console → Usage and billing."
                low.contains("recaptcha") || low.contains("safety net") || low.contains("play integrity") ->
                    "Device attestation blocked SMS verification. Retry on a standard network or update Google Play services."
                low.contains("too_short") || low.contains("invalid format") ->
                    "Phone number format not accepted. Check country code and digits."
                low.contains("missing_phone_number") ->
                    "Phone number missing. Re-enter your mobile number."
                else -> "Phone verification failed. Please try again."
            }
        }
    }
}
