package com.ganesh.hisabkitabpro.ui.businesscard

import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.util.Log
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardCategory
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import com.ganesh.hisabkitabpro.domain.businesscard.GoldenRatioGrid
import com.ganesh.hisabkitabpro.domain.businesscard.LogoAspectClassifier
import com.ganesh.hisabkitabpro.domain.businesscard.render.BusinessCardPainterRegistry
import com.ganesh.hisabkitabpro.domain.profile.ProfileBitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCardStudioScreen(
    onNavigateBack: () -> Unit,
    onEditBusinessProfile: () -> Unit,
    viewModel: BusinessCardStudioViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BusinessCardStudioEvent.SheetReady -> {
                    BusinessCardShare.sharePdf(context, event.pdfUri)
                    snackbarHost.showSnackbar("Master sheet ready")
                }
                is BusinessCardStudioEvent.MockupReady -> {
                    BusinessCardShare.shareImage(context, event.imageUri)
                }
                is BusinessCardStudioEvent.CardPngReady -> {
                    BusinessCardShare.shareImage(context, event.pngUri)
                }
                BusinessCardStudioEvent.MissingProfile -> {
                    Toast.makeText(context, "Add your business profile first", Toast.LENGTH_SHORT).show()
                }
                BusinessCardStudioEvent.ExportFailed -> {
                    snackbarHost.showSnackbar("Export failed. Please try again.")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Card Engine") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets(0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isProfileLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (!state.profile.hasIdentity) {
            EmptyProfileCta(
                modifier = Modifier.fillMaxSize().padding(padding),
                onEditBusinessProfile = onEditBusinessProfile,
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            HeroBanner(
                profile = state.profile,
                isExporting = state.isExporting,
                exportingMessage = state.exportingMessage,
                onExportSheet = viewModel::exportEntireSheet,
            )
            Spacer(modifier = Modifier.height(12.dp))
            CategoryFilterRow(
                selected = state.focusCategory,
                onSelect = viewModel::selectCategory,
            )
            Spacer(modifier = Modifier.height(12.dp))
            CardSheetGrid(
                variations = state.variations,
                focusCategory = state.focusCategory,
                profile = state.profile,
                logoPath = state.logoPath,
                qrBitmap = state.qrBitmap,
                onShareCard = viewModel::exportSingleCardPng,
                onShareMockup = viewModel::exportSingleMockup,
            )
        }
    }
}

@Composable
private fun HeroBanner(
    profile: BusinessCardProfile,
    isExporting: Boolean,
    exportingMessage: String?,
    onExportSheet: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "50 cards. Zero data entry.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Renders procedurally from ${profile.businessName.ifBlank { "your profile" }}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onExportSheet,
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(exportingMessage ?: "Exporting…")
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Export 50 print-ready cards (PDF)")
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selected: BusinessCardCategory?,
    onSelect: (BusinessCardCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All 50") },
            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        )
        for (cat in BusinessCardCategory.values()) {
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat.displayName) },
            )
        }
    }
}

@Composable
private fun CardSheetGrid(
    variations: List<BusinessCardVariation>,
    focusCategory: BusinessCardCategory?,
    profile: BusinessCardProfile,
    logoPath: String?,
    qrBitmap: android.graphics.Bitmap?,
    onShareCard: (BusinessCardVariation) -> Unit,
    onShareMockup: (BusinessCardVariation) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val visible = remember(variations, focusCategory) {
        if (focusCategory == null) variations else variations.filter { it.category == focusCategory }
    }
    // Lifecycle-safe logo loader.
    //
    // We deliberately avoid `produceState` here — when its key changes (or the screen
    // leaves composition) it simply drops the bitmap reference without recycling, which
    // historically leaked a ~9 MB ARGB_8888 bitmap on every profile edit / screen exit.
    // `LaunchedEffect(logoPath)` handles the swap-and-recycle on path change;
    // `DisposableEffect(Unit)` guarantees final recycle when the composable leaves.
    //
    // The preview tiles are thumbnails so [PREVIEW_LOGO_MAX_SIDE_PX] is more than
    // enough visual fidelity — quality-critical paths (PDF + PNG + mockup) decode at
    // print resolution through `BusinessCardBitmapRenderer`, completely independent
    // of this preview cache.
    var logoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(logoPath) {
        val decoded = if (logoPath.isNullOrBlank()) null
        else withContext(Dispatchers.IO) {
            ProfileBitmapLoader.loadBitmapMaxSide(appContext, logoPath, PREVIEW_LOGO_MAX_SIDE_PX)
        }
        val previous = logoBitmap
        logoBitmap = decoded
        if (previous != null && previous !== decoded && !previous.isRecycled) {
            previous.recycle()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val current = logoBitmap
            if (current != null && !current.isRecycled) {
                current.recycle()
            }
            logoBitmap = null
        }
    }
    val logoShape = remember(logoBitmap) {
        val bmp = logoBitmap
        if (bmp != null && !bmp.isRecycled) LogoAspectClassifier.classify(bmp.width, bmp.height)
        else LogoAspectClassifier.LogoShape.SQUARE
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(visible, key = { it.id }) { variation ->
            BusinessCardPreviewTile(
                variation = variation,
                profile = profile,
                logo = logoBitmap,
                qr = qrBitmap,
                logoShape = logoShape,
                onShareCard = { onShareCard(variation) },
                onShareMockup = { onShareMockup(variation) },
            )
        }
    }
}

@Composable
private fun BusinessCardPreviewTile(
    variation: BusinessCardVariation,
    profile: BusinessCardProfile,
    logo: android.graphics.Bitmap?,
    qr: android.graphics.Bitmap?,
    logoShape: LogoAspectClassifier.LogoShape,
    onShareCard: () -> Unit,
    onShareMockup: () -> Unit,
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatioPhi()
                .graphicsLayer {
                    rotationX = 4f
                    rotationY = -3f
                    cameraDistance = 14f * density
                    shadowElevation = 18f
                }
                .clickable { onShareMockup() },
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 4.dp,
            shadowElevation = 10.dp,
        ) {
            BusinessCardCanvas(
                variation = variation,
                profile = profile,
                logo = logo,
                qr = qr,
                logoShape = logoShape,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = variation.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = variation.category.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = onShareCard,
                label = { Text("PNG") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(),
            )
            Spacer(modifier = Modifier.size(6.dp))
            AssistChip(
                onClick = onShareMockup,
                label = { Text("Mockup") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(),
            )
        }
    }
}

@Composable
private fun BusinessCardCanvas(
    variation: BusinessCardVariation,
    profile: BusinessCardProfile,
    logo: android.graphics.Bitmap?,
    qr: android.graphics.Bitmap?,
    logoShape: LogoAspectClassifier.LogoShape,
) {
    val painter = remember(variation.id) { BusinessCardPainterRegistry.painterFor(variation.category) }
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val rect = RectF(0f, 0f, w, h)
        // Defensive: a recycled bitmap reaching the native canvas crashes with
        // `java.lang.RuntimeException: Canvas: trying to use a recycled bitmap`.
        // Compose can momentarily hold a snapshot of a freshly-recycled bitmap when
        // `logoPath` changes while a tile is being scrolled into view. We hand the
        // painter `null` in that race so the tile renders without the logo rather
        // than crashing the entire grid.
        val safeLogo = logo?.takeIf { !it.isRecycled }
        val safeQr = qr?.takeIf { !it.isRecycled }
        drawIntoCanvas { compCanvas ->
            try {
                painter.paint(
                    canvas = compCanvas.nativeCanvas,
                    bounds = rect,
                    variation = variation,
                    profile = profile,
                    logo = safeLogo,
                    qr = safeQr,
                    logoShape = logoShape,
                )
            } catch (t: Throwable) {
                Log.e(
                    "BusinessCardEngine",
                    "Template render failed: variation=${variation.id}, category=${variation.category}",
                    t
                )
                // Fault-tolerant fallback so one broken template never collapses the entire grid.
                val c = compCanvas.nativeCanvas
                c.drawColor(0xFFECEFF3.toInt())
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFB00020.toInt()
                    textSize = (rect.width() * 0.05f).coerceIn(14f, 28f)
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    isSubpixelText = true
                }
                c.drawText("Render error", rect.centerX(), rect.centerY(), paint)
            }
        }
    }
}

@Composable
private fun EmptyProfileCta(modifier: Modifier, onEditBusinessProfile: () -> Unit) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Add your business identity first", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The engine renders 50 unique cards directly from your saved profile. Open Business Profile to add your name and contact details.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onEditBusinessProfile) {
                    Text("Open Business Profile")
                }
            }
        }
    }
}

private fun Modifier.aspectRatioPhi(): Modifier =
    this.fillMaxWidth().aspectRatio(GoldenRatioGrid.PHI.toFloat())

/**
 * Maximum side length used when decoding the business logo for the on-screen preview
 * grid. The historical value of 1536 px allocated ~9 MB per decode and was retained
 * across orientation / nav cycles, contributing to the delayed-OOM crash pattern.
 * 768 px is visually indistinguishable inside a ~280 dp preview tile and trims the
 * working set by ~4× without affecting export quality (exports go through their own
 * print-DPI decode in `BusinessCardBitmapRenderer`).
 */
private const val PREVIEW_LOGO_MAX_SIDE_PX = 768
