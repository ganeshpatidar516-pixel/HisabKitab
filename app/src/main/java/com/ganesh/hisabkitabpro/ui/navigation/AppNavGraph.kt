package com.ganesh.hisabkitabpro.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ganesh.hisabkitabpro.MainActivity
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.commandos.SuperCommandService
import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.core.billing.PREFS_KEY_HTML_INVOICE_TEMPLATE_ID
import com.ganesh.hisabkitabpro.data.prefs.SupplierProfilePrefs
import com.ganesh.hisabkitabpro.core.crash.SafeFeature
import com.ganesh.hisabkitabpro.di.FeatureRecoveryEntryPoint
import com.ganesh.hisabkitabpro.di.OcrBillProcessorEntryPoint
import com.ganesh.hisabkitabpro.domain.ai.AIAction
import com.ganesh.hisabkitabpro.domain.ai.AIResponse
import com.ganesh.hisabkitabpro.domain.engine.AIKhataAssistant
import com.ganesh.hisabkitabpro.domain.engine.AssistantInputSplitter
import com.ganesh.hisabkitabpro.domain.engine.AssistantIntent
import com.ganesh.hisabkitabpro.domain.model.AppSettings
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.security.SecurityManager
import com.ganesh.hisabkitabpro.ui.ai.AIChatScreen
import com.ganesh.hisabkitabpro.ui.ai.ChatMessage
import com.ganesh.hisabkitabpro.ui.billing.TemplatePickerScreen
import com.ganesh.hisabkitabpro.ui.customers.*
import com.ganesh.hisabkitabpro.ui.help.HelpScreen
import com.ganesh.hisabkitabpro.ui.help.PrivacyPolicyScreen
import com.ganesh.hisabkitabpro.ui.inventory.InventoryScreen
import com.ganesh.hisabkitabpro.ui.ocr.OCRScannerScreen
import com.ganesh.hisabkitabpro.ui.screens.*
import com.ganesh.hisabkitabpro.ui.settings.*
import com.ganesh.hisabkitabpro.ui.staff.*
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierLedgerScreen
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierListScreen
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierStatementScreen
import com.ganesh.hisabkitabpro.ui.transactions.AddTransactionScreen
import com.ganesh.hisabkitabpro.ui.transactions.FullScreenBillEntryScreen
import com.ganesh.hisabkitabpro.ui.transactions.resolvePdfTemplateId
import com.ganesh.hisabkitabpro.ui.viewmodel.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.hisabAppNavGraph(
    navController: NavHostController,
    activity: FragmentActivity,
    transactionViewModel: TransactionViewModel,
    customerViewModel: CustomerViewModel,
    partyViewModel: PartyViewModel,
    settingsViewModel: SettingsViewModel,
    superCommandService: SuperCommandService,
    aiChatMessages: MutableList<ChatMessage>,
    pendingConfirmedCommand: MutableState<String?>,
    appSettings: AppSettings?,
) {
        composable("dashboard") {
            DashboardScreen(
                viewModel = transactionViewModel,
                onCustomersClick = { navController.navigate("customers") },
                onSuppliersClick = { navController.navigate("suppliers") },
                onInvoiceClick = { navController.navigate("customers") },
                onAiAssistantClick = {
                    navController.navigate("ai_assistant") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onViewTransactionsClick = { navController.navigate("customers") },
                onAddEntryClick = { navController.navigate("customers") },
                onScanBillClick = { navController.navigate("scan_bill/0") },
                onBusinessInsightsClick = { navController.navigate("business_insights") },
                onHelpClick = { navController.navigate("help_support") }
            )
        }

        composable("customers") { 
            CustomerListScreen(
                viewModel = customerViewModel,
                onCustomerClick = { id -> navController.navigate("customer_ledger/$id") },
                onAddCustomerClick = { navController.navigate("add_customer") }
            ) 
        }

        composable("suppliers") {
            LaunchedEffect(Unit) { partyViewModel.setSupplierTab(true) }
            SupplierListScreen(
                viewModel = partyViewModel,
                onSupplierClick = { id -> navController.navigate("supplier_ledger/$id") },
                onAddSupplierClick = { navController.navigate("add_supplier") }
            )
        }

        composable("ai_assistant") {
            val appContext = LocalContext.current.applicationContext
            val featureRecovery = remember(appContext) {
                EntryPointAccessors.fromApplication(
                    appContext,
                    FeatureRecoveryEntryPoint::class.java,
                ).featureRecoveryManager()
            }
            SafeFeature(featureId = "ai_assistant", recoveryManager = featureRecovery) {
            val scope = rememberCoroutineScope()
            val txState by transactionViewModel.allTransactions.collectAsStateWithLifecycle()
            val assistantCustomerNames by customerViewModel.assistantCustomerNames.collectAsStateWithLifecycle()

            suspend fun runLegacyAssistant(commandText: String, riskBadge: String) {
                val command = AIKhataAssistant.interpretCommand(commandText)
                val result = AIKhataAssistant.executeCommand(command, txState)
                aiChatMessages.add(
                    ChatMessage(
                        text = result.message,
                        isUser = false,
                        aiResponse = AIResponse(message = result.message),
                        riskBadge = riskBadge
                    )
                )
                when (command.intent) {
                    AssistantIntent.OPEN_SETTINGS -> navController.navigate("settings")
                    AssistantIntent.BUSINESS_ANALYSIS -> navController.navigate("business_insights")
                    else -> Unit
                }
            }

            suspend fun runSuperAssistant(commandText: String, userConfirmed: Boolean): Boolean {
                val result = superCommandService.run(
                    rawInput = commandText,
                    userConfirmed = userConfirmed
                )
                when (result) {
                    is CommandResult.Success -> {
                        val openPrefix = "OPEN_SCREEN::"
                        if (result.message.startsWith(openPrefix)) {
                            val route = result.message.removePrefix(openPrefix).trim()
                            aiChatMessages.add(
                                ChatMessage(
                                    text = "Navigation ready: $route",
                                    isUser = false,
                                    aiResponse = AIResponse(
                                        message = "Navigation ready: $route",
                                        actions = listOf(AIAction(label = "Open $route", route = route))
                                    ),
                                    riskBadge = "SUPER COMMAND"
                                )
                            )
                        } else {
                            aiChatMessages.add(
                                ChatMessage(
                                    text = result.message,
                                    isUser = false,
                                    aiResponse = AIResponse(message = result.message),
                                    riskBadge = "SUPER COMMAND"
                                )
                            )
                        }
                        return true
                    }
                    is CommandResult.ClarificationRequired -> {
                        pendingConfirmedCommand.value = commandText
                        val riskBadge = if (result.question.contains("High-risk", ignoreCase = true)) {
                            "HIGH RISK - CONFIRM REQUIRED"
                        } else {
                            "CONFIRMATION REQUIRED"
                        }
                        aiChatMessages.add(
                            ChatMessage(
                                text = result.question,
                                isUser = false,
                                aiResponse = AIResponse(message = result.question),
                                riskBadge = riskBadge
                            )
                        )
                        return true
                    }
                    is CommandResult.Rejected -> {
                        val normalizedReason = when {
                            result.reason.contains("confidence too low", ignoreCase = true) ->
                                "à¤•à¤®à¤¾à¤‚à¤¡ à¤¸à¥à¤ªà¤·à¥à¤Ÿ à¤¨à¤¹à¥€à¤‚ à¤¹à¥ˆà¥¤ à¤•à¥ƒà¤ªà¤¯à¤¾ à¤—à¥à¤°à¤¾à¤¹à¤• à¤•à¤¾ à¤¨à¤¾à¤® à¤”à¤° à¤°à¤¾à¤¶à¤¿ à¤¸à¤¾à¤« à¤²à¤¿à¤–à¥‡à¤‚à¥¤"
                            result.reason.contains("Customer not found", ignoreCase = true) ->
                                "à¤—à¥à¤°à¤¾à¤¹à¤• à¤¨à¤¹à¥€à¤‚ à¤®à¤¿à¤²à¤¾à¥¤ à¤¨à¤¾à¤® à¤œà¤¾à¤‚à¤šà¤•à¤° à¤«à¤¿à¤° à¤•à¥‹à¤¶à¤¿à¤¶ à¤•à¤°à¥‡à¤‚à¥¤"
                            result.reason.contains("missing", ignoreCase = true) ->
                                "à¤•à¤®à¤¾à¤‚à¤¡ à¤…à¤§à¥‚à¤°à¥€ à¤¹à¥ˆà¥¤ à¤•à¥ƒà¤ªà¤¯à¤¾ à¤†à¤µà¤¶à¥à¤¯à¤• à¤œà¤¾à¤¨à¤•à¤¾à¤°à¥€ à¤œà¥‹à¤¡à¤¼à¥‡à¤‚à¥¤"
                            result.reason.contains("Unknown command", ignoreCase = true) ->
                                "à¤¯à¤¹ à¤•à¤®à¤¾à¤‚à¤¡ à¤…à¤­à¥€ Super AI à¤®à¥‡à¤‚ à¤‰à¤ªà¤²à¤¬à¥à¤§ à¤¨à¤¹à¥€à¤‚ à¤¹à¥ˆà¥¤"
                            else -> "à¤¸à¥à¤°à¤•à¥à¤·à¤¾ à¤•à¤¾à¤°à¤£ à¤¸à¥‡ à¤¯à¤¹ à¤•à¤®à¤¾à¤‚à¤¡ à¤¨à¤¹à¥€à¤‚ à¤šà¤²à¥€: ${result.reason}"
                        }
                        aiChatMessages.add(
                            ChatMessage(
                                text = normalizedReason,
                                isUser = false,
                                aiResponse = AIResponse(message = normalizedReason),
                                riskBadge = "SAFE REJECTION"
                            )
                        )
                        val shouldFallbackToLegacy = result.reason.contains("Unknown command", ignoreCase = true) ||
                            result.reason.contains("confidence too low", ignoreCase = true)
                        return !shouldFallbackToLegacy
                    }
                }
            }

            AIChatScreen(
                assistantSampleCustomerNames = assistantCustomerNames,
                onOpenCustomersClick = {
                    navController.navigate("customers") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSendMessage = { message ->
                    aiChatMessages.add(ChatMessage(text = message, isUser = true))
                    scope.launch {
                        try {
                            if (message.trim().equals("CONFIRM", ignoreCase = true)) {
                                val pending = pendingConfirmedCommand.value
                                if (pending != null) {
                                    pendingConfirmedCommand.value = null
                                    val handled = runSuperAssistant(pending, userConfirmed = true)
                                    if (!handled) {
                                        runLegacyAssistant(pending, "CLASSIC")
                                    }
                                } else {
                                    aiChatMessages.add(
                                        ChatMessage(
                                            text = "No pending command to confirm. Send a command first.",
                                            isUser = false,
                                            aiResponse = AIResponse(message = "No pending command to confirm. Send a command first.")
                                        )
                                    )
                                }
                                return@launch
                            }
                            val segments = AssistantInputSplitter.splitForSequentialRun(message.trim())
                            if (segments.isEmpty()) return@launch
                            for (seg in segments) {
                                val handled = runSuperAssistant(seg, userConfirmed = false)
                                if (!handled) {
                                    runLegacyAssistant(seg, "CLASSIC FALLBACK")
                                }
                            }
                        } catch (_: Throwable) {
                            aiChatMessages.add(
                                ChatMessage(
                                    text = "Assistant temporary issue. Please try once again.",
                                    isUser = false,
                                    aiResponse = AIResponse(message = "Assistant temporary issue. Please try once again."),
                                    riskBadge = "SAFE RECOVERY"
                                )
                            )
                        }
                    }
                },
                chatMessages = aiChatMessages,
                onActionClick = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
            )
            }
        }

        composable("business_insights") {
            BusinessInsightsScreen(
                viewModel = transactionViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                showBackNavigation = false,
                onNavigateToBusinessProfile = { navController.navigate("business_profile") },
                onNavigateToVisitingCard = { navController.navigate("visiting_card_studio") },
                onNavigateToBillingSettings = { navController.navigate("settings_billing") },
                onNavigateToCloudSettings = { navController.navigate("settings_cloud") },
                onNavigateToHelpSupport = { navController.navigate("help_support") },
                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") },
                onNavigateToPrivacyAndData = { navController.navigate("privacy_and_data") },
                onNavigateToSecurityPin = { navController.navigate("security_pin") },
                onNavigateToStaffList = { navController.navigate("staff_list") },
                onNavigateToInventory = { navController.navigate("inventory") },
                viewModel = settingsViewModel,
                transactionViewModel = transactionViewModel
            )
        }

        composable("settings_billing") {
            SettingsBillingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInvoiceTemplates = { navController.navigate("html_brand_templates") },
                viewModel = settingsViewModel
            )
        }

        composable("settings_cloud") {
            SettingsCloudScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = settingsViewModel
            )
        }

        composable("privacy_and_data") {
            PrivacyAndDataScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("staff_list") {
            val staffViewModel: StaffViewModel = hiltViewModel()
            val staff by staffViewModel.staffList.collectAsStateWithLifecycle()
            val archived by staffViewModel.archivedStaff.collectAsStateWithLifecycle()
            StaffListScreen(
                staffList = staff,
                onAddStaffClick = { navController.navigate("staff_add") },
                onStaffClick = { id -> navController.navigate("staff_detail/$id") },
                onNavigateBack = { navController.popBackStack() },
                archivedStaff = archived,
                onRestoreStaff = { id -> staffViewModel.restoreStaff(id) },
                onDeleteStaff = { id -> staffViewModel.deleteStaff(id) }
            )
        }

        composable("staff_add") {
            val staffViewModel: StaffViewModel = hiltViewModel()
            val staff by staffViewModel.staffList.collectAsStateWithLifecycle()
            AddStaffScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveStaff = { entity -> staffViewModel.insertStaff(entity) },
                existingPhoneSet = staff.map { it.phone.filter(Char::isDigit) }.toSet()
            )
        }

        composable(
            route = "staff_detail/{staffId}",
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId").orEmpty()
            val staffViewModel: StaffViewModel = hiltViewModel()
            val selected by staffViewModel.selectedStaff.collectAsStateWithLifecycle()
            val payrollEntries by staffViewModel.payrollEntries.collectAsStateWithLifecycle()
            val payroll by staffViewModel.currentPayroll.collectAsStateWithLifecycle()
            val ctx = LocalContext.current

            LaunchedEffect(staffId) { staffViewModel.selectStaff(staffId) }

            LaunchedEffect(staffViewModel) {
                staffViewModel.slipShareEvents.collect { file ->
                    com.ganesh.hisabkitabpro.domain.payroll.StaffSlipShare.share(ctx, file)
                }
            }

            val staff = selected
            if (staff != null && staff.id == staffId) {
                StaffDetailScreen(
                    staff = staff,
                    payrollEntries = payrollEntries,
                    payroll = payroll,
                    onNavigateBack = {
                        staffViewModel.selectStaff(null)
                        navController.popBackStack()
                    },
                    onEdit = { navController.navigate("staff_edit/$staffId") },
                    onMarkAttendance = {
                        navController.navigate("staff_attendance/$staffId")
                    },
                    onAddPayrollEntry = { kind, amountPaise, note ->
                        staffViewModel.addPayrollEntry(
                            staffId = staffId,
                            kind = kind,
                            amountPaise = amountPaise,
                            note = note
                        )
                    },
                    onRemovePayrollEntry = { id ->
                        staffViewModel.softDeletePayrollEntry(id)
                    },
                    onGenerateSlip = {
                        try {
                            staffViewModel.generateSalarySlipPdf(staff).absolutePath
                        } catch (_: Exception) {
                            null
                        }
                    },
                    onArchive = {
                        staffViewModel.deleteStaff(staffId)
                        navController.popBackStack()
                    },
                    onRestore = { staffViewModel.restoreStaff(staffId) }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading staff profileâ€¦")
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Back")
                        }
                    }
                }
            }
        }

        composable(
            route = "staff_edit/{staffId}",
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId").orEmpty()
            val staffViewModel: StaffViewModel = hiltViewModel()
            val staff by staffViewModel.staffList.collectAsStateWithLifecycle()
            val entity = staff.firstOrNull { it.id == staffId }
            if (entity != null) {
                StaffEditScreen(
                    staff = entity,
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { updated ->
                        staffViewModel.updateStaff(updated)
                        navController.popBackStack()
                    },
                    onDelete = { id ->
                        staffViewModel.deleteStaff(id)
                        navController.popBackStack()
                    },
                    existingPhoneSet = staff
                        .filter { it.id != staffId }
                        .map { it.phone.filter(Char::isDigit) }
                        .toSet()
                )
            } else {
                LaunchedEffect(staffId) { navController.popBackStack() }
            }
        }

        composable(
            route = "staff_attendance/{staffId}",
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId").orEmpty()
            val staffViewModel: StaffViewModel = hiltViewModel()
            val selected by staffViewModel.selectedStaff.collectAsStateWithLifecycle()
            val attendance by staffViewModel.attendanceForPeriod.collectAsStateWithLifecycle()
            val periodStart by staffViewModel.attendancePeriodStart.collectAsStateWithLifecycle()

            LaunchedEffect(staffId) { staffViewModel.selectStaff(staffId) }

            val staff = selected
            if (staff != null && staff.id == staffId) {
                StaffAttendanceScreen(
                    staffName = staff.name,
                    periodStartMillis = periodStart,
                    attendance = attendance,
                    onNavigateBack = { navController.popBackStack() },
                    onChangePeriodStart = { staffViewModel.setAttendancePeriodStart(it) },
                    onMark = { day, status ->
                        staffViewModel.markAttendance(staffId, day, status)
                    },
                    onClear = { day -> staffViewModel.clearAttendance(staffId, day) }
                )
            }
        }

        composable("inventory") {
            val inventoryViewModel: InventoryViewModel = hiltViewModel()
            InventoryScreen(
                viewModel = inventoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("help_support") {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() },
                onAiChatClick = { navController.navigate("ai_assistant") },
                onPrivacyPolicyClick = { navController.navigate("privacy_policy") }
            )
        }

        composable("privacy_policy") {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("security_pin") {
            SecurityPinScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("business_profile") {
            BusinessProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = settingsViewModel
            )
        }

        composable("visiting_card_studio") {
            VisitingCardStudioScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = settingsViewModel,
                onEditBusinessProfile = { navController.navigate("business_profile") }
            )
        }

        composable("add_customer") {
            AddCustomerScreen(
                viewModel = customerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCustomerAdded = { navController.popBackStack() }
            )
        }

        composable(
            route = "supplier_ledger/{supplierId}",
            arguments = listOf(navArgument("supplierId") { type = NavType.LongType })
        ) { entry ->
            val supplierId = entry.arguments?.getLong("supplierId") ?: 0L
            var supplier by remember(supplierId) { mutableStateOf<com.ganesh.hisabkitabpro.domain.model.Party?>(null) }

            LaunchedEffect(supplierId) {
                if (supplierId != 0L) {
                    supplier = partyViewModel.repository.getPartyById(supplierId)
                }
            }

            when {
                supplierId == 0L -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Supplier not found", fontWeight = FontWeight.Medium)
                }
                supplier == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> {
                    val s = checkNotNull(supplier)
                    SupplierLedgerScreen(
                        supplier = s,
                        partyViewModel = partyViewModel,
                        onOpenLightOcr = { navController.navigate("scan_supplier_bill/${supplierId}") },
                        onNavigateBack = { navController.popBackStack() },
                        onOpenStatement = { navController.navigate("supplier_statement/${supplierId}") },
                        onReminderHistoryClick = {
                            val enc = Uri.encode(s.name)
                            navController.navigate("supplier_party_reminder_history/${supplierId}/$enc")
                        },
                        onReminderControlClick = {
                            val enc = Uri.encode(s.name)
                            navController.navigate("supplier_party_reminder_control/${supplierId}/$enc")
                        }
                    )
                }
            }
        }

        composable(
            route = "supplier_statement/{supplierId}",
            arguments = listOf(navArgument("supplierId") { type = NavType.LongType })
        ) { entry ->
            val supplierId = entry.arguments?.getLong("supplierId") ?: 0L
            var supplier by remember(supplierId) { mutableStateOf<com.ganesh.hisabkitabpro.domain.model.Party?>(null) }
            LaunchedEffect(supplierId) {
                if (supplierId != 0L) {
                    supplier = partyViewModel.repository.getPartyById(supplierId)
                }
            }
            when {
                supplierId == 0L -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Supplier not found", fontWeight = FontWeight.Medium)
                }
                supplier == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> SupplierStatementScreen(
                    supplier = checkNotNull(supplier),
                    partyViewModel = partyViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable("add_supplier") {
            var name by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var city by remember { mutableStateOf("") }
            var attemptedSubmit by remember { mutableStateOf(false) }
            val isNameValid = name.trim().length >= 2
            val isPhoneValid = phone.trim().length >= 10

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.add_supplier_title), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.add_supplier_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = attemptedSubmit && !isNameValid,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter(Char::isDigit).take(15) },
                        label = { Text(stringResource(R.string.add_supplier_phone_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = attemptedSubmit && !isPhoneValid,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(stringResource(R.string.add_supplier_city_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            attemptedSubmit = true
                            if (isNameValid && isPhoneValid) {
                                partyViewModel.addParty(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    isSupplier = true,
                                    city = city.trim().takeIf { it.isNotEmpty() },
                                    onAdded = { id ->
                                        SupplierProfilePrefs.setCity(activity, id, city)
                                        navController.popBackStack()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(stringResource(R.string.add_supplier_save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        composable(
            route = "customer_ledger/{customerId}", 
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { entry -> 
            val id = entry.arguments?.getLong("customerId") ?: 0L
            if (id == 0L) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Customer not found", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { navController.popBackStack() }) { Text("Go back") }
                    }
                }
            } else {
                val customerFlow = remember(id) { customerViewModel.getCustomerByIdFlow(id) }
                val customer by customerFlow.collectAsStateWithLifecycle(initialValue = null)
                when (customer) {
                    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> {
                        val c = checkNotNull(customer)
                        CustomerLedgerScreen(
                            customer = c,
                            transactionViewModel = transactionViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onBillClick = { navController.navigate("customer_statement/${c.id}") },
                            onProfileClick = { navController.navigate("customer_profile/${c.id}") },
                            onReminderHistoryClick = {
                                val enc = Uri.encode(c.name)
                                navController.navigate("customer_reminder_history/${c.id}/$enc")
                            },
                            onReminderControlClick = {
                                val enc = Uri.encode(c.name)
                                navController.navigate("customer_reminder_control/${c.id}/$enc")
                            },
                            onBulkRemindersClick = {
                                navController.navigate("bulk_reminder_schedule")
                            },
                            onAddEntry = { type ->
                                val enc = Uri.encode(c.name)
                                navController.navigate("add_transaction/${c.id}/$enc/${type.name}")
                            }
                        )
                    }
                }
            }
        }

        composable(
            route = "customer_profile/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { entry ->
            val customerId = entry.arguments?.getLong("customerId") ?: 0L
            if (customerId == 0L) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Customer not found", fontWeight = FontWeight.Medium)
                }
            } else {
                val customerFlow = remember(customerId) { customerViewModel.getCustomerByIdFlow(customerId) }
                val customer by customerFlow.collectAsStateWithLifecycle(initialValue = null)
                when (customer) {
                    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> {
                        val c = checkNotNull(customer)
                        CustomerProfileScreen(
                            customer = c,
                            customerViewModel = customerViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onEditClick = { navController.navigate("edit_customer/${c.id}") },
                            onLedgerClick = { navController.popBackStack() },
                            onAnalyticsClick = { navController.navigate("business_insights") }
                        )
                    }
                }
            }
        }

        composable(
            route = "edit_customer/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { entry ->
            val customerId = entry.arguments?.getLong("customerId") ?: 0L
            if (customerId == 0L) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Customer not found", fontWeight = FontWeight.Medium)
                }
            } else {
                val customerFlow = remember(customerId) { customerViewModel.getCustomerByIdFlow(customerId) }
                val customer by customerFlow.collectAsStateWithLifecycle(initialValue = null)
                when (customer) {
                    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> {
                        EditCustomerScreen(
                            customer = checkNotNull(customer),
                            viewModel = customerViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        composable(
            route = "customer_statement/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { entry ->
            val customerId = entry.arguments?.getLong("customerId") ?: 0L
            if (customerId == 0L) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Customer not found", fontWeight = FontWeight.Medium)
                }
            } else {
                val customerFlow = remember(customerId) { customerViewModel.getCustomerByIdFlow(customerId) }
                val customer by customerFlow.collectAsStateWithLifecycle(initialValue = null)
                when (customer) {
                    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> {
                        CustomerStatementScreen(
                            customer = checkNotNull(customer),
                            transactionViewModel = transactionViewModel,
                            settingsViewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        composable(
            route = "customer_reminder_control/{customerId}/{customerName}",
            arguments = listOf(
                navArgument("customerId") { type = NavType.LongType },
                navArgument("customerName") { type = NavType.StringType }
            )
        ) { entry ->
            val customerId = entry.arguments?.getLong("customerId") ?: 0L
            val customerName = Uri.decode(entry.arguments?.getString("customerName").orEmpty())
            ReminderControlScreen(
                subjectId = customerId,
                subjectName = customerName,
                subject = ReminderControlSubject.CUSTOMER,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "customer_reminder_history/{customerId}/{customerName}",
            arguments = listOf(
                navArgument("customerId") { type = NavType.LongType },
                navArgument("customerName") { type = NavType.StringType }
            )
        ) { entry ->
            val customerId = entry.arguments?.getLong("customerId") ?: 0L
            val customerName = Uri.decode(entry.arguments?.getString("customerName").orEmpty())
            ReminderHistoryScreen(
                subjectId = customerId,
                subjectName = customerName,
                subject = ReminderHistorySubject.CUSTOMER,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = "bulk_reminder_schedule") {
            BulkReminderScheduleScreen(
                viewModel = customerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "supplier_party_reminder_control/{partyId}/{partyName}",
            arguments = listOf(
                navArgument("partyId") { type = NavType.LongType },
                navArgument("partyName") { type = NavType.StringType }
            )
        ) { entry ->
            val partyId = entry.arguments?.getLong("partyId") ?: 0L
            val partyName = Uri.decode(entry.arguments?.getString("partyName").orEmpty())
            ReminderControlScreen(
                subjectId = partyId,
                subjectName = partyName,
                subject = ReminderControlSubject.PARTY_SUPPLIER,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "supplier_party_reminder_history/{partyId}/{partyName}",
            arguments = listOf(
                navArgument("partyId") { type = NavType.LongType },
                navArgument("partyName") { type = NavType.StringType }
            )
        ) { entry ->
            val partyId = entry.arguments?.getLong("partyId") ?: 0L
            val partyName = Uri.decode(entry.arguments?.getString("partyName").orEmpty())
            ReminderHistoryScreen(
                subjectId = partyId,
                subjectName = partyName,
                subject = ReminderHistorySubject.PARTY_SUPPLIER,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "add_transaction/{customerId}/{customerName}/{type}",
            arguments = listOf(
                navArgument("customerId") { type = NavType.LongType },
                navArgument("customerName") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val customerName = backStackEntry.arguments?.getString("customerName") ?: ""
            val typeStr = backStackEntry.arguments?.getString("type") ?: "CREDIT"
            val type = TransactionType.valueOf(typeStr)
            AddTransactionScreen(
                customerId = customerId,
                customerName = customerName,
                type = type,
                viewModel = transactionViewModel,
                onTransactionAdded = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
                onOpenScanBill = { navController.navigate("scan_bill/$customerId") },
                onOpenFullBill = {
                    val enc = Uri.encode(customerName)
                    navController.navigate("full_bill/$customerId/$enc/${type.name}")
                }
            )
        }

        composable(
            route = "full_bill/{customerId}/{customerName}/{type}",
            arguments = listOf(
                navArgument("customerId") { type = NavType.LongType },
                navArgument("customerName") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { entry ->
            val ctx = LocalContext.current
            val cid = entry.arguments?.getLong("customerId") ?: 0L
            val cnameRaw = entry.arguments?.getString("customerName").orEmpty()
            val cname = Uri.decode(cnameRaw)
            val typeStr = entry.arguments?.getString("type") ?: "CREDIT"
            val billType = TransactionType.valueOf(typeStr)
            val businessProfile by settingsViewModel.businessProfile.collectAsStateWithLifecycle(
                initialValue = null
            )
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle(initialValue = null)
            val htmlBrandId = remember(ctx) {
                ctx.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
                    .getString(PREFS_KEY_HTML_INVOICE_TEMPLATE_ID, null)
            }
            val pdfTemplateId = resolvePdfTemplateId(settings?.invoiceTemplateId, htmlBrandId)
            FullScreenBillEntryScreen(
                customerId = cid,
                customerName = cname,
                type = billType,
                businessProfile = businessProfile,
                pdfTemplateId = pdfTemplateId,
                settingsGstEnabled = settings?.gstEnabled == true,
                settingsGstRatePercent = if (settings?.gstEnabled == true) {
                    settings?.gstRate?.takeIf { it > 0 } ?: 18.0
                } else {
                    0.0
                },
                viewModel = transactionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onBillFlowComplete = {
                    navController.popBackStack()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "scan_supplier_bill/{supplierId}",
            arguments = listOf(navArgument("supplierId") { type = NavType.LongType })
        ) { entry ->
            val supplierId = entry.arguments?.getLong("supplierId") ?: 0L
            val context = LocalContext.current
            val processor = remember {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    OcrBillProcessorEntryPoint::class.java
                ).ocrBillProcessor()
            }
            OCRScannerScreen(
                processor = processor,
                customerId = 0L,
                scanForPrefill = true,
                liveLedgerAutoSaveEnabled = appSettings?.ocrLiveAutoSaveEnabled != false,
                onPrefill = { amountText, noteLine, billImageUri, lowConf ->
                    partyViewModel.setPendingSupplierScanPrefill(
                        supplierId,
                        amountText,
                        noteLine,
                        billImageUri,
                        lowConf,
                    )
                    navController.popBackStack()
                },
                onResult = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "scan_bill/{customerId}?prefillOnly={prefillOnly}",
            arguments = listOf(
                navArgument("customerId") { type = NavType.LongType },
                navArgument("prefillOnly") {
                    type = NavType.BoolType
                    defaultValue = true
                },
            ),
        ) { entry ->
            val scanCustomerId = entry.arguments?.getLong("customerId") ?: 0L
            val prefillOnly = entry.arguments?.getBoolean("prefillOnly") ?: true
            val scanForPrefill = scanCustomerId == 0L || prefillOnly
            val context = LocalContext.current
            val processor = remember {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    OcrBillProcessorEntryPoint::class.java
                ).ocrBillProcessor()
            }
            OCRScannerScreen(
                processor = processor,
                customerId = scanCustomerId,
                scanForPrefill = scanForPrefill,
                liveLedgerAutoSaveEnabled = appSettings?.ocrLiveAutoSaveEnabled != false,
                onPrefill = { amountText, noteLine, _, lowConf ->
                    transactionViewModel.setPendingScanPrefill(amountText, noteLine, lowConf)
                    navController.popBackStack()
                    if (scanCustomerId == 0L) {
                        navController.navigate("customers")
                    }
                },
                onResult = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("html_brand_templates") {
            val ctx = LocalContext.current
            val scheme = MaterialTheme.colorScheme
            Scaffold(
                containerColor = scheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Bill brand (HTML)",
                                color = scheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = scheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
                    )
                }
            ) { padding ->
                TemplatePickerScreen(
                    onTemplateSelected = {
                        Toast.makeText(ctx, "Template saved", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }

}
