package com.ganesh.hisabkitabpro.ui.customers

import android.content.ClipData
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganesh.hisabkitabpro.domain.ledger.LedgerPdfGenerator
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.ui.viewmodel.SettingsViewModel
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import com.ganesh.hisabkitabpro.util.WhatsAppBillSender
import com.ganesh.hisabkitabpro.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * HISABKITAB PRO - CUSTOMER STATEMENT (ULTIMATE ACTION ENGINE)
 * Fixed type inference and missing properties.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerStatementScreen(
    customer: Customer,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ✅ FIXED: Explicit type and initial value to prevent compilation freeze
    val transactions: List<Transaction> by transactionViewModel.allTransactions.collectAsState(initial = emptyList())
    val businessProfile: BusinessProfile? by settingsViewModel.businessProfile.collectAsStateWithLifecycle(initialValue = null)
    
    val customerTransactions = remember(transactions, customer.id) {
        transactions.filter { it.customerId == customer.id && !it.isDeleted }
            .sortedByDescending { it.createdAt }
    }
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dayKeyFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val filteredTransactions = remember(customerTransactions, startDate, endDate) {
        customerTransactions.filter { 
            val date = it.createdAt
            (startDate == null || date >= startDate!!) && (endDate == null || date <= endDate!!)
        }
    }
    val openingBalancePaise = remember(customerTransactions, startDate) {
        if (startDate == null) {
            0L
        } else {
            customerTransactions
                .filter { it.createdAt < startDate!! }
                .sumOf { if (it.isGiveEntry()) it.amount else -it.amount }
        }
    }
    val periodNetPaise = remember(filteredTransactions) {
        filteredTransactions.sumOf { if (it.isGiveEntry()) it.amount else -it.amount }
    }
    val closingBalancePaise = openingBalancePaise + periodNetPaise
    val ledgerItems = remember(filteredTransactions, dayKeyFormatter, sdf, openingBalancePaise) {
        val chronological = filteredTransactions.sortedBy { it.createdAt }
        var runningBalancePaise = openingBalancePaise
        val rows = mutableListOf<StatementListRow>()
        chronological.forEach { tx ->
            val dateLabel = sdf.format(Date(tx.createdAt))
            val dateKey = dayKeyFormatter.format(Date(tx.createdAt))
            val lastDateKey = (rows.lastOrNull() as? StatementListRow.Item)?.dateKey
            if (lastDateKey != dateKey) {
                rows.add(StatementListRow.Header(dateLabel = dateLabel, dateKey = dateKey))
            }
            val delta = if (tx.isGiveEntry()) tx.amount else -tx.amount
            runningBalancePaise += delta
            rows.add(
                StatementListRow.Item(
                    ledgerItem = StatementLedgerItem(tx = tx, runningBalancePaise = runningBalancePaise),
                    dateKey = dateKey
                )
            )
        }
        rows.reversed()
    }
    val currentDayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val thirtyDayStart = remember { currentDayStart - 29L * 24L * 60L * 60L * 1000L }
    val startPickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val endPickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Account statement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val generatedFile = LedgerPdfGenerator.generateLedgerPdf(
                                    context = context,
                                    customer = customer,
                                    transactions = customerTransactions,
                                    businessProfile = businessProfile,
                                    startDate = startDate,
                                    endDate = endDate
                                )
                                val file = generatedFile?.let {
                                    val desiredName = buildStatementFileName(customer.name, startDate, endDate)
                                    if (it.name == desiredName) {
                                        it
                                    } else {
                                        val renamed = File(it.parentFile ?: context.cacheDir, desiredName)
                                        if (renamed.exists()) {
                                            renamed.delete()
                                        }
                                        it.copyTo(renamed, overwrite = true)
                                        renamed
                                    }
                                }
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Statement - ${customer.name}")
                                        putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)
                                        clipData = ClipData.newRawUri("application/pdf", uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = Intent.createChooser(
                                        send,
                                        "Share ${file.nameWithoutExtension}"
                                    ).apply {
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    WhatsAppBillSender.startShareActivity(context, chooser)
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("CustomerStatement", "PDF generation failed", e)
                            }
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = startDate == null && endDate == null,
                    onClick = {
                        startDate = null
                        endDate = null
                    },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = startDate == currentDayStart,
                    onClick = {
                        startDate = currentDayStart
                        endDate = null
                    },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = startDate == thirtyDayStart,
                    onClick = {
                        startDate = thirtyDayStart
                        endDate = null
                    },
                    label = { Text("30 Days") }
                )
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("From: ${startDate?.let { sdf.format(Date(it)) } ?: "Start"}")
                }
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("To: ${endDate?.let { sdf.format(Date(it)) } ?: "Now"}")
                }
            }
            Text(
                text = "Range: ${startDate?.let { sdf.format(Date(it)) } ?: "Start"} - ${endDate?.let { sdf.format(Date(it)) } ?: "Now"}",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                fontSize = 12.sp,
                color = Color(0xFF546E7A)
            )
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalGave = filteredTransactions.filter { it.isGiveEntry() }.sumOf { it.amount } / 100.0
                val totalGot = filteredTransactions.filter { it.isReceiveEntry() }.sumOf { it.amount } / 100.0
                
                SummaryCard("GAVE", totalGave, PremiumGiveRed, Modifier.weight(1f))
                SummaryCard("GOT", totalGot, PremiumReceiveGreen, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Opening: ${currencyFormatter.format(openingBalancePaise / 100.0)}",
                    fontSize = 12.sp,
                    color = Color(0xFF455A64),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Closing: ${currencyFormatter.format(closingBalancePaise / 100.0)}",
                    fontSize = 12.sp,
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Balance Due: ${currencyFormatter.format(periodNetPaise / 100.0)}",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.End,
                fontSize = 12.sp,
                color = Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (ledgerItems.isEmpty()) {
                    item("empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions in selected range", color = Color.Gray)
                        }
                    }
                }
                items(
                    items = ledgerItems,
                    key = { row ->
                        when (row) {
                            is StatementListRow.Header -> "header_${row.dateKey}"
                            is StatementListRow.Item -> "item_${row.ledgerItem.tx.id}"
                        }
                    }
                ) { row ->
                    when (row) {
                        is StatementListRow.Header -> StatementDateHeader(row.dateLabel)
                        is StatementListRow.Item -> StatementItem(row.ledgerItem, currencyFormatter)
                    }
                }
            }
        }

        if (showStartPicker) {
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            startDate = startPickerState.selectedDateMillis?.let { normalizeStartOfDay(it) }
                            if (endDate != null && startDate != null && endDate!! < startDate!!) {
                                endDate = null
                            }
                            showStartPicker = false
                        }
                    ) { Text("Apply") }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                startDate = null
                                showStartPicker = false
                            }
                        ) { Text("Clear") }
                        TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
                    }
                }
            ) {
                DatePicker(state = startPickerState)
            }
        }

        if (showEndPicker) {
            DatePickerDialog(
                onDismissRequest = { showEndPicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            endDate = endPickerState.selectedDateMillis?.let { normalizeEndOfDay(it) }
                            showEndPicker = false
                        }
                    ) { Text("Apply") }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                endDate = null
                                showEndPicker = false
                            }
                        ) { Text("Clear") }
                        TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
                    }
                }
            ) {
                DatePicker(state = endPickerState)
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, amount: Double, color: Color, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp)
            Text("₹${String.format("%.2f", amount)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun StatementItem(item: StatementLedgerItem, formatter: NumberFormat) {
    val dateSdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val tx = item.tx
    val isCredit = tx.isGiveEntry()
    
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dateSdf.format(Date(tx.createdAt)), fontSize = 11.sp, color = Color.Gray)
                Surface(
                    color = if (isCredit) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isCredit) "Given" else "Received",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCredit) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    )
                }
                Text(tx.note?.takeIf { it.isNotBlank() } ?: tx.type.name, fontWeight = FontWeight.Medium)
                Text(
                    "Running: ${formatter.format(item.runningBalancePaise / 100.0)}",
                    fontSize = 11.sp,
                    color = Color(0xFF546E7A)
                )
            }
            Text(
                text = formatter.format(tx.amount / 100.0),
                fontWeight = FontWeight.Bold,
                color = if (isCredit) Color.Red else Color.Green
            )
        }
    }
}

@Composable
private fun StatementDateHeader(dateLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF90A4AE).copy(alpha = 0.85f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = dateLabel,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private data class StatementLedgerItem(
    val tx: Transaction,
    val runningBalancePaise: Long
)

private sealed interface StatementListRow {
    data class Header(
        val dateLabel: String,
        val dateKey: String
    ) : StatementListRow

    data class Item(
        val ledgerItem: StatementLedgerItem,
        val dateKey: String
    ) : StatementListRow
}

private fun Transaction.isGiveEntry(): Boolean {
    return type == TransactionType.CREDIT || type == TransactionType.INVOICE
}

private fun Transaction.isReceiveEntry(): Boolean {
    return type == TransactionType.DEBIT || type == TransactionType.PAYMENT
}

private fun normalizeStartOfDay(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun normalizeEndOfDay(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun buildStatementFileName(customerName: String, startDate: Long?, endDate: Long?): String {
    val safeName = customerName
        .trim()
        .replace(Regex("\\s+"), "_")
        .replace(Regex("[^A-Za-z0-9_-]"), "")
        .ifBlank { "Customer" }
    val rangeSdf = SimpleDateFormat("ddMMMyyyy", Locale.ENGLISH)
    val fromPart = startDate?.let { rangeSdf.format(Date(it)) } ?: "Beginning"
    val toPart = endDate?.let { rangeSdf.format(Date(it)) } ?: "Now"
    return "${safeName}_Statement_${fromPart}_to_${toPart}.pdf"
}
