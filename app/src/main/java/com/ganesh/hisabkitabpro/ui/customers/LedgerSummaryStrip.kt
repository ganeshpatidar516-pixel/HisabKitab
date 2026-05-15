package com.ganesh.hisabkitabpro.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat

private val Black = Color(0xFF000000)
private val Gold = Color(0xFFD4AF37)

/** Add-on strip at ledger scroll edge — existing bubbles unchanged. */
@Composable
fun LedgerSummaryStrip(
    customerName: String,
    balancePaise: Long,
    currencyFormatter: NumberFormat
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Black,
        border = BorderStroke(1.dp, Gold)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Summary · $customerName", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Balance due: ${currencyFormatter.format(balancePaise / 100.0)}",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
