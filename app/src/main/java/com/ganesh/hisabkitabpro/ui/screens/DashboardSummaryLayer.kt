package com.ganesh.hisabkitabpro.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.ui.viewmodel.DashboardState

/**
 * Home snapshot: same numbers as **Mere Grahak** — `SUM(balanceCache)` via
 * [DashboardState.overallCustomerNetBalancePaise], so it tracks bills/payments instantly.
 */
@Composable
fun DashboardSummaryLayer(state: DashboardState) {
    val colorScheme = MaterialTheme.colorScheme
    val paise = state.overallCustomerNetBalancePaise
    val netRupees = paise / 100.0
    val shouldReceive = netRupees >= 0
    val amountColor = if (shouldReceive) Color(0xFF2E7D32) else colorScheme.error
    val balanceLabel = if (shouldReceive) "GET" else "GIVE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .border(1.dp, colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overall Customer Net Balance",
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${String.format("%.0f", kotlin.math.abs(netRupees))}",
                    color = amountColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "$balanceLabel  •  Total: ${state.totalCustomers}",
                    color = amountColor.copy(alpha = 0.78f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
