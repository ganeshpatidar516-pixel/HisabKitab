@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.settings

import android.content.Context
import android.app.Activity
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.reminder.ReminderAutomationPrefs
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.core.locale.IndianLanguageCatalog
import com.ganesh.hisabkitabpro.domain.support.AppShareActions
import com.ganesh.hisabkitabpro.ui.auth.AccountSettingsSection
import com.ganesh.hisabkitabpro.domain.sync.SyncHealthMonitor
import com.ganesh.hisabkitabpro.ui.viewmodel.SettingsViewModel
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import com.ganesh.hisabkitabpro.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings — enterprise information architecture (presentation layer).
 * All toggles and navigation preserve existing [SettingsViewModel] / auth behaviour.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    showBackNavigation: Boolean = true,
    onNavigateToBusinessProfile: () -> Unit,
    onNavigateToVisitingCard: () -> Unit,
    onNavigateToBillingSettings: () -> Unit = {},
    onNavigateToCloudSettings: () -> Unit = {},
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToPrivacyAndData: () -> Unit = {},
    onNavigateToSecurityPin: () -> Unit,
    onNavigateToStaffList: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    viewModel: SettingsViewModel,
    transactionViewModel: TransactionViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val sharedKhataEnabled by viewModel.sharedKhataEnabled.collectAsState()
    val bankAutoSettleEnabled by viewModel.bankAutoSettleEnabled.collectAsState()
    val superCommandEnabled by viewModel.superCommandEnabled.collectAsState()
    val screenPrivacySecure by viewModel.screenPrivacySecure.collectAsState()
    val crashReportingEnabled by viewModel.crashReportingEnabled.collectAsState()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var autoPilotEnabled by remember { mutableStateOf(ReminderAutomationPrefs.isAutoPilotEnabled(context)) }
    var showBankAccessDisclosureDialog by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    fun activateBankAutoSettleAfterProminentConsent() {
        if (!viewModel.setBankAutoSettleEnabled(true)) {
            scope.launch {
                snackbarHostState.showSnackbar("Could not update Bank Match setting. Try again.")
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!isNotificationListenerEnabled(context)) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Enable Notification Access for bank hints to work."
                )
            }
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Allow app notifications to receive bank payment hints."
                )
            }
        }
        performHapticFeedback(context)
    }

    Scaffold(
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    if (showBackNavigation) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.settings_back),
                                tint = colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.primary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                EnterpriseSectionHeader(stringResource(R.string.settings_enterprise_section_business))
                SettingsItem(
                    title = stringResource(R.string.settings_business_profile_title),
                    description = stringResource(R.string.settings_enterprise_sub_business_profile),
                    icon = Icons.Default.Business,
                    onClick = onNavigateToBusinessProfile
                )
                SettingsItem(
                    title = stringResource(R.string.settings_enterprise_visiting_cards),
                    description = stringResource(R.string.settings_enterprise_sub_visiting_cards),
                    icon = Icons.Default.Badge,
                    onClick = onNavigateToVisitingCard
                )
            }

            item {
                EnterpriseSectionHeader(stringResource(R.string.settings_enterprise_section_account_security))
                SettingsItem(
                    title = stringResource(R.string.settings_enterprise_lock_title),
                    description = stringResource(R.string.settings_enterprise_sub_app_lock),
                    icon = Icons.Default.Lock,
                    onClick = onNavigateToSecurityPin
                )
                AccountSettingsSection(showSectionHeader = false)
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.settings_enterprise_section_billing),
                    description = stringResource(R.string.settings_hub_billing_sub),
                    icon = Icons.Default.ReceiptLong,
                    onClick = onNavigateToBillingSettings
                )
                SettingsItem(
                    title = stringResource(R.string.settings_enterprise_section_cloud),
                    description = stringResource(R.string.settings_hub_cloud_sub),
                    icon = Icons.Default.Cloud,
                    onClick = onNavigateToCloudSettings
                )
            }

            item {
                EnterpriseSectionHeader(stringResource(R.string.settings_enterprise_section_operations))
                SettingsToggleItem(
                    title = stringResource(R.string.settings_shared_khata_beta_title),
                    description = stringResource(R.string.settings_enterprise_sub_shared_khata),
                    icon = Icons.Default.Link,
                    isSelected = sharedKhataEnabled,
                    onCheckedChange = { enabled ->
                        if (!viewModel.setSharedKhataEnabled(enabled)) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Could not update Shared Khata setting. Try again.")
                            }
                        }
                        performHapticFeedback(context)
                    }
                )
                SettingsItem(
                    title = stringResource(R.string.settings_staff_title),
                    description = stringResource(R.string.settings_enterprise_sub_staff),
                    icon = Icons.Default.People,
                    onClick = onNavigateToStaffList
                )
                SettingsItem(
                    title = stringResource(R.string.settings_inventory_title),
                    description = stringResource(R.string.settings_enterprise_sub_inventory),
                    icon = Icons.Default.List,
                    onClick = onNavigateToInventory,
                    testTag = "sacred_settings_inventory"
                )
            }

            item {
                EnterpriseSectionHeader(stringResource(R.string.settings_enterprise_section_appearance))
                ThemeSelector(
                    currentTheme = normalizeDeprecatedThemeId(settings?.theme),
                    onThemeSelected = { newTheme ->
                        scope.launch {
                            performHapticFeedback(context)
                            viewModel.updateTheme(newTheme)
                        }
                    }
                )
                LanguageSelector(
                    currentLanguage = settings?.languageCode ?: AppLocaleManager.getSavedLanguageCode(context),
                    onLanguageSelected = { code ->
                        scope.launch {
                            val normalized = AppLocaleManager.normalizeLanguageCode(code)
                            if (!AppLocaleManager.saveLanguageCode(context, normalized)) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Could not save language. Try again.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                            AppLocaleManager.updateLocaleNow(context, normalized)
                            viewModel.updateLanguage(normalized)
                            performHapticFeedback(context)
                            (context as? Activity)?.recreate()
                        }
                    }
                )
            }

            item {
                EnterpriseSectionHeader(stringResource(R.string.settings_enterprise_section_ai))
                SettingsToggleItem(
                    title = stringResource(R.string.settings_auto_pilot_title),
                    description = stringResource(R.string.settings_enterprise_sub_autopilot),
                    icon = Icons.Default.AutoMode,
                    isSelected = autoPilotEnabled,
                    onCheckedChange = { enabled ->
                        autoPilotEnabled = enabled
                        ReminderAutomationPrefs.setAutoPilotEnabled(context, enabled)
                        performHapticFeedback(context)
                    }
                )
                SettingsToggleItem(
                    title = stringResource(R.string.settings_enterprise_super_ai_title),
                    description = stringResource(R.string.settings_enterprise_super_ai_sub),
                    icon = Icons.Default.AutoAwesome,
                    isSelected = superCommandEnabled,
                    onCheckedChange = { enabled ->
                        if (!viewModel.setSuperCommandEnabled(enabled)) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Could not update Super AI Assistant setting. Try again.")
                            }
                        }
                        performHapticFeedback(context)
                    }
                )
            }

            item {
                EnterpriseSectionHeader(stringResource(R.string.settings_enterprise_section_privacy))
                SettingsToggleItem(
                    title = stringResource(R.string.settings_privacy_screen_secure_title),
                    description = stringResource(R.string.settings_privacy_screen_secure_sub),
                    icon = Icons.Default.VisibilityOff,
                    isSelected = screenPrivacySecure,
                    onCheckedChange = { enabled ->
                        viewModel.setScreenPrivacySecureEnabled(enabled)
                        performHapticFeedback(context)
                    },
                )
                SettingsItem(
                    title = stringResource(R.string.settings_privacy_data_title),
                    description = stringResource(R.string.settings_enterprise_sub_privacy_data),
                    icon = Icons.Default.Shield,
                    onClick = onNavigateToPrivacyAndData,
                    testTag = "hk_settings_privacy_data"
                )
                SettingsItem(
                    title = stringResource(R.string.settings_enterprise_privacy_policy),
                    description = stringResource(R.string.settings_enterprise_sub_privacy_policy),
                    icon = Icons.Default.Policy,
                    onClick = onNavigateToPrivacyPolicy
                )
            }

            item {
                EnterpriseSectionHeader(stringResource(R.string.settings_enterprise_section_help))
                SettingsItem(
                    title = stringResource(R.string.settings_enterprise_help_center_title),
                    description = stringResource(R.string.settings_enterprise_sub_help),
                    icon = Icons.Default.SupportAgent,
                    onClick = onNavigateToHelpSupport
                )
                SettingsItem(
                    title = stringResource(R.string.settings_share_app_title),
                    description = stringResource(R.string.settings_enterprise_sub_share),
                    icon = Icons.Default.Share,
                    onClick = { AppShareActions.shareApp(context) }
                )
            }

            item {
                var advancedExpanded by remember { mutableStateOf(false) }
                EnterpriseSectionHeader(stringResource(R.string.settings_enterprise_section_advanced))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { advancedExpanded = !advancedExpanded },
                    color = colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (advancedExpanded) {
                                stringResource(R.string.settings_enterprise_advanced_hide)
                            } else {
                                stringResource(R.string.settings_enterprise_advanced_show)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (advancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = colorScheme.primary
                        )
                    }
                }
                if (advancedExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_bank_auto_settle_title),
                        description = stringResource(R.string.settings_enterprise_sub_bank_match),
                        icon = Icons.Default.CreditCard,
                        isSelected = bankAutoSettleEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !bankAutoSettleEnabled) {
                                showBankAccessDisclosureDialog = true
                                return@SettingsToggleItem
                            }
                            if (!viewModel.setBankAutoSettleEnabled(enabled)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Could not update Bank Match setting. Try again.")
                                }
                                return@SettingsToggleItem
                            }
                            performHapticFeedback(context)
                        }
                    )
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_ocr_live_auto_title),
                        description = stringResource(R.string.settings_ocr_live_auto_desc),
                        icon = Icons.Default.DocumentScanner,
                        isSelected = settings?.ocrLiveAutoSaveEnabled != false,
                        onCheckedChange = { enabled ->
                            viewModel.setOcrLiveAutoSaveEnabled(enabled)
                            performHapticFeedback(context)
                        }
                    )
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_crash_reporting_title),
                        description = stringResource(R.string.settings_crash_reporting_desc),
                        icon = Icons.Default.BugReport,
                        isSelected = crashReportingEnabled,
                        onCheckedChange = { enabled ->
                            if (!viewModel.setCrashReportingEnabled(enabled)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Could not update crash reporting setting. Try again.")
                                }
                            }
                            performHapticFeedback(context)
                        }
                    )
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_analytics_title),
                        description = stringResource(R.string.settings_analytics_desc),
                        icon = Icons.Default.Insights,
                        isSelected = analyticsEnabled,
                        onCheckedChange = { enabled ->
                            if (!viewModel.setAnalyticsEnabled(enabled)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Could not update analytics setting. Try again.")
                                }
                            }
                            performHapticFeedback(context)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    BankNotificationListenerProminentDisclosureDialog(
        dialogVisible = showBankAccessDisclosureDialog,
        onDismiss = { showBankAccessDisclosureDialog = false },
        onConfirmContinue = { activateBankAutoSettleAfterProminentConsent() },
        onOpenPrivacyPolicy = {
            showBankAccessDisclosureDialog = false
            onNavigateToPrivacyPolicy()
        },
    )
}

@Composable
fun SettingsBillingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInvoiceTemplates: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_enterprise_section_billing),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.primary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_bill_templates_title),
                    description = stringResource(R.string.settings_enterprise_sub_templates),
                    icon = Icons.Default.Description,
                    onClick = onNavigateToInvoiceTemplates
                )
                SettingsToggleItem(
                    title = stringResource(R.string.settings_gst_mode_title),
                    description = stringResource(R.string.settings_enterprise_sub_gst),
                    icon = Icons.Default.Receipt,
                    isSelected = settings?.gstEnabled ?: false,
                    onCheckedChange = { scope.launch { viewModel.toggleGst(it) } }
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun SettingsCloudScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncHealth by viewModel.syncHealth.collectAsState()
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_enterprise_section_cloud),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.primary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                SettingsItem(
                    title = stringResource(R.string.settings_cloud_backup_title),
                    description = stringResource(R.string.settings_enterprise_sub_cloud_backup),
                    icon = Icons.Default.CloudUpload,
                    onClick = { scope.launch { viewModel.syncData() } }
                )
                if (syncHealth.phase == SyncHealthMonitor.Phase.Syncing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.settings_sync_in_progress),
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                }
                val healthLine = buildString {
                    append(syncHealth.message)
                    if (syncHealth.totalOutstanding > 0) {
                        append(" · Pending: ").append(syncHealth.pending)
                        append(", Failed: ").append(syncHealth.failed)
                        if (syncHealth.queuedInMemory > 0) {
                            append(", Memory: ").append(syncHealth.queuedInMemory)
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = healthLine,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        color = colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                }
                syncStatus?.let { status ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        color = colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            color = colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (syncHealth.needsUserAttention || syncHealth.failed > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { viewModel.retryFailedSyncItems() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_enterprise_sync_retry),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun ThemeSelector(
    currentTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(PremiumThemeCatalog) { theme ->
            ThemeButton(
                theme = theme,
                isSelected = currentTheme == theme.id,
                onClick = { onThemeSelected(theme.id) },
                titleColor = colorScheme.onBackground
            )
        }
    }
}

@Composable
fun ThemeButton(
    theme: PremiumThemeSpec,
    isSelected: Boolean,
    onClick: () -> Unit,
    titleColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(theme.previewBackground)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) theme.previewPrimary else titleColor.copy(alpha = 0.22f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, null, tint = theme.previewPrimary, modifier = Modifier.size(26.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(theme.previewPrimary)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = theme.displayName, fontSize = 10.sp, color = titleColor, fontWeight = FontWeight.Bold)
    }
}

private data class LanguageOption(
    val label: String,
    val code: String
)

@Composable
fun LanguageSelector(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val options = IndianLanguageCatalog.supportedLanguages.map {
        LanguageOption(label = it.label, code = it.code)
    }
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull {
        it.code == AppLocaleManager.normalizeLanguageCode(currentLanguage)
    } ?: options.first()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.settings_language_label), fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            Text(
                stringResource(R.string.settings_language_choose),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selected.label,
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface,
                        focusedTrailingIconColor = colorScheme.primary,
                        unfocusedTrailingIconColor = colorScheme.onSurfaceVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = colorScheme.surface
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label, color = colorScheme.onSurface) },
                            onClick = {
                                expanded = false
                                onLanguageSelected(option.code)
                            }
                        )
                    }
                }
            }
        }
    }
}

fun performHapticFeedback(context: Context) {
    runCatching {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ).orEmpty()
    return enabled.contains(context.packageName, ignoreCase = true)
}

@Composable
private fun EnterpriseSectionHeader(title: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cs.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            letterSpacing = 0.2.sp
        )
    }
}

/** Legacy alias — routes to [EnterpriseSectionHeader]. */
@Composable
fun SettingsCategory(title: String) {
    EnterpriseSectionHeader(title)
}

@Composable
fun SettingsToggleItem(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = isSelected,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorScheme.primary,
                    checkedTrackColor = colorScheme.primary.copy(alpha = 0.45f),
                    uncheckedThumbColor = colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = colorScheme.outline.copy(alpha = 0.30f)
                )
            )
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .let { base -> if (testTag != null) base.testTag(testTag) else base }
        .clickable { onClick() }
    Surface(
        modifier = rowModifier,
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = colorScheme.onSurfaceVariant)
        }
    }
}
