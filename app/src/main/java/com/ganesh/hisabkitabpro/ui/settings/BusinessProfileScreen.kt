package com.ganesh.hisabkitabpro.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.GstGatekeeper
import com.ganesh.hisabkitabpro.domain.profile.LiveLocationEngine
import com.ganesh.hisabkitabpro.domain.businessidentity.BusinessIdentityInputNormalizer
import com.ganesh.hisabkitabpro.domain.businessidentity.ProfileCompletionCalculator
import com.ganesh.hisabkitabpro.domain.profile.MerchantLogoPipeline
import com.ganesh.hisabkitabpro.domain.profile.ProfileMediaPipeline
import com.ganesh.hisabkitabpro.ui.payment.FintechUpiPaymentCard
import com.ganesh.hisabkitabpro.ui.settings.businessidentity.ProfessionalInfoAlertDialog
import com.ganesh.hisabkitabpro.ui.viewmodel.SettingsViewModel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Collect states safely
    val profileState by viewModel.businessProfile.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val gstEnabled = settingsState?.gstEnabled == true

    // STABLE STATE INITIALIZATION (ZERO CRASH POLICY)
    var businessName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstNumber by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var qrImagePath by remember { mutableStateOf("") }
    var logoPath by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var servicesDescription by remember { mutableStateOf("") }
    var cardCtaText by remember { mutableStateOf("") }
    var businessCategory by remember { mutableStateOf("") }
    var operatingHours by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var instagramUrl by remember { mutableStateOf("") }
    var facebookUrl by remember { mutableStateOf("") }
    var linkedInUrl by remember { mutableStateOf("") }
    var youtubeUrl by remember { mutableStateOf("") }
    var twitterUrl by remember { mutableStateOf("") }
    var whatsAppBusinessUrl by remember { mutableStateOf("") }
    var googleBusinessProfileUrl by remember { mutableStateOf("") }
    var signatureImagePath by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var mapLink by remember { mutableStateOf("") }
    var locationLockedAt by remember { mutableLongStateOf(0L) }
    var showProfessionalInfoModal by remember { mutableStateOf(false) }
    var mediaStatus by remember { mutableStateOf<String?>(null) }
    var signatureLines by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var saveSuccess by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    // FILE PROVIDER SETUP - Strict matching with Manifest
    val providerAuthority = "com.ganesh.hisabkitabpro.provider"
    val qrFile = remember { AppStoragePaths.businessQrFile(context) }
    val logoFile = remember { AppStoragePaths.businessLogoFile(context) }
    val qrUri = remember { 
        try {
            FileProvider.getUriForFile(context, providerAuthority, qrFile)
        } catch (e: Exception) {
            Log.e("BusinessProfile", "CRITICAL: FileProvider Authority Mismatch or Config Error", e)
            null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                mediaStatus = "Optimizing QR image…"
                runCatching {
                    withContext(Dispatchers.IO) {
                        ProfileMediaPipeline.processQrImage(
                            context = context,
                            sourceUri = it,
                            logoPath = logoPath.ifBlank { null },
                            outputName = qrFile.name,
                        )
                    }
                }.onSuccess { file ->
                    qrImagePath = file.absolutePath
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    mediaStatus = "QR optimized and fitted"
                }.onFailure { error ->
                    Log.e("BusinessProfile", "Gallery Recovery: Failed to process QR", error)
                    mediaStatus = "Could not optimize QR. Please try another image."
                }
            }
        }
    }

    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                mediaStatus = "Saving logo…"
                runCatching {
                    withContext(Dispatchers.IO) {
                        MerchantLogoPipeline.processMerchantLogoFromUri(context, it, logoFile)
                    }
                }.onSuccess { file ->
                    logoPath = file.absolutePath
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    mediaStatus = "Logo saved. Re-upload QR to refresh the badge."
                }.onFailure { error ->
                    Log.e("BusinessProfile", "Logo upload failed", error)
                    mediaStatus = "Logo upload failed"
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            scope.launch {
                mediaStatus = "Deskewing captured QR…"
                runCatching {
                    withContext(Dispatchers.IO) {
                        ProfileMediaPipeline.processQrImage(
                            context = context,
                            sourceUri = Uri.fromFile(qrFile),
                            logoPath = logoPath.ifBlank { null },
                            outputName = qrFile.name,
                        )
                    }
                }.onSuccess { file ->
                    qrImagePath = file.absolutePath
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    mediaStatus = "Captured QR optimized"
                }.onFailure { error ->
                    Log.e("BusinessProfile", "Camera QR optimization failed", error)
                    qrImagePath = qrFile.absolutePath
                    mediaStatus = "QR captured. Optimization fallback used."
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && qrUri != null) {
            cameraLauncher.launch(qrUri)
        }
    }

    // SAFE UPDATE FROM DB
    LaunchedEffect(profileState) {
        profileState?.let {
            businessName = it.businessName
            ownerName = it.ownerName
            address = it.address
            phone = it.phone
            email = it.email
            gstNumber = it.gstNumber
            panNumber = it.panNumber
            upiId = it.upiId
            qrImagePath = it.qrImagePath
            logoPath = it.logoUrl
            tagline = it.tagline.orEmpty()
            servicesDescription = it.servicesDescription.orEmpty()
            cardCtaText = it.cardCtaText.orEmpty()
            businessCategory = it.businessCategory
            operatingHours = it.operatingHours
            websiteUrl = it.websiteUrl
            instagramUrl = it.instagramUrl
            facebookUrl = it.facebookUrl
            linkedInUrl = it.linkedInUrl.orEmpty()
            youtubeUrl = it.youtubeUrl.orEmpty()
            twitterUrl = it.twitterUrl.orEmpty()
            whatsAppBusinessUrl = it.whatsAppBusinessUrl.orEmpty()
            googleBusinessProfileUrl = it.googleBusinessProfileUrl.orEmpty()
            signatureImagePath = it.signatureImagePath
            latitudeText = it.latitude?.toString().orEmpty()
            longitudeText = it.longitude?.toString().orEmpty()
            mapLink = it.mapLink
            locationLockedAt = it.locationLockedAt
        }
    }
    val hasUnsavedChanges = remember(
        profileState, businessName, ownerName, address, phone, email, gstNumber, panNumber, upiId,
        qrImagePath, logoPath, tagline, servicesDescription, cardCtaText, businessCategory, operatingHours, websiteUrl, instagramUrl,
        facebookUrl, linkedInUrl, youtubeUrl, twitterUrl, whatsAppBusinessUrl, googleBusinessProfileUrl,
        signatureImagePath, latitudeText, longitudeText, mapLink, locationLockedAt
    ) {
        val original = profileState
        if (original == null) {
            listOf(
                businessName, ownerName, address, phone, email, gstNumber, panNumber, upiId,
                qrImagePath, logoPath, tagline, servicesDescription, cardCtaText, businessCategory, operatingHours, websiteUrl,
                instagramUrl, facebookUrl, linkedInUrl, youtubeUrl, twitterUrl, whatsAppBusinessUrl,
                googleBusinessProfileUrl, signatureImagePath, latitudeText, longitudeText, mapLink
            )
                .any { it.isNotBlank() }
        } else {
            businessName != original.businessName ||
                ownerName != original.ownerName ||
                address != original.address ||
                phone != original.phone ||
                email != original.email ||
                gstNumber != original.gstNumber ||
                panNumber != original.panNumber ||
                upiId != original.upiId ||
                qrImagePath != original.qrImagePath ||
                logoPath != original.logoUrl ||
                tagline != (original.tagline ?: "") ||
                servicesDescription != (original.servicesDescription ?: "") ||
                cardCtaText != (original.cardCtaText ?: "") ||
                businessCategory != original.businessCategory ||
                operatingHours != original.operatingHours ||
                websiteUrl != original.websiteUrl ||
                instagramUrl != original.instagramUrl ||
                facebookUrl != original.facebookUrl ||
                linkedInUrl != (original.linkedInUrl ?: "") ||
                youtubeUrl != (original.youtubeUrl ?: "") ||
                twitterUrl != (original.twitterUrl ?: "") ||
                whatsAppBusinessUrl != (original.whatsAppBusinessUrl ?: "") ||
                googleBusinessProfileUrl != (original.googleBusinessProfileUrl ?: "") ||
                signatureImagePath != original.signatureImagePath ||
                latitudeText != original.latitude?.toString().orEmpty() ||
                longitudeText != original.longitude?.toString().orEmpty() ||
                mapLink != original.mapLink ||
                locationLockedAt != original.locationLockedAt
        }
    }

    val profileCompletionPercent = remember(
        businessName, ownerName, address, phone, email, gstNumber, panNumber, upiId,
        qrImagePath, logoPath, tagline, servicesDescription, cardCtaText, businessCategory, operatingHours, websiteUrl, instagramUrl,
        facebookUrl, linkedInUrl, youtubeUrl, twitterUrl, whatsAppBusinessUrl, googleBusinessProfileUrl,
        signatureImagePath, latitudeText, longitudeText, mapLink, locationLockedAt, gstEnabled,
    ) {
        ProfileCompletionCalculator.percent(
            BusinessProfile(
                businessName = businessName,
                ownerName = ownerName,
                address = address,
                phone = phone,
                email = email,
                gstNumber = if (gstEnabled) GstGatekeeper.normalize(gstNumber) else "",
                panNumber = panNumber,
                upiId = upiId,
                qrImagePath = qrImagePath,
                logoUrl = logoPath,
                tagline = tagline.trim().ifBlank { null },
                servicesDescription = servicesDescription.trim().ifBlank { null },
                cardCtaText = cardCtaText.trim().ifBlank { null },
                businessCategory = businessCategory,
                operatingHours = operatingHours,
                websiteUrl = websiteUrl,
                instagramUrl = instagramUrl,
                facebookUrl = facebookUrl,
                linkedInUrl = linkedInUrl.ifBlank { null },
                youtubeUrl = youtubeUrl.ifBlank { null },
                twitterUrl = twitterUrl.ifBlank { null },
                whatsAppBusinessUrl = whatsAppBusinessUrl.ifBlank { null },
                googleBusinessProfileUrl = googleBusinessProfileUrl.ifBlank { null },
                signatureImagePath = signatureImagePath,
                latitude = latitudeText.toDoubleOrNull(),
                longitude = longitudeText.toDoubleOrNull(),
                mapLink = mapLink,
                locationLockedAt = locationLockedAt,
            ),
            gstEnabled,
        )
    }

    fun validateAndSave() {
        val business = businessName.trim()
        val owner = ownerName.trim()
        val mobile = phone.trim()
        val mail = email.trim()
        val gst = gstNumber.trim()
        val upi = upiId.trim()
        if (business.length < 2) {
            validationMessage = "Business Name is required."
            showValidationDialog = true
            return
        }
        if (owner.length < 2) {
            validationMessage = "Owner Name is required."
            showValidationDialog = true
            return
        }
        if (mobile.length < 10) {
            validationMessage = "Phone Number must be at least 10 digits."
            showValidationDialog = true
            return
        }
        if (mail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            validationMessage = "Email format is invalid."
            showValidationDialog = true
            return
        }
        val finalGst = if (gstEnabled) GstGatekeeper.normalize(gst) else ""
        if (gstEnabled && finalGst.isNotBlank() && !GstGatekeeper.isValid(finalGst)) {
            validationMessage = "GSTIN format is invalid."
            showValidationDialog = true
            return
        }
        if (upi.isNotBlank() && !upi.contains("@")) {
            validationMessage = "UPI ID must include '@'."
            showValidationDialog = true
            return
        }
        val webRaw = websiteUrl.trim()
        if (BusinessIdentityInputNormalizer.classifyWebsite(webRaw) ==
            BusinessIdentityInputNormalizer.WebsiteInputKind.LikelyEmail
        ) {
            validationMessage =
                "Website field looks like an email address. Use the Email field instead, then save again."
            showValidationDialog = true
            return
        }
        if (webRaw.isNotBlank() && !BusinessIdentityInputNormalizer.isAcceptableWebsiteForSave(webRaw)) {
            validationMessage =
                "Enter a valid website (for example yourshop.com or https://yourshop.com), or leave Website empty."
            showValidationDialog = true
            return
        }
        val finalWebsite = webRaw.ifBlank { "" }.let { raw ->
            if (raw.isBlank()) ""
            else BusinessIdentityInputNormalizer.normalizeWebsite(raw)?.trim().orEmpty()
        }
        val finalInstagram = BusinessIdentityInputNormalizer.normalizeInstagram(instagramUrl)
        val finalFacebook = BusinessIdentityInputNormalizer.normalizeFacebook(facebookUrl)
        val waRaw = whatsAppBusinessUrl.trim()
        if (waRaw.isNotBlank()) {
            val wWa = BusinessIdentityInputNormalizer.normalizeWhatsAppBusiness(waRaw)
            if (wWa.isBlank()) {
                validationMessage =
                    "WhatsApp Business: use digits with country code (e.g. 9198…), or paste a full https://wa.me/… link."
                showValidationDialog = true
                return
            }
        }
        val finalLinkedIn = BusinessIdentityInputNormalizer.normalizeLinkedIn(linkedInUrl).takeIf { it.isNotBlank() }
        val finalYoutube = BusinessIdentityInputNormalizer.normalizeYoutube(youtubeUrl).takeIf { it.isNotBlank() }
        val finalTwitter = BusinessIdentityInputNormalizer.normalizeTwitter(twitterUrl).takeIf { it.isNotBlank() }
        val finalWhatsApp = BusinessIdentityInputNormalizer.normalizeWhatsAppBusiness(whatsAppBusinessUrl)
            .takeIf { it.isNotBlank() }
        val finalGoogleBiz =
            BusinessIdentityInputNormalizer.normalizeGoogleBusinessProfile(googleBusinessProfileUrl)
                .takeIf { it.isNotBlank() }
        val lat = LiveLocationEngine.parseCoordinate(latitudeText, -90.0, 90.0)
        val lng = LiveLocationEngine.parseCoordinate(longitudeText, -180.0, 180.0)
        if ((latitudeText.isNotBlank() || longitudeText.isNotBlank()) && (lat == null || lng == null)) {
            validationMessage = "Location coordinates must be valid latitude and longitude."
            showValidationDialog = true
            return
        }
        val lockedMapLink = LiveLocationEngine.buildMapLink(lat, lng).ifBlank { mapLink.trim() }
        viewModel.updateBusinessProfile(
            BusinessProfile(
                userId = profileState?.userId ?: "default_user",
                businessName = business,
                ownerName = owner,
                address = address.trim(),
                phone = mobile,
                email = mail,
                gstNumber = finalGst,
                panNumber = panNumber.trim(),
                upiId = upi,
                qrImagePath = qrImagePath.trim(),
                logoUrl = logoPath.trim(),
                tagline = tagline.trim().take(280).ifBlank { null },
                servicesDescription = servicesDescription.trim().take(2000).ifBlank { null },
                cardCtaText = cardCtaText.trim().take(120).ifBlank { null },
                businessCategory = businessCategory.trim(),
                operatingHours = operatingHours.trim(),
                websiteUrl = finalWebsite,
                instagramUrl = finalInstagram,
                facebookUrl = finalFacebook,
                linkedInUrl = finalLinkedIn,
                youtubeUrl = finalYoutube,
                twitterUrl = finalTwitter,
                whatsAppBusinessUrl = finalWhatsApp,
                googleBusinessProfileUrl = finalGoogleBiz,
                signatureImagePath = signatureImagePath.trim(),
                latitude = lat,
                longitude = lng,
                locationLockedAt = if (lockedMapLink.isNotBlank()) locationLockedAt.takeIf { it > 0L } ?: System.currentTimeMillis() else 0L,
                mapLink = lockedMapLink,
                createdAt = profileState?.createdAt ?: System.currentTimeMillis(),
            )
        )
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        Toast.makeText(context, "Business profile saved", Toast.LENGTH_SHORT).show()
        saveSuccess = true
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showUnsavedChangesDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        validateAndSave()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val addressHeadline = remember(address) {
                address.trim().lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
            }

            ProfileStrengthStrip(percent = profileCompletionPercent)

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tagline,
                onValueChange = { if (it.length <= 280) tagline = it },
                label = { Text("Tagline") },
                supportingText = { Text("Shown under your name on business cards") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = servicesDescription,
                onValueChange = { if (it.length <= 2000) servicesDescription = it },
                label = { Text("Services (optional)") },
                supportingText = { Text("One line per service — appears on card rail") },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = cardCtaText,
                onValueChange = { if (it.length <= 120) cardCtaText = it },
                label = { Text("Call to action (optional)") },
                supportingText = { Text("Short line e.g. “Book today · Free quote”") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            MerchantBrandLogoRow(
                logoPath = logoPath,
                businessName = businessName,
                modifier = Modifier.fillMaxWidth(),
            )

            FintechUpiPaymentCard(
                businessName = businessName,
                ownerName = ownerName,
                logoPath = logoPath,
                qrImagePath = qrImagePath,
                upiId = upiId,
                amountPaise = null,
                onUpiChange = { upiId = it },
                mediaStatus = mediaStatus,
                onDismissMediaStatus = { mediaStatus = null },
                showPaymentToolbar = true,
                showUpiEditor = true,
                onCaptureQr = {
                    val permission = Manifest.permission.CAMERA
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        if (qrUri != null) cameraLauncher.launch(qrUri)
                        else Log.e("BusinessProfile", "Camera Mismatch: URI is NULL")
                    } else {
                        showCameraPermissionDialog = true
                    }
                },
                onUploadQr = { galleryLauncher.launch("image/*") },
                onUploadLogo = { logoLauncher.launch("image/*") },
                onShareQr = { shareBusinessQr(context, providerAuthority, qrImagePath) },
            )

            LocationVerifiedCard(
                addressPreview = addressHeadline,
                mapLink = mapLink,
                latitude = latitudeText,
                longitude = longitudeText,
                lockedAt = locationLockedAt,
                onEdit = { showProfessionalInfoModal = true },
            )
            OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            if (gstEnabled) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gstNumber,
                        onValueChange = {
                            gstNumber = GstGatekeeper.normalize(it)
                            GstGatekeeper.lookup(gstNumber)?.let { result ->
                                result.businessNameHint?.let { hint -> if (businessName.isBlank()) businessName = hint }
                                result.addressHint?.let { hint -> if (address.isBlank()) address = hint }
                            }
                        },
                        label = { Text("GSTIN") },
                        supportingText = { Text("GST automation active because GST toggle is ON") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(value = panNumber, onValueChange = { panNumber = it }, label = { Text("PAN") }, modifier = Modifier.weight(1f))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text("GST is OFF: GST fields and automation hidden") },
                        enabled = false
                    )
                    OutlinedTextField(value = panNumber, onValueChange = { panNumber = it }, label = { Text("PAN") }, modifier = Modifier.weight(1f))
                }
            }

            OutlinedButton(
                onClick = { showProfessionalInfoModal = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Edit Professional Info")
            }
            
            Button(
                onClick = {
                    validateAndSave()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedChangesDialog = false
                    onNavigateBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) { Text("Continue Editing") }
            }
        )
    }

    if (showValidationDialog) {
        AlertDialog(
            onDismissRequest = { showValidationDialog = false },
            title = { Text("Validation") },
            text = { Text(validationMessage) },
            confirmButton = {
                TextButton(onClick = { showValidationDialog = false }) { Text("OK") }
            }
        )
    }

    if (showProfessionalInfoModal) {
        ProfessionalInfoAlertDialog(
            onDismiss = { showProfessionalInfoModal = false },
            businessNameHint = businessName,
            businessCategory = businessCategory,
            onBusinessCategoryChange = { businessCategory = it },
            operatingHours = operatingHours,
            onOperatingHoursChange = { operatingHours = it },
            websiteUrl = websiteUrl,
            onWebsiteUrlChange = { websiteUrl = it },
            instagramUrl = instagramUrl,
            onInstagramUrlChange = { instagramUrl = it },
            facebookUrl = facebookUrl,
            onFacebookUrlChange = { facebookUrl = it },
            linkedInUrl = linkedInUrl,
            onLinkedInUrlChange = { linkedInUrl = it },
            youtubeUrl = youtubeUrl,
            onYoutubeUrlChange = { youtubeUrl = it },
            twitterUrl = twitterUrl,
            onTwitterUrlChange = { twitterUrl = it },
            whatsAppBusinessUrl = whatsAppBusinessUrl,
            onWhatsAppBusinessUrlChange = { whatsAppBusinessUrl = it },
            googleBusinessProfileUrl = googleBusinessProfileUrl,
            onGoogleBusinessProfileUrlChange = { googleBusinessProfileUrl = it },
            latitudeText = latitudeText,
            onLatitudeTextChange = { latitudeText = it },
            longitudeText = longitudeText,
            onLongitudeTextChange = { longitudeText = it },
            mapLink = mapLink,
            locationLockedAt = locationLockedAt,
            onApplyCoordinates = { lat, lng ->
                latitudeText = String.format(Locale.US, "%.7f", lat)
                longitudeText = String.format(Locale.US, "%.7f", lng)
                val link = LiveLocationEngine.buildMapLink(lat, lng)
                if (link.isNotBlank()) {
                    mapLink = link
                    locationLockedAt = System.currentTimeMillis()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onLockLiveLocation = {
                val lat = LiveLocationEngine.parseCoordinate(latitudeText, -90.0, 90.0)
                val lng = LiveLocationEngine.parseCoordinate(longitudeText, -180.0, 180.0)
                val link = LiveLocationEngine.buildMapLink(lat, lng)
                if (link.isNotBlank()) {
                    mapLink = link
                    locationLockedAt = System.currentTimeMillis()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else {
                    Toast.makeText(context, "Enter valid latitude/longitude first", Toast.LENGTH_SHORT).show()
                }
            },
            signatureLines = signatureLines,
            onSignatureLinesChange = { signatureLines = it },
            onSignatureImagePathChange = { signatureImagePath = it },
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

    if (saveSuccess) {
        // state consumed on screen exit; this keeps compose state deterministic if screen remains mounted briefly
        saveSuccess = false
    }
}

private fun merchantInitials(businessName: String): String {
    val letters = businessName.trim().uppercase(Locale.US).filter { it.isLetter() }
    if (letters.length >= 2) return letters.take(2)
    if (letters.isNotEmpty()) return letters.padEnd(2, '·')
    return "?"
}

@Composable
private fun MerchantBrandLogoRow(
    logoPath: String,
    businessName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                val exists = logoPath.isNotBlank() && File(logoPath).exists()
                if (exists) {
                    Image(
                        painter = rememberAsyncImagePainter(model = File(logoPath)),
                        contentDescription = "Business logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        merchantInitials(businessName),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column {
                Text(
                    "Brand logo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Bills, PDFs & business card",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileStrengthStrip(percent: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Profile strength",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { percent.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Text(
                "${percent.coerceIn(0, 100)}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LocationVerifiedCard(
    addressPreview: String,
    mapLink: String,
    latitude: String,
    longitude: String,
    lockedAt: Long,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val locked = LiveLocationEngine.isLocked(
        latitude = latitude.toDoubleOrNull(),
        longitude = longitude.toDoubleOrNull(),
        lockedAt = lockedAt,
    )
    var menuOpen by remember { mutableStateOf(false) }
    val preview = when {
        addressPreview.isNotBlank() -> addressPreview
        locked && mapLink.isNotBlank() -> "Pinned on map"
        mapLink.isNotBlank() -> "Map link saved"
        else -> "Add your shop location"
    }
    val subtitle = when {
        locked -> "Verified for bills and cards"
        else -> "Set in Professional Info"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (locked) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (locked) "Location verified" else "Location",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Location actions")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Open in Maps") },
                        onClick = {
                            menuOpen = false
                            openMapFromProfile(context, mapLink, latitude, longitude)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Copy map link") },
                        onClick = {
                            menuOpen = false
                            copyMapLinkToClipboard(context, mapLink)
                        },
                        enabled = mapLink.isNotBlank(),
                    )
                    DropdownMenuItem(
                        text = { Text("Edit location") },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                }
            }
        }
    }
}

private fun shareBusinessQr(context: Context, providerAuthority: String, qrImagePath: String) {
    val file = when {
        qrImagePath.isNotBlank() && File(qrImagePath).exists() -> File(qrImagePath)
        else -> null
    }
    if (file == null) {
        Toast.makeText(context, "Add a QR image first", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = runCatching { FileProvider.getUriForFile(context, providerAuthority, file) }.getOrNull()
    if (uri == null) {
        Toast.makeText(context, "Cannot share this file", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share QR"))
    }.onFailure {
        Toast.makeText(context, "Could not share QR", Toast.LENGTH_SHORT).show()
    }
}

private fun openMapFromProfile(context: Context, mapLink: String, latitude: String, longitude: String) {
    val lat = latitude.toDoubleOrNull()
    val lng = longitude.toDoubleOrNull()
    val uri = when {
        lat != null && lng != null -> Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        mapLink.startsWith("http") -> Uri.parse(mapLink)
        else -> null
    }
    if (uri == null) {
        Toast.makeText(context, "Set a location first", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }.onFailure {
        Toast.makeText(context, "Could not open Maps", Toast.LENGTH_SHORT).show()
    }
}

private fun copyMapLinkToClipboard(context: Context, mapLink: String) {
    if (mapLink.isBlank()) {
        Toast.makeText(context, "Nothing to copy yet", Toast.LENGTH_SHORT).show()
        return
    }
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Map link", mapLink))
    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
}

