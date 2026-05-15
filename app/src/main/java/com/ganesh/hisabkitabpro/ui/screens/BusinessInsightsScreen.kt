package com.ganesh.hisabkitabpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganesh.hisabkitabpro.domain.bi.GrowthForecastEngine
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessInsightsScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    val predictedSales = remember(recentTransactions) {
        GrowthForecastEngine.forecastNextMonthSales(recentTransactions)
    }
    val receivable = dashboardState.totalUdhaar
    val payable = dashboardState.totalSupplierPayable
    val netExposure = dashboardState.netExposure
    val colorScheme = MaterialTheme.colorScheme
    val nextMonthCashflowProjection = remember(predictedSales, payable) { predictedSales - payable }
    val agingRiskLabel = remember(payable, receivable) {
        when {
            payable <= 0.0 -> "Low risk"
            payable > receivable -> "High risk"
            payable > receivable * 0.6 -> "Moderate risk"
            else -> "Controlled"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Intelligence", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .background(colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text("Growth Prediction", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Estimated sales next month:", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                    Text(
                        "₹${String.format(Locale.US, "%.2f", predictedSales)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("AI Insights", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    dashboardState.advice.forEach { adviceText ->
                        Text("• $adviceText", fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Unified Party Exposure", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Customer Receivable: ₹${String.format(Locale.US, "%.2f", receivable)}")
                    Text("Supplier Payable: ₹${String.format(Locale.US, "%.2f", payable)}")
                    Text(
                        "Net Exposure (Receivable + Payable - Received): ₹${String.format(Locale.US, "%.2f", netExposure)}",
                        color = if (netExposure > 0) colorScheme.error else colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cashflow Forecast", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Projected inflow next month: ₹${String.format(Locale.US, "%.2f", predictedSales)}")
                    Text("Projected supplier outflow: ₹${String.format(Locale.US, "%.2f", payable)}")
                    Text(
                        "Estimated net cashflow: ₹${String.format(Locale.US, "%.2f", nextMonthCashflowProjection)}",
                        color = if (nextMonthCashflowProjection >= 0) colorScheme.tertiary else colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Supplier Aging Snapshot", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Aging risk based on current payable mix: $agingRiskLabel")
                    Text(
                        "Use supplier ledger Credit Terms to automate due dates and run follow-up reminders.",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
