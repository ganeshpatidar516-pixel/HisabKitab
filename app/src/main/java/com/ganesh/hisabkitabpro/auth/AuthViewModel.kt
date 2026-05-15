package com.ganesh.hisabkitabpro.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.auth.phone.PhoneAuthRepository
import com.ganesh.hisabkitabpro.auth.phone.PhoneVerificationStartResult
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI-facing wrapper around [AuthRepository].
 *
 * Exposes:
 * - [state] : current [AuthState] as a [StateFlow] for Compose collection.
 * - [messages] : transient feedback messages (snackbars).
 *
 * All operations are non-blocking and route through [viewModelScope].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val phoneAuthRepository: PhoneAuthRepository
) : ViewModel() {

    val state: StateFlow<AuthState> = repository.authStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = repository.currentState()
        )

    private val _messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages = _messages.asSharedFlow()

    /** Fires when Firebase SMS verification id is ready — show OTP UI. Not used for instant verify. */
    private val _phoneAwaitingCodeSignal = Channel<Unit>(capacity = Channel.BUFFERED)
    val phoneAwaitingCodeSignal = _phoneAwaitingCodeSignal.receiveAsFlow()

    fun resolveGoogleWebClientId(): String? = repository.resolveGoogleWebClientId()

    /**
     * Single source for the Google Sign-In client so UI and [AuthRepository]
     * never drift on [com.google.android.gms.auth.api.signin.GoogleSignInOptions].
     */
    fun buildGoogleSignInClient(): GoogleSignInClient? = repository.buildGoogleSignInClient()

    /** When `true`, sign-in sheet may show phone OTP entry (default off via prefs). */
    fun isPhoneLoginFeatureEnabled(): Boolean = phoneAuthRepository.isFeatureEnabled()

    /** Settings toggle — enables “Continue with mobile number” on the sign-in sheet. */
    fun setPhoneLoginFeatureEnabled(enabled: Boolean) {
        phoneAuthRepository.setPhoneLoginFeatureEnabled(enabled)
    }

    /** Clears in-memory phone verification state (e.g. user backs out of OTP UI). */
    fun clearPhoneSignInSession() {
        phoneAuthRepository.clearPhoneVerificationSession()
    }

    fun beginPhoneSignIn(activity: Activity, e164Phone: String, forceResend: Boolean = false) {
        viewModelScope.launch {
            when (val res = phoneAuthRepository.beginPhoneSignIn(activity, e164Phone, forceResend)) {
                PhoneVerificationStartResult.AwaitingSmsCode -> {
                    _messages.emit(
                        if (forceResend) "A new verification code was sent."
                        else "Verification code sent."
                    )
                    _phoneAwaitingCodeSignal.trySend(Unit)
                }
                PhoneVerificationStartResult.CompletedWithoutSmsUi -> {
                    _messages.emit("Signed in with mobile.")
                }
                is PhoneVerificationStartResult.Failed -> _messages.emit(res.message)
            }
        }
    }

    /** Link phone to the current Firebase user (Settings → Link mobile number). */
    fun linkPhoneNumber(activity: Activity, e164Phone: String, forceResend: Boolean = false) {
        viewModelScope.launch {
            when (val res = phoneAuthRepository.linkCurrentUserPhone(activity, e164Phone, forceResend)) {
                PhoneVerificationStartResult.AwaitingSmsCode -> {
                    _messages.emit(
                        if (forceResend) "A new verification code was sent."
                        else "Verification code sent."
                    )
                    _phoneAwaitingCodeSignal.trySend(Unit)
                }
                PhoneVerificationStartResult.CompletedWithoutSmsUi -> {
                    _messages.emit("Phone number linked.")
                }
                is PhoneVerificationStartResult.Failed -> _messages.emit(res.message)
            }
        }
    }

    fun confirmPhoneOtp(code: String, successUserMessage: String = "Signed in with mobile.") {
        viewModelScope.launch {
            when (val res = phoneAuthRepository.confirmPhoneOtp(code)) {
                is AuthOpResult.Success -> _messages.emit(successUserMessage)
                is AuthOpResult.Failure -> _messages.emit(res.userMessage)
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            when (val res = repository.signInWithGoogleIdToken(idToken)) {
                is AuthOpResult.Success -> _messages.emit("Signed in with Google.")
                is AuthOpResult.Failure -> _messages.emit(res.userMessage)
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            when (val res = repository.signInWithEmail(email, password)) {
                is AuthOpResult.Success -> _messages.emit("Welcome back!")
                is AuthOpResult.Failure -> _messages.emit(res.userMessage)
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            when (val res = repository.registerWithEmail(email, password)) {
                is AuthOpResult.Success -> _messages.emit("Account created. You're signed in.")
                is AuthOpResult.Failure -> _messages.emit(res.userMessage)
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            when (val res = repository.sendPasswordReset(email)) {
                is AuthOpResult.Success -> _messages.emit("Password reset email sent.")
                is AuthOpResult.Failure -> _messages.emit(res.userMessage)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            when (val res = repository.signOut()) {
                is AuthOpResult.Success -> _messages.emit("Signed out.")
                is AuthOpResult.Failure -> _messages.emit(res.userMessage)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            when (val res = repository.deleteAccount()) {
                is AuthOpResult.Success -> _messages.emit(
                    "Cloud sign-in removed. Local data on this device was not changed. " +
                        "To erase this device too, open Settings → Privacy & data."
                )
                is AuthOpResult.Failure -> _messages.emit(res.userMessage)
            }
        }
    }
}
