package com.ganesh.hisabkitabpro.core.crash

import androidx.compose.runtime.Composable
import com.ganesh.hisabkitabpro.core.feature.ActiveFeatureTracker
import com.ganesh.hisabkitabpro.core.feature.FeatureRecoveryManager
import androidx.compose.runtime.DisposableEffect

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
    DisposableEffect(featureId) {
        ActiveFeatureTracker.setActive(featureId)
        onDispose {
            if (ActiveFeatureTracker.activeFeatureId == featureId) {
                ActiveFeatureTracker.setActive(null)
            }
        }
    }
    ErrorBoundary(featureId = featureId, featureRecoveryManager = recoveryManager) {
        content()
    }
}
