package com.ganesh.hisabkitabpro.core.crash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ganesh.hisabkitabpro.core.feature.FeatureRecoveryManager

/**
 * Optional **feature-flag** gate for experimental modules ([SafeFeature]).
 *
 * P3 honesty: this does **not** catch Jetpack Compose composition/render exceptions.
 * Process-level crashes are handled by [GlobalCrashHandler] + Firebase Crashlytics.
 */
@Composable
fun ErrorBoundary(
    featureId: String? = null,
    featureRecoveryManager: FeatureRecoveryManager? = null,
    content: @Composable () -> Unit
) {
    if (featureId != null && featureRecoveryManager?.isFeatureEnabled(featureId) == false) {
        DisabledFeatureUI(featureId) {
            featureRecoveryManager.resetFeature(featureId)
        }
    } else {
        content()
    }
}

@Composable
fun DisabledFeatureUI(featureId: String, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Feature temporarily disabled", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Module '$featureId' was turned off after repeated failures. You can try enabling it again.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onReset) {
            Text("Enable again")
        }
    }
}
