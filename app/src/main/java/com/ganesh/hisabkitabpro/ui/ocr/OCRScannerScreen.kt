package com.ganesh.hisabkitabpro.ui.ocr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths
import com.ganesh.hisabkitabpro.domain.ocr.AutoBillOcrResult
import com.ganesh.hisabkitabpro.domain.ocr.BillAmountConfidence
import com.ganesh.hisabkitabpro.core.security.PrivacySecureEffect
import com.ganesh.hisabkitabpro.core.storage.OcrBillAttachmentUri
import com.ganesh.hisabkitabpro.domain.ocr.OCRBillProcessor
import com.ganesh.hisabkitabpro.domain.ocr.OcrDecodeForOcrResult
import com.ganesh.hisabkitabpro.domain.ocr.OcrGalleryImportCopy
import com.ganesh.hisabkitabpro.domain.ocr.OcrImageLoader
import com.ganesh.hisabkitabpro.domain.ocr.OcrTelemetry
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private enum class BillAcquisitionSource {
    CAMERA,
    GALLERY,
}

private data class PendingOcrPrefill(
    val amountKeypadText: String,
    val note: String,
    val billImageUri: String?,
)

private sealed class BillOcrUiOutcome {
    data object NoAmount : BillOcrUiOutcome()
    data class PrefillReady(
        val amountKeypadText: String,
        val note: String,
        val billImageUri: String?,
        val lowConfidence: Boolean,
    ) : BillOcrUiOutcome()
    data class ResultMessage(val message: String) : BillOcrUiOutcome()
}

/**
 * P1 — shared OCR path after a bitmap is decoded (camera file or gallery import).
 * Always recycles [bitmap] in [finally].
 */
private suspend fun runBillOcrFromDecodedBitmap(
    context: Context,
    lifecycle: Lifecycle,
    processor: OCRBillProcessor,
    bitmap: android.graphics.Bitmap,
    persistedJpegFile: File?,
    scanForPrefill: Boolean,
    customerId: Long,
): BillOcrUiOutcome {
    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        bitmap.recycle()
        return BillOcrUiOutcome.NoAmount
    }
    return try {
        if (scanForPrefill) {
            val snap = processor.extractForManualEntry(bitmap)
            if (snap == null) {
                return BillOcrUiOutcome.NoAmount
            }
            val amt = OCRBillProcessor.formatAmountForKeypad(snap.amountRupees)
            val noteLine = buildString {
                append("Bill scan · ").append(snap.vendorHint)
                if (snap.lineItemsSummary.isNotBlank()) {
                    append("\n").append(snap.lineItemsSummary)
                }
            }.trim()
            BillOcrUiOutcome.PrefillReady(
                amountKeypadText = amt,
                note = noteLine,
                billImageUri = persistedJpegFile?.let { OcrBillAttachmentUri.fromCacheFile(context, it) },
                lowConfidence = snap.amountConfidence == BillAmountConfidence.LOW,
            )
        } else {
            persistedJpegFile?.delete()
            BillOcrUiOutcome.ResultMessage(processor.processAndSaveBill(bitmap, customerId))
        }
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

private suspend fun handleBillOcrOutcome(
    context: Context,
    outcome: BillOcrUiOutcome,
    onPrefill: (amountKeypadText: String, note: String, billImageUri: String?, ocrLowConfidenceAmount: Boolean) -> Unit,
    onResult: (String) -> Unit,
    onLowConfidencePrefill: (PendingOcrPrefill) -> Unit,
) {
    withContext(Dispatchers.Main) {
        when (outcome) {
            BillOcrUiOutcome.NoAmount -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.ocr_no_amount_on_bill),
                    Toast.LENGTH_LONG,
                ).show()
            }
            is BillOcrUiOutcome.PrefillReady -> {
                if (outcome.lowConfidence) {
                    onLowConfidencePrefill(
                        PendingOcrPrefill(
                            outcome.amountKeypadText,
                            outcome.note,
                            outcome.billImageUri,
                        ),
                    )
                } else {
                    onPrefill(outcome.amountKeypadText, outcome.note, outcome.billImageUri, false)
                    Toast.makeText(
                        context,
                        context.getString(R.string.ocr_prefill_toast_ok),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            is BillOcrUiOutcome.ResultMessage -> onResult(outcome.message)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRScannerScreen(
    processor: OCRBillProcessor,
    customerId: Long = 0L,
    /** When true (e.g. opened from add entry with a known customer), only OCR-fill amount/note — no auto-save. */
    scanForPrefill: Boolean = false,
    /**
     * When false, never binds live-frame [ImageAnalysis] auto-save (Wave 0 kill-switch).
     * Null/absent in settings means enabled (legacy behavior).
     */
    liveLedgerAutoSaveEnabled: Boolean = true,
    onPrefill: (amountKeypadText: String, note: String, billImageUri: String?, ocrLowConfidenceAmount: Boolean) -> Unit = { _, _, _, _ -> },
    onResult: (String) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    PrivacySecureEffect()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ocrJob = remember { SupervisorJob() }
    val coroutineScope = rememberCoroutineScope { ocrJob }
    /** Wave 2 — serializes live-frame auto-save so queued jobs cannot each insert before [autoSaved] flips. */
    val liveAutoSaveMutex = remember { Mutex() }
    val autoOcrFromLedger = customerId > 0L && !scanForPrefill && liveLedgerAutoSaveEnabled
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            ocrJob.cancel()
            analysisExecutor.shutdown()
            CoroutineScope(Dispatchers.IO).launch {
                AppStoragePaths.ocrCacheDir(context).listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("ocr_import_")) {
                        file.delete()
                    }
                }
            }
        }
    }

    var showAcquisitionSheet by remember { mutableStateOf(true) }
    var chosenSource by remember { mutableStateOf<BillAcquisitionSource?>(null) }
    var galleryProcessing by remember { mutableStateOf(false) }
    var pendingLowConfPrefill by remember { mutableStateOf<PendingOcrPrefill?>(null) }

    LaunchedEffect(customerId, scanForPrefill, liveLedgerAutoSaveEnabled) {
        OcrTelemetry.event(
            "scanner_mount",
            mapOf(
                "prefill" to scanForPrefill.toString(),
                "liveLedgerAuto" to autoOcrFromLedger.toString(),
                "customerScoped" to (customerId > 0L).toString(),
            ),
        )
    }

    var hasCameraPermission by remember { mutableStateOf(false) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        hasCameraPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    val pickVisualMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(context, context.getString(R.string.ocr_gallery_cancelled), Toast.LENGTH_SHORT).show()
            chosenSource = null
            showAcquisitionSheet = true
            return@rememberLauncherForActivityResult
        }
        galleryProcessing = true
        coroutineScope.launch {
            try {
                val copyResult = withContext(Dispatchers.IO) {
                    OcrImageLoader.copyContentUriToOcrCacheFile(context, uri)
                }
                val cacheFile = when (copyResult) {
                    OcrGalleryImportCopy.TooLarge,
                    OcrGalleryImportCopy.Failed,
                    -> {
                        val msgRes = when (copyResult) {
                            OcrGalleryImportCopy.TooLarge -> R.string.ocr_gallery_file_too_large
                            else -> R.string.ocr_gallery_import_failed
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_LONG).show()
                        }
                        chosenSource = null
                        showAcquisitionSheet = true
                        return@launch
                    }
                    is OcrGalleryImportCopy.Ok -> copyResult.file
                }
                val decodeResult = withContext(Dispatchers.IO) {
                    OcrImageLoader.decodeJpegFileForOcr(cacheFile.absolutePath)
                }
                when (decodeResult) {
                    OcrDecodeForOcrResult.ExceedsDecodeLimits,
                    OcrDecodeForOcrResult.Unreadable,
                    -> {
                        cacheFile.delete()
                        val msgRes = when (decodeResult) {
                            OcrDecodeForOcrResult.ExceedsDecodeLimits -> R.string.ocr_image_exceeds_decode_limits
                            else -> R.string.ocr_gallery_import_failed
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_LONG).show()
                        }
                        chosenSource = null
                        showAcquisitionSheet = true
                        return@launch
                    }
                    is OcrDecodeForOcrResult.Decoded -> {
                        val bitmap = decodeResult.bitmap
                        val outcome = runBillOcrFromDecodedBitmap(
                            context = context,
                            lifecycle = lifecycleOwner.lifecycle,
                            processor = processor,
                            bitmap = bitmap,
                            persistedJpegFile = cacheFile,
                            scanForPrefill = scanForPrefill,
                            customerId = customerId,
                        )
                        handleBillOcrOutcome(
                            context = context,
                            outcome = outcome,
                            onPrefill = onPrefill,
                            onResult = onResult,
                            onLowConfidencePrefill = { pendingLowConfPrefill = it },
                        )
                    }
                }
            } finally {
                galleryProcessing = false
            }
        }
    }

    LaunchedEffect(chosenSource) {
        if (chosenSource == BillAcquisitionSource.GALLERY) {
            pickVisualMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }

    val acquisitionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showAcquisitionSheet) {
        ModalBottomSheet(
            onDismissRequest = onNavigateBack,
            sheetState = acquisitionSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.ocr_acquire_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.ocr_acquire_camera)) },
                    leadingContent = { Icon(Icons.Default.Camera, contentDescription = null) },
                    modifier = Modifier.clickable {
                        OcrTelemetry.event("scanner_acquire", mapOf("source" to "camera"))
                        showAcquisitionSheet = false
                        chosenSource = BillAcquisitionSource.CAMERA
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.ocr_acquire_gallery)) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable {
                        OcrTelemetry.event("scanner_acquire", mapOf("source" to "gallery"))
                        showAcquisitionSheet = false
                        chosenSource = BillAcquisitionSource.GALLERY
                    },
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (scanForPrefill) "Scan bill — fill amount" else "Scan Bill") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (chosenSource == BillAcquisitionSource.CAMERA && hasCameraPermission) {
                FloatingActionButton(
                    onClick = {
                        val capture = imageCapture ?: return@FloatingActionButton
                        val photoFile = File(
                            AppStoragePaths.ocrCacheDir(context),
                            "ocr_scan_${System.currentTimeMillis()}.jpg",
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    coroutineScope.launch {
                                        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                            if (photoFile.exists()) photoFile.delete()
                                            return@launch
                                        }
                                        val decodeResult = withContext(Dispatchers.IO) {
                                            OcrImageLoader.decodeJpegFileForOcr(photoFile.absolutePath)
                                        }
                                        when (decodeResult) {
                                            OcrDecodeForOcrResult.ExceedsDecodeLimits,
                                            OcrDecodeForOcrResult.Unreadable,
                                            -> {
                                                if (photoFile.exists()) photoFile.delete()
                                                val msgRes = when (decodeResult) {
                                                    OcrDecodeForOcrResult.ExceedsDecodeLimits ->
                                                        R.string.ocr_image_exceeds_decode_limits
                                                    else -> R.string.ocr_camera_photo_decode_failed
                                                }
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(msgRes),
                                                        Toast.LENGTH_LONG,
                                                    ).show()
                                                }
                                            }
                                            is OcrDecodeForOcrResult.Decoded -> {
                                                val outcome = runBillOcrFromDecodedBitmap(
                                                    context = context,
                                                    lifecycle = lifecycleOwner.lifecycle,
                                                    processor = processor,
                                                    bitmap = decodeResult.bitmap,
                                                    persistedJpegFile = photoFile,
                                                    scanForPrefill = scanForPrefill,
                                                    customerId = customerId,
                                                )
                                                handleBillOcrOutcome(
                                                    context = context,
                                                    outcome = outcome,
                                                    onPrefill = onPrefill,
                                                    onResult = onResult,
                                                    onLowConfidencePrefill = { pendingLowConfPrefill = it },
                                                )
                                            }
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    if (photoFile.exists()) photoFile.delete()
                                    Log.e("OCR", "Capture failed", exception)
                                    Toast.makeText(context, "Camera capture failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Capture")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        when (chosenSource) {
            null -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.ocr_acquire_sheet_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            BillAcquisitionSource.GALLERY -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (galleryProcessing) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = stringResource(R.string.ocr_gallery_opening),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            BillAcquisitionSource.CAMERA -> {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val executor = ContextCompat.getMainExecutor(ctx)
                            val autoSaved = AtomicBoolean(false)
                            val lastAnalysisMs = AtomicLong(0L)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val bindList = mutableListOf<UseCase>(preview, capture)
                                if (autoOcrFromLedger) {
                                    val analysisResolution = ResolutionSelector.Builder()
                                        .setResolutionStrategy(
                                            ResolutionStrategy(
                                                Size(1280, 720),
                                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                            ),
                                        )
                                        .build()
                                    val analysis = ImageAnalysis.Builder()
                                        .setResolutionSelector(analysisResolution)
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                        if (autoSaved.get()) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        val mediaImage = imageProxy.image
                                        if (mediaImage == null) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        val now = SystemClock.elapsedRealtime()
                                        val last = lastAnalysisMs.get()
                                        if (last != 0L && now - last < 2800L) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        lastAnalysisMs.set(now)
                                        val input = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        coroutineScope.launch {
                                            try {
                                                liveAutoSaveMutex.withLock {
                                                    if (autoSaved.get()) return@withLock
                                                    when (
                                                        val r = processor.processInputImageForKnownCustomer(
                                                            input,
                                                            customerId
                                                        )
                                                    ) {
                                                        is AutoBillOcrResult.Saved -> {
                                                            if (autoSaved.compareAndSet(false, true)) {
                                                                withContext(Dispatchers.Main) {
                                                                    Toast.makeText(
                                                                        ctx,
                                                                        r.userMessage,
                                                                        Toast.LENGTH_LONG
                                                                    ).show()
                                                                    onResult(r.userMessage)
                                                                }
                                                            }
                                                        }
                                                        is AutoBillOcrResult.LowConfidence -> {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    ctx,
                                                                    r.userMessage,
                                                                    Toast.LENGTH_LONG,
                                                                ).show()
                                                            }
                                                        }
                                                        else -> Unit
                                                    }
                                                }
                                            } finally {
                                                imageProxy.close()
                                            }
                                        }
                                    }
                                    bindList.add(analysis)
                                }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        *bindList.toTypedArray()
                                    )
                                } catch (e: Exception) {
                                    Log.e("OCR", "Binding failed", e)
                                }
                            }, executor)
                            previewView
                        },
                        modifier = Modifier.padding(padding).fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(onClick = { showCameraPermissionDialog = true }) {
                            Text(stringResource(R.string.permission_camera_title))
                        }
                    }
                }
            }
        }
    }

    pendingLowConfPrefill?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingLowConfPrefill = null },
            title = { Text(stringResource(R.string.ocr_confirm_amount_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.ocr_confirm_amount_body,
                        pending.amountKeypadText,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPrefill(pending.amountKeypadText, pending.note, pending.billImageUri, true)
                        pendingLowConfPrefill = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.ocr_prefill_toast_low_confidence),
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                ) {
                    Text(stringResource(R.string.ocr_confirm_amount_use))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLowConfPrefill = null }) {
                    Text(stringResource(R.string.ocr_confirm_amount_cancel))
                }
            },
        )
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_camera_title)) },
            text = { Text(stringResource(R.string.permission_camera_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCameraPermissionDialog = false
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
