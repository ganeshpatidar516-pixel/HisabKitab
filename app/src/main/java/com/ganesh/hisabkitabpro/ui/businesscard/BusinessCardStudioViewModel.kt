package com.ganesh.hisabkitabpro.ui.businesscard

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardCategory
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import com.ganesh.hisabkitabpro.domain.businesscard.export.BusinessCardMockupRenderer
import com.ganesh.hisabkitabpro.domain.businesscard.export.BusinessCardPdfExporter
import com.ganesh.hisabkitabpro.domain.businesscard.qr.VCardQrEncoder
import com.ganesh.hisabkitabpro.domain.businesscard.repository.BusinessCardRepository
import com.ganesh.hisabkitabpro.domain.businesscard.vcard.VCardEncoder
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BusinessCardStudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardRepository: BusinessCardRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val variations: List<BusinessCardVariation> = cardRepository.generateAll()

    private val _uiState = MutableStateFlow(
        BusinessCardStudioUiState(
            isProfileLoading = true,
            profile = BusinessCardProfile.EMPTY,
            variations = variations,
        )
    )
    val uiState: StateFlow<BusinessCardStudioUiState> = _uiState.asStateFlow()

    private val _events = Channel<BusinessCardStudioEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val rawProfileFlow = settingsRepository.getBusinessProfile()

    init {
        // Trim stale shareable artefacts on entry so the cache cannot grow unbounded across
        // long usage sessions. Kept intentionally conservative — files newer than the TTL
        // are preserved so an in-flight share that still owns the FileProvider URI is safe.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { trimStaleShareableArtefacts(context) }
        }
        viewModelScope.launch {
            cardRepository.observeProfile().collect { profile ->
                val qr = withContext(Dispatchers.Default) { buildVCardQr(profile) }
                val previous = _uiState.value
                _uiState.value = previous.copy(
                    isProfileLoading = false,
                    profile = profile,
                    qrBitmap = qr,
                )
                // Replace-then-recycle: now that downstream consumers have observed the new
                // bitmap, the old one is dereferenced from state. Recycling here prevents
                // long usage sessions (profile edits, re-emissions) from leaking 720×720
                // ARGB_8888 bitmaps (~2 MB each) which historically compounded native heap.
                val stale = previous.qrBitmap
                if (stale != null && stale !== qr && !stale.isRecycled) {
                    stale.recycle()
                }
            }
        }
        viewModelScope.launch {
            rawProfileFlow.collect { entity ->
                _uiState.value = _uiState.value.copy(
                    logoPath = entity?.logoUrl?.takeIf { it.isNotBlank() },
                    qrFallbackPath = entity?.qrImagePath?.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val qr = _uiState.value.qrBitmap
        if (qr != null && !qr.isRecycled) {
            runCatching { qr.recycle() }
        }
    }

    private fun trimStaleShareableArtefacts(context: Context) {
        val dir = File(context.cacheDir, "business_cards")
        if (!dir.exists() || !dir.isDirectory) return
        val files = dir.listFiles().orEmpty()
        if (files.isEmpty()) return
        val ttlMs = SHAREABLE_ARTEFACT_TTL_MS
        val cutoff = System.currentTimeMillis() - ttlMs
        // Always keep the most recent few files even if older than the TTL — a user who
        // just shared a card may still have the chooser pinned and we must not yank the
        // file from under the receiving app.
        val sortedNewestFirst = files.sortedByDescending { it.lastModified() }
        var purged = 0
        sortedNewestFirst.drop(SHAREABLE_ARTEFACT_RECENT_KEEP).forEach { file ->
            if (file.lastModified() < cutoff) {
                if (file.delete()) purged++
            }
        }
        if (purged > 0) {
            Log.i("BusinessCardEngine", "Trimmed $purged stale shareable artefact(s) from cache.")
        }
    }

    companion object {
        // 24h is long enough for normal share flows to complete; long enough for a user to
        // re-share the same file from their gallery without us regenerating it.
        private const val SHAREABLE_ARTEFACT_TTL_MS = 24L * 60L * 60L * 1000L
        // Files we always retain regardless of age — a hard floor against accidentally
        // yanking an in-flight share that owns an active FileProvider URI.
        private const val SHAREABLE_ARTEFACT_RECENT_KEEP = 8
    }

    private fun buildVCardQr(profile: BusinessCardProfile): Bitmap? {
        if (!profile.hasIdentity) return null
        val rawProfile = com.ganesh.hisabkitabpro.domain.model.BusinessProfile(
            businessName = profile.businessName,
            ownerName = profile.ownerName,
            address = profile.address,
            phone = profile.phone,
            email = profile.email,
            gstNumber = profile.gstin,
            panNumber = profile.pan,
            upiId = profile.upi,
            websiteUrl = profile.website,
            businessCategory = profile.businessCategory,
            operatingHours = profile.operatingHours,
            instagramUrl = profile.instagramUrl,
            facebookUrl = profile.facebookUrl,
            linkedInUrl = profile.linkedInUrl,
            youtubeUrl = profile.youtubeUrl,
            twitterUrl = profile.twitterUrl,
            whatsAppBusinessUrl = profile.whatsAppBusinessUrl,
            googleBusinessProfileUrl = profile.googleBusinessProfileUrl,
            mapLink = profile.mapLink,
            tagline = profile.tagline,
            servicesDescription = profile.servicesDescription,
            cardCtaText = profile.cardCtaText,
        )
        val payload = VCardEncoder.encode(rawProfile)
        return VCardQrEncoder.encode(payload, sizePx = 720)
    }

    fun selectCategory(category: BusinessCardCategory?) {
        _uiState.value = _uiState.value.copy(focusCategory = category)
    }

    fun exportEntireSheet() {
        val state = _uiState.value
        if (!state.profile.hasIdentity) {
            viewModelScope.launch { _events.send(BusinessCardStudioEvent.MissingProfile) }
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isExporting = true, exportingMessage = "Exporting 50 cards…")
            val uri = withContext(Dispatchers.IO) {
                runCatching {
                    BusinessCardPdfExporter.exportAll(
                        context = context,
                        variations = state.variations,
                        profile = state.profile,
                        logoPath = state.logoPath,
                        qrBitmap = state.qrBitmap,
                    )
                }.getOrNull()
            }
            _uiState.value = _uiState.value.copy(isExporting = false, exportingMessage = null)
            if (uri != null) {
                _events.send(BusinessCardStudioEvent.SheetReady(uri))
            } else {
                _events.send(BusinessCardStudioEvent.ExportFailed)
            }
        }
    }

    fun exportSingleMockup(variation: BusinessCardVariation) {
        val state = _uiState.value
        if (!state.profile.hasIdentity) {
            viewModelScope.launch { _events.send(BusinessCardStudioEvent.MissingProfile) }
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isExporting = true, exportingMessage = "Rendering mockup…")
            val uri = withContext(Dispatchers.IO) {
                runCatching {
                    BusinessCardMockupRenderer.renderMockup(
                        context = context,
                        variation = variation,
                        profile = state.profile,
                        logoPath = state.logoPath,
                        qrBitmap = state.qrBitmap,
                    )
                }.getOrNull()
            }
            _uiState.value = _uiState.value.copy(isExporting = false, exportingMessage = null)
            if (uri != null) {
                _events.send(BusinessCardStudioEvent.MockupReady(uri, variation.id))
            } else {
                _events.send(BusinessCardStudioEvent.ExportFailed)
            }
        }
    }

    fun exportSingleCardPng(variation: BusinessCardVariation) {
        val state = _uiState.value
        if (!state.profile.hasIdentity) {
            viewModelScope.launch { _events.send(BusinessCardStudioEvent.MissingProfile) }
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isExporting = true, exportingMessage = "Exporting PNG…")
            val uri = withContext(Dispatchers.IO) {
                runCatching {
                    BusinessCardPdfExporter.exportSingleAsImage(
                        context = context,
                        variation = variation,
                        profile = state.profile,
                        logoPath = state.logoPath,
                        qrBitmap = state.qrBitmap,
                    )
                }.getOrNull()
            }
            _uiState.value = _uiState.value.copy(isExporting = false, exportingMessage = null)
            if (uri != null) {
                _events.send(BusinessCardStudioEvent.CardPngReady(uri, variation.id))
            } else {
                _events.send(BusinessCardStudioEvent.ExportFailed)
            }
        }
    }
}

data class BusinessCardStudioUiState(
    val isProfileLoading: Boolean,
    val profile: BusinessCardProfile,
    val variations: List<BusinessCardVariation>,
    val focusCategory: BusinessCardCategory? = null,
    val logoPath: String? = null,
    val qrFallbackPath: String? = null,
    val qrBitmap: Bitmap? = null,
    val isExporting: Boolean = false,
    val exportingMessage: String? = null,
)

sealed interface BusinessCardStudioEvent {
    data class SheetReady(val pdfUri: Uri) : BusinessCardStudioEvent
    data class MockupReady(val imageUri: Uri, val variationId: String) : BusinessCardStudioEvent
    data class CardPngReady(val pngUri: Uri, val variationId: String) : BusinessCardStudioEvent
    data object MissingProfile : BusinessCardStudioEvent
    data object ExportFailed : BusinessCardStudioEvent
}
