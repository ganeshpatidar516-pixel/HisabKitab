package com.ganesh.hisabkitabpro.ui.suppliers

import android.content.Intent
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import android.util.Log
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.model.Party
import com.ganesh.hisabkitabpro.domain.profile.StatementPdfBranding
import com.ganesh.hisabkitabpro.ui.viewmodel.PartyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierStatementScreen(
    supplier: Party,
    partyViewModel: PartyViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dayKeyFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val logs by partyViewModel.supplierLedgerLogs.collectAsState()
    val businessProfile by androidx.compose.runtime.produceState<com.ganesh.hisabkitabpro.domain.model.BusinessProfile?>(initialValue = null, supplier.id) {
        value = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).businessProfileDao().getBusinessProfileOnce()
        }
    }
    val isReconciliationVerified = remember(supplier.id) { SupplierReconciliationPrefs.isVerified(context, supplier.id) }

    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

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

    LaunchedEffect(supplier.id) {
        partyViewModel.refreshSupplierLedger(supplier.id)
    }

    val statementRows = remember(logs, startDate, endDate, dayKeyFormatter, sdf) {
        val filtered = logs.filter {
            (startDate == null || it.createdAt >= startDate!!) &&
                (endDate == null || it.createdAt <= endDate!!)
        }.sortedBy { it.createdAt }
        val dayStats = filtered.groupBy { dayKeyFormatter.format(Date(it.createdAt)) }.mapValues { (_, rows) ->
            val purchases = rows.count { !it.action.contains("PAYMENT", ignoreCase = true) }
            val payments = rows.count { it.action.contains("PAYMENT", ignoreCase = true) }
            val netPaise = rows.sumOf { row ->
                val amount = SupplierLedgerDetailParser.parse(row.detail).amountPaise
                if (row.action.contains("PAYMENT", ignoreCase = true)) -amount else amount
            }
            DayStats(purchases = purchases, payments = payments, netPaise = netPaise)
        }
        var runningPaise = 0L
        val out = mutableListOf<SupplierStatementRow>()
        filtered.forEach { row ->
            val amount = SupplierLedgerDetailParser.parse(row.detail).amountPaise
            val isPayment = row.action.contains("PAYMENT", ignoreCase = true)
            runningPaise += if (isPayment) -amount else amount
            val dateLabel = sdf.format(Date(row.createdAt))
            val dateKey = dayKeyFormatter.format(Date(row.createdAt))
            val last = (out.lastOrNull() as? SupplierStatementRow.Item)?.dateKey
            if (last != dateKey) {
                val stats = dayStats[dateKey] ?: DayStats()
                out.add(
                    SupplierStatementRow.Header(
                        dateLabel = dateLabel,
                        dateKey = dateKey,
                        netPaise = stats.netPaise,
                        purchaseCount = stats.purchases,
                        paymentCount = stats.payments
                    )
                )
            }
            out.add(SupplierStatementRow.Item(row, runningPaise, dateKey))
        }
        out.reversed()
    }
    val sortedLogs = remember(logs) { logs.sortedBy { it.createdAt } }
    val openingPayablePaise = remember(sortedLogs, startDate) {
        if (startDate == null) {
            0L
        } else {
            sortedLogs
                .filter { it.createdAt < startDate!! }
                .sumOf { row ->
                    val amount = SupplierLedgerDetailParser.parse(row.detail).amountPaise
                    if (row.action.contains("PAYMENT", ignoreCase = true)) -amount else amount
                }
        }
    }
    val periodEntries = remember(sortedLogs, startDate, endDate) {
        sortedLogs.filter {
            (startDate == null || it.createdAt >= startDate!!) &&
                (endDate == null || it.createdAt <= endDate!!)
        }
    }
    val purchaseCount = remember(periodEntries) { periodEntries.count { !it.action.contains("PAYMENT", ignoreCase = true) } }
    val paymentCount = remember(periodEntries) { periodEntries.count { it.action.contains("PAYMENT", ignoreCase = true) } }
    val periodNetPaise = remember(periodEntries) {
        periodEntries.sumOf { row ->
            val amount = SupplierLedgerDetailParser.parse(row.detail).amountPaise
            if (row.action.contains("PAYMENT", ignoreCase = true)) -amount else amount
        }
    }
    val closingPayablePaise = openingPayablePaise + periodNetPaise

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Supplier Statement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val file = generateSupplierStatementPdf(
                                context = context,
                                supplier = supplier,
                                rows = statementRows,
                                formatter = formatter,
                                businessProfile = businessProfile,
                                openingPayablePaise = openingPayablePaise,
                                closingPayablePaise = closingPayablePaise,
                                reconciliationVerified = isReconciliationVerified,
                                startDate = startDate,
                                endDate = endDate
                            )
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Supplier Statement - ${supplier.name}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share ${file.nameWithoutExtension}"))
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Share PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = startDate == null && endDate == null, onClick = { startDate = null; endDate = null }, label = { Text("All") })
                FilterChip(selected = startDate == currentDayStart, onClick = { startDate = currentDayStart; endDate = null }, label = { Text("Today") })
                FilterChip(selected = startDate == thirtyDayStart, onClick = { startDate = thirtyDayStart; endDate = null }, label = { Text("30 Days") })
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                    Text("From: ${startDate?.let { sdf.format(Date(it)) } ?: "Start"}")
                }
                OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
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
            Text(
                text = if (isReconciliationVerified) "Reconciliation: Verified" else "Reconciliation: Pending verification",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .fillMaxWidth(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isReconciliationVerified) Color(0xFF2E7D32) else Color(0xFFEF6C00)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SupplierStatementSummaryCard(
                    label = "Opening",
                    value = formatter.format(openingPayablePaise / 100.0),
                    color = Color(0xFF455A64),
                    modifier = Modifier.weight(1f)
                )
                SupplierStatementSummaryCard(
                    label = "Closing",
                    value = formatter.format(closingPayablePaise / 100.0),
                    color = if (closingPayablePaise >= 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SupplierMetricChip("Purchases: $purchaseCount", Color(0xFFFFEBEE), Color(0xFFD32F2F))
                SupplierMetricChip("Payments: $paymentCount", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                SupplierMetricChip("Net: ${formatter.format(periodNetPaise / 100.0)}", Color(0xFFE3F2FD), Color(0xFF1565C0))
            }

            LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
                if (statementRows.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 36.dp), contentAlignment = Alignment.Center) {
                            Text("No supplier entries in selected range", color = Color.Gray)
                        }
                    }
                }
                items(statementRows, key = {
                    when (it) {
                        is SupplierStatementRow.Header -> "h_${it.dateKey}"
                        is SupplierStatementRow.Item -> "i_${it.entry.id}"
                    }
                }) { row ->
                    when (row) {
                        is SupplierStatementRow.Header -> SupplierStatementDateHeader(
                            dateLabel = row.dateLabel,
                            netPaise = row.netPaise,
                            purchaseCount = row.purchaseCount,
                            paymentCount = row.paymentCount,
                            formatter = formatter
                        )
                        is SupplierStatementRow.Item -> SupplierStatementItem(row.entry, row.runningPaise, formatter)
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = startPickerState.selectedDateMillis?.let { normalizeStartOfDay(it) }
                    if (endDate != null && startDate != null && endDate!! < startDate!!) endDate = null
                    showStartPicker = false
                }) { Text("Apply") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { startDate = null; showStartPicker = false }) { Text("Clear") }
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
                TextButton(onClick = {
                    endDate = endPickerState.selectedDateMillis?.let { normalizeEndOfDay(it) }
                    showEndPicker = false
                }) { Text("Apply") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { endDate = null; showEndPicker = false }) { Text("Clear") }
                    TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }
}

@Composable
private fun SupplierStatementItem(entry: AuditLogEntry, runningPaise: Long, formatter: NumberFormat) {
    val parsed = remember(entry.detail) { SupplierLedgerDetailParser.parse(entry.detail) }
    val amountPaise = parsed.amountPaise
    val isPayment = remember(entry.action) { entry.action.contains("PAYMENT", ignoreCase = true) }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sdf.format(Date(entry.createdAt)), fontSize = 11.sp, color = Color.Gray)
                Surface(
                    color = if (isPayment) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isPayment) "Paid" else "Purchase",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPayment) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                }
                Text(parsed.noteDisplay.ifBlank { entry.action }, fontWeight = FontWeight.Medium)
                Text("Running: ${formatter.format(runningPaise / 100.0)}", fontSize = 11.sp, color = Color(0xFF546E7A))
            }
            Text(
                text = formatter.format(amountPaise / 100.0),
                fontWeight = FontWeight.Bold,
                color = if (isPayment) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
private fun SupplierStatementDateHeader(
    dateLabel: String,
    netPaise: Long,
    purchaseCount: Int,
    paymentCount: Int,
    formatter: NumberFormat
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(color = Color(0xFF90A4AE).copy(alpha = 0.9f), shape = RoundedCornerShape(14.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateLabel,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "P: $purchaseCount | Pay: $paymentCount | Net: ${formatter.format(netPaise / 100.0)}",
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
    }
}

private sealed interface SupplierStatementRow {
    data class Header(
        val dateLabel: String,
        val dateKey: String,
        val netPaise: Long,
        val purchaseCount: Int,
        val paymentCount: Int
    ) : SupplierStatementRow
    data class Item(val entry: AuditLogEntry, val runningPaise: Long, val dateKey: String) : SupplierStatementRow
}

private data class DayStats(
    val purchases: Int = 0,
    val payments: Int = 0,
    val netPaise: Long = 0L
)

@Composable
private fun SupplierStatementSummaryCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = Color(0xFF607D8B))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
        }
    }
}

@Composable
private fun SupplierMetricChip(
    text: String,
    background: Color,
    foreground: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = background
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}

private fun generateSupplierStatementPdf(
    context: android.content.Context,
    supplier: Party,
    rows: List<SupplierStatementRow>,
    formatter: NumberFormat,
    businessProfile: com.ganesh.hisabkitabpro.domain.model.BusinessProfile?,
    openingPayablePaise: Long,
    closingPayablePaise: Long,
    reconciliationVerified: Boolean,
    startDate: Long?,
    endDate: Long?
): File? {
    val startMs = System.currentTimeMillis()
    val document = PdfDocument()
    return try {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val appCtx = context.applicationContext
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var y = 56f
        val margin = 32f

        val businessName = businessProfile?.businessName?.trim().takeUnless { it.isNullOrEmpty() } ?: "HisabKitab Pro Business"
        val titleLeft = StatementPdfBranding.titleStartXAfterBranding(
            appCtx,
            canvas,
            businessProfile,
            margin,
            y,
        )
        paint.color = android.graphics.Color.parseColor("#B8860B")
        paint.textSize = 26f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText(businessName, titleLeft, y, paint)
        y += 18f
        paint.color = android.graphics.Color.DKGRAY
        paint.textSize = 11f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("Powered by HisabKitab Pro", margin, y, paint)
        y += 16f
        businessProfile?.ownerName?.trim()?.takeIf { it.isNotEmpty() }?.let { owner ->
            paint.color = android.graphics.Color.BLACK
            canvas.drawText(owner, margin, y, paint)
            y += 14f
        }
        businessProfile?.address?.trim()?.takeIf { it.isNotEmpty() }?.let { addr ->
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(addr.take(85), margin, y, paint)
            y += 14f
        }
        val phone = businessProfile?.phone?.trim().orEmpty()
        val gst = businessProfile?.gstNumber?.trim().orEmpty()
        if (phone.isNotEmpty() || gst.isNotEmpty()) {
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(
                buildString {
                    if (phone.isNotEmpty()) append("Phone: $phone")
                    if (phone.isNotEmpty() && gst.isNotEmpty()) append("  |  ")
                    if (gst.isNotEmpty()) append("GSTIN: $gst")
                },
                margin,
                y,
                paint,
            )
            y += 14f
        }
        paint.color = android.graphics.Color.BLACK
        y += 8f
        canvas.drawText("SUPPLIER STATEMENT", margin, y, paint.apply { textSize = 13f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
        y += 18f
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textSize = 11f
        canvas.drawText("Supplier: ${supplier.name}", margin, y, paint)
        canvas.drawText("Range: ${startDate?.let { sdf.format(Date(it)) } ?: "Beginning"} - ${endDate?.let { sdf.format(Date(it)) } ?: "Today"}", 280f, y, paint)
        y += 14f
        canvas.drawText("Reconciliation: ${if (reconciliationVerified) "Verified" else "Pending"}", margin, y, paint)
        y += 14f
        canvas.drawText("Opening Payable: ${formatter.format(openingPayablePaise / 100.0)}", margin, y, paint)
        y += 14f
        canvas.drawText("Closing Payable: ${formatter.format(closingPayablePaise / 100.0)}", margin, y, paint)
        y += 18f

        val bg = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY }
        canvas.drawRect(28f, y - 14f, 568f, y + 8f, bg)
        paint.color = android.graphics.Color.BLACK
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("Date", 34f, y, paint)
        canvas.drawText("Details", 130f, y, paint)
        canvas.drawText("Type", 392f, y, paint)
        canvas.drawText("Amount", 468f, y, paint)
        y += 22f
        paint.typeface = android.graphics.Typeface.DEFAULT

        rows.asSequence().filterIsInstance<SupplierStatementRow.Item>().take(24).forEach { item ->
            if (y > 790f) return@forEach
            val parsed = SupplierLedgerDetailParser.parse(item.entry.detail)
            val amountPaise = parsed.amountPaise
            val isPayment = item.entry.action.contains("PAYMENT", ignoreCase = true)
            canvas.drawText(sdf.format(Date(item.entry.createdAt)), 34f, y, paint)
            canvas.drawText(parsed.noteDisplay.ifBlank { item.entry.action }.take(34), 130f, y, paint)
            paint.color = if (isPayment) android.graphics.Color.parseColor("#388E3C") else android.graphics.Color.parseColor("#D32F2F")
            canvas.drawText(if (isPayment) "PAID" else "PURCHASE", 392f, y, paint)
            canvas.drawText(formatter.format(amountPaise / 100.0), 468f, y, paint)
            paint.color = android.graphics.Color.BLACK
            y += 20f
        }

        document.finishPage(page)
        val safeName = supplier.name.trim().replace(Regex("\\s+"), "_").replace(Regex("[^A-Za-z0-9_-]"), "").ifBlank { "Supplier" }
        val rangeSdf = SimpleDateFormat("ddMMMyyyy", Locale.ENGLISH)
        val fromPart = startDate?.let { rangeSdf.format(Date(it)) } ?: "Beginning"
        val toPart = endDate?.let { rangeSdf.format(Date(it)) } ?: "Now"
        val file = File(
            AppStoragePaths.exportsCacheDir(context),
            "${safeName}_Supplier_Statement_${fromPart}_to_${toPart}.pdf",
        )
        file.outputStream().use { document.writeTo(it) }
        Log.i("SupplierStatementPdf", "PDF generated in ${System.currentTimeMillis() - startMs} ms")
        file
    } catch (_: Exception) {
        null
    } finally {
        document.close()
    }
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
