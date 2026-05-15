package com.ganesh.hisabkitabpro.core.security

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ganesh.hisabkitabpro.security.SecurityManager

/**
 * Blocks screenshots / recent-task thumbnails on sensitive screens when privacy mode is on.
 * Restores previous [WindowManager.LayoutParams.FLAG_SECURE] state on dispose.
 */
@Composable
fun ApplyPrivacyScreenSecure(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled, context) {
        val activity = context as? Activity
        if (activity != null && enabled) {
            val window = activity.window
            val hadSecure = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onDispose {
                if (!hadSecure) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        } else {
            onDispose { }
        }
    }
}

/** Reads [SecurityManager.KEY_SCREEN_PRIVACY_SECURE] from primary prefs (default on). */
@Composable
fun PrivacySecureEffect() {
    val context = LocalContext.current
    val enabled = remember(context) {
        context.applicationContext
            .getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
            .getBoolean(SecurityManager.KEY_SCREEN_PRIVACY_SECURE, true)
    }
    ApplyPrivacyScreenSecure(enabled)
}
