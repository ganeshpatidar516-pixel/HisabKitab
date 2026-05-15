package com.ganesh.hisabkitabpro.utils

import android.os.SystemClock
import androidx.compose.runtime.*

@Composable
fun rememberSafeClick(
    delay: Long = 800L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    return {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= delay) {
            lastClickTime = currentTime
            onClick()
        }
    }
}
