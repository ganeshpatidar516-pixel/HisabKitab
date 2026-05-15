package com.ganesh.hisabkitabpro.ui.settings.businessidentity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths
import com.ganesh.hisabkitabpro.domain.businessidentity.BusinessIdentityInputNormalizer
import com.ganesh.hisabkitabpro.domain.businessidentity.BusinessIdentityInputNormalizer.WebsiteInputKind
import java.io.File
import java.io.FileOutputStream

/**
 * Isolated Professional Info dialog (Phase 1). Save path remains in [com.ganesh.hisabkitabpro.ui.settings.BusinessProfileScreen].
 */
@Composable
fun ProfessionalInfoAlertDialog(
    onDismiss: () -> Unit,
    businessNameHint: String,
    businessCategory: String,
    onBusinessCategoryChange: (String) -> Unit,
    operatingHours: String,
    onOperatingHoursChange: (String) -> Unit,
    websiteUrl: String,
    onWebsiteUrlChange: (String) -> Unit,
    instagramUrl: String,
    onInstagramUrlChange: (String) -> Unit,
    facebookUrl: String,
    onFacebookUrlChange: (String) -> Unit,
    linkedInUrl: String,
    onLinkedInUrlChange: (String) -> Unit,
    youtubeUrl: String,
    onYoutubeUrlChange: (String) -> Unit,
    twitterUrl: String,
    onTwitterUrlChange: (String) -> Unit,
    whatsAppBusinessUrl: String,
    onWhatsAppBusinessUrlChange: (String) -> Unit,
    googleBusinessProfileUrl: String,
    onGoogleBusinessProfileUrlChange: (String) -> Unit,
    latitudeText: String,
    onLatitudeTextChange: (String) -> Unit,
    longitudeText: String,
    onLongitudeTextChange: (String) -> Unit,
    mapLink: String,
    locationLockedAt: Long,
    onApplyCoordinates: (Double, Double) -> Unit,
    onLockLiveLocation: () -> Unit,
    signatureLines: List<List<Offset>>,
    onSignatureLinesChange: (List<List<Offset>>) -> Unit,
    onSignatureImagePathChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val websiteKind = remember(websiteUrl) { BusinessIdentityInputNormalizer.classifyWebsite(websiteUrl) }
    var showExtraSocial by remember { mutableStateOf(false) }

    LaunchedEffect(linkedInUrl, youtubeUrl, twitterUrl, whatsAppBusinessUrl, googleBusinessProfileUrl) {
        val anyExtra = listOf(linkedInUrl, youtubeUrl, twitterUrl, whatsAppBusinessUrl, googleBusinessProfileUrl)
            .any { it.isNotBlank() }
        if (anyExtra) showExtraSocial = true
    }

    val softIssueColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Professional Info") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    "Updates documents after you save Business Profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ProfessionalSectionGroup("Business") {
                    BusinessCategoryTaxonomyField(
                        businessCategory = businessCategory,
                        onBusinessCategoryChange = onBusinessCategoryChange,
                        businessNameHint = businessNameHint,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BusinessHoursSection(
                        operatingHours = operatingHours,
                        onOperatingHoursChange = onOperatingHoursChange,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ProfessionalSectionGroup("Website") {
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = onWebsiteUrlChange,
                        label = { Text("Website") },
                        isError = false,
                        supportingText = when {
                            websiteKind == WebsiteInputKind.LikelyEmail -> {
                                {
                                    Text(
                                        "Use the Email field on Business Profile for email.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = softIssueColor,
                                    )
                                }
                            }
                            websiteKind == WebsiteInputKind.Other && websiteUrl.isNotBlank() -> {
                                {
                                    Text(
                                        "Web address (https://… or yourshop.com).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            else -> null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ProfessionalSectionGroup("Social") {
                    OutlinedTextField(
                        value = instagramUrl,
                        onValueChange = onInstagramUrlChange,
                        label = { Text("Instagram") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = iconTint,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = facebookUrl,
                        onValueChange = onFacebookUrlChange,
                        label = { Text("Facebook") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = iconTint,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { showExtraSocial = !showExtraSocial }) {
                        Text(if (showExtraSocial) "Hide more profiles" else "More profiles")
                    }
                    AnimatedVisibility(
                        visible = showExtraSocial,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = linkedInUrl,
                                onValueChange = onLinkedInUrlChange,
                                label = { Text("LinkedIn") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, null, Modifier.size(20.dp), tint = iconTint)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = youtubeUrl,
                                onValueChange = onYoutubeUrlChange,
                                label = { Text("YouTube") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, null, Modifier.size(20.dp), tint = iconTint)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = twitterUrl,
                                onValueChange = onTwitterUrlChange,
                                label = { Text("X") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, null, Modifier.size(20.dp), tint = iconTint)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = whatsAppBusinessUrl,
                                onValueChange = onWhatsAppBusinessUrlChange,
                                label = { Text("WhatsApp Business") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, null, Modifier.size(20.dp), tint = iconTint)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = googleBusinessProfileUrl,
                                onValueChange = onGoogleBusinessProfileUrlChange,
                                label = { Text("Google Business") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, null, Modifier.size(20.dp), tint = iconTint)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                ProfessionalSectionGroup("Location") {
                    SmartLocationSection(
                        latitudeText = latitudeText,
                        longitudeText = longitudeText,
                        mapLink = mapLink,
                        locationLockedAt = locationLockedAt,
                        onLatitudeTextChange = onLatitudeTextChange,
                        onLongitudeTextChange = onLongitudeTextChange,
                        onApplyCoordinates = onApplyCoordinates,
                        onLockFromFields = onLockLiveLocation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ProfessionalSectionGroup("Signature") {
                    SignaturePad(
                        lines = signatureLines,
                        onLinesChanged = onSignatureLinesChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = {
                                onSignatureLinesChange(emptyList())
                                onSignatureImagePathChange("")
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Clear") }
                        FilledTonalButton(
                            onClick = {
                                if (signatureLines.isNotEmpty()) {
                                    val file = saveSignatureBitmap(
                                        AppStoragePaths.mediaSignaturesDir(context),
                                        signatureLines,
                                    )
                                    onSignatureImagePathChange(file.absolutePath)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Save") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun ProfessionalSectionGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SignaturePad(
    lines: List<List<Offset>>,
    onLinesChanged: (List<List<Offset>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentLine by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val shape = RoundedCornerShape(12.dp)
    val edge = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    Surface(
        modifier = modifier.border(0.5.dp, edge, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start -> currentLine = listOf(start) },
                        onDrag = { change, _ -> currentLine = currentLine + change.position },
                        onDragEnd = {
                            if (currentLine.size > 1) onLinesChanged(lines + listOf(currentLine))
                            currentLine = emptyList()
                        },
                        onDragCancel = { currentLine = emptyList() },
                    )
                }
                .padding(10.dp),
        ) {
            fun drawLineSet(points: List<Offset>) {
                for (i in 1 until points.size) {
                    drawLine(
                        color = strokeColor,
                        start = points[i - 1],
                        end = points[i],
                        strokeWidth = 2.8f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            lines.forEach(::drawLineSet)
            drawLineSet(currentLine)
        }
    }
}

private fun saveSignatureBitmap(outDir: File, lines: List<List<Offset>>): File {
    val width = 1200
    val height = 420
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(20, 24, 32)
        strokeWidth = 8f
        strokeCap = android.graphics.Paint.Cap.ROUND
        style = android.graphics.Paint.Style.STROKE
    }
    lines.forEach { line ->
        for (i in 1 until line.size) {
            canvas.drawLine(line[i - 1].x, line[i - 1].y, line[i].x, line[i].y, paint)
        }
    }
    val outFile = File(outDir, "business_signature.png")
    FileOutputStream(outFile).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return outFile
}
