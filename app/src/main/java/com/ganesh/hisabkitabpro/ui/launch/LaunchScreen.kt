package com.ganesh.hisabkitabpro.ui.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Brand & Trust Launch Screen ("Security Shield") for HisabKitab Pro.
 *
 * MODULE BOUNDARY:
 * - Pure Compose, no DI / DB / domain dependencies.
 * - Safe to overlay on top of [com.ganesh.hisabkitabpro.MainContainer]; the existing
 *   ledger / billing / settings flows remain 100% untouched.
 *
 * DESIGN:
 * - Pure AMOLED Black background (#000000).
 * - Royal Gold (#D4AF37) accents with subtle shimmer on the wordmark.
 * - Vault-style concentric progress rings (slow + fast counter-rotation).
 * - Sequential trust badges: "Encryption Active" -> "Security Audit Passed".
 * - Minimalist "Make in India" emblem.
 * - Footer: "Secured by OMNI-v5 System Architecture".
 */

private val AmoledBlack = Color(0xFF000000)
private val RoyalGold = Color(0xFFD4AF37)
private val RoyalGoldDeep = Color(0xFF8C6F1F)
private val RoyalGoldLight = Color(0xFFF1D67A)
private val GoldOnDark = Color(0xFFE9C75A)

// "Make in India" tricolor (subtle, premium, not loud).
private val FlagSaffron = Color(0xFFFF9933)
private val FlagWhite = Color(0xFFEFEFEF)
private val FlagGreen = Color(0xFF138808)
private val FlagNavy = Color(0xFF0A2972)

/**
 * Top-level launch composable.
 *
 * @param onLaunchComplete invoked once the trust sequence finishes. The host
 *   (e.g. MainActivity) should fade this screen out and keep the underlying
 *   MainContainer mounted for a flicker-free transition.
 */
@Composable
fun HisabKitabLaunchScreen(
    onLaunchComplete: () -> Unit
) {
    var encryptionVisible by remember { mutableStateOf(false) }
    var auditVisible by remember { mutableStateOf(false) }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Vault opens (deterministic, eased) — feels like a real safe.
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        delay(650)
        encryptionVisible = true
        delay(900)
        auditVisible = true
        delay(950)
        onLaunchComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .testTag("hk_launch_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            BrandHeader()

            VaultProgress(progress = progress.value)

            TrustBadgeStack(
                encryptionVisible = encryptionVisible,
                auditVisible = auditVisible
            )

            BrandFooter()
        }
    }
}

/* ----------------------------- Brand header ----------------------------- */

@Composable
private fun BrandHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        ShimmerWordmark(
            text = "HisabKitab Pro",
            modifier = Modifier.testTag("hk_launch_wordmark")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .height(1.dp)
                .width(72.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, RoyalGold, Color.Transparent)
                    )
                )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "FINANCIAL · PRECISION · TRUST",
            color = RoyalGold.copy(alpha = 0.78f),
            fontSize = 11.sp,
            letterSpacing = 4.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ShimmerWordmark(
    text: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "wordmark_shimmer")
    val offsetX by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            RoyalGoldDeep,
            RoyalGold,
            RoyalGoldLight,
            RoyalGold,
            RoyalGoldDeep
        ),
        start = Offset(offsetX, 0f),
        end = Offset(offsetX + 600f, 0f)
    )

    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = shimmerBrush,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = 38.sp,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center
        )
    )
}

/* ----------------------------- Vault progress ----------------------------- */

/**
 * Concentric gold rings that evoke a safe vault opening.
 * - Outer ring: slow clockwise rotation, faint gold dust.
 * - Middle ring: deterministic progress arc (the "tumbler").
 * - Inner ring: fast counter-clockwise, suggests precision movement.
 */
@Composable
private fun VaultProgress(progress: Float) {
    val transition = rememberInfiniteTransition(label = "vault_rings")
    val outerRot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )
    val innerRot by transition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    Box(
        modifier = Modifier
            .size(196.dp)
            .testTag("hk_launch_vault"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.5f.dp.toPx()

            // Outer ring (subtle, atmospheric)
            rotate(outerRot, center) {
                drawCircle(
                    color = RoyalGold.copy(alpha = 0.18f),
                    radius = size.minDimension / 2f - stroke,
                    style = Stroke(width = stroke)
                )
                // Twin "vault marker" ticks on outer ring
                val r = size.minDimension / 2f - stroke
                drawCircle(
                    color = RoyalGold,
                    radius = stroke * 0.9f,
                    center = Offset(center.x, center.y - r)
                )
                drawCircle(
                    color = RoyalGold,
                    radius = stroke * 0.6f,
                    center = Offset(center.x, center.y + r)
                )
            }

            // Middle ring (progress — the safe opening)
            val midPad = size.minDimension * 0.12f
            val midSize = Size(size.width - midPad * 2, size.height - midPad * 2)
            val midTopLeft = Offset(midPad, midPad)
            drawArc(
                color = RoyalGoldDeep.copy(alpha = 0.35f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = midTopLeft,
                size = midSize,
                style = Stroke(width = stroke * 1.6f)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(RoyalGoldDeep, RoyalGold, RoyalGoldLight, RoyalGold, RoyalGoldDeep),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = midTopLeft,
                size = midSize,
                style = Stroke(width = stroke * 1.8f)
            )

            // Inner ring (precision)
            rotate(innerRot, center) {
                val innerPad = size.minDimension * 0.28f
                val innerSize = Size(size.width - innerPad * 2, size.height - innerPad * 2)
                val innerTopLeft = Offset(innerPad, innerPad)
                drawArc(
                    color = RoyalGold.copy(alpha = 0.55f),
                    startAngle = 30f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = stroke * 0.9f)
                )
                drawArc(
                    color = RoyalGoldLight.copy(alpha = 0.7f),
                    startAngle = 210f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = stroke * 0.9f)
                )
            }
        }

        // Center lock medallion
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RoyalGold.copy(alpha = 0.18f),
                            AmoledBlack
                        )
                    )
                )
                .border(width = 1.dp, color = RoyalGold.copy(alpha = 0.7f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = RoyalGold,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

/* ----------------------------- Trust badges ----------------------------- */

@Composable
private fun TrustBadgeStack(
    encryptionVisible: Boolean,
    auditVisible: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = encryptionVisible,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 4 } + expandVertically(tween(450))
        ) {
            TrustBadge(
                icon = Icons.Default.Lock,
                label = "ENCRYPTION ACTIVE",
                tone = BadgeTone.Soft
            )
        }

        AnimatedVisibility(
            visible = auditVisible,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 4 } + expandVertically(tween(450))
        ) {
            TrustBadge(
                icon = Icons.Default.Verified,
                label = "SECURITY AUDIT PASSED",
                tone = BadgeTone.Strong
            )
        }
    }
}

private enum class BadgeTone { Soft, Strong }

@Composable
private fun TrustBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tone: BadgeTone
) {
    val containerAlpha = if (tone == BadgeTone.Strong) 0.10f else 0.06f
    val borderAlpha = if (tone == BadgeTone.Strong) 0.65f else 0.40f

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(RoyalGold.copy(alpha = containerAlpha))
            .border(
                width = 1.dp,
                brush = SolidColor(RoyalGold.copy(alpha = borderAlpha)),
                shape = RoundedCornerShape(50)
            )
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RoyalGold,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = GoldOnDark,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ----------------------------- Footer ----------------------------- */

@Composable
private fun BrandFooter() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        MakeInIndiaEmblem()

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Secured by OMNI-v5 System Architecture",
            color = RoyalGold.copy(alpha = 0.55f),
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("hk_launch_omni_footer")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "v5 · End-to-End Encrypted Ledger",
            color = Color.White.copy(alpha = 0.32f),
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun MakeInIndiaEmblem() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.04f))
            .border(
                width = 0.6.dp,
                color = RoyalGold.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        TricolorDot()
        Text(
            text = "MAKE IN INDIA",
            color = RoyalGoldLight,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TricolorDot() {
    Canvas(modifier = Modifier.size(width = 22.dp, height = 14.dp)) {
        val stripeHeight = size.height / 3f
        // Saffron
        drawRect(
            color = FlagSaffron,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, stripeHeight)
        )
        // White (with small navy chakra dot)
        drawRect(
            color = FlagWhite,
            topLeft = Offset(0f, stripeHeight),
            size = Size(size.width, stripeHeight)
        )
        drawCircle(
            color = FlagNavy,
            radius = stripeHeight * 0.32f,
            center = Offset(size.width / 2f, stripeHeight + stripeHeight / 2f)
        )
        // Green
        drawRect(
            color = FlagGreen,
            topLeft = Offset(0f, stripeHeight * 2f),
            size = Size(size.width, stripeHeight)
        )
    }
}
