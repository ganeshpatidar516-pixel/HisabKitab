package com.ganesh.hisabkitabpro.ui.customers

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAnalyticsScreen(
    customer: Customer,
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Analytics", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Insights for ${customer.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Risk Score", fontWeight = FontWeight.Bold)
                    val riskColor = when {
                        customer.riskScore < 30 -> Color.Green
                        customer.riskScore < 70 -> Color.Yellow
                        else -> Color.Red
                    }
                    LinearProgressIndicator(
                        progress = { customer.riskScore / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = riskColor
                    )
                    Text(
                        text = "Score: ${customer.riskScore}%",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Payment Pattern", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Balance: ₹${customer.balanceCache / 100.0}")
                    customer.lastTransactionDate?.let {
                        Text("Last Transaction: ${java.text.SimpleDateFormat("dd MMM yyyy").format(java.util.Date(it))}")
                    }
                }
            }
        }
    }
}
