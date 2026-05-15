package com.ganesh.hisabkitabpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * LEGACY splash composable (not reachable from the live nav graph). The live
 * splash flow is `com.ganesh.hisabkitabpro.ui.launch.LaunchScreen`. Preserved
 * under the "Preserve Working Systems" directive — R8 strips it from the
 * release AAB while it has no callers.
 */
@Deprecated(
    message = "Legacy splash. Use ui.launch.LaunchScreen instead. Not in the live navigation graph.",
    level = DeprecationLevel.WARNING
)
@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1.1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500) // 2.5 सेकंड का प्रीमियम अहसास
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.background,
                        colorScheme.primary.copy(alpha = 0.45f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // 3D Style Animated Logo Container
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp),
                shape = CircleShape,
                color = colorScheme.surface.copy(alpha = 0.18f),
                tonalElevation = 12.dp,
                shadowElevation = 20.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Logo",
                        modifier = Modifier.size(70.dp),
                        tint = colorScheme.tertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "HisabKitab Pro",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.onBackground,
                letterSpacing = 1.sp
            )
            
            Text(
                text = "THE ULTRA AI OPERATING SYSTEM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 5.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Simple Loading Indicator for a Professional Feel
            LinearProgressIndicator(
                modifier = Modifier
                    .width(150.dp)
                    .height(2.dp),
                color = colorScheme.tertiary,
                trackColor = colorScheme.onBackground.copy(alpha = 0.1f)
            )
        }
    }
}
