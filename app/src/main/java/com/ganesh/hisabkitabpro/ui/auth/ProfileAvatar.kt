package com.ganesh.hisabkitabpro.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.ganesh.hisabkitabpro.auth.AuthState
import com.ganesh.hisabkitabpro.auth.AuthViewModel

/**
 * Theme-aware authentication gateway icon for the dashboard header.
 *
 * THEME RULES (per spec):
 * - Pulls every color from [MaterialTheme.colorScheme] — no hardcoded colors.
 *   It therefore inherits Light, Dark, AMOLED, and any custom theme defined in
 *   the existing theme engine.
 *
 * STATES:
 * - [AuthState.LoggedOut]      → silhouette icon. Click triggers
 *   [com.ganesh.hisabkitabpro.ui.auth.SignInBottomSheet].
 * - [AuthState.LoggedIn]       → real profile picture (or initial fallback)
 *   with a primary-colored circular border. Click opens
 *   [com.ganesh.hisabkitabpro.ui.auth.ProfileSummaryBottomSheet].
 * - [AuthState.Loading]        → silhouette behind a primary-tinted halo.
 *
 * INTEGRATION (single drop-in):
 *   Replace the static `Surface { Icon(Icons.Default.Person, ...) }` in
 *   `DashboardScreen.kt` with a single call to [ProfileAvatar].
 *
 *   Example:
 *   ```kotlin
 *   ProfileAvatar()  // Hilt-VM auto-resolved
 *   ```
 */
@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 40,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberAuthSheetState()

    AuthSheetHost(
        sheet = sheet,
        onDismiss = { sheet = AuthSheet.None },
        viewModel = viewModel
    )

    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .testTag("hk_profile_avatar"),
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is AuthState.LoggedIn -> {
                Surface(
                    modifier = Modifier
                        .size(sizeDp.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = colorScheme.primary,
                            shape = CircleShape
                        ),
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.12f),
                    onClick = { sheet = AuthSheet.Profile }
                ) {
                    val photo = s.photoUrl
                    if (!photo.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photo)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(sizeDp.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        InitialFallback(
                            display = s.displayName ?: s.email ?: "U",
                            sizeDp = sizeDp
                        )
                    }
                }
            }

            else -> {
                Surface(
                    modifier = Modifier.size(sizeDp.dp),
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.18f),
                    onClick = { sheet = AuthSheet.SignIn }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Sign in",
                            tint = colorScheme.onBackground,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialFallback(display: String, sizeDp: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val initial = display.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .background(colorScheme.primary.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = initial,
            color = colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
