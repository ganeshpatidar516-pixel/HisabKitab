package com.ganesh.hisabkitabpro.ui.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppSender
import com.ganesh.hisabkitabpro.ui.viewmodel.CustomerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    customer: Customer,
    customerViewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onLedgerClick: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    val context = LocalContext.current
    val businessProfile by produceState<BusinessProfile?>(null) {
        value = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context.applicationContext).businessProfileDao().getBusinessProfileOnce()
        }
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete customer?") },
            text = { Text("Permanently remove ${customer.name}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    customerViewModel.deleteCustomer(customer.id)
                    showDeleteDialog = false
                    onNavigateBack()
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(customer.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(customer.phone ?: "", fontSize = 16.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { 
                    val intent = Intent(Intent.ACTION_DIAL, "tel:${customer.phone}".toUri())
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Call")
                }
                OutlinedButton(onClick = { 
                    val sent = WhatsAppSender.sendPaymentReminder(
                        context,
                        customer.phone,
                        customer.balanceCache / 100.0,
                        businessProfile,
                    )
                    if (sent) {
                        customerViewModel.markCustomerReminderSent(customer.id)
                    }
                }) {
                    Text("WhatsApp")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLedgerClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Ledger")
                }
                
                Button(
                    onClick = onAnalyticsClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Analytics")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Financial Info Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Financial Summary", fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    InfoRow("Credit limit", "₹${customer.creditLimit / 100.0}")
                    InfoRow("Tags", customer.tags?.joinToString(", ") ?: "None")
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Balance", fontWeight = FontWeight.Bold)
                        val color = if (customer.balanceCache >= 0) Color(0xFF4CAF50) else Color(0xFFE91E63)
                        Text("₹${customer.balanceCache / 100.0}", fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
