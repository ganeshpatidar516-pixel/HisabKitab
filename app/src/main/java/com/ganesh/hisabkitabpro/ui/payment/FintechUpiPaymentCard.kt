package com.ganesh.hisabkitabpro.ui.payment

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.ganesh.hisabkitabpro.domain.qr.QrPaymentManager
import com.ganesh.hisabkitabpro.domain.qr.UpiPaymentDetails
import java.io.File
import kotlinx.coroutines.yield
import java.text.NumberFormat
import java.util.Locale

/**
 * Premium fintech-style UPI payment block: identity header, amount hero (fixed or flexible),
 * sharp QR (dynamic amount QR when [amountPaise] > 0, else static image file), pay-to footer,
 * optional toolbar and UPI editor (business profile).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FintechUpiPaymentCard(
    businessName: String,
    ownerName: String,
    logoPath: String,
    qrImagePath: String,
    upiId: String,
    amountPaise: Long? = null,
    balanceContextLabel: String? = null,
    paymentNoteForQr: String? = null,
    mediaStatus: String? = null,
    onDismissMediaStatus: (() -> Unit)? = null,
    showPaymentToolbar: Boolean = true,
    showUpiEditor: Boolean = true,
    onUpiChange: (String) -> Unit = {},
    onCaptureQr: () -> Unit = {},
    onUploadQr: () -> Unit = {},
    onUploadLogo: () -> Unit = {},
    onShareQr: () -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val upiOk = upiId.contains('@') && upiId.trim().length > 4
    val displayName = businessName.trim().ifBlank { "Your business" }.take(42)
    val displayOwner = ownerName.trim().ifBlank { "Merchant" }.take(36)
    val fileOk = qrImagePath.isNotEmpty() && File(qrImagePath).exists()
    val logoOk = logoPath.isNotBlank() && File(logoPath).exists()
    val qrInnerDp = 232.dp
    val qrDecodePx = remember(qrImagePath, qrInnerDp) {
        with(density) { qrInnerDp.roundToPx().coerceIn(256, 720) }
    }
    val qrPainter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(if (fileOk) File(qrImagePath) else null)
            .size(qrDecodePx, qrDecodePx)
            .scale(Scale.FIT)
            .crossfade(false)
            .allowRgb565(false)
            .build(),
    )
    val logoPainter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(if (logoOk) File(logoPath) else null)
            .size(with(density) { 88.dp.roundToPx() })
            .scale(Scale.FIT)
            .crossfade(false)
            .build(),
    )
    val fixedAmount = amountPaise != null && amountPaise > 0L
    val currencyFmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val amountDisplay = remember(amountPaise) {
        if (fixedAmount) currencyFmt.format((amountPaise ?: 0L) / 100.0) else null
    }
    var generatedQr by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(fixedAmount, upiOk, upiId, amountPaise, displayName, paymentNoteForQr, balanceContextLabel) {
        if (!fixedAmount || !upiOk) {
            val cleared = generatedQr
            generatedQr = null
            yield()
            if (cleared != null && !cleared.isRecycled) cleared.recycle()
            return@LaunchedEffect
        }
        val amtStr = String.format(Locale.US, "%.2f", (amountPaise ?: 0L) / 100.0)
        val note = (paymentNoteForQr ?: balanceContextLabel?.let { "Payment $it" } ?: "Payment").take(80)
        val bmp = QrPaymentManager.generateDynamicUpiQr(
            UpiPaymentDetails(
                upiId = upiId.trim(),
                name = displayName,
                amount = amtStr,
                note = note,
            ),
            size = 480,
        )
        if (bmp == null) {
            Log.w("FintechUpiPaymentCard", "Dynamic UPI QR generation failed")
        }
        val previous = generatedQr
        generatedQr = bmp
        yield()
        if (previous != null && previous !== bmp && !previous.isRecycled) {
            previous.recycle()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            generatedQr?.let { b ->
                if (!b.isRecycled) b.recycle()
            }
        }
    }
    val qrWellBorder = Color(0xFFE2E8F0)
    val amountBandColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = Color(0x1A000000), spotColor = Color(0x33000000)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                text = "UPI payment",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    if (logoOk) {
                        Image(
                            painter = logoPainter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = displayOwner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = amountBandColor,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "PAY WITH UPI",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (fixedAmount && amountDisplay != null) {
                        balanceContextLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { hint ->
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp,
                                    lineHeight = 42.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = amountDisplay,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Amount is encoded in the QR below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp,
                                    lineHeight = 42.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Any amount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Customer enters amount in their UPI app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    tonalElevation = 0.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, qrWellBorder, RoundedCornerShape(20.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(qrInnerDp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8FAFC)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val gen = generatedQr
                            when {
                                gen != null && !gen.isRecycled -> {
                                    Image(
                                        bitmap = gen.asImageBitmap(),
                                        contentDescription = "Payment QR",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                                fileOk -> {
                                    Image(
                                        painter = qrPainter,
                                        contentDescription = "Payment QR",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                                else -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(52.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Add your static QR",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            "Camera or gallery · auto-framed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (mediaStatus != null && onDismissMediaStatus != null) {
                    AssistChip(
                        onClick = onDismissMediaStatus,
                        label = {
                            Text(
                                mediaStatus,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Pay to",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = upiId.trim().ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (fixedAmount && generatedQr != null) {
                    "Scan with any UPI app — amount is pre-filled"
                } else {
                    "Scan the code above with any UPI app"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showPaymentToolbar) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UpiCardActionSlot(Icons.Default.CameraAlt, "Capture", onCaptureQr)
                    UpiCardActionSlot(Icons.Default.PhotoLibrary, "Upload", onUploadQr)
                    UpiCardActionSlot(Icons.Outlined.Store, "Logo", onUploadLogo)
                    UpiCardActionSlot(Icons.Outlined.Share, "Share", onShareQr)
                }
            }
            if (showUpiEditor) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = upiId,
                    onValueChange = onUpiChange,
                    label = { Text("UPI ID") },
                    placeholder = { Text("name@bank") },
                    supportingText = if (upiId.isBlank()) {
                        null
                    } else {
                        {
                            Text(
                                if (upiOk) "Ready for UPI payments" else "Use yourname@bank",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (upiOk) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun UpiCardActionSlot(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
