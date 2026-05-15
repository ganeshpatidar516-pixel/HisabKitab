package com.ganesh.hisabkitabpro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.auth.AuthOpResult
import com.ganesh.hisabkitabpro.auth.AuthRepository
import com.ganesh.hisabkitabpro.privacy.AccountDeletionCoordinator
import com.ganesh.hisabkitabpro.privacy.AccountDeletionOutcome
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AccountDeletionViewModel @Inject constructor(
    private val coordinator: AccountDeletionCoordinator,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun runDeletion(
        typedDeleteOk: Boolean,
        email: String,
        password: String?,
        googleIdToken: String?,
        onResult: (AccountDeletionOutcome) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(executeDeletion(typedDeleteOk, email, password, googleIdToken))
        }
    }

    suspend fun executeDeletion(
        typedDeleteOk: Boolean,
        email: String,
        password: String?,
        googleIdToken: String?,
    ): AccountDeletionOutcome {
        if (!typedDeleteOk) {
            return AccountDeletionOutcome.Failed("Type DELETE exactly to continue.")
        }

        _busy.value = true
        return try {
            val user = firebaseAuth.currentUser
            val providerIds = user?.providerData?.map { it.providerId }?.toSet().orEmpty()

            if (user != null) {
                val hasEmail = providerIds.contains(EmailAuthProvider.PROVIDER_ID)
                val hasGoogle = providerIds.contains(GoogleAuthProvider.PROVIDER_ID)

                if (!googleIdToken.isNullOrBlank()) {
                    when (val r = authRepository.reauthenticateWithGoogleIdToken(googleIdToken)) {
                        is AuthOpResult.Failure -> return AccountDeletionOutcome.Failed(r.userMessage)
                        is AuthOpResult.Success -> Unit
                    }
                } else if (hasEmail && !password.isNullOrBlank()) {
                    val em = email.trim().ifBlank { user.email.orEmpty() }
                    if (em.isBlank()) {
                        return AccountDeletionOutcome.Failed(
                            "No email on file for re-authentication. Use Google verify."
                        )
                    }
                    when (val r = authRepository.reauthenticateWithEmailPassword(em, password)) {
                        is AuthOpResult.Failure -> return AccountDeletionOutcome.Failed(r.userMessage)
                        is AuthOpResult.Success -> Unit
                    }
                } else {
                    if (hasEmail && password.isNullOrBlank()) {
                        return AccountDeletionOutcome.Failed("Enter your account password to continue.")
                    }
                    if (hasGoogle && googleIdToken.isNullOrBlank()) {
                        return AccountDeletionOutcome.Failed(
                            "Verify with Google to continue (or use your password if you use email sign-in)."
                        )
                    }
                }
            }

            coordinator.runPlayStoreAccountDeletionPipeline()
        } finally {
            _busy.value = false
        }
    }
}
