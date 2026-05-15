package com.ganesh.hisabkitabpro.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganesh.hisabkitabpro.R

/**
 * SCAFFOLDING (not a runtime caller yet) — paired with [GuidePreferences].
 *
 * Designed wiring (intended host: DashboardScreen):
 * ```
 *   val ctx = LocalContext.current
 *   val guidePrefs = remember { GuidePreferences(ctx) }
 *   var showGuide by remember { mutableStateOf(guidePrefs.shouldShowDashboardGuide()) }
 *   if (showGuide) {
 *     DashboardFirstRunGuideOverlay(onDismiss = {
 *       guidePrefs.markDashboardGuideDone()
 *       showGuide = false
 *     })
 *   }
 * ```
 *
 * Preserved intentionally per the "Preserve Working Systems" directive — the
 * androidTest `HisabMainScreen.guideDone` selector and `SacredFlowComposeHiltTest`
 * defensively dismiss this overlay if present, so wiring it later will not break
 * the existing Sacred Flow instrumentation suite. R8 strips this composable from
 * release builds while it remains unused.
 */
@Composable
fun DashboardFirstRunGuideOverlay(
    onDismiss: () -> Unit
) {
    val steps = listOf(
        stringResource(R.string.guide_dashboard_step1),
        stringResource(R.string.guide_dashboard_step2),
        stringResource(R.string.guide_dashboard_step3)
    )
    var index by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sacred_guide_overlay")
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.guide_dashboard_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    steps[index],
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("sacred_guide_done")
                    ) {
                        Text(stringResource(R.string.guide_done))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (index < steps.lastIndex) index++
                            else onDismiss()
                        }
                    ) {
                        Text(
                            if (index < steps.lastIndex) {
                                stringResource(R.string.guide_next)
                            } else {
                                stringResource(R.string.guide_done)
                            }
                        )
                    }
                }
            }
        }
    }
}
