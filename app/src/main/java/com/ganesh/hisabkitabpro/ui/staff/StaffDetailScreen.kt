package com.ganesh.hisabkitabpro.ui.staff

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.data.local.StaffEntity
import com.ganesh.hisabkitabpro.data.local.StaffPayrollEntryEntity
import com.ganesh.hisabkitabpro.domain.payroll.StaffPayrollEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailScreen(
    staff: StaffEntity,
    payrollEntries: List<StaffPayrollEntryEntity>,
    payroll: StaffPayrollEngine.PayrollResult?,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    onMarkAttendance: () -> Unit,
    onAddPayrollEntry: (kind: String, amountPaise: Long, note: String) -> Unit,
    onRemovePayrollEntry: (Long) -> Unit,
    onGenerateSlip: suspend () -> String?,
    onArchive: () -> Unit,
    onRestore: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf<String?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(staff.name, fontWeight = FontWeight.Bold)
                        AnimatedVisibility(visible = staff.isDeleted == 1, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                "Archived",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEdit()
                        },
                        enabled = staff.isDeleted == 0
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (staff.isDeleted == 0) {
                            DropdownMenuItem(
                                text = { Text("Delete staff") },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showArchiveConfirm = true
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Restore staff") },
                                leadingIcon = {
                                    Icon(Icons.Default.Restore, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRestore()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Staff restored.")
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StaffHeaderCard(staff)
            }

            item {
                PayrollSummaryCard(payroll)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onMarkAttendance,
                        enabled = staff.isDeleted == 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null)
                        Text("Attendance", modifier = Modifier.padding(start = 6.dp))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val path = onGenerateSlip()
                                val msg = if (path != null) "Salary slip ready — opening share sheet." else "Could not generate salary slip."
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Text("Salary Slip", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            item {
                Text(
                    "Quick Actions",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val canEdit = staff.isDeleted == 0
                    AssistChip(
                        onClick = { if (canEdit) showAddDialog = StaffPayrollEntryEntity.KIND_ADVANCE },
                        label = { Text("+ Advance") },
                        enabled = canEdit,
                        colors = AssistChipDefaults.assistChipColors()
                    )
                    AssistChip(
                        onClick = { if (canEdit) showAddDialog = StaffPayrollEntryEntity.KIND_BONUS },
                        label = { Text("+ Bonus") },
                        enabled = canEdit
                    )
                    AssistChip(
                        onClick = { if (canEdit) showAddDialog = StaffPayrollEntryEntity.KIND_SALARY_PAID },
                        label = { Text("+ Salary Paid") },
                        enabled = canEdit
                    )
                    AssistChip(
                        onClick = { if (canEdit) showAddDialog = StaffPayrollEntryEntity.KIND_DEDUCTION },
                        label = { Text("+ Deduction") },
                        enabled = canEdit
                    )
                }
            }

            item {
                Text(
                    "Passbook",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (payrollEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No payroll entries yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(payrollEntries, key = { it.id }) { entry ->
                    PayrollEntryRow(
                        entry = entry,
                        onRemove = {
                            onRemovePayrollEntry(entry.id)
                            scope.launch {
                                val r = snackbarHostState.showSnackbar(
                                    message = "Entry removed.",
                                    actionLabel = "OK"
                                )
                                if (r == SnackbarResult.ActionPerformed) Unit
                            }
                        }
                    )
                }
            }
        }
    }

    showAddDialog?.let { kind ->
        PayrollEntryDialog(
            kind = kind,
            onDismiss = { showAddDialog = null },
            onSubmit = { amountPaise, note ->
                onAddPayrollEntry(kind, amountPaise, note)
                showAddDialog = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "${kind.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() }} recorded."
                    )
                }
            }
        )
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Delete staff?") },
            text = {
                Text(
                    "This permanently removes the staff record, attendance, payroll entries, " +
                        "and cloud access rights. This cannot be undone."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showArchiveConfirm = false
                    onArchive()
                    scope.launch {
                        snackbarHostState.showSnackbar("Delete requested.")
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StaffHeaderCard(staff: StaffEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WorkOutline, contentDescription = null)
                Text(
                    staff.designation.ifBlank { staff.role },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                "Phone: ${staff.phone.ifBlank { "—" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Text(
                "Joined: ${StaffFormatting.formatDate(staff.joiningDate)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Text(
                "Base Pay: ${StaffFormatting.formatPaise(staff.salaryAmountPaise)} / ${
                    staff.salaryType.lowercase()
                }",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            if (staff.address.isNotBlank()) {
                Text(
                    staff.address,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PayrollSummaryCard(payroll: StaffPayrollEngine.PayrollResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Current Cycle",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            if (payroll == null) {
                Text(
                    "No data — mark attendance to compute.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                return@Column
            }
            SummaryRow(
                label = "Effective Days",
                value = "${StaffFormatting.formatDays(payroll.effectiveDays)} / ${payroll.totalDays}"
            )
            SummaryRow(
                label = "Earned",
                value = StaffFormatting.formatPaise(payroll.earnedPaise),
                emphasized = true
            )
            if (payroll.lossOfPayPaise > 0) {
                SummaryRow(
                    label = "Loss of Pay",
                    value = "- ${StaffFormatting.formatPaise(payroll.lossOfPayPaise)}"
                )
            }
            SummaryRow(label = "Bonuses", value = "+ ${StaffFormatting.formatPaise(payroll.bonusesPaise)}")
            SummaryRow(label = "Advances", value = "- ${StaffFormatting.formatPaise(payroll.advancesPaise)}")
            if (payroll.deductionsPaise > 0) {
                SummaryRow(
                    label = "Deductions",
                    value = "- ${StaffFormatting.formatPaise(payroll.deductionsPaise)}"
                )
            }
            SummaryRow(label = "Paid", value = "- ${StaffFormatting.formatPaise(payroll.salaryPaidPaise)}")
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Net Payable",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    StaffFormatting.formatPaise(payroll.netPayablePaise),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            value,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PayrollEntryRow(
    entry: StaffPayrollEntryEntity,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.kind.replace("_", " "), fontWeight = FontWeight.SemiBold)
                Text(
                    StaffFormatting.formatDate(entry.dateMillis),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        entry.note,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                StaffFormatting.formatPaise(entry.amountPaise),
                fontWeight = FontWeight.Bold,
                color = when (entry.kind) {
                    StaffPayrollEntryEntity.KIND_BONUS -> MaterialTheme.colorScheme.primary
                    StaffPayrollEntryEntity.KIND_ADVANCE,
                    StaffPayrollEntryEntity.KIND_DEDUCTION,
                    StaffPayrollEntryEntity.KIND_SALARY_PAID -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PayrollEntryDialog(
    kind: String,
    onDismiss: () -> Unit,
    onSubmit: (amountPaise: Long, note: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${kind.replace("_", " ")}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { ch -> ch.isDigit() || ch == '.' }
                        error = null
                    },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val paise = StaffFormatting.parseRupeesToPaise(amount)
                if (paise == null || paise <= 0L) {
                    error = "Enter a valid amount"
                    return@Button
                }
                onSubmit(paise, note.trim())
            }) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Filter chip helper used by attendance screen — shared here to avoid duplicate definitions. */
@Composable
internal fun StatusFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
