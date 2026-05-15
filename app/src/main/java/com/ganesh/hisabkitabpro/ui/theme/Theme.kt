package com.ganesh.hisabkitabpro.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * HISABKITAB PRO - MASTER THEME SYSTEM
 * Professional, Error-Free, Global Theme Management.
 */

data class PremiumThemeSpec(
    val id: String,
    val displayName: String,
    val previewPrimary: Color,
    val previewBackground: Color
)

val PremiumThemeCatalog = listOf(
    PremiumThemeSpec(
        id = "amoled_gold",
        displayName = "Amoled Gold",
        previewPrimary = RajwadiGold,
        previewBackground = Color.Black
    ),
    PremiumThemeSpec(
        id = "slate_luxury",
        displayName = "Slate Luxury",
        previewPrimary = SlateLuxuryPrimary,
        previewBackground = SlateLuxuryBg
    ),
    PremiumThemeSpec(
        id = "ivory_gold",
        displayName = "Ivory Gold",
        previewPrimary = IvoryGoldPrimary,
        previewBackground = IvoryGoldBg
    ),
    PremiumThemeSpec(
        id = "midnight_pro",
        displayName = "Midnight Pro",
        previewPrimary = MidnightProPrimary,
        previewBackground = MidnightProBg
    )
)

/**
 * [LocalView.current.context] is often a [ContextThemeWrapper], not [Activity] — casting crashes the process.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> {
        val base = baseContext
        if (base === this) null else base.findActivity()
    }
    else -> null
}

/** Maps removed theme ids so persisted settings and the theme picker stay consistent. */
fun normalizeDeprecatedThemeId(themeId: String?): String {
    val raw = (themeId ?: "amoled_gold").lowercase().trim()
    return if (raw == "oceanic_blue") "amoled_gold" else raw
}

private val AmoledGoldScheme = darkColorScheme(
    primary = RajwadiGold,
    onPrimary = Color.Black,
    primaryContainer = RajwadiGoldDeep,
    onPrimaryContainer = Color.White,
    secondary = RajwadiGoldLight,
    onSecondary = Color.Black,
    background = Color.Black,
    surface = Color.Black,
    onBackground = RajwadiGold,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    outline = RajwadiGold.copy(alpha = 0.5f)
)

private val MidnightProScheme = darkColorScheme(
    primary = MidnightProPrimary,
    onPrimary = Color.Black,
    background = MidnightProBg,
    surface = MidnightProBg,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF1C1C1E),
    onPrimaryContainer = Color.White
)

private val DeepVelvetScheme = darkColorScheme(
    primary = DeepVelvetPrimary,
    onPrimary = Color.Black,
    background = DeepVelvetBg,
    surface = DeepVelvetBg,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF4D1C52),
    onPrimaryContainer = DeepVelvetPrimary
)

private val ForestDarkScheme = darkColorScheme(
    primary = ForestDarkPrimary,
    onPrimary = Color.Black,
    background = ForestDarkBg,
    surface = ForestDarkBg,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF064420),
    onPrimaryContainer = ForestDarkPrimary
)

private val GunmetalScheme = darkColorScheme(
    primary = GunmetalPrimary,
    onPrimary = Color.Black,
    background = GunmetalBg,
    surface = GunmetalBg,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = GunmetalPrimary
)

private val RoyalMaroonScheme = darkColorScheme(
    primary = RoyalMaroonPrimary,
    onPrimary = Color.Black,
    background = RoyalMaroonBg,
    surface = RoyalMaroonBg,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Color(0xFF5A0101),
    onPrimaryContainer = RoyalMaroonPrimary
)

private val SlateLuxuryScheme = darkColorScheme(
    primary = SlateLuxuryPrimary,
    onPrimary = SlateLuxuryBg,
    background = DeepCharcoal, 
    surface = DeepCharcoal,
    onBackground = SlateLuxuryPrimary,
    onSurface = Color.White,
    surfaceVariant = GlassWhite,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f)
)

private val IvoryGoldScheme = lightColorScheme(
    primary = Color(0xFFD4AF37), // Gold accent on light
    onPrimary = Color.White,
    background = IvoryGoldBg,
    surface = IvoryGoldBg,
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceVariant = Color.Black.copy(alpha = 0.05f),
    onSurfaceVariant = Color.Black.copy(alpha = 0.6f),
    primaryContainer = Color.White,
    onPrimaryContainer = Color(0xFFD4AF37)
)

@Composable
fun HisabKitabProTheme(
    themeType: String = "amoled_gold",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType.lowercase()) {
        "amoled_gold" -> AmoledGoldScheme
        "midnight_pro" -> MidnightProScheme
        "deep_velvet" -> DeepVelvetScheme
        "forest_dark" -> ForestDarkScheme
        "gunmetal" -> GunmetalScheme
        "royal_maroon" -> RoyalMaroonScheme
        "slate_luxury" -> SlateLuxuryScheme
        "ivory_gold" -> IvoryGoldScheme
        else -> AmoledGoldScheme
    }

    val view = LocalView.current
    val composeContext = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = composeContext.findActivity() ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = themeType.lowercase() == "ivory_gold"
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                content = content
            )
        }
    )
}
