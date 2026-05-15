@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.suppliers

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.addon.reminder.AutoReminderChannel
import com.ganesh.hisabkitabpro.addon.reminder.AutoReminderTone
import com.ganesh.hisabkitabpro.addon.reminder.ReminderAutomationPrefs
import com.ganesh.hisabkitabpro.addon.reminder.ReminderBehaviorEngine
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.ProfileMapFooter
import com.ganesh.hisabkitabpro.domain.model.Party
import com.ganesh.hisabkitabpro.domain.reminder.ReminderLocalization
import com.ganesh.hisabkitabpro.domain.reminder.SmsPaymentReminder
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppSender
import com.ganesh.hisabkitabpro.ui.viewmodel.PartyViewModel
import com.ganesh.hisabkitabpro.util.safeClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun SupplierLedgerScreen(
    supplier: Party,
    partyViewModel: PartyViewModel,
    onOpenLightOcr: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onOpenStatement: () -> Unit = {},
    onReminderHistoryClick: () -> Unit = {},
    onReminderControlClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    var balancePaise by remember(supplier.id) { mutableLongStateOf(supplier.totalBalance) }
    var refreshKey by remember(supplier.id) { mutableStateOf(0) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var billImageUriText by remember { mutableStateOf("") }
    var showAdvancedSheet by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showQuickBillSheet by remember { mutableStateOf(false) }
    var showAttachedBillPreview by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var supplierOcrLowConfidenceBanner by remember { mutableStateOf(false) }
    var dialogIsPurchase by remember { mutableStateOf(true) }
    var showBalanceBreakdown by remember { mutableStateOf(false) }
    var termDays by remember(supplier.id) { mutableStateOf(SupplierCreditTermsPrefs.getTermDays(context, supplier.id)) }
    var isReconciliationVerified by remember(supplier.id) { mutableStateOf(SupplierReconciliationPrefs.isVerified(context, supplier.id)) }
    var lastReconcileRequestedAt by remember(supplier.id) { mutableLongStateOf(SupplierReconciliationPrefs.getLastRequestedAt(context, supplier.id)) }

    val scope = rememberCoroutineScope()
    val amountFocusRequester = remember { FocusRequester() }
    val businessProfile by produceState<BusinessProfile?>(initialValue = null, supplier.id) {
        value = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).businessProfileDao().getBusinessProfileOnce()
        }
    }
    var nextAutoReminderAt by remember(supplier.id) {
        mutableStateOf(
            ReminderAutomationPrefs.getSupplierPartyPauseUntil(context, supplier.id)
                .takeIf { it > System.currentTimeMillis() }
                ?: (System.currentTimeMillis() + 12L * 60L * 60L * 1000L)
        )
    }

    LaunchedEffect(supplier.id, refreshKey) {
        partyViewModel.refreshSupplierLedger(supplier.id)
    }
    val logs by partyViewModel.supplierLedgerLogs.collectAsState()

    fun runSupplierAdaptiveReminder() {
        if (balancePaise <= 0L) {
            Toast.makeText(context, ReminderLocalization.noDueBalanceText(context), Toast.LENGTH_SHORT).show()
            return
        }
        val businessTitle = businessProfile?.businessName?.trim()?.ifBlank { null } ?: "HisabKitab Pro"
        val digits = supplier.phone.filter { it.isDigit() }
        if (digits.isBlank()) {
            Toast.makeText(context, ReminderLocalization.phoneUnavailableText(context), Toast.LENGTH_SHORT).show()
            return
        }
        val oldestAt = logs.minOfOrNull { it.createdAt } ?: System.currentTimeMillis()
        val daysOverdue = ((System.currentTimeMillis() - oldestAt) / (24L * 60L * 60L * 1000L)).toInt().coerceAtLeast(0)
        val previousAttempts = ReminderAutomationPrefs.getReminderAttemptsSupplierParty(context, supplier.id)
        val preferredChannel = ReminderAutomationPrefs.getPreferredChannelSupplierParty(context, supplier.id)
        val plan = ReminderBehaviorEngine.selectPlan(
            daysOverdue = daysOverdue,
            netDuePaise = balancePaise,
            previousAttempts = previousAttempts,
            preferredChannel = preferredChannel
        )
        val lc = AppLocaleManager.wrapContext(context)
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val message = when (plan.tone) {
            AutoReminderTone.POLITE -> lc.getString(R.string.reminder_supplier_master_polite, supplier.name, nf.format(balancePaise / 100.0))
            AutoReminderTone.PROFESSIONAL -> lc.getString(R.string.reminder_supplier_master_professional, supplier.name, nf.format(balancePaise / 100.0))
            AutoReminderTone.STRICT -> lc.getString(R.string.reminder_supplier_master_strict, supplier.name, nf.format(balancePaise / 100.0))
            AutoReminderTone.PARTIAL_OFFER -> {
                val upfrontPaise = (balancePaise * 25L / 100L).coerceAtLeast(1L)
                lc.getString(
                    R.string.reminder_supplier_partial,
                    supplier.name,
                    nf.format(balancePaise / 100.0),
                    nf.format(upfrontPaise / 100.0)
                )
            }
        }
        val branded = buildString {
            append("*").append(businessTitle).append("*\n")
            append(lc.getString(R.string.reminder_branding_powered)).append("\n\n")
            append(message).append("\n")
            append(lc.getString(R.string.reminder_qr_footer))
            ProfileMapFooter.mapFooter(businessProfile)?.let { foot ->
                append("\n\n").append(foot)
            }
        }

        val finalChannel = when (plan.channel) {
            AutoReminderChannel.WHATSAPP -> {
                val waOpened = WhatsAppSender.sendTextReminder(context, digits, branded)
                if (waOpened) {
                    AutoReminderChannel.WHATSAPP
                } else {
                    val smsOpened = SmsPaymentReminder.openSupplierPayableReminder(
                        context = context,
                        supplierName = supplier.name,
                        rawPhone = supplier.phone,
                        payablePaise = balancePaise,
                        businessName = businessTitle,
                        currencyFormatter = formatter
                    )
                    if (smsOpened) AutoReminderChannel.SMS else null
                }
            }
            AutoReminderChannel.SMS -> {
                val smsOpened = SmsPaymentReminder.openSupplierPayableReminder(
                    context = context,
                    supplierName = supplier.name,
                    rawPhone = supplier.phone,
                    payablePaise = balancePaise,
                    businessName = businessTitle,
                    currencyFormatter = formatter
                )
                if (smsOpened) {
                    AutoReminderChannel.SMS
                } else {
                    val waOpened = WhatsAppSender.sendTextReminder(context, digits, branded)
                    if (waOpened) AutoReminderChannel.WHATSAPP else null
                }
            }
        }

        if (finalChannel == null) {
            Toast.makeText(context, ReminderLocalization.channelUnavailableText(context), Toast.LENGTH_SHORT).show()
            return
        }

        ReminderAutomationPrefs.markManualReminderSentSupplierParty(context, supplier.id, transactionId = 0L)
        ReminderAutomationPrefs.setManualPauseForSupplierParty(context, supplier.id, days = 7)
        ReminderAutomationPrefs.incrementReminderAttemptsSupplierParty(context, supplier.id)
        ReminderAutomationPrefs.setPreferredChannelSupplierParty(context, supplier.id, finalChannel)
        nextAutoReminderAt = ReminderAutomationPrefs.getSupplierPartyPauseUntil(context, supplier.id)
            .takeIf { it > System.currentTimeMillis() }
            ?: nextAutoReminderAt

        scope.launch(Dispatchers.IO) {
            try {
                AppDatabase.getDatabase(context).auditLogDao().insert(
                    AuditLogEntry(
                        entityType = "REMINDER",
                        entityId = supplier.id,
                        action = "AUTO_PLAN_${plan.tone}_${finalChannel.name}",
                        detail = "partySupplierId=${supplier.id},daysOverdue=$daysOverdue,attempts=$previousAttempts,netPayable=$balancePaise"
                    )
                )
            } catch (_: Exception) {
            }
        }
        Toast.makeText(
            context,
            if (finalChannel == AutoReminderChannel.WHATSAPP) ReminderLocalization.whatsappOpenedText(context)
            else ReminderLocalization.smsOpenedText(context),
            Toast.LENGTH_SHORT
        ).show()
    }

    val scanPrefill by partyViewModel.pendingSupplierScanPrefill.collectAsState()
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            billImageUriText = uri.toString()
            if (noteText.isBlank()) noteText = context.getString(R.string.supplier_note_from_gallery)
            showQuickBillSheet = false
            // Ensure gallery flow is actionable even when opened directly from bottom bar "Add Bill".
            dialogIsPurchase = true
            showEntryDialog = true
            Toast.makeText(context, context.getString(R.string.supplier_toast_bill_selected_review), Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(scanPrefill?.supplierId, supplier.id) {
        val prefill = scanPrefill
        if (prefill != null && prefill.supplierId == supplier.id) {
            amountText = prefill.amountText
            if (noteText.isBlank()) noteText = prefill.note
            if (!prefill.billImageUri.isNullOrBlank()) billImageUriText = prefill.billImageUri
            supplierOcrLowConfidenceBanner = prefill.lowConfidenceAmount
            partyViewModel.consumePendingSupplierScanPrefill()
            dialogIsPurchase = true
            showEntryDialog = true
        }
    }
    LaunchedEffect(showEntryDialog) {
        if (showEntryDialog) {
            amountFocusRequester.requestFocus()
        }
    }

    fun applyQuickEntry(isPurchase: Boolean) {
        val amountPaise = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
        if (amountPaise <= 0L) {
            Toast.makeText(context, context.getString(R.string.supplier_toast_enter_valid_amount), Toast.LENGTH_SHORT).show()
            return
        }
        val dueAt = if (isPurchase) System.currentTimeMillis() + termDays * 24L * 60L * 60L * 1000L else null
        partyViewModel.recordSupplierEntry(
            supplierId = supplier.id,
            amountPaise = amountPaise,
            isPurchase = isPurchase,
            note = noteText,
            tag = "General",
            dueAt = dueAt,
            billImageUri = billImageUriText.ifBlank { null }
        ) { ok ->
            if (ok) {
                balancePaise += if (isPurchase) amountPaise else -amountPaise
                scheduleSupplierDueReminder(context, supplier.name, amountPaise, dueAt)
                refreshKey++
                amountText = ""
                billImageUriText = ""
            }
        }
    }

    fun exportCsv() {
        exportSupplierLedgerCsv(context, supplier, logs)
    }

    fun exportPdf() {
        exportSupplierLedgerPdf(context, supplier, businessProfile, logs, balancePaise, formatter)
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground,
                    navigationIconContentColor = colorScheme.onBackground
                ),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(supplier.name, fontWeight = FontWeight.ExtraBold)
                                if (isReconciliationVerified) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.supplier_verified),
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.supplier_view_profile),
                                color = colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                stringResource(R.string.supplier_total),
                                color = colorScheme.error,
                                fontSize = 11.sp,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                formatter.format(abs(balancePaise / 100.0)),
                                color = if (balancePaise >= 0) colorScheme.error else SupplierPayActionGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                modifier = Modifier.clickable { showBalanceBreakdown = true }
                            )
                        }
                    }
                },
                navigationIcon = {
                            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back)) }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onOpenStatement, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.supplier_report))
                        }
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))) },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            )
                        ) { Text(stringResource(R.string.supplier_call)) }
                    }
                    SupplierLedgerAutoReminderStrip(
                        balancePaise = balancePaise,
                        nextAutoReminderAt = nextAutoReminderAt,
                        formatter = formatter,
                        onNextAutoClick = onReminderControlClick,
                        onOpenReminderHistory = onReminderHistoryClick,
                        onOpenReminderControl = onReminderControlClick,
                        onRemindNow = { runSupplierAdaptiveReminder() }
                    )
                    Surface(
                        color = colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.supplier_balance_due), color = colorScheme.onSurfaceVariant)
                            Text(
                                formatter.format(abs(balancePaise / 100.0)),
                                color = colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                dialogIsPurchase = false
                                showEntryDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text(stringResource(R.string.supplier_received), color = SupplierPayActionGreen) }
                        OutlinedButton(
                            onClick = {
                                dialogIsPurchase = true
                                showEntryDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text(stringResource(R.string.supplier_given), color = SupplierPayableRed) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://api.whatsapp.com/send?phone=91${supplier.phone}")
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(22.dp)
                        ) { Text(stringResource(R.string.supplier_whatsapp), color = SupplierVerifiedBlue) }
                        OutlinedButton(
                            onClick = { showQuickBillSheet = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(22.dp)
                        ) { Text(stringResource(R.string.supplier_add_bill), color = colorScheme.primary) }
                        TextButton(
                            onClick = { showAdvancedSheet = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = colorScheme.primary)
                            Text(
                                stringResource(R.string.supplier_more),
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colorScheme.background,
                            colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            Spacer(Modifier.height(6.dp))

            if (logs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.supplier_no_transactions), color = colorScheme.onSurfaceVariant)
                }
            } else {
                val timelineTodayLabel = stringResource(R.string.supplier_timeline_today)
                val timelineRows = remember(logs, timelineTodayLabel) { buildTimelineRows(logs, timelineTodayLabel) }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(timelineRows, key = { it.key }) { row ->
                        when (row) {
                            is TimelineRow.Header -> TimelineDateHeader(row.label)
                            is TimelineRow.Entry -> SupplierTimelineItem(
                                entry = row.entry,
                                formatter = formatter,
                                supplierId = supplier.id
                            )
                        }
                    }
                }
            }

        }
    }

    if (showQuickBillSheet) {
        ModalBottomSheet(onDismissRequest = { showQuickBillSheet = false }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(onClick = {
                        showQuickBillSheet = false
                        onOpenLightOcr()
                    }) { Text(stringResource(R.string.supplier_camera)) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(onClick = {
                        galleryPicker.launch("image/*")
                    }) { Text(stringResource(R.string.supplier_gallery)) }
                }
            }
        }
    }

    if (showAttachedBillPreview && billImageUriText.isNotBlank()) {
        val imageBitmap = remember(billImageUriText) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(billImageUriText))?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
        AlertDialog(
            onDismissRequest = { showAttachedBillPreview = false },
            title = { Text(stringResource(R.string.supplier_bill_preview)) },
            text = {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = stringResource(R.string.supplier_cd_attached_bill),
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                } else {
                    Text(stringResource(R.string.supplier_unable_preview_image))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachedBillPreview = false }) { Text(stringResource(R.string.common_close)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        billImageUriText = ""
                        showAttachedBillPreview = false
                    }
                ) { Text(stringResource(R.string.supplier_remove_bill), color = Color(0xFFD32F2F)) }
            }
        )
    }

    if (showTermsDialog) {
        SupplierTermDaysDialog(
            currentDays = termDays,
            onDismiss = { showTermsDialog = false },
            onSave = {
                termDays = it
                SupplierCreditTermsPrefs.setTermDays(context, supplier.id, it)
                showTermsDialog = false
            }
        )
    }

    if (showEntryDialog) {
        AlertDialog(
            onDismissRequest = {
                showEntryDialog = false
                supplierOcrLowConfidenceBanner = false
            },
            title = { Text(if (dialogIsPurchase) stringResource(R.string.supplier_add_given_entry) else stringResource(R.string.supplier_add_received_entry), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (supplierOcrLowConfidenceBanner) {
                        Text(
                            text = stringResource(R.string.ocr_low_confidence_banner),
                            color = Color(0xFFB06000),
                            fontSize = 12.sp,
                        )
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(amountFocusRequester),
                        label = { Text(stringResource(R.string.supplier_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.supplier_add_note_optional)) }
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showQuickBillSheet = true }) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Text(stringResource(R.string.supplier_add_bill))
                        }
                        if (billImageUriText.isNotBlank()) {
                            TextButton(onClick = { showAttachedBillPreview = true }) { Text(stringResource(R.string.supplier_preview_bill)) }
                        }
                    }
                    if (billImageUriText.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.supplier_bill_attached), color = Color(0xFF2E7D32), fontSize = 12.sp)
                            TextButton(onClick = { showAttachedBillPreview = true }) { Text(stringResource(R.string.supplier_preview)) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        applyQuickEntry(isPurchase = dialogIsPurchase)
                        showEntryDialog = false
                        supplierOcrLowConfidenceBanner = false
                    }
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEntryDialog = false
                    supplierOcrLowConfidenceBanner = false
                }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showAdvancedSheet) {
        ModalBottomSheet(onDismissRequest = { showAdvancedSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.supplier_more_tools), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.supplier_reminder_section), color = SupplierLedgerMutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            showAdvancedSheet = false
                            runSupplierAdaptiveReminder()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Text(stringResource(R.string.customer_remind_now))
                    }
                    OutlinedButton(
                        onClick = {
                            showAdvancedSheet = false
                            onReminderHistoryClick()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Text(stringResource(R.string.customer_reminder_history))
                    }
                }
                OutlinedButton(
                    onClick = {
                        showAdvancedSheet = false
                        onReminderControlClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Text(stringResource(R.string.reminder_auto_settings_short))
                }
                Text(stringResource(R.string.supplier_communication), color = SupplierLedgerMutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))) },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.supplier_call)) }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=91${supplier.phone}"))) },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.supplier_whatsapp)) }
                }
                OutlinedButton(onClick = { showQuickBillSheet = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.supplier_add_bill)) }
                Text(stringResource(R.string.supplier_payment_credit), color = SupplierLedgerMutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { showTermsDialog = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.supplier_set_credit_terms)) }
                OutlinedButton(onClick = {
                    val msg = buildSupplierFollowUpMessage(context, supplier.name, balancePaise, System.currentTimeMillis() + termDays * 24L * 60L * 60L * 1000L)
                    val encoded = Uri.encode(msg)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=91${supplier.phone}&text=$encoded")))
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.supplier_follow_up_reminder)) }
                Text(stringResource(R.string.supplier_reconciliation), color = SupplierLedgerMutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = {
                    val verifyLink = "https://hisabkitab.local/reconcile?supplierId=${supplier.id}&amount=$balancePaise"
                    val lc = AppLocaleManager.wrapContext(context)
                    val body = lc.getString(
                        R.string.supplier_reconcile_whatsapp_message,
                        formatter.format(balancePaise / 100.0),
                        verifyLink
                    )
                    val msg = Uri.encode(body)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=91${supplier.phone}&text=$msg")))
                    lastReconcileRequestedAt = System.currentTimeMillis()
                    SupplierReconciliationPrefs.setLastRequestedAt(context, supplier.id, lastReconcileRequestedAt)
                    SupplierReconciliationPrefs.setVerified(context, supplier.id, false)
                    isReconciliationVerified = false
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.supplier_send_reconcile_link)) }
                OutlinedButton(onClick = {
                    isReconciliationVerified = !isReconciliationVerified
                    SupplierReconciliationPrefs.setVerified(context, supplier.id, isReconciliationVerified)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isReconciliationVerified) stringResource(R.string.supplier_verified) else stringResource(R.string.supplier_mark_verified))
                }
                if (lastReconcileRequestedAt > 0L) {
                    val lastFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(lastReconcileRequestedAt))
                    Text(
                        stringResource(R.string.supplier_reconcile_last_request, lastFmt),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Text(stringResource(R.string.supplier_export), color = SupplierLedgerMutedText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { exportCsv() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.supplier_export_csv)) }
                OutlinedButton(onClick = { exportPdf() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.supplier_export_pdf)) }
            }
        }
    }

    if (showBalanceBreakdown) {
        val payable = if (balancePaise > 0) balancePaise else 0L
        val advance = if (balancePaise < 0) -balancePaise else 0L
        AlertDialog(
            onDismissRequest = { showBalanceBreakdown = false },
            title = { Text(stringResource(R.string.supplier_balance_breakdown), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${stringResource(R.string.supplier_total_payable)} ${formatter.format(payable / 100.0)}")
                    Text("${stringResource(R.string.supplier_advance_paid)} ${formatter.format(advance / 100.0)}")
                    Text(
                        if (balancePaise >= 0) stringResource(R.string.supplier_status_pay)
                        else stringResource(R.string.supplier_status_advance),
                        color = if (balancePaise >= 0) SupplierPayableRed else SupplierPayActionGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBalanceBreakdown = false }) { Text(stringResource(R.string.common_close)) }
            }
        )
    }
}
