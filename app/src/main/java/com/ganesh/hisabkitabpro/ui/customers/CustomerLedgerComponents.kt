@file:OptIn(ExperimentalFoundationApi::class)

package com.ganesh.hisabkitabpro.ui.customers

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.settlement.SettlementKind
import com.ganesh.hisabkitabpro.domain.ledger.LedgerPdfFacade
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.util.safeClickable
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DateHeader(date: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(color = Color(0xFF90A4AE).copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp)) {
            Text(text = date, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
internal fun BillStyleTransactionBubble(
    tx: Transaction,
    formatter: NumberFormat,
    onLongClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    onSettlementKind: (String?) -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember(tx.id) { mutableStateOf(false) }
    val isCredit = tx.type == TransactionType.CREDIT || tx.type == TransactionType.INVOICE
    val sdfTime = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val sdfDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val sdfWhatsApp = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val billLabel = tx.invoiceNo?.takeIf { it.isNotBlank() } ?: "BILL-${tx.id}"
    val canOpenBillPdf = tx.billId != null || tx.type == TransactionType.INVOICE

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = if (isCredit) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        onLongClick()
                        onDeleteRequest()
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isCredit) Color(0xFFE0F2F1) else Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.customer_bill), fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_delete), color = Color(0xFFD32F2F)) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteRequest()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFD32F2F))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.customer_tag_full_settlement), color = Color(0xFFD4AF37)) },
                                onClick = {
                                    menuExpanded = false
                                    onSettlementKind(SettlementKind.FULL)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.customer_tag_partial_settlement), color = Color(0xFFD4AF37)) },
                                onClick = {
                                    menuExpanded = false
                                    onSettlementKind(SettlementKind.PARTIAL)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.customer_tag_clear_settlement), color = Color.Gray) },
                                onClick = {
                                    menuExpanded = false
                                    onSettlementKind(null)
                                }
                            )
                        }
                    }
                }
                if (tx.billId != null || tx.type == TransactionType.INVOICE) {
                    Spacer(Modifier.height(4.dp))
                    Text(billLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                    Text(
                        sdfDate.format(Date(tx.createdAt)),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(4.dp))
                    val sentAt = tx.whatsappSentAt
                    if (sentAt != null) {
                        Text(
                            "WhatsApp sent at ${sdfWhatsApp.format(Date(sentAt))}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF128C7E)
                        )
                    } else {
                        Text(
                            "WhatsApp not sent yet",
                            fontSize = 10.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCredit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isCredit) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(formatter.format(tx.amount / 100.0), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(sdfTime.format(Date(tx.createdAt)), fontSize = 10.sp, color = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = Color(0xFF43A047))
                }
                if (tx.billId != null || tx.type == TransactionType.INVOICE) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable(enabled = canOpenBillPdf) {
                                if (!LedgerPdfFacade.openBillPdf(context, tx.id)) {
                                    Toast.makeText(context, context.getString(R.string.customer_pdf_not_on_device), Toast.LENGTH_SHORT).show()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = Color.Red, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.customer_view_bill), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(billLabel, fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (!tx.settlementKind.isNullOrBlank()) {
            Text(
                "Settlement: ${tx.settlementKind}",
                fontSize = 10.sp,
                color = Color(0xFFD4AF37),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text("${formatter.format(tx.amount / 100.0)} ${stringResource(R.string.customer_due_suffix)}", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun UltraProCompactPanel(
    customer: Customer,
    formatter: NumberFormat,
    netBalancePaise: Long,
    nextAutoReminderAt: Long,
    onAddEntry: (TransactionType) -> Unit,
    onOpenStatement: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenReminderHistory: () -> Unit,
    onOpenReminderControl: () -> Unit,
    onRemindNow: () -> Unit,
    onPayUpi: () -> Unit,
    onChat: () -> Unit,
    onCall: () -> Unit,
    onShare: () -> Unit
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val dateTimeSdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val hasDue = netBalancePaise > 0L
    Surface(
        modifier = Modifier.shadow(24.dp),
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                CompactAction(Icons.AutoMirrored.Filled.MenuBook, Color(0xFF8D6E63)) { onOpenStatement() }
                CompactAction(Icons.Default.Payment, Color(0xFF2E7D32)) { onPayUpi() }
                CompactAction(Icons.AutoMirrored.Filled.Chat, Color(0xFF8D6E63)) { onChat() }
                CompactAction(Icons.Default.Call, Color(0xFF8D6E63)) { onCall() }
                CompactAction(Icons.Default.Share, Color(0xFF8D6E63)) { onShare() }
                Box {
                    CompactAction(Icons.Default.MoreHoriz, Color.Black) { moreMenuExpanded = true }
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.customer_view_profile)) },
                            onClick = {
                                moreMenuExpanded = false
                                onOpenProfile()
                            },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.customer_open_statement)) },
                            onClick = {
                                moreMenuExpanded = false
                                onOpenStatement()
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.customer_reminder_history)) },
                            onClick = {
                                moreMenuExpanded = false
                                onOpenReminderHistory()
                            },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).safeClickable { onOpenReminderControl() }
                ) {
                    Text(stringResource(R.string.customer_next_auto_reminder), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        dateTimeSdf.format(Date(nextAutoReminderAt)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2E7D32)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val brown = Color(0xFF8D6E63)
                    Button(
                        onClick = onRemindNow,
                        enabled = hasDue,
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, brown.copy(alpha = if (hasDue) 0.55f else 0.25f)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = brown,
                            disabledContainerColor = Color(0xFFF5F5F5),
                            disabledContentColor = Color(0xFFBDBDBD)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.customer_remind_now), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    if (!hasDue) {
                        Text(
                            "No balance due",
                            fontSize = 9.sp,
                            color = Color(0xFF9E9E9E),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.customer_total_balance_due), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        formatter.format(netBalancePaise / 100.0),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onAddEntry(TransactionType.DEBIT) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.supplier_received), color = Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
                Button(
                    onClick = { onAddEntry(TransactionType.CREDIT) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.2f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.supplier_given), color = Color(0xFFD32F2F), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompactAction(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(CircleShape).safeClickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
    }
}
