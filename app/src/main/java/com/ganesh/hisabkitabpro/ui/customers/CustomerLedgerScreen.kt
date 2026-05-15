@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.ganesh.hisabkitabpro.ui.customers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ganesh.hisabkitabpro.ui.common.PagingListStateOverlay
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.addon.reminder.AutoReminderChannel
import com.ganesh.hisabkitabpro.addon.reminder.AutoReminderTone
import com.ganesh.hisabkitabpro.addon.reminder.ReminderAutomationPrefs
import com.ganesh.hisabkitabpro.addon.reminder.ReminderBehaviorEngine
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.ledger.LedgerPdfFacade
import com.ganesh.hisabkitabpro.domain.cloud.CloudBusinessIdentity
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.payment.UpiIntentBuilder
import com.ganesh.hisabkitabpro.domain.reminder.SmsPaymentReminder
import com.ganesh.hisabkitabpro.domain.reminder.ReminderLocalization
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppPaymentShowcaseRenderer
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppSender
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppQrAttachmentValidator
import com.ganesh.hisabkitabpro.domain.profile.ProfileMapFooter
import com.ganesh.hisabkitabpro.feature.sharedkhata.SharedKhataAccessManager
import com.ganesh.hisabkitabpro.feature.sharedkhata.SharedKhataFeatureToggle
import com.ganesh.hisabkitabpro.feature.sharedkhata.SharedKhataReadOnlyLinkGenerator
import com.ganesh.hisabkitabpro.network.api.SharedKhataLinePayload
import com.ganesh.hisabkitabpro.network.api.SharedKhataPublishRequestDto
import com.ganesh.hisabkitabpro.ui.payment.FintechUpiPaymentCard
import com.ganesh.hisabkitabpro.core.security.PrivacySecureEffect
import com.ganesh.hisabkitabpro.ui.viewmodel.CustomerViewModel
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import com.ganesh.hisabkitabpro.util.safeClickable
import com.ganesh.hisabkitabpro.util.WhatsAppBillSender
import com.ganesh.hisabkitabpro.addon.finance.InterestCalculator
import com.ganesh.hisabkitabpro.addon.settlement.SettlementKind
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerLedgerScreen(
    customer: Customer,
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    onBillClick: () -> Unit,
    onProfileClick: () -> Unit,
    onReminderHistoryClick: () -> Unit,
    onReminderControlClick: () -> Unit,
    onBulkRemindersClick: () -> Unit = {},
    onAddEntry: (TransactionType) -> Unit
) {
    PrivacySecureEffect()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val ledgerViewModel: CustomerLedgerViewModel = hiltViewModel()
    val customerViewModel: CustomerViewModel = hiltViewModel()
    val prefs = remember(context.applicationContext) {
        context.applicationContext.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
    }
    val sharedKhataAccessManager = remember(prefs) { SharedKhataAccessManager(prefs) }

    // âœ… CRITICAL FIX: Remember the paging flow to prevent infinite recomposition loop
    val pagedTransactions = remember(customer.id) { 
        transactionViewModel.getTransactionsByCustomerPaged(customer.id) 
    }.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val loadErrorMessage = stringResource(R.string.common_load_error)
    val liveCustomer by customerViewModel.getCustomerByIdFlow(customer.id)
        .collectAsStateWithLifecycle(initialValue = customer)
    val netBalancePaise = liveCustomer?.balanceCache ?: customer.balanceCache
    val businessProfile by ledgerViewModel.businessProfile.collectAsStateWithLifecycle()
    var nextAutoReminderAt by remember(customer.id) {
        mutableStateOf(
            ReminderAutomationPrefs.getCustomerPauseUntil(context, customer.id).takeIf { it > System.currentTimeMillis() }
                ?: (System.currentTimeMillis() + 12L * 60L * 60L * 1000L)
        )
    }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val sdfDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showFeatureHub by remember { mutableStateOf(false) }
    var showUpiPaymentSheet by remember { mutableStateOf(false) }
    var interestDialog by remember { mutableStateOf(false) }
    var statementMenuExpanded by remember { mutableStateOf(false) }
    var sharedKhataDialog by remember { mutableStateOf<Triple<String, String, Long>?>(null) }
    var sharedKhataPublishing by remember { mutableStateOf(false) }

    fun publishSharedKhataSnapshot() {
        if (!SharedKhataFeatureToggle(prefs).isEnabled()) return
        if (sharedKhataPublishing) return
        if (sharedKhataAccessManager.isRevoked()) {
            Toast.makeText(context, "Online khata sharing is revoked. Enable it again from settings flow.", Toast.LENGTH_LONG).show()
            return
        }
        if (!sharedKhataAccessManager.canPublishNow()) {
            val seconds = (sharedKhataAccessManager.remainingCooldownMs() / 1000L).coerceAtLeast(1L)
            Toast.makeText(context, "Please wait ${seconds}s before publishing again.", Toast.LENGTH_LONG).show()
            return
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val online = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val active = cm?.activeNetwork
            if (active == null) {
                false
            } else {
                val cap = cm.getNetworkCapabilities(active)
                cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }
        } else {
            @Suppress("DEPRECATION")
            cm?.activeNetworkInfo?.isConnected == true
        }
        if (!online) {
            Toast.makeText(context, "Internet required to publish online khata.", Toast.LENGTH_LONG).show()
            return
        }
        sharedKhataPublishing = true
        Toast.makeText(context, context.getString(R.string.customer_share_online_khata_publishing), Toast.LENGTH_SHORT).show()
        scope.launch(Dispatchers.IO) {
            try {
                val snapshot = transactionViewModel.getCustomerTransactionsSnapshot(customer.id)
                val lines = snapshot
                    .sortedByDescending { it.createdAt }
                    .map { tx ->
                        SharedKhataLinePayload(
                            amountPaise = tx.amount,
                            type = tx.type.name,
                            note = tx.note?.take(500),
                            createdAt = tx.createdAt
                        )
                    }
                val req = SharedKhataPublishRequestDto(
                    customerName = customer.name.trim().take(200).ifBlank { "Customer" },
                    customerLocalId = customer.id.toString(),
                    balancePaise = netBalancePaise,
                    lines = lines,
                    ttlHours = 24,
                    merchantId = CloudBusinessIdentity.ensureBusinessId(prefs),
                    businessId = CloudBusinessIdentity.ensureBusinessId(prefs),
                    createdByUserId = FirebaseAuth.getInstance().currentUser?.uid
                )
                val published = SharedKhataReadOnlyLinkGenerator.publishSnapshot(req)
                withContext(Dispatchers.Main) {
                    sharedKhataPublishing = false
                    published.fold(
                        onSuccess = { link ->
                            sharedKhataAccessManager.onPublished(expiresAt = link.expiresAt)
                            sharedKhataDialog = Triple(link.otp, link.viewUrl, link.expiresAt)
                        },
                        onFailure = { e ->
                            Toast.makeText(
                                context,
                                e.message?.ifBlank { null }
                                    ?: context.getString(R.string.customer_share_online_khata_failed),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    sharedKhataPublishing = false
                    Toast.makeText(context, context.getString(R.string.customer_share_online_khata_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun runAdaptiveReminder() {
        scope.launch {
            if (netBalancePaise <= 0L) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, ReminderLocalization.noDueBalanceText(context), Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val digits = customer.phone.filter { it.isDigit() }
            if (digits.isBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, ReminderLocalization.phoneUnavailableText(context), Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // STEP 1-3: fetch latest business profile + active QR config, then validate integrity.
            val latestBusinessProfile = ledgerViewModel.loadBusinessProfileOnce()

            val latestUpi = latestBusinessProfile?.upiId
            val businessTitle = latestBusinessProfile?.businessName?.trim()?.ifBlank { null }
                ?: "GOLDEN RAJWADI CHAI"

            val payLink = buildUpiPayLink(
                upiId = latestUpi,
                amountPaise = netBalancePaise,
                customerName = customer.name,
                payeeDisplayName = businessTitle
            )

            val qrFile = latestBusinessProfile
                ?.qrImagePath
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.exists() }

            val qrReason = WhatsAppQrAttachmentValidator.validateQrImageFileOrReason(qrFile)
            val qrOk = qrReason == null

            // QR validation failure: block WhatsApp image attachment path, but allow text fallback.
            if (!qrOk) {
                android.util.Log.w(
                    "BusinessCardEngine",
                    "WhatsApp reminder QR invalid: customerId=${customer.id}, qrReason=$qrReason"
                )
                Toast.makeText(
                    context,
                    "QR invalid: $qrReason (sending text fallback)",
                    Toast.LENGTH_LONG
                ).show()
            }

            val oldestTxnAt = liveCustomer?.lastTransactionDate
                ?: System.currentTimeMillis()
            val daysOverdue = ((System.currentTimeMillis() - oldestTxnAt) / (24L * 60L * 60L * 1000L)).toInt()
                .coerceAtLeast(0)
            val previousAttempts = ReminderAutomationPrefs.getReminderAttempts(context, customer.id)
            val preferredChannel = ReminderAutomationPrefs.getPreferredChannel(context, customer.id)
            val plan = ReminderBehaviorEngine.selectPlan(
                daysOverdue = daysOverdue,
                netDuePaise = netBalancePaise,
                previousAttempts = previousAttempts,
                preferredChannel = preferredChannel
            )

            val message = when (plan.tone) {
                AutoReminderTone.POLITE -> buildMasterReminderMessage(
                    context = context,
                    businessName = businessTitle,
                    customerName = customer.name,
                    netBalancePaise = netBalancePaise,
                    payLink = payLink,
                    tone = ReminderTone.POLITE
                )
                AutoReminderTone.PROFESSIONAL -> buildMasterReminderMessage(
                    context = context,
                    businessName = businessTitle,
                    customerName = customer.name,
                    netBalancePaise = netBalancePaise,
                    payLink = payLink,
                    tone = ReminderTone.PROFESSIONAL
                )
                AutoReminderTone.STRICT -> buildMasterReminderMessage(
                    context = context,
                    businessName = businessTitle,
                    customerName = customer.name,
                    netBalancePaise = netBalancePaise,
                    payLink = payLink,
                    tone = ReminderTone.STRICT
                )
                AutoReminderTone.PARTIAL_OFFER -> buildPartialOfferReminderMessage(
                    context = context,
                    businessName = businessTitle,
                    customerName = customer.name,
                    netBalancePaise = netBalancePaise,
                    payLink = payLink,
                    upfrontPercent = 25
                )
            }

            // Footer uses the latest profile (single source of truth).
            val messageWithProfileFooter = ProfileMapFooter.mapFooter(latestBusinessProfile)
                ?.let { "$message\n\n$it" }
                ?: message

            // STEP 4: generate professional QR card dynamically from the same Business Profile QR file.
            val waImageFile = withContext(Dispatchers.IO) {
                if (!qrOk || qrFile == null) return@withContext null
                WhatsAppPaymentShowcaseRenderer.renderToCacheFileOrNull(
                    context = context,
                    profile = latestBusinessProfile,
                    customerName = customer.name,
                    amountPaise = netBalancePaise,
                    qrImageFile = qrFile
                )
            }?.takeIf { it.exists() && it.length() > 1024L }

            val waFinalAttachmentFile = (waImageFile ?: (if (qrOk) qrFile else null))
                ?.takeIf { it.exists() && it.length() > 1024L }

            // STEP 5-6: attach only if we have a verified professional QR image.
            withContext(Dispatchers.Main) {
                val finalChannel = when (plan.channel) {
                    AutoReminderChannel.WHATSAPP -> {
                        val waOpened = if (waFinalAttachmentFile != null) {
                            WhatsAppSender.sendReminderWithImage(context, digits, messageWithProfileFooter, waFinalAttachmentFile)
                        } else {
                            WhatsAppSender.sendTextReminder(context, digits, messageWithProfileFooter)
                        }
                        if (waOpened) AutoReminderChannel.WHATSAPP else {
                            val smsOpened = SmsPaymentReminder.openLedgerReminder(
                                context = context,
                                customerName = customer.name,
                                rawPhone = customer.phone,
                                netDuePaise = netBalancePaise,
                                businessName = businessTitle,
                                upiId = latestUpi,
                                currencyFormatter = currencyFormatter
                            )
                            if (smsOpened) AutoReminderChannel.SMS else null
                        }
                    }
                    AutoReminderChannel.SMS -> {
                        val smsOpened = SmsPaymentReminder.openLedgerReminder(
                            context = context,
                            customerName = customer.name,
                            rawPhone = customer.phone,
                            netDuePaise = netBalancePaise,
                            businessName = businessTitle,
                            upiId = latestUpi,
                            currencyFormatter = currencyFormatter
                        )
                        if (smsOpened) {
                            AutoReminderChannel.SMS
                        } else {
                            val waOpened = if (waFinalAttachmentFile != null) {
                                WhatsAppSender.sendReminderWithImage(context, digits, messageWithProfileFooter, waFinalAttachmentFile)
                            } else {
                                WhatsAppSender.sendTextReminder(context, digits, messageWithProfileFooter)
                            }
                            if (waOpened) AutoReminderChannel.WHATSAPP else null
                        }
                    }
                }

                if (finalChannel == null) {
                    Toast.makeText(context, ReminderLocalization.channelUnavailableText(context), Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                ReminderAutomationPrefs.markManualReminderSent(context, customer.id, transactionId = 0L)
                ReminderAutomationPrefs.setManualPauseForCustomer(context, customer.id, days = 7)
                ReminderAutomationPrefs.incrementReminderAttempts(context, customer.id)
                ReminderAutomationPrefs.setPreferredChannel(context, customer.id, finalChannel)
                nextAutoReminderAt = ReminderAutomationPrefs.getCustomerPauseUntil(context, customer.id)

                ledgerViewModel.insertAuditLog(
                    AuditLogEntry(
                        entityType = "REMINDER",
                        entityId = customer.id,
                        action = "AUTO_PLAN_${plan.tone}_${finalChannel.name}",
                        detail = "customerId=${customer.id},daysOverdue=$daysOverdue,attempts=$previousAttempts,netDue=$netBalancePaise,payLink=${payLink ?: "NA"},qrValid=$qrOk"
                    ),
                )

                Toast.makeText(
                    context,
                    if (finalChannel == AutoReminderChannel.WHATSAPP) ReminderLocalization.whatsappOpenedText(context)
                    else ReminderLocalization.smsOpenedText(context),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    LaunchedEffect(pagedTransactions.loadState.append) {
        if (pagedTransactions.loadState.append is LoadState.Error) {
            snackbarHostState.showSnackbar(loadErrorMessage)
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111111),
                    navigationIconContentColor = Color(0xFF212121),
                    actionIconContentColor = Color(0xFF424242)
                ),
                title = {
                    Column {
                        Text(
                            customer.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111111)
                        )
                        Text(
                            "View Profile", 
                            fontSize = 11.sp, 
                            color = Color(0xFF43A047), 
                            modifier = Modifier.safeClickable { onProfileClick() }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    Box {
                        IconButton(onClick = { statementMenuExpanded = true }) {
                            Icon(Icons.Default.Add, null, tint = Color.DarkGray)
                        }
                        DropdownMenu(
                            expanded = statementMenuExpanded,
                            onDismissRequest = { statementMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.customer_statement)) },
                                onClick = {
                                    statementMenuExpanded = false
                                    onBillClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                                }
                            )
                            if (SharedKhataFeatureToggle(prefs).isEnabled()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.customer_share_online_khata)) },
                                    onClick = {
                                        statementMenuExpanded = false
                                        publishSharedKhataSnapshot()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Link, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { 
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))) 
                        } catch (e: Exception) {}
                    }) { 
                        Icon(Icons.Default.Call, null, tint = Color.DarkGray) 
                    }
                    IconButton(onClick = { showFeatureHub = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.DarkGray)
                    }
                }
            )
        },
        bottomBar = {
            UltraProCompactPanel(
                customer = customer,
                formatter = currencyFormatter,
                netBalancePaise = netBalancePaise,
                nextAutoReminderAt = nextAutoReminderAt,
                onAddEntry = onAddEntry,
                onOpenStatement = onBillClick,
                onOpenProfile = onProfileClick,
                onOpenReminderHistory = onReminderHistoryClick,
                onOpenReminderControl = onReminderControlClick,
                onRemindNow = ::runAdaptiveReminder,
                onPayUpi = {
                    val upi = businessProfile?.upiId?.trim().orEmpty()
                    when {
                        upi.isBlank() -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.customer_upi_missing),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        netBalancePaise <= 0L -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.customer_upi_no_due),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            showUpiPaymentSheet = true
                        }
                    }
                },
                onChat = {
                    val bizName = businessProfile?.businessName?.trim()?.ifBlank { null }
                    val opened = SmsPaymentReminder.openLedgerReminder(
                        context = context,
                        customerName = customer.name,
                        rawPhone = customer.phone,
                        netDuePaise = netBalancePaise,
                        businessName = bizName,
                        upiId = businessProfile?.upiId,
                        currencyFormatter = currencyFormatter
                    )
                    if (opened) {
                        ledgerViewModel.insertAuditLog(
                            AuditLogEntry(
                                entityType = "REMINDER",
                                entityId = customer.id,
                                action = "SMS_REMINDER_COMPOSE",
                                detail = "customerId=${customer.id},netDue=$netBalancePaise"
                            ),
                        )
                        Toast.makeText(context, context.getString(R.string.customer_sms_ready), Toast.LENGTH_SHORT).show()
                    }
                },
                onCall = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}")))
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.common_unable_open_dialer), Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    try {
                        val shareMessage = buildString {
                            append("Customer: ${customer.name}\n")
                            append("Balance Due: ${currencyFormatter.format(customer.balanceCache / 100.0)}\n")
                            if (customer.phone.isNotBlank()) {
                                append("Phone: ${customer.phone}\n")
                            }
                            append("Shared from HisabKitab Pro")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.customer_ledger_summary_subject, customer.name))
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        val chooser = Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.customer_share_summary_chooser)
                        )
                        WhatsAppBillSender.startShareActivity(context, chooser)
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.common_unable_share_now), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
                reverseLayout = true,
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(
                    count = pagedTransactions.itemCount,
                    key = pagedTransactions.itemKey { it.id }
                ) { index ->
                    val tx = pagedTransactions[index] ?: return@items

                    val showDate = if (index == pagedTransactions.itemCount - 1) true
                    else {
                        val nextTx = pagedTransactions[index + 1]
                        nextTx != null && sdfDate.format(Date(tx.createdAt)) != sdfDate.format(Date(nextTx.createdAt))
                    }

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        if (showDate) DateHeader(sdfDate.format(Date(tx.createdAt)))

                        BillStyleTransactionBubble(
                            tx = tx,
                            formatter = currencyFormatter,
                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDeleteRequest = { pendingDelete = tx },
                            onSettlementKind = { kind ->
                                transactionViewModel.setSettlementKind(tx.id, kind)
                                pagedTransactions.refresh()
                            }
                        )
                    }
                }

                when (pagedTransactions.loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            TextButton(
                                onClick = { pagedTransactions.retry() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                    }
                    else -> Unit
                }
            }

            PagingListStateOverlay(
                itemCount = pagedTransactions.itemCount,
                refreshLoadState = pagedTransactions.loadState.refresh,
                onRetry = { pagedTransactions.retry() },
                emptyMessage = stringResource(R.string.ledger_empty_transactions),
            )

            pendingDelete?.let { tx ->
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text(stringResource(R.string.customer_delete_entry_title), fontWeight = FontWeight.SemiBold) },
                    text = { Text(stringResource(R.string.customer_delete_entry_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                transactionViewModel.deleteTransaction(customer.id, tx.id)
                                pendingDelete = null
                                pagedTransactions.refresh()
                                Toast.makeText(context, context.getString(R.string.customer_entry_deleted), Toast.LENGTH_SHORT).show()
                            }
                        ) { Text(stringResource(R.string.common_delete), color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
                    }
                )
            }

            sharedKhataDialog?.let { (otp, viewUrl, expiresAt) ->
                AlertDialog(
                    onDismissRequest = { sharedKhataDialog = null },
                    title = {
                        Text(
                            stringResource(R.string.customer_shared_khata_dialog_title),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(R.string.customer_shared_khata_dialog_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "OTP: $otp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                viewUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val expiryDate = remember(expiresAt) { Date(expiresAt) }
                            Text(
                                "Valid till: ${sdfDate.format(expiryDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("khata", viewUrl))
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.customer_shared_khata_link_copied),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) { Text(stringResource(R.string.customer_shared_khata_copy_link)) }
                                TextButton(
                                    onClick = {
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_SUBJECT,
                                                context.getString(R.string.customer_ledger_summary_subject, customer.name)
                                            )
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                context.getString(
                                                    R.string.customer_shared_khata_share_message,
                                                    businessProfile?.businessName?.trim()?.ifBlank { null } ?: "our business",
                                                    viewUrl,
                                                    otp
                                                )
                                            )
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                send,
                                                context.getString(R.string.customer_shared_khata_share_via)
                                            )
                                        )
                                    }
                                ) { Text(stringResource(R.string.customer_shared_khata_share_via)) }
                                TextButton(
                                    onClick = {
                                        sharedKhataAccessManager.revoke()
                                        sharedKhataDialog = null
                                        Toast.makeText(
                                            context,
                                            "Online khata access revoked on this device.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) { Text("Revoke") }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { sharedKhataDialog = null }) {
                            Text(stringResource(R.string.common_ok))
                        }
                    }
                )
            }

            if (interestDialog) {
                var interestRate by remember { mutableFloatStateOf(18f) }
                var interestDays by remember { mutableIntStateOf(30) }
                val interestPaise = InterestCalculator.flatAnnualInterest(
                    customer.balanceCache,
                    interestRate.toDouble(),
                    interestDays
                )
                AlertDialog(
                    onDismissRequest = { interestDialog = false },
                    title = { Text(stringResource(R.string.customer_interest_overlay_title), fontWeight = FontWeight.SemiBold) },
                    text = {
                        val scroll = rememberScrollState()
                        Column(Modifier.verticalScroll(scroll)) {
                            Text(
                                stringResource(R.string.customer_interest_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.customer_total_balance_due),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                currencyFormatter.format(customer.balanceCache / 100.0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.customer_interest_rate_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Slider(
                                value = interestRate,
                                onValueChange = { interestRate = it },
                                valueRange = 1f..36f,
                                steps = 34
                            )
                            Text(
                                "${String.format(Locale.getDefault(), "%.1f", interestRate)}% " +
                                    stringResource(R.string.customer_interest_p_a_suffix),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.customer_interest_days_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Slider(
                                value = interestDays.toFloat(),
                                onValueChange = { interestDays = it.toInt().coerceIn(1, 365) },
                                valueRange = 1f..365f
                            )
                            Text(
                                stringResource(R.string.customer_interest_days_value, interestDays),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        stringResource(R.string.customer_interest_accrued_label),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        currencyFormatter.format(interestPaise / 100.0),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    val principalStr = currencyFormatter.format(customer.balanceCache / 100.0)
                                    val interestStr = currencyFormatter.format(interestPaise / 100.0)
                                    val rateStr = String.format(Locale.US, "%.2f%%", interestRate)
                                    val body = context.getString(
                                        R.string.interest_estimate_share_text,
                                        principalStr,
                                        rateStr,
                                        interestDays,
                                        interestStr
                                    )
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.customer_interest_overlay_title))
                                        putExtra(Intent.EXTRA_TEXT, body)
                                    }
                                    runCatching {
                                        context.startActivity(
                                            Intent.createChooser(send, context.getString(R.string.interest_share))
                                        )
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.common_unable_share_now),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) { Text(stringResource(R.string.interest_share)) }
                            TextButton(onClick = { interestDialog = false }) {
                                Text(stringResource(R.string.common_ok))
                            }
                        }
                    }
                )
            }

            if (showUpiPaymentSheet) {
                val prof = businessProfile
                if (prof != null && netBalancePaise > 0L && prof.upiId.trim().isNotEmpty()) {
                    val payNote = remember(customer.name) { "Dues ${customer.name}".take(80) }
                    ModalBottomSheet(
                        onDismissRequest = { showUpiPaymentSheet = false },
                        containerColor = Color(0xFFF5F5F5),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .navigationBarsPadding(),
                        ) {
                            Text(
                                stringResource(R.string.customer_upi_review_payment),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                            FintechUpiPaymentCard(
                                businessName = prof.businessName,
                                ownerName = prof.ownerName,
                                logoPath = prof.logoUrl,
                                qrImagePath = prof.qrImagePath,
                                upiId = prof.upiId,
                                amountPaise = netBalancePaise,
                                balanceContextLabel = context.getString(R.string.customer_upi_due_from, customer.name),
                                paymentNoteForQr = payNote,
                                mediaStatus = null,
                                onDismissMediaStatus = null,
                                showPaymentToolbar = false,
                                showUpiEditor = false,
                            )
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 16.dp),
                                onClick = {
                                    val bizTitle = prof.businessName.trim().ifBlank { "Business" }
                                    val amountText = String.format(Locale.US, "%.2f", netBalancePaise / 100.0)
                                    val txnRef = "HK${customer.id}_${System.currentTimeMillis()}"
                                    val uri = UpiIntentBuilder.buildPayUri(
                                        payeeVpa = prof.upiId.trim(),
                                        payeeName = bizTitle,
                                        amountRupee = amountText,
                                        transactionNote = payNote,
                                        txnRef = txnRef,
                                    )
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.customer_upi_none_installed),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                    showUpiPaymentSheet = false
                                },
                            ) {
                                Text(stringResource(R.string.customer_upi_pay_in_app))
                            }
                        }
                    }
                }
            }

            if (showFeatureHub) {
                val features = listOf(
                    FeatureHubItemSpec("Statement", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF8D6E63)) {
                        showFeatureHub = false
                        onBillClick()
                    },
                    FeatureHubItemSpec("SMS", Icons.Default.Chat, Color(0xFF1E88E5)) {
                        showFeatureHub = false
                        val bizName = businessProfile?.businessName?.trim()?.ifBlank { null }
                        SmsPaymentReminder.openLedgerReminder(
                            context = context,
                            customerName = customer.name,
                            rawPhone = customer.phone,
                            netDuePaise = netBalancePaise,
                            businessName = bizName,
                            upiId = businessProfile?.upiId,
                            currencyFormatter = currencyFormatter
                        )
                    },
                    FeatureHubItemSpec("Call", Icons.Default.Call, Color(0xFF43A047)) {
                        showFeatureHub = false
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}")))
                        } catch (_: Exception) {
                        }
                    },
                    FeatureHubItemSpec("Share", Icons.Default.Share, Color(0xFF8D6E63)) {
                        showFeatureHub = false
                        val shareMessage = buildString {
                            append("Customer: ${customer.name}\n")
                            append("Balance Due: ${currencyFormatter.format(customer.balanceCache / 100.0)}\n")
                            if (customer.phone.isNotBlank()) {
                                append("Phone: ${customer.phone}\n")
                            }
                            append("Shared from HisabKitab Pro")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.customer_ledger_summary_subject, customer.name))
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        val chooser = Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.customer_share_summary_chooser)
                        )
                        WhatsAppBillSender.startShareActivity(context, chooser)
                    },
                    FeatureHubItemSpec("Remind Now", Icons.Default.NotificationsActive, Color(0xFF8D6E63)) {
                        showFeatureHub = false
                        runAdaptiveReminder()
                    },
                    FeatureHubItemSpec("History", Icons.Default.History, Color(0xFF7E57C2)) {
                        showFeatureHub = false
                        onReminderHistoryClick()
                    },
                    FeatureHubItemSpec("Auto Control", Icons.Default.Settings, Color(0xFF607D8B)) {
                        showFeatureHub = false
                        onReminderControlClick()
                    },
                    FeatureHubItemSpec("Bulk", Icons.Default.AlarmOn, Color(0xFFE65100)) {
                        showFeatureHub = false
                        onBulkRemindersClick()
                    },
                    FeatureHubItemSpec("Interest", Icons.Default.Percent, Color(0xFF00897B)) {
                        showFeatureHub = false
                        interestDialog = true
                    },
                    FeatureHubItemSpec("Received", Icons.Default.ArrowDownward, Color(0xFF2E7D32)) {
                        showFeatureHub = false
                        onAddEntry(TransactionType.DEBIT)
                    },
                    FeatureHubItemSpec("Given", Icons.Default.ArrowUpward, Color(0xFFD32F2F)) {
                        showFeatureHub = false
                        onAddEntry(TransactionType.CREDIT)
                    }
                )
                FeatureHubSheet(
                    features = features,
                    onDismiss = { showFeatureHub = false }
                )
            }

        }
    }
}

private fun buildMasterReminderMessage(
    context: Context,
    businessName: String,
    customerName: String,
    netBalancePaise: Long,
    payLink: String?,
    tone: ReminderTone = ReminderTone.PROFESSIONAL
): String {
    val lc = AppLocaleManager.wrapContext(context)
    val amount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(netBalancePaise / 100.0)
    val bodyRes = when (tone) {
        ReminderTone.POLITE -> R.string.reminder_master_polite
        ReminderTone.PROFESSIONAL -> R.string.reminder_master_professional
        ReminderTone.STRICT -> R.string.reminder_master_strict
    }
    val body = lc.getString(bodyRes, customerName, amount)
    val linkLine = payLink?.let { "\n" + lc.getString(R.string.reminder_pay_line, it) } ?: ""
    return buildString {
        append("*").append(businessName).append("*\n")
        append(lc.getString(R.string.reminder_branding_powered)).append("\n\n")
        append(body).append("\n")
        append(lc.getString(R.string.reminder_qr_footer))
        append(linkLine)
    }
}

private fun buildPartialOfferReminderMessage(
    context: Context,
    businessName: String,
    customerName: String,
    netBalancePaise: Long,
    payLink: String?,
    upfrontPercent: Int
): String {
    val lc = AppLocaleManager.wrapContext(context)
    val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val totalAmount = nf.format(netBalancePaise / 100.0)
    val upfrontPaise = (netBalancePaise * upfrontPercent / 100.0).toLong()
    val upfrontAmount = nf.format(upfrontPaise / 100.0)
    val linkLine = payLink?.let { "\n" + lc.getString(R.string.reminder_pay_line, it) } ?: ""
    val body = lc.getString(R.string.reminder_partial_body, customerName, totalAmount, upfrontAmount)
    return buildString {
        append("*").append(businessName).append("*\n")
        append(lc.getString(R.string.reminder_branding_powered)).append("\n\n")
        append(body)
        append(linkLine)
    }
}

private enum class ReminderTone {
    POLITE,
    PROFESSIONAL,
    STRICT
}

private data class FeatureHubItemSpec(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureHubSheet(
    features: List<FeatureHubItemSpec>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F5F5)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "All Features",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF37474F)
            )
            Spacer(Modifier.height(12.dp))
            features.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .safeClickable { item.onClick() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = item.tint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    item.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun buildUpiPayLink(
    upiId: String?,
    amountPaise: Long,
    customerName: String,
    payeeDisplayName: String
): String? {
    if (upiId.isNullOrBlank()) return null
    val amountText = String.format(Locale.US, "%.2f", amountPaise / 100.0)
    val note = "Payment from $customerName - HisabKitab".take(80)
    val txnRef = "HKR_${System.currentTimeMillis()}"
    return UpiIntentBuilder.buildPayUri(
        payeeVpa = upiId,
        payeeName = payeeDisplayName.ifBlank { "Business" },
        amountRupee = amountText,
        transactionNote = note,
        txnRef = txnRef
    ).toString()
}
