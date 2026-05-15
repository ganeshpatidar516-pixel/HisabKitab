package com.ganesh.hisabkitabpro.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.domain.ledger.TransactionFilterEngine
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * HISABKITAB PRO - TRANSACTION HISTORY
 * Optimized for 100% Stability and Zero-Lag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    // ✅ FIXED: Explicit type to ensure compiler stability
    val transactions: List<Transaction> by viewModel.allTransactions.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    
    var selectedFilter by remember { mutableStateOf("All") }

    val displayTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "Today" -> TransactionFilterEngine.filterToday(transactions)
            "Week" -> TransactionFilterEngine.filterThisWeek(transactions)
            "Month" -> TransactionFilterEngine.filterThisMonth(transactions)
            "Recycle Bin" -> transactions.filter { it.isDeleted }
            else -> transactions.filter { !it.isDeleted }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedFilter == "Recycle Bin") "Recycle Bin" else "History", fontWeight = FontWeight.Bold) },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("All") }, onClick = { selectedFilter = "All"; showMenu = false })
                        DropdownMenuItem(text = { Text("Today") }, onClick = { selectedFilter = "Today"; showMenu = false })
                        DropdownMenuItem(text = { Text("Recycle Bin") }, onClick = { selectedFilter = "Recycle Bin"; showMenu = false })
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedFilter != "Recycle Bin") {
                FloatingActionButton(onClick = onAddTransactionClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        if (displayTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No transactions found", color = colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayTransactions, key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        isDeletedView = selectedFilter == "Recycle Bin",
                        onDeleteClick = { viewModel.deleteTransaction(transaction.customerId, transaction.id) },
                        onRestoreClick = { viewModel.restoreTransaction(transaction.id, transaction.customerId) },
                        onTransactionClick = onTransactionClick
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction, 
    isDeletedView: Boolean,
    onDeleteClick: () -> Unit, 
    onRestoreClick: () -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = sdf.format(Date(transaction.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth().clickable { if (!isDeletedView) onTransactionClick(transaction.id) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "ID: ${transaction.id}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = dateStr, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val isCredit = transaction.type == TransactionType.CREDIT || transaction.type == TransactionType.INVOICE
                Text(
                    text = "₹${transaction.amount / 100.0}",
                    color = if (isCredit) colorScheme.error else colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (isDeletedView) {
                    IconButton(onClick = onRestoreClick) {
                        Icon(
                            Icons.Default.RestoreFromTrash,
                            contentDescription = "Restore",
                            tint = colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
