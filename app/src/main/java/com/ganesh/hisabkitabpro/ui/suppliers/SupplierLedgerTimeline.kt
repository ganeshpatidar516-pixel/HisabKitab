package com.ganesh.hisabkitabpro.ui.suppliers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.receiver.ReminderBroadcastReceiver
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun parseAmountPaise(detail: String?): Long? {
    if (detail.isNullOrBlank()) return null
    val regex = Regex("""amountPaise=([^,]+)""")
    return regex.find(detail)?.groupValues?.getOrNull(1)?.toLongOrNull()
}

internal fun buildSupplierFollowUpMessage(
    context: Context,
    supplierName: String,
    payablePaise: Long,
    dueAt: Long?,
): String {
    val amount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(payablePaise / 100.0)
    val dueLine = if (dueAt == null) {
        context.getString(R.string.supplier_followup_due_generic)
    } else {
        val dueStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueAt))
        context.getString(R.string.supplier_followup_due_dated, dueStr)
    }
    return context.getString(R.string.supplier_followup_body, supplierName, amount, dueLine)
}

@Composable
internal fun TimelineDateHeader(label: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFE7F1EF)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                color = Color(0xFF4E5D58),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

internal sealed class TimelineRow(val key: String) {
    class Header(val label: String) : TimelineRow("header_$label")
    class Entry(val entry: AuditLogEntry) : TimelineRow("entry_${entry.id}")
}

internal fun buildTimelineRows(logs: List<AuditLogEntry>, todayLabelWord: String): List<TimelineRow> {
    val out = mutableListOf<TimelineRow>()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val todayLabel = dateFormat.format(Date(System.currentTimeMillis()))
    var lastLabel: String? = null
    logs.forEach { entry ->
        val raw = dateFormat.format(Date(entry.createdAt))
        val label = if (raw == todayLabel) todayLabelWord else raw
        if (label != lastLabel) {
            out += TimelineRow.Header(label)
            lastLabel = label
        }
        out += TimelineRow.Entry(entry)
    }
    return out
}

internal fun scheduleSupplierDueReminder(
    context: Context,
    supplierName: String,
    amountPaise: Long,
    dueAt: Long?
) {
    if (dueAt == null || dueAt <= System.currentTimeMillis()) return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
        putExtra("CUSTOMER_NAME", supplierName)
        putExtra("AMOUNT", NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amountPaise / 100.0))
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        (supplierName.hashCode() xor dueAt.toInt()),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    runCatching {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
    }.onFailure {
        runCatching { alarmManager.set(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent) }
    }
}
