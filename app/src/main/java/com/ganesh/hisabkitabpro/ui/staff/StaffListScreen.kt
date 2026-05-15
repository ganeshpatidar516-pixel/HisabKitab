package com.ganesh.hisabkitabpro.ui.staff

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.data.local.StaffEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StaffListScreen(
    staffList: List<StaffEntity>,
    onAddStaffClick: () -> Unit,
    onStaffClick: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    archivedStaff: List<StaffEntity> = emptyList(),
    onRestoreStaff: (String) -> Unit = {},
    onDeleteStaff: (String) -> Unit = {}
) {
    var search by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("ALL") }
    var showArchived by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<StaffEntity?>(null) }
    val haptic = LocalHapticFeedback.current
    val roleOptions = listOf("ALL", "ADMIN", "MANAGER", "STAFF")

    val workingList = if (showArchived) archivedStaff else staffList

    val filteredStaff = remember(workingList, search, roleFilter) {
        val q = search.trim().lowercase()
        workingList.filter { staff ->
            val byRole = roleFilter == "ALL" ||
                staff.role.equals(roleFilter, ignoreCase = true)
            val byQuery = q.isBlank() ||
                staff.name.lowercase().contains(q) ||
                staff.phone.contains(q) ||
                staff.designation.lowercase().contains(q)
            byRole && byQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showArchived) {
                FloatingActionButton(onClick = onAddStaffClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add Staff")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by name, phone or designation") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showArchived,
                    onClick = { showArchived = false },
                    label = { Text("Active (${staffList.size})") }
                )
                FilterChip(
                    selected = showArchived,
                    onClick = { showArchived = true },
                    label = { Text("Archived (${archivedStaff.size})") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roleOptions.forEach { role ->
                    FilterChip(
                        selected = roleFilter == role,
                        onClick = { roleFilter = role },
                        label = { Text(role) }
                    )
                }
            }

            if (filteredStaff.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (showArchived) "No archived staff." else "No matching staff found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredStaff,
                        key = { it.id }
                    ) { staff ->
                        StaffItem(
                            staff = staff,
                            archived = showArchived,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (showArchived) onRestoreStaff(staff.id) else onStaffClick(staff.id)
                            },
                            onLongDelete = {
                                if (!showArchived) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pendingDelete = staff
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { staff ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${staff.name}?") },
            text = {
                Text(
                    "This permanently removes the staff record, attendance, payroll entries, " +
                        "and cloud access rights. This cannot be undone."
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingDelete = null
                    onDeleteStaff(staff.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun StaffItem(
    staff: StaffEntity,
    archived: Boolean,
    onClick: () -> Unit,
    onLongDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongDelete
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (archived)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = if (archived)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(staff.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val sub = listOfNotNull(
                        staff.designation.takeIf { it.isNotBlank() },
                        staff.role.takeIf { it.isNotBlank() }
                    ).joinToString(separator = " • ")
                    if (sub.isNotBlank()) {
                        Text(
                            sub,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (staff.salaryAmountPaise > 0) {
                        Text(
                            "${StaffFormatting.formatPaise(staff.salaryAmountPaise)} / ${
                                staff.salaryType.lowercase()
                            }",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            val statusLabel = when {
                archived -> "Tap to restore"
                staff.isActive -> "Active"
                else -> "Inactive"
            }
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(statusLabel) },
                colors = AssistChipDefaults.assistChipColors(
                    disabledLabelColor = when {
                        archived -> MaterialTheme.colorScheme.onTertiaryContainer
                        staff.isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    disabledContainerColor = when {
                        archived -> MaterialTheme.colorScheme.tertiaryContainer
                        staff.isActive -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            )
        }
    }

    Spacer(Modifier.height(2.dp))
}
