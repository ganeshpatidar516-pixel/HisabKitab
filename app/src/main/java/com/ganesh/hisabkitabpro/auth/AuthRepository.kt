package com.ganesh.hisabkitabpro.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ganesh.hisabkitabpro.network.FirebaseRetrofitAuthBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the user's Firebase Auth session.
 *
 * SAFETY GUARANTEES:
 * - Normal sign-in / sign-out paths never touch the Room/SQLCipher database or any business DAOs.
 * - Account **erasure** (explicit user request under Privacy & data) is handled only by
 *   [com.ganesh.hisabkitabpro.privacy.AccountDeletionCoordinator], not by routine auth ops.
 * - Never alters Biometric Lock state — the lock is gated separately in
 *   [com.ganesh.hisabkitabpro.MainActivity] and is unaware of Firebase Auth.
 * - All operations are best-effort: failures are converted to user-friendly
 *   [AuthOpResult.Failure] messages so the UI never sees raw exceptions.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val authDataMerger: AuthDataMerger,
    private val prefs: SharedPreferences,
) {

    private val emailSlotLock = Any()

    /** P1 — sync Retrofit bearer when Firebase session changes (refresh / sign-out / revoke). */
    private val bearerSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Hot stream of [AuthState] tracking [FirebaseAuth.AuthStateListener].
     */
    val authStateFlow: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val state = auth.currentUser.toAuthState()
            trySend(state)
            bearerSyncScope.launch {
                runCatching { FirebaseRetrofitAuthBridge.syncBearerFromFirebase(firebaseAuth) }
                    .onFailure {
                        Log.w(TAG, "Bearer sync on auth state change failed (${it::class.java.simpleName})")
                    }
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser.toAuthState())
        bearerSyncScope.launch {
            runCatching { FirebaseRetrofitAuthBridge.syncBearerFromFirebase(firebaseAuth) }
        }
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    /** Snapshot of the current state, useful for non-reactive call sites. */
    fun currentState(): AuthState = firebaseAuth.currentUser.toAuthState()

    /**
     * Returns the Google web client id required by [GoogleAuthProvider]. If the
     * `default_web_client_id` resource is missing (Firebase Console OAuth not yet
     * configured), this returns `null` so the UI can show a friendly fallback
     * instead of crashing on startup.
     */
    fun resolveGoogleWebClientId(): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return runCatching { context.getString(resId) }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /**
     * Builds a [GoogleSignInClient] configured for Firebase Auth ID-token sign-in.
     * Returns `null` if [resolveGoogleWebClientId] is unavailable.
     */
    fun buildGoogleSignInClient(): GoogleSignInClient? {
        val webClientId = resolveGoogleWebClientId() ?: return null
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** Exchanges a Google ID-token for a Firebase Auth session. */
    suspend fun signInWithGoogleIdToken(idToken: String): AuthOpResult {
        val res = runOp {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.uid?.let { authDataMerger.onSignInSuccess(it) }
        }
        if (res is AuthOpResult.Success) {
            runCatching { FirebaseRetrofitAuthBridge.syncBearerFromFirebase(firebaseAuth) }
                .onFailure { Log.w(TAG, "Retrofit bearer sync failed (${it::class.java.simpleName})") }
        }
        return res
    }

    /** Sign-in with email + password (provider must be enabled in Firebase Console). */
    suspend fun signInWithEmail(email: String, password: String): AuthOpResult {
        acquireEmailAuthSlot()?.let { return AuthOpResult.Failure(it) }
        val res = runOp {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            result.user?.uid?.let { authDataMerger.onSignInSuccess(it) }
        }
        if (res is AuthOpResult.Success) {
            runCatching { FirebaseRetrofitAuthBridge.syncBearerFromFirebase(firebaseAuth) }
                .onFailure { Log.w(TAG, "Retrofit bearer sync failed (${it::class.java.simpleName})") }
        }
        return res
    }

    /** Create a new email + password account. */
    suspend fun registerWithEmail(email: String, password: String): AuthOpResult {
        acquireEmailAuthSlot()?.let { return AuthOpResult.Failure(it) }
        val res = runOp {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            result.user?.uid?.let { authDataMerger.onSignInSuccess(it) }
        }
        if (res is AuthOpResult.Success) {
            runCatching { FirebaseRetrofitAuthBridge.syncBearerFromFirebase(firebaseAuth) }
                .onFailure { Log.w(TAG, "Retrofit bearer sync failed (${it::class.java.simpleName})") }
        }
        return res
    }

    /** Send a password-reset email. */
    suspend fun sendPasswordReset(email: String): AuthOpResult {
        acquireEmailAuthSlot()?.let { return AuthOpResult.Failure(it) }
        return runOp {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
        }
    }

    /**
     * Signs the user out everywhere: Firebase + the cached Google credential so
     * a subsequent Google Sign-In actually shows the account picker.
     */
    suspend fun signOut(): AuthOpResult {
        val res = runOp {
            firebaseAuth.signOut()
            runCatching { buildGoogleSignInClient()?.signOut()?.await() }
        }
        runCatching { FirebaseRetrofitAuthBridge.syncBearerFromFirebase(firebaseAuth) }
            .onFailure { Log.w(TAG, "Retrofit bearer clear failed (${it::class.java.simpleName})") }
        return res
    }

    /**
     * Permanently delete the Firebase Auth user.
     *
     * For Play-compliant full account removal (including local business data),
     * the app routes through [com.ganesh.hisabkitabpro.privacy.AccountDeletionCoordinator],
     * which clears on-device storage first, then calls [deleteFirebaseUser].
     */
    suspend fun deleteFirebaseUser(): AuthOpResult {
        val res = runOp {
            val user = firebaseAuth.currentUser
                ?: error("Not signed in")
            runCatching { buildGoogleSignInClient()?.signOut()?.await() }
            user.delete().await()
        }
        runCatching { FirebaseRetrofitAuthBridge.syncBearerFromFirebase(firebaseAuth) }
            .onFailure { Log.w(TAG, "Retrofit bearer clear failed (${it::class.java.simpleName})") }
        return res
    }

    suspend fun deleteAccount(): AuthOpResult = deleteFirebaseUser()

    /**
     * Re-authenticate with email + password (required before sensitive operations
     * such as account deletion for password-based Firebase users).
     */
    suspend fun reauthenticateWithEmailPassword(email: String, password: String): AuthOpResult {
        acquireEmailAuthSlot()?.let { return AuthOpResult.Failure(it) }
        return runOp {
            val user = firebaseAuth.currentUser
                ?: error("Not signed in")
            val cred = EmailAuthProvider.getCredential(email.trim(), password)
            user.reauthenticate(cred).await()
        }
    }

    /** Re-authenticate with a fresh Google ID token for the currently signed-in user. */
    suspend fun reauthenticateWithGoogleIdToken(idToken: String): AuthOpResult {
        return runOp {
            val user = firebaseAuth.currentUser
                ?: error("Not signed in")
            val cred = GoogleAuthProvider.getCredential(idToken, null)
            user.reauthenticate(cred).await()
        }
    }

    private inline fun runOp(block: () -> Unit): AuthOpResult = try {
        block()
        AuthOpResult.Success
    } catch (t: Throwable) {
        // Never log full exception chains — Firebase messages can contain email fragments.
        Log.w(TAG, "Auth op failed (${t::class.java.simpleName})")
        AuthOpResult.Failure(humanize(t))
    }

    /**
     * Client-side throttle for email/password/reset calls (same device).
     * Does not replace server-side Firebase abuse protections.
     */
    private fun acquireEmailAuthSlot(): String? = synchronized(emailSlotLock) {
        val now = System.currentTimeMillis()
        var windowStart = prefs.getLong(PREF_EMAIL_AUTH_WINDOW_START_MS, 0L)
        var count = prefs.getInt(PREF_EMAIL_AUTH_WINDOW_COUNT, 0)
        if (windowStart == 0L || now - windowStart > EMAIL_AUTH_WINDOW_MS) {
            windowStart = now
            count = 0
        }
        if (count >= MAX_EMAIL_AUTH_PER_WINDOW) {
            val waitMin =
                ((EMAIL_AUTH_WINDOW_MS - (now - windowStart)) / 60_000L).coerceAtLeast(1L)
            return@synchronized "Too many attempts this hour. Try again in about ${waitMin} min."
        }
        prefs.edit()
            .putLong(PREF_EMAIL_AUTH_WINDOW_START_MS, windowStart)
            .putInt(PREF_EMAIL_AUTH_WINDOW_COUNT, count + 1)
            .apply()
        null
    }

    private fun humanize(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            raw.contains("network", ignoreCase = true) ->
                "Network unavailable. Check your connection and try again."
            raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                raw.contains("wrong password", ignoreCase = true) ->
                "Incorrect email or password."
            raw.contains("no user record", ignoreCase = true) ||
                raw.contains("user-not-found", ignoreCase = true) ->
                "No account found for this email."
            raw.contains("email address is badly formatted", ignoreCase = true) ||
                raw.contains("badly formatted", ignoreCase = true) ->
                "Please enter a valid email address."
            raw.contains("password should be at least", ignoreCase = true) ||
                raw.contains("WEAK_PASSWORD", ignoreCase = true) ->
                "Password is too weak. Use at least 6 characters."
            raw.contains("email address is already in use", ignoreCase = true) ||
                raw.contains("EMAIL_EXISTS", ignoreCase = true) ->
                "An account with this email already exists."
            raw.contains("requires recent login", ignoreCase = true) ->
                "Please sign in again to continue."
            raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                "Sign-in provider is not enabled. Enable Email/Password in Firebase Console."
            else -> "Authentication failed. Please try again."
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val EMAIL_AUTH_WINDOW_MS = 3_600_000L
        private const val MAX_EMAIL_AUTH_PER_WINDOW = 24
        private const val PREF_EMAIL_AUTH_WINDOW_START_MS = "auth.email_auth_window_start_ms"
        private const val PREF_EMAIL_AUTH_WINDOW_COUNT = "auth.email_auth_window_count"
    }
}

/** Internal projection of [com.google.firebase.auth.FirebaseUser] -> [AuthState]. */
private fun com.google.firebase.auth.FirebaseUser?.toAuthState(): AuthState {
    if (this == null) return AuthState.LoggedOut
    return AuthState.LoggedIn(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
        providerId = providerData
            .firstOrNull { it.providerId != "firebase" }
            ?.providerId,
        isEmailVerified = isEmailVerified
    )
}

/**
 * Convenience snapshot reader used by the header avatar so it doesn't have to
 * collect the whole flow before the first emission.
 */
@Suppress("unused")
fun StateFlow<AuthState>.snapshotOrLoggedOut(): AuthState = value.takeIf { it !is AuthState.Loading }
    ?: AuthState.LoggedOut

@Suppress("unused")
internal fun MutableStateFlow<AuthState>.toAuthState(): AuthState = value
