package com.ganesh.hisabkitabpro.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.data.local.StaffAttendanceEntity
import com.ganesh.hisabkitabpro.domain.payroll.AttendanceDayKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceScreen(
    staffName: String,
    periodStartMillis: Long,
    attendance: List<StaffAttendanceEntity>,
    onNavigateBack: () -> Unit,
    onChangePeriodStart: (Long) -> Unit,
    onMark: (epochMillis: Long, status: String) -> Unit,
    onClear: (epochMillis: Long) -> Unit
) {
    val cal = remember(periodStartMillis) {
        Calendar.getInstance().apply { timeInMillis = periodStartMillis }
    }
    val monthLabel = remember(periodStartMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }
    val totalDays = remember(periodStartMillis) {
        AttendanceDayKey.daysInMonth(periodStartMillis)
    }
    val today = remember { AttendanceDayKey.startOfDay(System.currentTimeMillis()) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val attendanceByDay = remember(attendance) {
        attendance.associateBy { it.dateMillis }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Attendance", fontWeight = FontWeight.Bold)
                        Text(staffName, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {
                    val prev = Calendar.getInstance().apply {
                        timeInMillis = periodStartMillis
                        add(Calendar.MONTH, -1)
                    }
                    onChangePeriodStart(prev.timeInMillis)
                }) { Text("‹ Prev") }
                Text(monthLabel, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = {
                    val next = Calendar.getInstance().apply {
                        timeInMillis = periodStartMillis
                        add(Calendar.MONTH, 1)
                    }
                    onChangePeriodStart(next.timeInMillis)
                }) { Text("Next ›") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "PRESENT", "ABSENT", "HALF_DAY", "LEAVE").forEach { f ->
                    StatusFilterChip(
                        selected = selectedFilter == f,
                        label = f.replace('_', ' '),
                        onClick = { selectedFilter = f }
                    )
                }
            }

            // Build the day grid (one row per day in the month)
            val days = remember(periodStartMillis, totalDays) {
                (0 until totalDays).map { offset ->
                    val c = Calendar.getInstance().apply {
                        timeInMillis = periodStartMillis
                        add(Calendar.DAY_OF_MONTH, offset)
                    }
                    AttendanceDayKey.startOfDay(c.timeInMillis)
                }
            }

            val visibleDays = remember(days, attendanceByDay, selectedFilter) {
                if (selectedFilter == "ALL") days
                else days.filter { d -> attendanceByDay[d]?.status == selectedFilter }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(visibleDays, key = { it }) { dayMillis ->
                    DayRow(
                        dayMillis = dayMillis,
                        record = attendanceByDay[dayMillis],
                        isToday = dayMillis == today,
                        onMark = { status -> onMark(dayMillis, status) },
                        onClear = { onClear(dayMillis) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    dayMillis: Long,
    record: StaffAttendanceEntity?,
    isToday: Boolean,
    onMark: (String) -> Unit,
    onClear: () -> Unit
) {
    val container = if (isToday)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                        .format(java.util.Date(dayMillis)),
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    record?.status?.replace("_", " ") ?: "—",
                    color = when (record?.status) {
                        StaffAttendanceEntity.STATUS_PRESENT -> MaterialTheme.colorScheme.primary
                        StaffAttendanceEntity.STATUS_HALF_DAY -> MaterialTheme.colorScheme.tertiary
                        StaffAttendanceEntity.STATUS_ABSENT -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionButton(
                    label = "P",
                    selected = record?.status == StaffAttendanceEntity.STATUS_PRESENT,
                    onClick = { onMark(StaffAttendanceEntity.STATUS_PRESENT) }
                )
                ActionButton(
                    label = "A",
                    selected = record?.status == StaffAttendanceEntity.STATUS_ABSENT,
                    onClick = { onMark(StaffAttendanceEntity.STATUS_ABSENT) }
                )
                ActionButton(
                    label = "HD",
                    selected = record?.status == StaffAttendanceEntity.STATUS_HALF_DAY,
                    onClick = { onMark(StaffAttendanceEntity.STATUS_HALF_DAY) }
                )
                ActionButton(
                    label = "L",
                    selected = record?.status == StaffAttendanceEntity.STATUS_LEAVE,
                    onClick = { onMark(StaffAttendanceEntity.STATUS_LEAVE) }
                )
                if (record != null) {
                    OutlinedButton(onClick = onClear) { Text("Clear") }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}
