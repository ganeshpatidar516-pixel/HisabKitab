package com.ganesh.hisabkitabpro.ui.customers

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.addon.reminder.ReminderAutomationPrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Customer ledger vs unified supplier party — separate prefs keys. */
enum class ReminderControlSubject {
    CUSTOMER,
    PARTY_SUPPLIER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderControlScreen(
    subjectId: Long,
    subjectName: String,
    subject: ReminderControlSubject = ReminderControlSubject.CUSTOMER,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember(subjectId, subject) {
        mutableStateOf(
            when (subject) {
                ReminderControlSubject.PARTY_SUPPLIER ->
                    ReminderAutomationPrefs.isSupplierPartyReminderEnabled(context, subjectId)
                ReminderControlSubject.CUSTOMER ->
                    ReminderAutomationPrefs.isCustomerReminderEnabled(context, subjectId)
            }
        )
    }
    var nextAutoAt by remember(subjectId, subject) {
        mutableStateOf(
            when (subject) {
                ReminderControlSubject.PARTY_SUPPLIER ->
                    ReminderAutomationPrefs.getSupplierPartyPauseUntil(context, subjectId)
                        .takeIf { it > System.currentTimeMillis() }
                        ?: (System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12))
                ReminderControlSubject.CUSTOMER ->
                    ReminderAutomationPrefs.getCustomerPauseUntil(context, subjectId)
                        .takeIf { it > System.currentTimeMillis() }
                        ?: (System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12))
            }
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextAutoAt)
    val dateTimeSdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateOnlySdf = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    fun applyQuickOffset(days: Int) {
        val next = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        when (subject) {
            ReminderControlSubject.PARTY_SUPPLIER ->
                ReminderAutomationPrefs.setSupplierPartyPauseUntil(context, subjectId, next)
            ReminderControlSubject.CUSTOMER ->
                ReminderAutomationPrefs.setCustomerPauseUntil(context, subjectId, next)
        }
        nextAutoAt = next
    }

    Scaffold(
        containerColor = Color(0xFFF7F9F8),
        topBar = {
            TopAppBar(
                title = { Text("Auto Reminder", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(subjectName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
            Spacer(Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCEBDD)),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text("Auto Reminder", fontWeight = FontWeight.Bold)
                            Text(if (isEnabled) "Running" else "Stopped", color = Color(0xFF546E7A), fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            when (subject) {
                                ReminderControlSubject.PARTY_SUPPLIER ->
                                    ReminderAutomationPrefs.setSupplierPartyReminderEnabled(context, subjectId, it)
                                ReminderControlSubject.CUSTOMER ->
                                    ReminderAutomationPrefs.setCustomerReminderEnabled(context, subjectId, it)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF2E7D32))
                        Text("Next Auto Reminder", fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                    }
                    Text(dateTimeSdf.format(Date(nextAutoAt)), color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = { applyQuickOffset(1) }, label = { Text("+1 day") })
                        FilterChip(selected = false, onClick = { applyQuickOffset(3) }, label = { Text("+3 days") })
                        FilterChip(selected = false, onClick = { applyQuickOffset(7) }, label = { Text("+7 days") })
                    }
                    Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Set Custom Date & Time")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reminder Flow", fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                    ReminderFlowRow("1st SMS/WhatsApp Reminder", "Adaptive channel by behavior")
                    ReminderFlowRow("2nd Follow-up", "Escalation + status tracking")
                    ReminderFlowRow("Payment Pending Alert", "Action prompt for call")
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F6F3))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFF546E7A))
                    Text(
                        "SMS may incur charges per your carrier plan.",
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFFDCEBDD))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Set Start Date", fontSize = 12.sp, color = Color(0xFF546E7A))
                    Text(dateOnlySdf.format(Date(nextAutoAt)), fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text("View Details")
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        val cal = Calendar.getInstance().apply { timeInMillis = selected }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                cal.set(Calendar.HOUR_OF_DAY, hour)
                                cal.set(Calendar.MINUTE, minute)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                val finalTime = cal.timeInMillis
                                when (subject) {
                                    ReminderControlSubject.PARTY_SUPPLIER ->
                                        ReminderAutomationPrefs.setSupplierPartyPauseUntil(context, subjectId, finalTime)
                                    ReminderControlSubject.CUSTOMER ->
                                        ReminderAutomationPrefs.setCustomerPauseUntil(context, subjectId, finalTime)
                                }
                                nextAutoAt = finalTime
                            },
                            9,
                            0,
                            false
                        ).show()
                    }
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ReminderFlowRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(Color(0xFFF3F7F5))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF37474F))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF90A4AE))
    }
}
