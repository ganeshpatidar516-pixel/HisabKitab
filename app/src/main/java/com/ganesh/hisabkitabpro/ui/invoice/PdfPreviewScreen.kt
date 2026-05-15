package com.ganesh.hisabkitabpro.ui.invoice

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "PdfPreviewScreen"
private const val MAX_PREVIEW_PAGES = 32
private const val MAX_PREVIEW_EDGE_PX = 1600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(pdfFile: File, onBack: () -> Unit, onShare: () -> Unit) {
    var pageCount by remember(pdfFile) { mutableIntStateOf(0) }
    val pageBitmaps = remember(pdfFile) { mutableStateMapOf<Int, Bitmap>() }

    LaunchedEffect(pdfFile) {
        pageBitmaps.clear()
        pageCount = withContext(Dispatchers.IO) {
            runCatching {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        renderer.pageCount.coerceAtMost(MAX_PREVIEW_PAGES)
                    }
                }
            }.getOrElse {
                Log.e(TAG, "Failed to open PDF for preview", it)
                0
            }
        }
    }

    DisposableEffect(pdfFile) {
        onDispose {
            pageBitmaps.values.forEach { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
            pageBitmaps.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = onShare) { Icon(Icons.Default.Share, null) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Gray.copy(alpha = 0.1f)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                count = pageCount,
                key = { index -> "${pdfFile.absolutePath}-$index" },
            ) { index ->
                PdfPreviewPage(
                    pdfFile = pdfFile,
                    pageIndex = index,
                    cached = pageBitmaps,
                )
            }
        }
    }
}

@Composable
private fun PdfPreviewPage(
    pdfFile: File,
    pageIndex: Int,
    cached: MutableMap<Int, Bitmap>,
) {
    var bitmap by remember(pdfFile, pageIndex) { mutableStateOf<Bitmap?>(cached[pageIndex]) }

    LaunchedEffect(pdfFile, pageIndex) {
        if (bitmap != null) return@LaunchedEffect
        val rendered = withContext(Dispatchers.IO) {
            renderPdfPage(pdfFile, pageIndex)
        }
        if (rendered != null) {
            cached[pageIndex] = rendered
            bitmap = rendered
        }
    }

    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        when (val bmp = bitmap) {
            null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }
            }
            else -> {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun renderPdfPage(file: File, pageIndex: Int): Bitmap? {
    return runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                renderer.openPage(pageIndex).use { page ->
                    val scale = minOf(
                        2f,
                        MAX_PREVIEW_EDGE_PX.toFloat() / page.width.coerceAtLeast(1),
                        MAX_PREVIEW_EDGE_PX.toFloat() / page.height.coerceAtLeast(1),
                    )
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }.getOrElse {
        Log.e(TAG, "renderPdfPage failed index=$pageIndex", it)
        null
    }
}
