package com.ganesh.hisabkitabpro.ui.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import android.content.Context
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.domain.model.Party
import com.ganesh.hisabkitabpro.ui.viewmodel.PartyViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SupplierListScreen(
    viewModel: PartyViewModel,
    onSupplierClick: (Long) -> Unit,
    onAddSupplierClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val pagedSuppliers = viewModel.pagedParties.collectAsLazyPagingItems()
    val suppliers = pagedSuppliers.itemSnapshotList.items
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(suppliers, query, context) {
        suppliers.filter {
            val resolvedCity = supplierDisplayCity(context, it)
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.phone.contains(query, ignoreCase = true) ||
                resolvedCity.contains(query, ignoreCase = true)
        }
    }
    val totalPayablePaise by viewModel.activeSuppliersTotalBalancePaise.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.supplier_list_total_payable_label), color = colorScheme.error, fontSize = 12.sp)
                Text(
                    formatter.format(totalPayablePaise / 100.0),
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    color = colorScheme.error
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.setSearchQuery(it.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.supplier_list_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedLeadingIconColor = colorScheme.primary,
                        unfocusedLeadingIconColor = colorScheme.onSurfaceVariant
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered.size) { idx ->
                    val supplier = filtered[idx]
                    SupplierCard(
                        supplier = supplier,
                        formatter = formatter,
                        city = supplierDisplayCity(context, supplier),
                        onClick = { onSupplierClick(supplier.id) }
                    )
                }
                if (pagedSuppliers.loadState.append is androidx.paging.LoadState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddSupplierClick,
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Supplier")
        }
    }
}

@Composable
private fun SupplierCard(
    supplier: Party,
    formatter: NumberFormat,
    city: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = supplier.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(supplier.name, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    Text(
                        text = "${city.ifBlank { "Unknown city" }} • ${prettyLastTxnLabel(supplier.updatedAt)}",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Text(
                    formatter.format(kotlin.math.abs(supplier.totalBalance / 100.0)),
                    fontWeight = FontWeight.Black,
                    color = colorScheme.error
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.error)
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.supplier_given), color = colorScheme.error)
                }
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF388E3C))
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF388E3C))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.supplier_received), color = Color(0xFF388E3C))
                }
            }
        }
    }
}

private fun supplierDisplayCity(context: Context, party: Party): String {
    val fromRow = party.city?.trim().orEmpty()
    if (fromRow.isNotEmpty()) return fromRow
    return SupplierProfilePrefs.getCity(context, party.id)
}

private fun prettyLastTxnLabel(lastAt: Long): String {
    val days = ((System.currentTimeMillis() - lastAt) / (24L * 60L * 60L * 1000L)).toInt().coerceAtLeast(0)
    return when {
        days == 0 -> "Last transaction: today"
        days == 1 -> "Last transaction: 1 day ago"
        days < 30 -> "Last transaction: $days days ago"
        else -> {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            "Last transaction: ${sdf.format(java.util.Date(lastAt))}"
        }
    }
}
