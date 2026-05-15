package com.ganesh.hisabkitabpro.ui.customers

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.profile.StatementPdfBranding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReminderHistorySubject {
    CUSTOMER,
    PARTY_SUPPLIER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderHistoryScreen(
    subjectId: Long,
    subjectName: String,
    subject: ReminderHistorySubject = ReminderHistorySubject.CUSTOMER,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val detailToken = when (subject) {
        ReminderHistorySubject.PARTY_SUPPLIER -> "partySupplierId=$subjectId"
        ReminderHistorySubject.CUSTOMER -> "customerId=$subjectId"
    }
    var filter by remember { mutableStateOf(ReminderHistoryFilter.ALL) }
    val logs by produceState(initialValue = emptyList<AuditLogEntry>(), subjectId, subject) {
        value = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context)
                .auditLogDao()
                .recentByEntityAndDetailToken(
                    entityType = "REMINDER",
                    detailToken = detailToken,
                    limit = 300
                )
        }
    }
    val filteredLogs = remember(logs, filter) { logs.filter { filter.matches(it) } }
    val timeSdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    var showExportFormatDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = { Text("Export reminder history") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Pick a format, then choose WhatsApp, Drive, Files, or another app on the next screen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = {
                            showExportFormatDialog = false
                            shareReminderHistoryCsv(
                                context,
                                subjectId,
                                subject,
                                subjectName,
                                filteredLogs,
                                timeSdf
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Excel sheet (CSV)") }
                    TextButton(
                        onClick = {
                            showExportFormatDialog = false
                            scope.launch(Dispatchers.IO) {
                                val file = buildReminderHistoryPdf(
                                    context.applicationContext,
                                    subjectId,
                                    subject,
                                    subjectName,
                                    filteredLogs
                                )
                                withContext(Dispatchers.Main) {
                                    if (file != null) {
                                        shareReminderHistoryFile(
                                            context,
                                            file,
                                            "application/pdf",
                                            "Reminder history - $subjectName"
                                        )
                                    } else {
                                        Toast.makeText(context, "Could not create PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("PDF") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportFormatDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder History - $subjectName", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        if (filteredLogs.isEmpty()) {
                            Toast.makeText(context, "Nothing to export for this filter.", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        showExportFormatDialog = true
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                },
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
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderHistoryFilter.entries.forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item.label) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item("empty") {
                        Text("No reminder history for selected filter.", color = ComposeColor.Gray)
                    }
                }
                items(filteredLogs, key = { it.id }) { entry ->
                    ReminderHistoryCard(entry, timeSdf.format(Date(entry.createdAt)))
                }
            }
        }
    }
}

@Composable
private fun ReminderHistoryCard(entry: AuditLogEntry, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFFFAFAFA))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.action, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(time, fontSize = 11.sp, color = ComposeColor.Gray)
            }
            Spacer(Modifier.height(6.dp))
            Text(entry.detail.orEmpty(), fontSize = 12.sp, color = ComposeColor.DarkGray)
        }
    }
}

private fun shareReminderHistoryCsv(
    context: Context,
    subjectId: Long,
    subject: ReminderHistorySubject,
    subjectName: String,
    entries: List<AuditLogEntry>,
    timeSdf: SimpleDateFormat
) {
    val header = "Action,Detail,Time\n"
    val rows = entries.joinToString(separator = "\n") { entry ->
        val detail = (entry.detail ?: "").replace(",", ";").replace("\n", " ")
        "\"${entry.action}\",\"$detail\",\"${timeSdf.format(Date(entry.createdAt))}\""
    }
        val file = File(
            AppStoragePaths.exportsCacheDir(context),
            "reminder_history_${subjectId}_${subject.name}.csv",
        )
    file.writeText(header + rows)
    shareReminderHistoryFile(context, file, "text/csv", "Reminder history - $subjectName")
}

private fun shareReminderHistoryFile(context: Context, file: File, mime: String, subject: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export reminder history"))
}

/**
 * Multi-page A4 PDF — same rows as CSV (action, time, detail), opens correctly in PDF viewers / WhatsApp.
 * First page includes merchant logo/QR from [BusinessProfile] when available.
 */
private suspend fun buildReminderHistoryPdf(
    context: Context,
    subjectId: Long,
    subject: ReminderHistorySubject,
    subjectName: String,
    entries: List<AuditLogEntry>
): File? {
    val profile = AppDatabase.getDatabase(context).businessProfileDao().getBusinessProfileOnce()
    val timeSdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val document = PdfDocument()
    val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val labelPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }
    val margin = 40f
    val pageBottom = 780f
    val lineStep = 11f
    fun wrapDetailLines(raw: String): List<String> =
        raw.replace("\n", " ").ifBlank { "—" }.chunked(72)

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
    var canvas: Canvas = page.canvas
    var y = 56f

    fun startNewPage() {
        document.finishPage(page)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        canvas = page.canvas
        y = 50f
        val brand = profile?.businessName?.trim()?.takeIf { it.isNotEmpty() } ?: "HisabKitab Pro"
        canvas.drawText("$brand — Reminder history (page $pageNumber)", margin, y, labelPaint)
        y += 22f
    }

    fun needSpace(lines: Int) {
        if (y + lines * lineStep > pageBottom) startNewPage()
    }

    val titleLeft = StatementPdfBranding.titleStartXAfterBranding(
        context,
        canvas,
        profile,
        margin,
        y,
    )
    canvas.drawText("Reminder history — $subjectName", titleLeft, y, titlePaint)
    y += 20f
    canvas.drawText(
        "Exported ${entries.size} row(s)  •  ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}",
        margin,
        y,
        labelPaint
    )
    y += 22f

    try {
        for (entry in entries) {
            val time = timeSdf.format(Date(entry.createdAt))
            val actionLine = "Action: ${entry.action}"
            val timeLine = "Time: $time"
            val detailPrefix = "Detail: "
            val detailBody = (entry.detail ?: "").ifBlank { "—" }

            val blockLines = mutableListOf<String>()
            blockLines.add(actionLine)
            blockLines.add(timeLine)
            blockLines.addAll(
                buildList {
                    val wrapped = wrapDetailLines(detailBody)
                    add(detailPrefix + wrapped.first())
                    for (i in 1 until wrapped.size) {
                        add("    ${wrapped[i]}")
                    }
                }
            )
            blockLines.add("")

            needSpace(blockLines.size + 1)
            for (line in blockLines) {
                needSpace(1)
                canvas.drawText(line.take(200), margin, y, bodyPaint)
                y += lineStep
            }
        }

        document.finishPage(page)
        val out = File(
            AppStoragePaths.exportsCacheDir(context),
            "reminder_history_${subjectId}_${subject.name}.pdf",
        )
        FileOutputStream(out).use { document.writeTo(it) }
        document.close()
        return out
    } catch (_: Exception) {
        try {
            document.close()
        } catch (_: Exception) {
        }
        return null
    }
}

private enum class ReminderHistoryFilter(val label: String) {
    ALL("All"),
    WHATSAPP("WhatsApp"),
    SMS("SMS"),
    AUTO("Auto"),
    MANUAL("Manual");

    fun matches(entry: AuditLogEntry): Boolean {
        val action = entry.action.uppercase(Locale.getDefault())
        return when (this) {
            ALL -> true
            WHATSAPP -> action.contains("WHATSAPP")
            SMS -> action.contains("SMS")
            AUTO -> action.contains("AUTO") || action.contains("FOLLOW_UP")
            MANUAL -> action.contains("MASTER") || action.contains("MANUAL")
        }
    }
}
