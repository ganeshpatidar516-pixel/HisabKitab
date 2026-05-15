@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.auth.AuthState
import com.ganesh.hisabkitabpro.auth.AuthViewModel
import com.ganesh.hisabkitabpro.auth.phone.PhoneDialCodes
import com.ganesh.hisabkitabpro.auth.phone.PhoneDialOption
import com.ganesh.hisabkitabpro.auth.providerLabel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Theme-aware modal sheets for authentication.
 *
 * Every color, surface, and border uses [MaterialTheme.colorScheme] so that
 * the auth UI automatically adapts to whichever theme the user has selected
 * in Settings (Amoled Gold, Slate Luxury, Ivory Gold, Royal Maroon, etc.).
 */

internal enum class AuthSheet { None, SignIn, Profile, LinkPhone }

@Composable
internal fun rememberAuthSheetState(): MutableState<AuthSheet> = remember {
    mutableStateOf(AuthSheet.None)
}

/**
 * Single host that decides which sheet to render based on [sheet]. Allows
 * [ProfileAvatar] and the settings section to share the exact same UX.
 */
@Composable
internal fun AuthSheetHost(
    sheet: AuthSheet,
    onDismiss: () -> Unit,
    viewModel: AuthViewModel
) {
    when (sheet) {
        AuthSheet.None -> Unit
        AuthSheet.SignIn -> SignInBottomSheet(
            viewModel = viewModel,
            onDismiss = onDismiss
        )
        AuthSheet.Profile -> ProfileSummaryBottomSheet(
            viewModel = viewModel,
            onDismiss = onDismiss
        )
        AuthSheet.LinkPhone -> LinkPhoneBottomSheet(
            viewModel = viewModel,
            onDismiss = onDismiss
        )
    }
}

/* ----------------------------- Sign-in sheet ----------------------------- */

@Composable
fun SignInBottomSheet(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var attemptedSubmit by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }

    val phoneLoginEnabled = viewModel.isPhoneLoginFeatureEnabled()
    var phonePanel by remember { mutableStateOf(SignInPhonePanel.Hidden) }
    var phoneDial by remember { mutableStateOf(PhoneDialCodes.default) }
    var nationalDigits by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var resendSeconds by remember { mutableStateOf(0) }
    var lastPhoneE164 by remember { mutableStateOf("") }

    LaunchedEffect(mode) {
        if (mode != AuthMode.SignIn) {
            phonePanel = SignInPhonePanel.Hidden
            phoneDial = PhoneDialCodes.default
            nationalDigits = ""
            otpCode = ""
            resendSeconds = 0
            viewModel.clearPhoneSignInSession()
        }
    }
    LaunchedEffect(phoneLoginEnabled) {
        if (!phoneLoginEnabled) {
            phonePanel = SignInPhonePanel.Hidden
            phoneDial = PhoneDialCodes.default
            nationalDigits = ""
            otpCode = ""
            resendSeconds = 0
        }
    }
    LaunchedEffect(phoneDial) {
        nationalDigits = nationalDigits.filter { it.isDigit() }.take(phoneDial.nationalMaxLen)
    }
    LaunchedEffect(resendSeconds) {
        if (resendSeconds <= 0) return@LaunchedEffect
        delay(1000)
        resendSeconds = resendSeconds - 1
    }
    LaunchedEffect(Unit) {
        viewModel.phoneAwaitingCodeSignal.collect {
            phonePanel = SignInPhonePanel.EnterOtp
            resendSeconds = 45
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            working = false
            // Sheet is dismissed by AuthState listener once sign-in succeeds.
        }
    }
    LaunchedEffect(Unit) {
        viewModel.state.collect { state ->
            if (state is AuthState.LoggedIn) {
                onDismiss()
            }
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: androidx.activity.result.ActivityResult ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(context, "Google sign-in did not return an ID token.", Toast.LENGTH_SHORT).show()
                working = false
            } else {
                viewModel.signInWithGoogleIdToken(idToken)
            }
        } catch (e: ApiException) {
            working = false
            val msg = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                    "Google sign-in was cancelled."
                CommonStatusCodes.NETWORK_ERROR ->
                    "No internet. Check your connection and try again."
                ConnectionResult.DEVELOPER_ERROR ->
                    "Google Sign-In is not set up for this build (SHA-1/256 or OAuth client). Use email or contact support."
                GoogleSignInStatusCodes.SIGN_IN_REQUIRED,
                GoogleSignInStatusCodes.SIGN_IN_FAILED ->
                    "Google sign-in could not complete. Try again or use email."
                else ->
                    "Google sign-in failed (code ${e.statusCode}). Try again or use email."
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        } catch (_: Throwable) {
            working = false
            Toast.makeText(context, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
        }
    }

    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordValid = password.length >= 6

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (mode == AuthMode.SignIn) "Sign in to HisabKitab Pro"
                else "Create your HisabKitab account",
                color = colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Bank-grade encryption · Your local data stays safe.",
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(20.dp))

            // Google: requires `default_web_client_id` from Firebase (merged into R.string).
            val googleSignInAvailable = viewModel.resolveGoogleWebClientId() != null
            GoogleSignInButton(
                enabled = googleSignInAvailable,
                onClick = {
                    val client = viewModel.buildGoogleSignInClient() ?: return@GoogleSignInButton
                    working = true
                    googleLauncher.launch(client.signInIntent)
                }
            )
            if (!googleSignInAvailable) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.auth_google_signin_unavailable_body),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            if (phoneLoginEnabled && mode == AuthMode.SignIn) {
                Spacer(Modifier.height(12.dp))
                when (phonePanel) {
                    SignInPhonePanel.Hidden -> {
                        PhoneSecondarySignInButton(
                            onClick = { phonePanel = SignInPhonePanel.EnterNumber }
                        )
                    }
                    SignInPhonePanel.EnterNumber -> {
                        PhoneNumberOtpStep(
                            phoneDial = phoneDial,
                            onPhoneDialChange = { phoneDial = it },
                            nationalDigits = nationalDigits,
                            onNationalDigitsChange = { raw ->
                                nationalDigits = raw.filter { it.isDigit() }.take(phoneDial.nationalMaxLen)
                            },
                            working = working,
                            onSend = {
                                val digits = nationalDigits.filter { it.isDigit() }.take(phoneDial.nationalMaxLen)
                                if (!phoneDial.isValidNational(digits)) {
                                    Toast.makeText(
                                        context,
                                        "Enter a valid ${phoneDial.digitHint}.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PhoneNumberOtpStep
                                }
                                val act = context.findActivity()
                                if (act == null) {
                                    Toast.makeText(
                                        context,
                                        "Cannot start phone sign-in from here.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PhoneNumberOtpStep
                                }
                                val e164 = "${phoneDial.e164Prefix}$digits"
                                lastPhoneE164 = e164
                                working = true
                                viewModel.beginPhoneSignIn(act, e164, forceResend = false)
                            },
                            onBack = {
                                phonePanel = SignInPhonePanel.Hidden
                                nationalDigits = ""
                                viewModel.clearPhoneSignInSession()
                            }
                        )
                    }
                    SignInPhonePanel.EnterOtp -> {
                        PhoneOtpVerifyStep(
                            otpCode = otpCode,
                            onOtpChange = { s -> otpCode = s.filter { ch -> ch.isDigit() }.take(8) },
                            resendSeconds = resendSeconds,
                            working = working,
                            onVerify = {
                                if (otpCode.trim().length < 4) {
                                    Toast.makeText(
                                        context,
                                        "Enter the code from your SMS.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PhoneOtpVerifyStep
                                }
                                working = true
                                viewModel.confirmPhoneOtp(otpCode.trim())
                            },
                            onResend = {
                                val act = context.findActivity()
                                if (act == null || lastPhoneE164.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Request a new code from the previous step.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PhoneOtpVerifyStep
                                }
                                if (resendSeconds > 0) return@PhoneOtpVerifyStep
                                working = true
                                viewModel.beginPhoneSignIn(act, lastPhoneE164, forceResend = true)
                            },
                            onBack = {
                                phonePanel = SignInPhonePanel.EnterNumber
                                otpCode = ""
                                viewModel.clearPhoneSignInSession()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            DividerWithLabel(label = "OR")
            Spacer(Modifier.height(18.dp))

            // Email / password
            ThemedOutlinedField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                isError = attemptedSubmit && !emailValid,
                supporting = if (attemptedSubmit && !emailValid) "Enter a valid email" else null
            )
            Spacer(Modifier.height(12.dp))
            ThemedOutlinedField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                isError = attemptedSubmit && !passwordValid,
                supporting = if (attemptedSubmit && !passwordValid)
                    "At least 6 characters" else null
            )

            if (mode == AuthMode.SignIn) {
                TextButton(
                    onClick = {
                        if (emailValid) {
                            viewModel.sendPasswordReset(email)
                        } else {
                            attemptedSubmit = true
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot password?", color = colorScheme.primary, fontSize = 12.sp)
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    attemptedSubmit = true
                    if (!emailValid || !passwordValid) return@Button
                    working = true
                    scope.launch {
                        if (mode == AuthMode.SignIn) viewModel.signInWithEmail(email, password)
                        else viewModel.registerWithEmail(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !working,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                if (working) {
                    CircularProgressIndicator(
                        color = colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (mode == AuthMode.SignIn) "Sign in" else "Create account",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {
                    mode = if (mode == AuthMode.SignIn) AuthMode.Register else AuthMode.SignIn
                    attemptedSubmit = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (mode == AuthMode.SignIn) "New here? Create an account"
                    else "Already have an account? Sign in",
                    color = colorScheme.primary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "By continuing you agree to keep your account credentials secure.",
                color = colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ----------------------------- Link phone (Settings) ----------------------------- */

@Composable
fun LinkPhoneBottomSheet(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var phonePanel by remember { mutableStateOf(SignInPhonePanel.EnterNumber) }
    var phoneDial by remember { mutableStateOf(PhoneDialCodes.default) }
    var nationalDigits by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var resendSeconds by remember { mutableStateOf(0) }
    var lastPhoneE164 by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.clearPhoneSignInSession()
        onDispose { viewModel.clearPhoneSignInSession() }
    }

    LaunchedEffect(phoneDial) {
        nationalDigits = nationalDigits.filter { it.isDigit() }.take(phoneDial.nationalMaxLen)
    }
    LaunchedEffect(resendSeconds) {
        if (resendSeconds <= 0) return@LaunchedEffect
        delay(1000)
        resendSeconds = resendSeconds - 1
    }
    LaunchedEffect(Unit) {
        viewModel.phoneAwaitingCodeSignal.collect {
            phonePanel = SignInPhonePanel.EnterOtp
            resendSeconds = 45
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            working = false
            if (msg == "Phone number linked.") {
                viewModel.clearPhoneSignInSession()
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearPhoneSignInSession()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Link mobile number",
                color = colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Adds phone sign-in to this account after SMS verification. " +
                    "Standard SMS rates may apply.",
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(20.dp))

            when (phonePanel) {
                SignInPhonePanel.EnterNumber -> {
                    PhoneNumberOtpStep(
                        phoneDial = phoneDial,
                        onPhoneDialChange = { phoneDial = it },
                        nationalDigits = nationalDigits,
                        onNationalDigitsChange = { raw ->
                            nationalDigits = raw.filter { it.isDigit() }.take(phoneDial.nationalMaxLen)
                        },
                        working = working,
                        onSend = {
                            val digits = nationalDigits.filter { it.isDigit() }.take(phoneDial.nationalMaxLen)
                            if (!phoneDial.isValidNational(digits)) {
                                Toast.makeText(
                                    context,
                                    "Enter a valid ${phoneDial.digitHint}.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@PhoneNumberOtpStep
                            }
                            val act = context.findActivity()
                            if (act == null) {
                                Toast.makeText(
                                    context,
                                    "Cannot start verification from here.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@PhoneNumberOtpStep
                            }
                            val e164 = "${phoneDial.e164Prefix}$digits"
                            lastPhoneE164 = e164
                            working = true
                            viewModel.linkPhoneNumber(act, e164, forceResend = false)
                        },
                        onBack = {
                            viewModel.clearPhoneSignInSession()
                            onDismiss()
                        },
                        backLabel = "Cancel",
                    )
                }
                SignInPhonePanel.EnterOtp -> {
                    PhoneOtpVerifyStep(
                        otpCode = otpCode,
                        onOtpChange = { s -> otpCode = s.filter { ch -> ch.isDigit() }.take(8) },
                        resendSeconds = resendSeconds,
                        working = working,
                        onVerify = {
                            if (otpCode.trim().length < 4) {
                                Toast.makeText(
                                    context,
                                    "Enter the code from your SMS.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@PhoneOtpVerifyStep
                            }
                            working = true
                            viewModel.confirmPhoneOtp(otpCode.trim(), successUserMessage = "Phone number linked.")
                        },
                        onResend = {
                            val act = context.findActivity()
                            if (act == null || lastPhoneE164.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Go back and send a code first.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@PhoneOtpVerifyStep
                            }
                            if (resendSeconds > 0) return@PhoneOtpVerifyStep
                            working = true
                            viewModel.linkPhoneNumber(act, lastPhoneE164, forceResend = true)
                        },
                        onBack = {
                            phonePanel = SignInPhonePanel.EnterNumber
                            otpCode = ""
                            viewModel.clearPhoneSignInSession()
                        },
                        verifyButtonLabel = "Verify & link",
                    )
                }
                SignInPhonePanel.Hidden -> Unit
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private enum class AuthMode { SignIn, Register }

private enum class SignInPhonePanel { Hidden, EnterNumber, EnterOtp }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun PhoneSecondarySignInButton(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colorScheme.outline.copy(alpha = 0.55f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colorScheme.onSurface,
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Continue with mobile number", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PhoneNumberOtpStep(
    phoneDial: PhoneDialOption,
    onPhoneDialChange: (PhoneDialOption) -> Unit,
    nationalDigits: String,
    onNationalDigitsChange: (String) -> Unit,
    working: Boolean,
    onSend: () -> Unit,
    onBack: () -> Unit,
    backLabel: String = "Use Google or email",
) {
    val colorScheme = MaterialTheme.colorScheme
    var dialMenuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Mobile number",
            color = colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = phoneDial.displayLabel,
                onValueChange = {},
                readOnly = true,
                enabled = !working,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { dialMenuExpanded = true },
                label = { Text("Country / region") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    focusedLabelColor = colorScheme.primary,
                    unfocusedLabelColor = colorScheme.onSurfaceVariant,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedTrailingIconColor = colorScheme.primary,
                    unfocusedTrailingIconColor = colorScheme.onSurfaceVariant,
                    cursorColor = colorScheme.primary
                )
            )
            DropdownMenu(
                expanded = dialMenuExpanded,
                onDismissRequest = { dialMenuExpanded = false },
                modifier = Modifier.fillMaxWidth(),
                containerColor = colorScheme.surface
            ) {
                PhoneDialCodes.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayLabel, color = colorScheme.onSurface) },
                        onClick = {
                            dialMenuExpanded = false
                            onPhoneDialChange(option)
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = phoneDial.e164Prefix,
                color = colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            OutlinedTextField(
                value = nationalDigits,
                onValueChange = onNationalDigitsChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(phoneDial.digitHint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    focusedLabelColor = colorScheme.primary,
                    unfocusedLabelColor = colorScheme.onSurfaceVariant,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    cursorColor = colorScheme.primary
                )
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !working,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
        ) {
            if (working) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send verification code", fontWeight = FontWeight.SemiBold)
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(backLabel, color = colorScheme.primary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PhoneOtpVerifyStep(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    resendSeconds: Int,
    working: Boolean,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    verifyButtonLabel: String = "Verify & sign in",
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Enter SMS code",
            color = colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = otpCode,
            onValueChange = onOtpChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Verification code") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outline,
                focusedLabelColor = colorScheme.primary,
                unfocusedLabelColor = colorScheme.onSurfaceVariant,
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface,
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface,
                cursorColor = colorScheme.primary
            )
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !working,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
        ) {
            if (working) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(verifyButtonLabel, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Change number", color = colorScheme.primary, fontSize = 13.sp)
            }
            TextButton(
                onClick = onResend,
                enabled = resendSeconds <= 0 && !working
            ) {
                Text(
                    text = if (resendSeconds > 0) "Resend in ${resendSeconds}s" else "Resend code",
                    color = if (resendSeconds > 0) colorScheme.onSurfaceVariant else colorScheme.primary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/* ----------------------------- Profile sheet ----------------------------- */

@Composable
fun ProfileSummaryBottomSheet(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthState.LoggedOut) onDismiss()
    }

    val user = state as? AuthState.LoggedIn
    if (user == null) {
        // Guard against a race where the user signed out while the sheet was opening.
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, colorScheme.primary, CircleShape)
                        .background(colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val photo = user.photoUrl
                    if (!photo.isNullOrBlank()) {
                        AsyncImage(
                            model = photo,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = colorScheme.onBackground
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName?.takeIf { it.isNotBlank() }
                            ?: user.email?.takeIf { it.isNotBlank() }
                            ?: "Signed in",
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    user.email?.takeIf { it.isNotBlank() && it != user.displayName }?.let {
                        Text(it, color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${providerLabel(user.providerId)} · " +
                                if (user.isEmailVerified) "Verified" else "Unverified",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            ProfileActionRow(
                icon = Icons.Default.ManageAccounts,
                label = "Manage account",
                description = "Open the Account & Cloud Security section in Settings."
            ) {
                onDismiss()
                // The settings entry-point lives in the Settings screen at the
                // bottom; we just dismiss here so the user can scroll there.
            }

            Spacer(Modifier.height(8.dp))

            ProfileActionRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "Sign out",
                description = "End this session on this device.",
                tone = ActionTone.Primary
            ) {
                viewModel.signOut()
            }

            Spacer(Modifier.height(8.dp))

            ProfileActionRow(
                icon = Icons.Default.Lock,
                label = "Delete cloud sign-in",
                description = "Removes Firebase account only. For full device + data erasure, use Settings → Privacy & data.",
                tone = ActionTone.Destructive
            ) {
                showDelete = true
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurfaceVariant,
            title = { Text("Delete cloud sign-in only?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes your Firebase sign-in from Google servers. " +
                        "Your local ledger on this device is not changed. " +
                        "To permanently erase local data and mirrored cloud copies together, " +
                        "use Settings → Privacy & data → Erase data & delete account."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        viewModel.deleteAccount()
                    }
                ) { Text("Delete", color = colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel", color = colorScheme.onSurface)
                }
            }
        )
    }
}

/* ----------------------------- Atoms ----------------------------- */

@Composable
private fun GoogleSignInButton(enabled: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colorScheme.outline.copy(alpha = if (enabled) 0.55f else 0.25f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colorScheme.onSurface,
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(colorScheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Text("G", color = colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text("Continue with Google", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DividerWithLabel(label: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colorScheme.outline.copy(alpha = 0.35f))
        )
        Text(
            text = label,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colorScheme.outline.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun ThemedOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    isError: Boolean = false,
    supporting: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, null) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        supportingText = supporting?.let { { Text(it, fontSize = 11.sp) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outline,
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.onSurfaceVariant,
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            focusedContainerColor = colorScheme.surface,
            unfocusedContainerColor = colorScheme.surface,
            focusedLeadingIconColor = colorScheme.primary,
            unfocusedLeadingIconColor = colorScheme.onSurfaceVariant,
            cursorColor = colorScheme.primary,
            errorBorderColor = colorScheme.error,
            errorLabelColor = colorScheme.error
        )
    )
}

private enum class ActionTone { Default, Primary, Destructive }

@Composable
private fun ProfileActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    tone: ActionTone = ActionTone.Default,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = when (tone) {
        ActionTone.Default -> colorScheme.onSurface
        ActionTone.Primary -> colorScheme.primary
        ActionTone.Destructive -> colorScheme.error
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colorScheme.outline.copy(alpha = 0.30f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = accent, fontWeight = FontWeight.SemiBold)
                Text(description, color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
