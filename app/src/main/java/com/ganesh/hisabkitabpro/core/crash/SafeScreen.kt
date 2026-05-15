package com.ganesh.hisabkitabpro.core.crash

import androidx.compose.runtime.Composable
import com.ganesh.hisabkitabpro.core.feature.FeatureRecoveryManager

/**
 * Root Compose wrapper — passes through content unchanged.
 *
 * Uncaught exceptions in composables are **not** isolated here; they terminate the process
 * and are recorded by [GlobalCrashHandler]. Use [SafeFeature] only when a module is wired
 * to [FeatureRecoveryManager] for explicit disable-after-repeated-crashes.
 */
@Composable
fun SafeScreen(content: @Composable () -> Unit) {
    ErrorBoundary {
        content()
    }
}

@Composable
fun SafeFeature(
    featureId: String,
    recoveryManager: FeatureRecoveryManager,
    content: @Composable () -> Unit
) {
    ErrorBoundary(featureId = featureId, featureRecoveryManager = recoveryManager) {
        content()
    }
}
