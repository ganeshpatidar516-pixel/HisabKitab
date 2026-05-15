@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganesh.hisabkitabpro.MainActivity
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.auth.AuthViewModel
import com.ganesh.hisabkitabpro.privacy.AccountDeletionOutcome
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun PrivacyAndDataScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountDeletionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val googleSignInAvailable = authViewModel.resolveGoogleWebClientId() != null
    var confirmText by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pendingGoogleIdToken by remember { mutableStateOf<String?>(null) }
    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }

    LaunchedEffect(firebaseUser?.email) {
        if (email.isBlank()) {
            email = firebaseUser?.email.orEmpty()
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(context, context.getString(R.string.privacy_data_google_failed), Toast.LENGTH_SHORT)
                    .show()
            } else {
                pendingGoogleIdToken = idToken
                Toast.makeText(context, "Google verified. Tap erase to finish.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign-in was cancelled."
                CommonStatusCodes.NETWORK_ERROR -> "No internet. Check your connection."
                ConnectionResult.DEVELOPER_ERROR ->
                    "Google Sign-In is not set up for this build (SHA / OAuth client)."
                else -> "Google sign-in failed (code ${e.statusCode})."
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        } catch (_: Throwable) {
            Toast.makeText(context, context.getString(R.string.privacy_data_google_failed), Toast.LENGTH_SHORT)
                .show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.privacy_data_screen_title),
                        fontWeight = FontWeight.Black,
                        color = colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.primary,
                    navigationIconContentColor = colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(R.string.privacy_data_intro),
                color = colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.privacy_data_what_removed_title),
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            Bullet(stringResource(R.string.privacy_data_bullet_local))
            Bullet(stringResource(R.string.privacy_data_bullet_files))
            Bullet(stringResource(R.string.privacy_data_bullet_prefs))
            Bullet(stringResource(R.string.privacy_data_bullet_drive))
            Bullet(stringResource(R.string.privacy_data_bullet_cloud))
            Bullet(stringResource(R.string.privacy_data_bullet_locale))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.privacy_data_warning),
                color = colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            if (firebaseUser == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.privacy_data_signed_out_note),
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = confirmText,
                onValueChange = { confirmText = it },
                label = { Text(stringResource(R.string.privacy_data_type_delete_label)) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            if (firebaseUser != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.privacy_data_email_label)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.privacy_data_password_label)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val act = context.findActivity() ?: return@OutlinedButton
                        val client = authViewModel.buildGoogleSignInClient() ?: return@OutlinedButton
                        googleLauncher.launch(client.signInIntent)
                    },
                    enabled = !busy && googleSignInAvailable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.privacy_data_google_verify))
                }
                if (!googleSignInAvailable) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.auth_google_signin_unavailable_body),
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch {
                        val outcome = viewModel.executeDeletion(
                            typedDeleteOk = confirmText.trim() == "DELETE",
                            email = email,
                            password = password.takeIf { it.isNotBlank() },
                            googleIdToken = pendingGoogleIdToken,
                        )
                        when (outcome) {
                            is AccountDeletionOutcome.Failed ->
                                Toast.makeText(context, outcome.userMessage, Toast.LENGTH_LONG).show()
                            is AccountDeletionOutcome.CompletedDeviceOnly,
                            is AccountDeletionOutcome.CompletedFullRemoval -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.privacy_data_success_restart),
                                    Toast.LENGTH_SHORT
                                ).show()
                                val act = context.findActivity() ?: return@launch
                                act.finishAffinity()
                                act.startActivity(
                                    Intent(act, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    }
                                )
                            }
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(22.dp)
                                .padding(end = 10.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.onError
                        )
                    }
                    Text(
                        if (busy) stringResource(R.string.privacy_data_deleting)
                        else stringResource(R.string.privacy_data_delete_button),
                        color = colorScheme.onError
                    )
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text(
        "• $text",
        modifier = Modifier.padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
