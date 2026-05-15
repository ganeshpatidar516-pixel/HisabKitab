package com.ganesh.hisabkitabpro.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ganesh.hisabkitabpro.data.local.StaffEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffEditScreen(
    staff: StaffEntity,
    onNavigateBack: () -> Unit,
    onSave: (StaffEntity) -> Unit,
    onDelete: (String) -> Unit,
    existingPhoneSet: Set<String> = emptySet()
) {
    var name by remember(staff.id) { mutableStateOf(staff.name) }
    var designation by remember(staff.id) { mutableStateOf(staff.designation) }
    var phone by remember(staff.id) { mutableStateOf(staff.phone) }
    var role by remember(staff.id) { mutableStateOf(staff.role) }
    var permissions by remember(staff.id) { mutableStateOf(staff.permissions) }
    var isActive by remember(staff.id) { mutableStateOf(staff.isActive) }
    var salaryType by remember(staff.id) { mutableStateOf(staff.salaryType) }
    var salaryRupees by remember(staff.id) {
        mutableStateOf(
            if (staff.salaryAmountPaise > 0)
                "%.2f".format(staff.salaryAmountPaise / 100.0)
            else ""
        )
    }
    var workdaysPerWeek by remember(staff.id) { mutableStateOf(staff.workdaysPerWeek.toString()) }
    var address by remember(staff.id) { mutableStateOf(staff.address) }
    var notes by remember(staff.id) { mutableStateOf(staff.notes) }
    var formError by remember { mutableStateOf<String?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val roles = listOf("ADMIN", "MANAGER", "STAFF")
    val permissionLevels = listOf("FULL_ACCESS", "EDIT_ACCESS", "VIEW_ONLY")
    val salaryTypes = listOf(
        StaffEntity.SALARY_TYPE_MONTHLY,
        StaffEntity.SALARY_TYPE_DAILY,
        StaffEntity.SALARY_TYPE_WEEKLY
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Staff Member", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; formError = null },
                label = { Text("Staff Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = designation,
                onValueChange = { designation = it },
                label = { Text("Designation") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it.filter(Char::isDigit).take(10)
                    formError = null
                },
                label = { Text("Phone Number *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Active Status", fontWeight = FontWeight.Bold)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            Text("Role", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEach { r ->
                    FilterChip(
                        selected = role == r,
                        onClick = { role = r },
                        label = { Text(r) }
                    )
                }
            }

            Text("Permissions", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                permissionLevels.forEach { p ->
                    FilterChip(
                        selected = permissions == p,
                        onClick = { permissions = p },
                        label = { Text(p.replace("_", " ")) }
                    )
                }
            }

            Text("Salary Type", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                salaryTypes.forEach { t ->
                    FilterChip(
                        selected = salaryType == t,
                        onClick = { salaryType = t },
                        label = { Text(t) }
                    )
                }
            }

            OutlinedTextField(
                value = salaryRupees,
                onValueChange = {
                    salaryRupees = it.filter { ch -> ch.isDigit() || ch == '.' }
                    formError = null
                },
                label = { Text("Base Salary Amount (₹) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            if (salaryType == StaffEntity.SALARY_TYPE_WEEKLY) {
                OutlinedTextField(
                    value = workdaysPerWeek,
                    onValueChange = { workdaysPerWeek = it.filter(Char::isDigit).take(1) },
                    label = { Text("Workdays / Week (1-7)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            formError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.padding(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showArchiveConfirm = true
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Delete") }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val safeName = name.trim()
                        if (safeName.isBlank()) {
                            formError = "Staff name is required."
                            return@Button
                        }
                        if (phone.length != 10) {
                            formError = "Enter valid 10-digit phone number."
                            return@Button
                        }
                        if (existingPhoneSet.contains(phone)) {
                            formError = "This phone number already exists."
                            return@Button
                        }
                        val salaryPaise = StaffFormatting.parseRupeesToPaise(salaryRupees) ?: 0L
                        if (salaryPaise <= 0L) {
                            formError = "Enter a valid base salary amount."
                            return@Button
                        }
                        val workdays = workdaysPerWeek.toIntOrNull()?.coerceIn(1, 7) ?: 6

                        onSave(
                            staff.copy(
                                name = safeName,
                                designation = designation.trim(),
                                phone = phone,
                                role = role,
                                permissions = permissions,
                                isActive = isActive,
                                salaryType = salaryType,
                                salaryAmountPaise = salaryPaise,
                                workdaysPerWeek = workdays,
                                address = address.trim(),
                                notes = notes.trim()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Save") }
            }
        }
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Delete ${staff.name}?") },
            text = {
                Text(
                    "This permanently removes the staff record, attendance, payroll entries, " +
                        "and cloud access rights. This cannot be undone."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showArchiveConfirm = false
                    onDelete(staff.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
