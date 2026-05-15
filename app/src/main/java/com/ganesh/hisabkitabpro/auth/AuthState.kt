package com.ganesh.hisabkitabpro.auth

/**
 * Authentication state surfaced by [AuthRepository].
 *
 * Pure UI-friendly model — no Firebase types leak out so consumers can be
 * unit-tested and previewed without the Firebase SDK on the classpath.
 */
sealed interface AuthState {
    /** No user signed in (silhouette icon shown in the header). */
    data object LoggedOut : AuthState

    /** Authentication is in-flight (e.g. signing in / signing out). */
    data object Loading : AuthState

    /**
     * A user is signed in.
     *
     * @param uid Firebase UID (stable identifier).
     * @param email Email address (may be null for some providers).
     * @param displayName Provider-supplied display name (may be null).
     * @param photoUrl Profile picture URL (may be null if user has none).
     * @param providerId Primary provider id (e.g. "google.com", "password").
     * @param isEmailVerified Whether the user has verified their email.
     */
    data class LoggedIn(
        val uid: String,
        val email: String?,
        val displayName: String?,
        val photoUrl: String?,
        val providerId: String?,
        val isEmailVerified: Boolean
    ) : AuthState
}

/** Result envelope for one-shot auth operations. */
sealed interface AuthOpResult {
    data object Success : AuthOpResult

    /**
     * @param userMessage A human-friendly message safe to show in UI (no stack
     *   traces, no provider internals). All values are translated up-front in
     *   [AuthRepository] so the UI layer can render them as-is.
     */
    data class Failure(val userMessage: String) : AuthOpResult
}

/** Friendly provider name resolution kept in one place. */
internal fun providerLabel(providerId: String?): String = when (providerId) {
    "google.com" -> "Google"
    "password" -> "Email & Password"
    "phone" -> "Phone"
    "apple.com" -> "Apple"
    null, "" -> "Account"
    else -> providerId.replaceFirstChar { it.titlecase() }
}
