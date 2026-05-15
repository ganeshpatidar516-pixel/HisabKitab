package com.ganesh.hisabkitabpro.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit
) {
    // ✅ FIXED: Using direct state collection with null safety
    val transactionFlow = remember(transactionId) { viewModel.getTransactionById(transactionId) }
    val transaction by transactionFlow.collectAsState(initial = null)
    val sdf = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()) }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(colorScheme.background)) {
            transaction?.let { tx ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isReceived = tx.type == TransactionType.DEBIT
                    Text(
                        text = "₹${tx.amount / 100.0}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isReceived) colorScheme.tertiary else colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    DetailItem("Customer ID", tx.customerId.toString())
                    DetailItem("Date", sdf.format(Date(tx.createdAt)))
                    DetailItem("Note", tx.note ?: "No note")

                    Spacer(modifier = Modifier.weight(1f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                            Text("Edit")
                        }
                        Button(
                            onClick = { 
                                viewModel.deleteTransaction(tx.customerId, tx.id)
                                onNavigateBack()
                            }, 
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.error,
                                contentColor = colorScheme.onError
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
