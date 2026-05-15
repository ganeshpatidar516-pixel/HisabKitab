package com.ganesh.hisabkitabpro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * LEGACY bootstrap "Start" button (not reachable from the live nav graph).
 * Preserved under the "Preserve Working Systems" directive — R8 strips it
 * from the release AAB while it has no callers. The live cold-start flow
 * goes directly to the `dashboard` route.
 */
@Deprecated(
    message = "Legacy bootstrap. Not in the live navigation graph; cold-start lands on the dashboard route.",
    level = DeprecationLevel.WARNING
)
@Composable
fun StartScreen(
    onStartClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "HisabKitab Pro",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartClick
            ) {
                Text("Start")
            }
        }
    }
}