package com.ganesh.hisabkitabpro.util

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * HISABKITAB PRO - ULTRA STABLE UI EXTENSIONS
 * Prevents double-taps and ensures smooth interaction.
 * Optimized for 100% Stability - No unnecessary coroutine launches.
 */

fun Modifier.safeClickable(
    enabled: Boolean = true,
    debounceTime: Long = 500L,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    clickable(enabled = enabled) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > debounceTime) {
            lastClickTime = currentTime
            onClick()
        }
    }
}
