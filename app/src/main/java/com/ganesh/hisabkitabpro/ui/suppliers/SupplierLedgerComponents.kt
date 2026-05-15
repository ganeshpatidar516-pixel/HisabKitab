@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.suppliers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.util.safeClickable
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SupplierTimelineItem(
    entry: AuditLogEntry,
    formatter: NumberFormat,
    supplierId: Long
) {
    val context = LocalContext.current
    val parsed = remember(entry.detail) { SupplierLedgerDetailParser.parse(entry.detail) }
    val amountPaise = parsed.amountPaise
    val billUri = parsed.billImageUri
    var showBillFullscreen by remember { mutableStateOf(false) }
    val isPayment = remember(entry.action) { entry.action.contains("PAYMENT", ignoreCase = true) }
    val amountPrefix = if (isPayment) "↓" else "↑"
    val amountColor = if (isPayment) SupplierPayActionGreen else SupplierPayableRed

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (billUri.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(billUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.supplier_bill),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showBillFullscreen = true }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$amountPrefix ${formatter.format(amountPaise / 100.0)}",
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            fontSize = 22.sp
                        )
                        Text(
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(entry.createdAt)),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    if (parsed.showTag) {
                        Spacer(Modifier.height(4.dp))
                        Text("#${parsed.tagRaw}", color = Color(0xFF616161), fontSize = 11.sp)
                    }
                    if (parsed.noteDisplay.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(parsed.noteDisplay, color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (billUri.isNotBlank()) {
                    TextButton(onClick = { showBillFullscreen = true }) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = stringResource(R.string.supplier_bill))
                        Text(stringResource(R.string.supplier_bill))
                    }
                }
                TextButton(
                    onClick = {
                        val verifyLink =
                            "https://hisabkitab.local/reconcile?supplierId=$supplierId&amount=${parsed.balanceAfterPaise ?: amountPaise}"
                        val lc = AppLocaleManager.wrapContext(context)
                        val shareText = lc.getString(
                            R.string.supplier_share_outstanding_message,
                            formatter.format((parsed.balanceAfterPaise ?: amountPaise) / 100.0),
                            verifyLink
                        )
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = if (billUri.isNotBlank()) "image/*" else "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            if (billUri.isNotBlank()) {
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(billUri))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }
                        context.startActivity(
                            Intent.createChooser(share, context.getString(R.string.supplier_share_entry))
                        )
                    }
                ) { Text(stringResource(R.string.supplier_share)) }
            }
        }
    }

    if (showBillFullscreen && billUri.isNotBlank()) {
        Dialog(
            onDismissRequest = { showBillFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showBillFullscreen = false },
                color = Color.Black
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(billUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.supplier_bill_preview),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
internal fun SupplierTermDaysDialog(
    currentDays: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember(currentDays) { mutableStateOf(currentDays.toString()) }
    val days = text.toIntOrNull()?.coerceAtLeast(1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.supplier_credit_terms)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(3) },
                label = { Text(stringResource(R.string.supplier_credit_term_days)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = { TextButton(onClick = { onSave(days ?: currentDays) }, enabled = days != null) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Composable
internal fun SupplierLedgerAutoReminderStrip(
    balancePaise: Long,
    nextAutoReminderAt: Long,
    formatter: NumberFormat,
    onNextAutoClick: () -> Unit,
    onOpenReminderHistory: () -> Unit,
    onOpenReminderControl: () -> Unit,
    onRemindNow: () -> Unit
) {
    val hasPayable = balancePaise > 0L
    val dateTimeSdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var menuExpanded by remember { mutableStateOf(false) }
    val brown = Color(0xFF8D6E63)
    Surface(
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).safeClickable { onNextAutoClick() }) {
                Text(
                    stringResource(R.string.customer_next_auto_reminder),
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    dateTimeSdf.format(Date(nextAutoReminderAt)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2E7D32)
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onRemindNow,
                    enabled = hasPayable,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, brown.copy(alpha = if (hasPayable) 0.55f else 0.25f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = brown,
                        disabledContainerColor = Color(0xFFF5F5F5),
                        disabledContentColor = Color(0xFFBDBDBD)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.customer_remind_now), fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                if (!hasPayable) {
                    Text(
                        stringResource(R.string.supplier_reminder_no_payable),
                        fontSize = 9.sp,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.supplier_balance_due), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        formatter.format(balancePaise / 100.0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = SupplierPayableRed
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.supplier_more), tint = Color.DarkGray)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.customer_reminder_history)) },
                            onClick = {
                                menuExpanded = false
                                onOpenReminderHistory()
                            },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reminder_auto_settings_short)) },
                            onClick = {
                                menuExpanded = false
                                onOpenReminderControl()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}
