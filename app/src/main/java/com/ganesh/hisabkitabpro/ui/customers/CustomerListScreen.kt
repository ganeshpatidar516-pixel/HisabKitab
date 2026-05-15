@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.customers

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.reminder.ReminderAutomationPrefs
import com.ganesh.hisabkitabpro.domain.customers.CustomerListMenuTab
import com.ganesh.hisabkitabpro.domain.customers.CustomerListReminderSegment
import com.ganesh.hisabkitabpro.domain.customers.CustomerListSortOption
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppSender
import com.ganesh.hisabkitabpro.ui.viewmodel.CustomerViewModel
import com.ganesh.hisabkitabpro.util.safeClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HISABKITAB PRO - CUSTOMER LIST SCREEN (ULTRA-PRO MAX)
 * Amoled Black & Gold Theme with Paging 3.
 * Optimized with safeClickable to prevent UI freezes.
 */
@Composable
fun CustomerListScreen(
    viewModel: CustomerViewModel,
    onCustomerClick: (Long) -> Unit,
    onAddCustomerClick: () -> Unit
) {
    val pagedCustomers = viewModel.pagedCustomers.collectAsLazyPagingItems()
    val customerListOverview by viewModel.customerListOverview.collectAsStateWithLifecycle()
    val remindedCustomers by viewModel.remindedCustomersOverview.collectAsStateWithLifecycle()
    val notRemindedCustomers by viewModel.notRemindedCustomersOverview.collectAsStateWithLifecycle()
    val dueNowCustomers by viewModel.dueNowCustomersOverview.collectAsStateWithLifecycle()
    val autoOffCustomerCount by viewModel.autoOffCustomerCount.collectAsStateWithLifecycle()
    val remindedCustomerIds by viewModel.remindedCustomerIds.collectAsStateWithLifecycle()
    val dueReminderCustomerIds by viewModel.dueReminderCustomerIds.collectAsStateWithLifecycle()
    val appliedMenuTab by viewModel.menuTab.collectAsStateWithLifecycle()
    val appliedSortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val appliedReminderSegments by viewModel.reminderSegments.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showSortSheet by remember { mutableStateOf(false) }
    var reminderSheetMode by remember { mutableStateOf<ReminderSheetMode?>(null) }
    var showDueNowSheet by remember { mutableStateOf(false) }
    var draftMenuTab by remember { mutableStateOf(CustomerListMenuTab.SORT_BY) }
    var draftSortOption by remember { mutableStateOf(CustomerListSortOption.DEFAULT) }
    var draftReminderSegments by remember { mutableStateOf(emptySet<CustomerListReminderSegment>()) }
    val context = LocalContext.current
    val businessProfile by produceState<BusinessProfile?>(null) {
        value = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context.applicationContext).businessProfileDao().getBusinessProfileOnce()
        }
    }
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme
    val sentTodayCount = remindedCustomerIds.size
    val autoPilotEnabled = remember(context) {
        ReminderAutomationPrefs.isAutoPilotEnabled(context)
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "MERE GRAHAK",
                        fontWeight = FontWeight.Black,
                        color = colorScheme.primary,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            draftMenuTab = appliedMenuTab
                            draftSortOption = appliedSortOption
                            draftReminderSegments = appliedReminderSegments
                            showSortSheet = true
                        }
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.customer_menu_sort_filter),
                            tint = colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("sacred_fab_add_customer")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            CustomerOverviewCard(
                totalCustomers = customerListOverview.totalCustomers,
                overallNetBalancePaise = customerListOverview.overallNetBalancePaise,
                remindedCustomers = customerListOverview.remindedCustomers,
                notRemindedCustomers = customerListOverview.notRemindedCustomers,
                remindersDueNow = customerListOverview.remindersDueNow,
                autoPilotEnabled = autoPilotEnabled,
                sentTodayCount = sentTodayCount,
                autoOffCustomerCount = autoOffCustomerCount,
                onDueNowClick = { showDueNowSheet = true },
                onRemindedClick = { reminderSheetMode = ReminderSheetMode.REMINDED },
                onNotRemindedClick = { reminderSheetMode = ReminderSheetMode.NOT_REMINDED }
            )

            // Modern Amoled Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it 
                            viewModel.setSearchQuery(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 16.sp),
                        cursorBrush = SolidColor(colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search by name or phone...",
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = pagedCustomers.itemCount,
                    key = pagedCustomers.itemKey { it.id }
                ) { index ->
                    val customer = pagedCustomers[index]
                    if (customer != null) {
                        CustomerListItem(
                            customer = customer,
                            onClick = { onCustomerClick(customer.id) },
                            onWhatsAppClick = { 
                                try {
                                    val sent = WhatsAppSender.sendPaymentReminder(
                                        context.applicationContext,
                                        customer.phone,
                                        customer.balanceCache.toDouble() / 100.0,
                                        businessProfile,
                                    )
                                    if (sent) {
                                        viewModel.markCustomerReminderSent(customer.id)
                                    }
                                } catch (e: Exception) {}
                            }
                        )
                    }
                }

                // Handle Loading/Error States
                when (val state = pagedCustomers.loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = colorScheme.primary)
                            }
                        }
                    }
                    else -> {}
                }
            }

            if (pagedCustomers.itemCount == 0 && pagedCustomers.loadState.refresh is LoadState.NotLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "NO CUSTOMERS FOUND",
                        color = colorScheme.primary.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        CustomerListSortSheet(
            draftTab = draftMenuTab,
            onDraftTabChange = { draftMenuTab = it },
            draftSort = draftSortOption,
            onDraftSortChange = { draftSortOption = it },
            draftReminderSegments = draftReminderSegments,
            onDraftReminderSegmentsChange = { draftReminderSegments = it },
            onDismiss = { showSortSheet = false },
            onClearDraft = {
                draftMenuTab = CustomerListMenuTab.SORT_BY
                draftSortOption = CustomerListSortOption.DEFAULT
                draftReminderSegments = emptySet()
            },
            onApply = {
                focusManager.clearFocus(force = true)
                viewModel.applyListSort(draftMenuTab, draftSortOption, draftReminderSegments)
                pagedCustomers.refresh()
                showSortSheet = false
                Toast.makeText(
                    context,
                    context.getString(R.string.customer_sort_applied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    reminderSheetMode?.let { mode ->
        ReminderCustomerDetailSheet(
            mode = mode,
            customers = if (mode == ReminderSheetMode.REMINDED) remindedCustomers else notRemindedCustomers,
            autoPilotEnabled = autoPilotEnabled,
            onOpenCustomer = { onCustomerClick(it) },
            onRemindNow = { customer ->
                val sent = WhatsAppSender.sendPaymentReminder(
                    context.applicationContext,
                    customer.phone,
                    customer.balanceCache.toDouble() / 100.0,
                    businessProfile,
                )
                if (sent) {
                    viewModel.markCustomerReminderSent(customer.id)
                }
            },
            onDismiss = { reminderSheetMode = null }
        )
    }

    if (showDueNowSheet) {
        ReminderCustomerDetailSheet(
            mode = ReminderSheetMode.DUE_NOW,
            customers = dueNowCustomers,
            autoPilotEnabled = autoPilotEnabled,
            onOpenCustomer = { onCustomerClick(it) },
            onRemindNow = { customer ->
                val sent = WhatsAppSender.sendPaymentReminder(
                    context.applicationContext,
                    customer.phone,
                    customer.balanceCache.toDouble() / 100.0,
                    businessProfile,
                )
                if (sent) {
                    viewModel.markCustomerReminderSent(customer.id)
                }
            },
            onDismiss = { showDueNowSheet = false }
        )
    }
}

@Composable
private fun CustomerOverviewCard(
    totalCustomers: Int,
    overallNetBalancePaise: Long,
    remindedCustomers: Int,
    notRemindedCustomers: Int,
    remindersDueNow: Int,
    autoPilotEnabled: Boolean,
    sentTodayCount: Int,
    autoOffCustomerCount: Int,
    onDueNowClick: () -> Unit,
    onRemindedClick: () -> Unit,
    onNotRemindedClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val netBalanceRupees = overallNetBalancePaise / 100.0
    val shouldReceive = netBalanceRupees >= 0
    val amountColor = if (shouldReceive) Color(0xFF2E7D32) else colorScheme.error
    val balanceLabel = if (shouldReceive) "GET" else "GIVE"
    val reminderCoverage = if (totalCustomers > 0) {
        ((remindedCustomers.toDouble() / totalCustomers.toDouble()) * 100.0)
    } else {
        0.0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .safeClickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Overall Customer Net Balance",
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₹${String.format(Locale.US, "%.0f", kotlin.math.abs(netBalanceRupees))}",
                        color = amountColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "$balanceLabel  •  Total: $totalCustomers",
                        color = amountColor.copy(alpha = 0.78f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle summary",
                    tint = colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onRemindedClick,
                    label = { Text("Reminded: $remindedCustomers") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = colorScheme.primary.copy(alpha = 0.10f),
                        labelColor = colorScheme.onSurface
                    )
                )
                AssistChip(
                    onClick = onNotRemindedClick,
                    label = { Text("Not Reminded: $notRemindedCustomers") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = colorScheme.error.copy(alpha = 0.10f),
                        labelColor = colorScheme.onSurface
                    )
                )
            }

            val modeText = if (autoPilotEnabled) "Auto Reminder: ON" else "Auto Reminder: OFF"
            val modeColor = if (autoPilotEnabled) Color(0xFF2E7D32) else colorScheme.error
            Text(
                text = "$modeText  |  Due Now: $remindersDueNow",
                color = modeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.safeClickable { onDueNowClick() }
            )
            Text(
                text = "Reminder effectiveness: ${String.format(Locale.US, "%.0f", reminderCoverage)}%",
                color = colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onRemindedClick,
                        label = { Text("Sent: $sentTodayCount") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f),
                            labelColor = colorScheme.onSurface
                        )
                    )
                    AssistChip(
                        onClick = onDueNowClick,
                        label = { Text("Due: $remindersDueNow") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colorScheme.error.copy(alpha = 0.10f),
                            labelColor = colorScheme.onSurface
                        )
                    )
                    AssistChip(
                        onClick = onNotRemindedClick,
                        label = { Text("Auto Off: $autoOffCustomerCount") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colorScheme.outline.copy(alpha = 0.14f),
                            labelColor = colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

private enum class ReminderSheetMode {
    REMINDED,
    NOT_REMINDED,
    DUE_NOW
}

@Composable
private fun ReminderCustomerDetailSheet(
    mode: ReminderSheetMode,
    customers: List<CustomerViewModel.ReminderOverviewCustomer>,
    autoPilotEnabled: Boolean,
    onOpenCustomer: (Long) -> Unit,
    onRemindNow: (CustomerViewModel.ReminderOverviewCustomer) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val title = when (mode) {
        ReminderSheetMode.REMINDED -> "Reminded Customers"
        ReminderSheetMode.NOT_REMINDED -> "Not Reminded Customers"
        ReminderSheetMode.DUE_NOW -> "Due Now Reminders"
    }
    val helper = if (autoPilotEnabled) {
        "Auto reminder mode is ON. Customer-level auto reminder status is shown below."
    } else {
        "Auto reminder mode is OFF. Automatic reminders will not go until it is enabled in Settings."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(helper, color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))

            if (customers.isEmpty()) {
                Text("No customers in this section.", color = colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers.size) { index ->
                        val customer = customers[index]
                        ReminderDetailRow(
                            customer = customer,
                            mode = mode,
                            autoPilotEnabled = autoPilotEnabled,
                            onOpenCustomer = { onOpenCustomer(customer.id) },
                            onRemindNow = { onRemindNow(customer) }
                        )
                    }
                }
            }
        }
    }
}

private fun formatPauseUntil(timeMillis: Long): String {
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timeMillis))
}

@Composable
private fun ReminderDetailRow(
    customer: CustomerViewModel.ReminderOverviewCustomer,
    mode: ReminderSheetMode,
    autoPilotEnabled: Boolean,
    onOpenCustomer: () -> Unit,
    onRemindNow: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val amountColor = if (customer.balanceCache >= 0) Color(0xFF2E7D32) else colorScheme.error
    val pauseUntil = ReminderAutomationPrefs.getCustomerPauseUntil(context, customer.id)
    val perCustomerAutoEnabled = ReminderAutomationPrefs.isCustomerReminderEnabled(context, customer.id)
    val persistedCustomerReason = ReminderAutomationPrefs.getLastCustomerSkipReason(context, customer.id)
    val persistedGlobalReason = ReminderAutomationPrefs.getLastGlobalSkipReason(context)
    val reason = when (mode) {
        ReminderSheetMode.REMINDED -> "Reminder already sent"
        ReminderSheetMode.DUE_NOW -> "Reminder due now and pending send"
        ReminderSheetMode.NOT_REMINDED -> when {
            !autoPilotEnabled -> persistedGlobalReason ?: "Global auto reminder is OFF"
            customer.phone.isBlank() -> "Phone number missing"
            !perCustomerAutoEnabled -> persistedCustomerReason ?: "Customer auto reminder is OFF"
            pauseUntil > System.currentTimeMillis() -> persistedCustomerReason ?: "Paused until ${formatPauseUntil(pauseUntil)}"
            else -> persistedCustomerReason ?: "Pending auto schedule / manual reminder not sent"
        }
    }

    Surface(
        color = colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    Text(customer.phone.ifBlank { "No phone" }, color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text(
                    text = "₹${String.format(Locale.US, "%.0f", kotlin.math.abs(customer.balanceCache / 100.0))}",
                    color = amountColor,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(8.dp))
            val statusText = if (customer.autoReminderEnabled) "Auto reminder active" else "Auto reminder off"
            val statusColor = if (customer.autoReminderEnabled) Color(0xFF2E7D32) else colorScheme.error
            Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(reason, color = colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenCustomer,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Customer")
                }
                Button(
                    onClick = onRemindNow,
                    modifier = Modifier.weight(1f),
                    enabled = customer.phone.isNotBlank()
                ) {
                    Text("Remind Now")
                }
            }
        }
    }
}

@Composable
fun CustomerListItem(
    customer: Customer,
    onClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .safeClickable { onClick() }, // ✅ FIX: Use safeClickable to prevent UI stutter/multiple clicks
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            customer.name.take(1).uppercase(),
                            fontWeight = FontWeight.Black,
                            color = colorScheme.primary,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colorScheme.onSurface)
                    Text(customer.phone ?: "No Phone", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                }

                Column(horizontalAlignment = Alignment.End) {
                    val balance = customer.balanceCache / 100.0
                    val isDue = balance >= 0
                    val balanceColor = if (isDue) Color(0xFF2E7D32) else colorScheme.error
                    val label = if (isDue) "GET" else "GIVE"
                    Text(
                        label,
                        fontSize = 10.sp,
                        color = balanceColor.copy(alpha = 0.76f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "₹${String.format(Locale.US, "%.0f", Math.abs(balance))}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = balanceColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.size(32.dp).background(Color(0xFF25D366).copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp).background(colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
