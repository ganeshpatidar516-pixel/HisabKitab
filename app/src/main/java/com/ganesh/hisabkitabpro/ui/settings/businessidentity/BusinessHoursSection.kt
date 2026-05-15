package com.ganesh.hisabkitabpro.ui.settings.businessidentity

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ganesh.hisabkitabpro.domain.businessidentity.DaySlot
import com.ganesh.hisabkitabpro.domain.businessidentity.OperatingHoursCodec
import com.ganesh.hisabkitabpro.domain.businessidentity.WeeklyHoursV1

private enum class HoursMode { Plain, Weekly }

private val dayTitle = mapOf(
    "MON" to "Monday",
    "TUE" to "Tuesday",
    "WED" to "Wednesday",
    "THU" to "Thursday",
    "FRI" to "Friday",
    "SAT" to "Saturday",
    "SUN" to "Sunday",
)

@Composable
fun BusinessHoursSection(
    operatingHours: String,
    onOperatingHoursChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var mode by remember {
        mutableStateOf(
            if (OperatingHoursCodec.isStructuredJson(operatingHours)) HoursMode.Weekly else HoursMode.Plain,
        )
    }
    var schedule by remember {
        mutableStateOf(OperatingHoursCodec.parse(operatingHours) ?: OperatingHoursCodec.defaultWeekly())
    }
    var weeklyEditorExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        if (mode == HoursMode.Weekly) weeklyEditorExpanded = false
    }

    LaunchedEffect(operatingHours) {
        if (OperatingHoursCodec.isStructuredJson(operatingHours)) {
            mode = HoursMode.Weekly
            OperatingHoursCodec.parse(operatingHours)?.let { schedule = it }
        } else if (mode == HoursMode.Plain) {
            // Parent holds plain text; avoid fighting the user while typing.
        }
    }

    fun weeklyValid(): Boolean {
        for (slot in schedule.weekly) {
            if (slot.closed) continue
            if (!OperatingHoursCodec.isValidTimeSlot(slot.from, slot.to)) return false
        }
        return true
    }

    fun pushWeeklyIfValid() {
        if (!weeklyValid()) return
        onOperatingHoursChange(OperatingHoursCodec.toJson(schedule))
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Hours",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = mode == HoursMode.Plain,
                onClick = {
                    if (mode == HoursMode.Weekly) {
                        if (!weeklyValid()) {
                            Toast.makeText(
                                context,
                                "Fix invalid times before switching to plain text.",
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@FilterChip
                        }
                        onOperatingHoursChange(OperatingHoursCodec.formatWeeklyHuman(schedule))
                    }
                    mode = HoursMode.Plain
                },
                label = { Text("Plain") },
            )
            FilterChip(
                selected = mode == HoursMode.Weekly,
                onClick = {
                    if (mode == HoursMode.Plain) {
                        schedule = if (OperatingHoursCodec.isStructuredJson(operatingHours)) {
                            OperatingHoursCodec.parse(operatingHours) ?: OperatingHoursCodec.defaultWeekly()
                        } else {
                            OperatingHoursCodec.defaultWeekly()
                        }
                    }
                    mode = HoursMode.Weekly
                    pushWeeklyIfValid()
                },
                label = { Text("Weekly") },
            )
        }

        when (mode) {
            HoursMode.Plain -> {
                OutlinedTextField(
                    value = operatingHours,
                    onValueChange = onOperatingHoursChange,
                    label = { Text("Hours") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HoursMode.Weekly -> {
                Text(
                    OperatingHoursCodec.formatWeeklyAmPmSummary(schedule),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Editor uses 24h times.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!weeklyValid()) {
                    Text(
                        "Check open days: use HH:mm with end after start.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { weeklyEditorExpanded = !weeklyEditorExpanded }) {
                    Text(if (weeklyEditorExpanded) "Hide schedule" else "Edit schedule")
                }
                AnimatedVisibility(
                    visible = weeklyEditorExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OperatingHoursCodec.dayOrder.forEach { code ->
                            val slot = schedule.weekly.find { it.day == code }
                                ?: DaySlot(day = code, closed = true, from = "", to = "")
                            DayHoursRow(
                                title = dayTitle[code] ?: code,
                                slot = slot,
                                onChange = { updated ->
                                    val by = schedule.weekly.associateBy { it.day }.toMutableMap()
                                    by[code] = updated
                                    schedule = schedule.copy(
                                        weekly = OperatingHoursCodec.dayOrder.map { d ->
                                            by[d] ?: DaySlot(day = d, closed = true, from = "", to = "")
                                        },
                                    )
                                    pushWeeklyIfValid()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHoursRow(
    title: String,
    slot: DaySlot,
    onChange: (DaySlot) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, modifier = Modifier.widthIn(min = 88.dp), style = MaterialTheme.typography.bodyMedium)
        Checkbox(
            checked = !slot.closed,
            onCheckedChange = { open ->
                onChange(
                    slot.copy(
                        closed = !open,
                        from = if (!open) "" else slot.from.ifBlank { "09:00" },
                        to = if (!open) "" else slot.to.ifBlank { "21:00" },
                    ),
                )
            },
        )
        Text("Open", style = MaterialTheme.typography.bodySmall)
        if (!slot.closed) {
            OutlinedTextField(
                value = slot.from,
                onValueChange = { onChange(slot.copy(from = it)) },
                label = { Text("From") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = slot.to,
                onValueChange = { onChange(slot.copy(to = it)) },
                label = { Text("To") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
