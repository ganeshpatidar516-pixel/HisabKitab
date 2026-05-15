package com.ganesh.hisabkitabpro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ganesh.hisabkitabpro.core.crash.SafeScreen
import com.ganesh.hisabkitabpro.core.play.PlayStoreFlexibleUpdate
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.ui.screens.*
import com.ganesh.hisabkitabpro.ui.customers.*
import com.ganesh.hisabkitabpro.di.OcrBillProcessorEntryPoint
import com.ganesh.hisabkitabpro.ui.ocr.OCRScannerScreen
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierLedgerScreen
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierListScreen
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierStatementScreen
import com.ganesh.hisabkitabpro.ui.transactions.AddTransactionScreen
import com.ganesh.hisabkitabpro.core.billing.PREFS_KEY_HTML_INVOICE_TEMPLATE_ID
import com.ganesh.hisabkitabpro.ui.transactions.FullScreenBillEntryScreen
import com.ganesh.hisabkitabpro.ui.transactions.resolvePdfTemplateId
import com.ganesh.hisabkitabpro.ui.billing.TemplatePickerScreen
import com.ganesh.hisabkitabpro.ui.settings.BusinessProfileScreen
import com.ganesh.hisabkitabpro.ui.settings.PrivacyAndDataScreen
import com.ganesh.hisabkitabpro.ui.settings.SecurityPinScreen
import com.ganesh.hisabkitabpro.ui.settings.SettingsBillingScreen
import com.ganesh.hisabkitabpro.ui.settings.SettingsCloudScreen
import com.ganesh.hisabkitabpro.ui.settings.SettingsScreen
import com.ganesh.hisabkitabpro.ui.settings.VisitingCardStudioScreen
import com.ganesh.hisabkitabpro.ui.staff.AddStaffScreen
import com.ganesh.hisabkitabpro.ui.staff.StaffAttendanceScreen
import com.ganesh.hisabkitabpro.ui.staff.StaffDetailScreen
import com.ganesh.hisabkitabpro.ui.staff.StaffEditScreen
import com.ganesh.hisabkitabpro.ui.staff.StaffListScreen
import com.ganesh.hisabkitabpro.ui.inventory.InventoryScreen
import com.ganesh.hisabkitabpro.ui.help.HelpScreen
import com.ganesh.hisabkitabpro.ui.help.PrivacyPolicyScreen
import com.ganesh.hisabkitabpro.ui.navigation.AppRoutes
import com.ganesh.hisabkitabpro.ui.navigation.bottomNavItems
import com.ganesh.hisabkitabpro.ui.navigation.hisabAppNavGraph
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.ai.AIAction
import com.ganesh.hisabkitabpro.domain.ai.AIResponse
import com.ganesh.hisabkitabpro.commandos.SuperCommandService
import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.domain.engine.AIKhataAssistant
import com.ganesh.hisabkitabpro.domain.engine.AssistantInputSplitter
import com.ganesh.hisabkitabpro.domain.engine.AssistantIntent
import com.ganesh.hisabkitabpro.ui.ai.AIChatScreen
import com.ganesh.hisabkitabpro.ui.ai.ChatMessage
import com.ganesh.hisabkitabpro.ui.launch.HisabKitabLaunchScreen
import com.ganesh.hisabkitabpro.ui.viewmodel.*
import com.ganesh.hisabkitabpro.ui.theme.HisabKitabProTheme
import com.ganesh.hisabkitabpro.ui.theme.normalizeDeprecatedThemeId
import com.ganesh.hisabkitabpro.security.SecurityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    /**
     * When [true], the next activity [ON_STOP] must not re-arm the compose app lock â€” used when
     * we intentionally leave for WhatsApp/share chooser so return does not look like a crash or cold gate.
     */
    @Volatile
    internal var deferAppLockRearmOnNextStop: Boolean = false

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var superCommandService: SuperCommandService

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle(initialValue = null)

            LaunchedEffect(settings?.languageCode) {
                val raw = settings?.languageCode ?: return@LaunchedEffect
                val desired = AppLocaleManager.normalizeLanguageCode(raw)
                val savedNorm = AppLocaleManager.normalizeLanguageCode(
                    AppLocaleManager.getSavedLanguageCode(this@MainActivity)
                )
                if (savedNorm == desired) return@LaunchedEffect
                if (!AppLocaleManager.saveLanguageCode(this@MainActivity, desired)) {
                    Log.w("MainActivity", "Locale prefs commit failed; skipping locale apply")
                    return@LaunchedEffect
                }
                AppLocaleManager.updateLocaleNow(this@MainActivity, desired)
                // Do not recreate here: (1) avoids activity churn on every DB/settings emission,
                // (2) full UI refresh for language is handled when user changes language in Settings (explicit recreate).
            }
            
            HisabKitabProTheme(themeType = normalizeDeprecatedThemeId(settings?.theme)) {
                SafeScreen {
                    // Brand & Trust Launch Shield is overlaid on top of the existing
                    // MainContainer so the underlying ledger/billing flow is unaffected
                    // and the reveal is flicker-free (MainContainer is already mounted).
                    var showLaunch by remember { mutableStateOf(true) }
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainContainer(
                            activity = this@MainActivity,
                            settingsViewModel = settingsViewModel,
                            securityManager = securityManager,
                            superCommandService = superCommandService
                        )
                        AnimatedVisibility(
                            visible = showLaunch,
                            enter = androidx.compose.animation.fadeIn(tween(0)),
                            exit = fadeOut(tween(durationMillis = 450))
                        ) {
                            HisabKitabLaunchScreen(onLaunchComplete = { showLaunch = false })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching { PlayStoreFlexibleUpdate.maybeStartFlexibleUpdate(this) }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        PlayStoreFlexibleUpdate.handleActivityResult(requestCode, resultCode, data)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    activity: FragmentActivity,
    settingsViewModel: SettingsViewModel,
    securityManager: SecurityManager,
    superCommandService: SuperCommandService
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val colorScheme = MaterialTheme.colorScheme
    
    val transactionViewModel: TransactionViewModel = hiltViewModel()
    val customerViewModel: CustomerViewModel = hiltViewModel()
    val partyViewModel: PartyViewModel = hiltViewModel()
    val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    val appLockEnabled by settingsViewModel.isBiometricEnabled.collectAsStateWithLifecycle(initialValue = false)
    val pinHash = appSettings?.securityPinHash?.takeIf { it.isNotBlank() }
    val biometricAvailable = remember { securityManager.canAuthenticate() }
    val lockRequired = appLockEnabled && (biometricAvailable || !pinHash.isNullOrBlank())
    var appUnlocked by remember(lockRequired, pinHash, biometricAvailable) { mutableStateOf(!lockRequired) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    DisposableEffect(lockRequired, lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (lockRequired) {
                        val defer = (activity as? MainActivity)?.deferAppLockRearmOnNextStop == true
                        if (!defer) {
                            appUnlocked = false
                        }
                    }
                }
                Lifecycle.Event.ON_START -> {
                    (activity as? MainActivity)?.deferAppLockRearmOnNextStop = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val aiChatMessages = remember { mutableStateListOf<ChatMessage>() }
    val pendingConfirmedCommand = remember { mutableStateOf<String?>(null) }

    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("hisab_nav_host_root")
    ) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = colorScheme.surface,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        val navLabel = stringResource(screen.titleRes)
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = navLabel, modifier = Modifier.size(24.dp)) },
                            label = { Text(navLabel, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                            selected = selected,
                            alwaysShowLabel = true,
                            modifier = Modifier.testTag("bottom_nav_${screen.route}"),
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = colorScheme.primary.copy(alpha = 0.16f),
                                selectedIconColor = colorScheme.primary,
                                unselectedIconColor = colorScheme.onSurfaceVariant,
                                selectedTextColor = colorScheme.onSurface,
                                unselectedTextColor = colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                if (currentDestination?.route != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.Dashboard,
            modifier = Modifier.padding(innerPadding)
        ) {
            hisabAppNavGraph(
                navController = navController,
                activity = activity,
                transactionViewModel = transactionViewModel,
                customerViewModel = customerViewModel,
                partyViewModel = partyViewModel,
                settingsViewModel = settingsViewModel,
                superCommandService = superCommandService,
                aiChatMessages = aiChatMessages,
                pendingConfirmedCommand = pendingConfirmedCommand,
                appSettings = appSettings,
            )
        }
    }

    if (!appUnlocked) {
        AppLockGate(
            activity = activity,
            biometricEnabled = appLockEnabled,
            pinHash = pinHash,
            securityManager = securityManager,
            onUnlocked = { appUnlocked = true }
        )
    }
    }
}

@Composable
private fun AppLockGate(
    activity: FragmentActivity,
    biometricEnabled: Boolean,
    pinHash: String?,
    securityManager: SecurityManager,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val canUseBiometric = biometricEnabled && securityManager.canAuthenticate()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("App Locked", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(
                "Authenticate to continue",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            if (canUseBiometric) {
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = {
                        securityManager.authenticate(
                            activity = activity,
                            onSuccess = onUnlocked,
                            onError = { msg -> error = msg.ifBlank { "Authentication failed" } }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with biometrics")
                }
            }

            if (pinHash != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all(Char::isDigit)) {
                            pin = it
                            error = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Enter 4-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (pin.length != 4) {
                            error = "PIN must be 4 digits"
                        } else if (securityManager.verifyPin(pin, pinHash)) {
                            onUnlocked()
                        } else {
                            error = "Incorrect PIN"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock with PIN")
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}
