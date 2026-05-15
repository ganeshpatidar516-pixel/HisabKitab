package com.ganesh.hisabkitabpro.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ganesh.hisabkitabpro.auth.AuthState
import com.ganesh.hisabkitabpro.auth.AuthViewModel
import com.ganesh.hisabkitabpro.auth.providerLabel

/**
 * "Account & Cloud Security" section to be appended at the very bottom of the
 * existing Settings screen.
 *
 * THEME RULES:
 * - Inherits 100% from [MaterialTheme.colorScheme]; no hardcoded colors.
 *
 * SAFETY:
 * - Self-contained — does not modify any existing setting, ViewModel state,
 *   or DB schema.
 * - Drives sign-in / sign-out via the same [AuthViewModel] used by
 *   [ProfileAvatar], so behaviour is consistent across the app.
 *
 * USAGE: place inside the trailing `LazyColumn { item { ... } }` of
 * `SettingsScreen.kt`.
 */
@Composable
fun AccountSettingsSection(
    modifier: Modifier = Modifier,
    /** When false, omits the uppercase in-section title (use when Settings already shows a parent section header). */
    showSectionHeader: Boolean = true,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberAuthSheetState()

    AuthSheetHost(
        sheet = sheet,
        onDismiss = { sheet = AuthSheet.None },
        viewModel = viewModel
    )

    Column(modifier = modifier.fillMaxWidth().testTag("hk_settings_account_section")) {
        if (showSectionHeader) {
            SectionHeader(title = "ACCOUNT & CLOUD SECURITY")
        }

        AccountStatusCard(state = state)

        Spacer(Modifier.height(12.dp))

        when (state) {
            is AuthState.LoggedIn -> {
                LoggedInActions(
                    onSignOutClick = { viewModel.signOut() },
                    onProfileClick = { sheet = AuthSheet.Profile }
                )
                Spacer(Modifier.height(8.dp))
                AccountActionRow(
                    icon = Icons.Default.Phone,
                    title = "Link mobile number",
                    description = "Verify by SMS to add phone sign-in to this account. " +
                        "Requires Phone auth in Firebase Console.",
                    onClick = { sheet = AuthSheet.LinkPhone },
                    testTag = "hk_settings_link_phone"
                )
            }
            else -> {
                LoggedOutActions(onSignInClick = { sheet = AuthSheet.SignIn })
            }
        }

        Spacer(Modifier.height(10.dp))

        PhoneSignInBetaToggleRow(viewModel = viewModel)

        Spacer(Modifier.height(8.dp))

        if (showSectionHeader) {
            Text(
                text = "International security: Firebase Authentication, " +
                    "AES-encrypted local storage, and end-to-end TLS for all sync.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
        } else {
            Text(
                text = "Firebase sign-in · encrypted device storage · TLS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

/* ----------------------------- internals ----------------------------- */

@Composable
private fun PhoneSignInBetaToggleRow(viewModel: AuthViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    var checked by remember { mutableStateOf(viewModel.isPhoneLoginFeatureEnabled()) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("hk_settings_phone_sign_in_beta"),
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mobile OTP sign-in (beta)",
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Shows \"Continue with mobile number\" on the sign-in sheet only. " +
                        "Linking from Settings works separately. Requires Phone auth in Firebase Console.",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = { v ->
                    viewModel.setPhoneLoginFeatureEnabled(v)
                    checked = v
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorScheme.primary,
                    checkedTrackColor = colorScheme.primary.copy(alpha = 0.45f)
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 12.dp),
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp
    )
}

@Composable
private fun AccountStatusCard(state: AuthState) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                is AuthState.LoggedIn -> {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(2.dp, colorScheme.primary, CircleShape)
                            .background(colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val photo = state.photoUrl
                        if (!photo.isNullOrBlank()) {
                            AsyncImage(
                                model = photo,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(CircleShape)
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
                            state.displayName?.takeIf { it.isNotBlank() }
                                ?: state.email
                                ?: "Signed in",
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        state.email?.takeIf { it.isNotBlank() && it != state.displayName }?.let {
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
                                text = providerLabel(state.providerId) + " · " +
                                    if (state.isEmailVerified) "Verified" else "Unverified",
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                is AuthState.LoggedOut, AuthState.Loading -> {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Not signed in",
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Sign in to enable cloud sync and protect your ledger.",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoggedInActions(
    onSignOutClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountActionRow(
            icon = Icons.Default.AccountCircle,
            title = "Manage account",
            description = "View profile, provider, and email verification status.",
            onClick = onProfileClick,
            testTag = "hk_settings_account_manage"
        )
        AccountActionRow(
            icon = Icons.Default.CloudSync,
            title = "Cloud sync (account-scoped)",
            description = "Future ledger sync runs are stamped with your account UID.",
            onClick = onProfileClick
        )
        AccountActionRow(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Sign out",
            description = "End your session on this device.",
            onClick = onSignOutClick,
            tone = AccountActionTone.Primary,
            testTag = "hk_settings_account_signout"
        )
    }
}

@Composable
private fun LoggedOutActions(onSignInClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountActionRow(
            icon = Icons.AutoMirrored.Filled.Login,
            title = "Sign in / Create account",
            description = "Use Google or Email & Password.",
            onClick = onSignInClick,
            tone = AccountActionTone.Primary,
            testTag = "hk_settings_account_signin"
        )
    }
}

private enum class AccountActionTone { Default, Primary }

@Composable
private fun AccountActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    tone: AccountActionTone = AccountActionTone.Default,
    testTag: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = when (tone) {
        AccountActionTone.Default -> colorScheme.onSurface
        AccountActionTone.Primary -> colorScheme.primary
    }
    val baseModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
    val rowModifier = if (testTag != null) baseModifier.testTag(testTag) else baseModifier

    Surface(
        modifier = rowModifier,
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = accent, fontWeight = FontWeight.SemiBold)
                Text(description, color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
