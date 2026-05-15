@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.customers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.bulk.BulkReminderCoordinator
import com.ganesh.hisabkitabpro.addon.reminder.ReminderAutomationPrefs
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.local.ReminderCounterpartyKind
import com.ganesh.hisabkitabpro.data.local.ReminderEntity
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.ui.viewmodel.CustomerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private data class TimeSlot(val hour: Int, val minute: Int, val label: String)

private val defaultSlots = listOf(
    TimeSlot(14, 0, "2:00 PM"),
    TimeSlot(16, 0, "4:00 PM"),
    TimeSlot(17, 0, "5:00 PM")
)

private fun millisNextOccurrence(hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    if (cal.timeInMillis <= System.currentTimeMillis()) {
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return cal.timeInMillis
}

private suspend fun scheduleRemindersForCustomers(
    appContext: Context,
    customerIds: Iterable<Long>,
    scheduledAt: Long
): Pair<Int, Int> {
    val db = AppDatabase.getDatabase(appContext)
    val lc = AppLocaleManager.wrapContext(appContext)
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    var scheduled = 0
    var skipped = 0
    for (id in customerIds) {
        val cust = db.customerDao().getCustomerById(id) ?: continue
        if (cust.balanceCache <= 0L) {
            skipped++
            continue
        }
        ReminderAutomationPrefs.setCustomerReminderEnabled(appContext, id, true)
        db.reminderDao().deletePendingUnsentForCustomer(id)
        val amountStr = fmt.format(cust.balanceCache / 100.0)
        val message = lc.getString(R.string.reminder_auto_pending_message, cust.name, amountStr)
        db.reminderDao().insertReminder(
            ReminderEntity(
                customerId = id,
                counterpartyKind = ReminderCounterpartyKind.CUSTOMER,
                partyId = 0L,
                message = message,
                scheduledAt = scheduledAt,
                isSent = false,
                priority = "NORMAL",
                type = "PAYMENT",
                lastEscalationTier = 0
            )
        )
        scheduled++
    }
    return scheduled to skipped
}

@Composable
fun BulkReminderScheduleScreen(
    viewModel: CustomerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.bulkReminderDebtors.collectAsStateWithLifecycle()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var slotIndex by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val slots = defaultSlots
    val currencyFmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.bulk_reminder_title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bulk_reminder_run_pass)) },
                                onClick = {
                                    menuExpanded = false
                                    BulkReminderCoordinator.enqueueProcessAll(context)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.bulk_reminder_pass_toast),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                stringResource(R.string.bulk_reminder_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        selectedIds = customers.filter { it.balanceCache > 0L }.map { it.id }.toSet()
                    }
                ) { Text(stringResource(R.string.bulk_reminder_select_debtors)) }
                TextButton(onClick = { selectedIds = emptySet() }) {
                    Text(stringResource(R.string.bulk_reminder_clear))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.bulk_reminder_time_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                slots.forEachIndexed { index, s ->
                    FilterChip(
                        selected = slotIndex == index,
                        onClick = { slotIndex = index },
                        label = { Text(s.label) }
                    )
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(customers, key = { it.id }) { c ->
                    BulkCustomerRow(
                        customer = c,
                        currencyFormatter = currencyFmt,
                        checked = selectedIds.contains(c.id),
                        onToggle = { checked ->
                            selectedIds = if (checked) selectedIds + c.id else selectedIds - c.id
                        },
                        onCall = {
                            val phone = c.phone.trim()
                            if (phone.isBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.customer_no_phone_for_call),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
                                    )
                                }
                            }
                        }
                    )
                }
            }
            TextButton(
                onClick = {
                    if (selectedIds.isEmpty()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.bulk_reminder_none_selected),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TextButton
                    }
                    val slotNow = slots[slotIndex.coerceIn(0, slots.lastIndex)]
                    val at = millisNextOccurrence(slotNow.hour, slotNow.minute)
                    scope.launch(Dispatchers.IO) {
                        val (done, skip) = scheduleRemindersForCustomers(
                            context.applicationContext,
                            selectedIds,
                            at
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.bulk_reminder_scheduled_result, done, skip),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(stringResource(R.string.bulk_reminder_schedule), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BulkCustomerRow(
    customer: Customer,
    currencyFormatter: NumberFormat,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    onCall: () -> Unit
) {
    val due = customer.balanceCache > 0L
    val amountColor =
        if (due) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, fontWeight = FontWeight.SemiBold)
            Text(customer.phone, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            currencyFormatter.format(customer.balanceCache / 100.0),
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
        IconButton(onClick = onCall) {
            Icon(Icons.Default.Call, contentDescription = stringResource(R.string.common_call))
        }
    }
}
