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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ganesh.hisabkitabpro.data.local.StaffEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStaffScreen(
    onNavigateBack: () -> Unit,
    onSaveStaff: (StaffEntity) -> Unit,
    existingPhoneSet: Set<String> = emptySet()
) {
    var name by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STAFF") }
    var permissions by remember { mutableStateOf("VIEW_ONLY") }
    var salaryType by remember { mutableStateOf(StaffEntity.SALARY_TYPE_MONTHLY) }
    var salaryRupees by remember { mutableStateOf("") }
    var workdaysPerWeek by remember { mutableStateOf("6") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

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
                title = { Text("Add Staff Member", fontWeight = FontWeight.Bold) },
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
                onValueChange = {
                    name = it
                    formError = null
                },
                label = { Text("Staff Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = designation,
                onValueChange = { designation = it },
                label = { Text("Designation (e.g. Cashier, Helper)") },
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
                label = { Text("Address (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
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

            Button(
                onClick = {
                    val safeName = name.trim()
                    val safeDesignation = designation.trim()
                    val safeAddress = address.trim()
                    val safeNotes = notes.trim()
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

                    val staff = StaffEntity(
                        id = System.currentTimeMillis().toString(),
                        name = safeName,
                        phone = phone,
                        role = role,
                        permissions = permissions,
                        businessId = "default_business",
                        designation = safeDesignation,
                        salaryType = salaryType,
                        salaryAmountPaise = salaryPaise,
                        joiningDate = System.currentTimeMillis(),
                        workdaysPerWeek = workdays,
                        address = safeAddress,
                        notes = safeNotes
                    )
                    onSaveStaff(staff)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save Staff Member", modifier = Modifier.padding(8.dp))
            }
        }
    }
}
