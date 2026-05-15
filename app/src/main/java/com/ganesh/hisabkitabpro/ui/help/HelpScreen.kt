package com.ganesh.hisabkitabpro.ui.help

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppSender
import com.ganesh.hisabkitabpro.domain.support.HelpSupportActions
import com.ganesh.hisabkitabpro.domain.support.SupportBusinessHours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    onAiChatClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val waPrefill = stringResource(R.string.support_whatsapp_prefill)
    val liveChatSubtitle = stringResource(R.string.support_live_chat_subtitle)
    val offHoursWaPrefix = stringResource(R.string.support_whatsapp_off_hours_prefix)
    val supportHoursTitle = stringResource(R.string.support_hours_dialog_title)
    val supportHoursBody = stringResource(R.string.support_hours_dialog_body)
    val supportHoursOpenWa = stringResource(R.string.support_hours_open_whatsapp)
    val helpTagline = stringResource(R.string.help_screen_tagline)
    val aiFabCd = stringResource(R.string.help_fab_ai_content_description)
    val videoGuideUrl = stringResource(R.string.help_video_guide_url)
    val videoGuideSubtitle = stringResource(R.string.help_video_guide_subtitle)
    val submitTicketSubtitle = stringResource(R.string.help_submit_ticket_subtitle)
    val ticketEmail = stringResource(R.string.support_ticket_email)
    val ticketMailSubject = stringResource(R.string.support_ticket_email_subject)
    val ticketBodyFooter = stringResource(R.string.support_ticket_body_footer)
    val ticketDialogTitle = stringResource(R.string.support_ticket_dialog_title)
    val ticketDialogHint = stringResource(R.string.support_ticket_dialog_hint)
    val ticketDialogSend = stringResource(R.string.support_ticket_dialog_send)
    val ticketEmptyMsg = stringResource(R.string.support_ticket_empty)
    val privacyPolicySubtitle = stringResource(R.string.help_privacy_policy_subtitle)
    var offHoursDialogVisible by remember { mutableStateOf(false) }
    var ticketDialogVisible by remember { mutableStateOf(false) }
    var ticketDescription by remember { mutableStateOf("") }

    val faqs = listOf(
        FAQ("How to add a transaction?", "Click the 'Given' or 'Received' button on the customer ledger screen, enter the amount, and save."),
        FAQ("How to share a bill?", "Go to the ledger, click 'Create Bill', add items, and tap 'Share Professional Bill'."),
        FAQ("Is my data safe?", "Yes, HisabKitab Pro uses industry-standard encryption and local backup systems to keep your data secure."),
        FAQ("How to set a reminder?", "Open the customer ledger and use the 'Auto Reminder' option in the More menu."),
        FAQ("Can I use it offline?", "Yes, the app works perfectly offline. Data will sync once you are back online.")
    )

    val filteredFaqs = faqs.filter { it.question.contains(searchQuery, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Help & Support", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surface,
                        titleContentColor = colorScheme.onSurface,
                        navigationIconContentColor = colorScheme.onSurface
                    )
                )
            },
            floatingActionButton = {
                SmallFloatingActionButton(
                    onClick = onAiChatClick,
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = aiFabCd)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(colorScheme.background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Text(
                        helpTagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search help topics...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = colorScheme.surface,
                            focusedContainerColor = colorScheme.surface,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = colorScheme.primary
                        )
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Direct Channels",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SupportActionCard(
                        title = "Live Chat",
                        subtitle = liveChatSubtitle,
                        icon = Icons.AutoMirrored.Filled.Chat,
                        color = Color(0xFF25D366),
                        onClick = {
                            if (SupportBusinessHours.isWithinLiveSupportNow()) {
                                WhatsAppSender.openSupportChat(context, waPrefill)
                            } else {
                                offHoursDialogVisible = true
                            }
                        }
                    )
                    SupportActionCard(
                        title = "Video Guide",
                        subtitle = videoGuideSubtitle,
                        icon = Icons.Default.PlayCircle,
                        color = Color(0xFFE53935),
                        onClick = { HelpSupportActions.openVideoGuide(context, videoGuideUrl) }
                    )
                    SupportActionCard(
                        title = "Submit Ticket",
                        subtitle = submitTicketSubtitle,
                        icon = Icons.Default.ConfirmationNumber,
                        color = Color(0xFFFB8C00),
                        onClick = { ticketDialogVisible = true }
                    )
                    SupportActionCard(
                        title = "Privacy Policy",
                        subtitle = privacyPolicySubtitle,
                        icon = Icons.Default.Shield,
                        color = Color(0xFF3949AB),
                        onClick = onPrivacyPolicyClick
                    )
                }

                item {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        "Common Questions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(filteredFaqs) { faq ->
                    FAQItem(faq)
                }

                item {
                    Spacer(Modifier.height(72.dp))
                }
            }
        }

        if (ticketDialogVisible) {
            val cancelLabel = stringResource(R.string.common_cancel)
            AlertDialog(
                onDismissRequest = {
                    ticketDialogVisible = false
                    ticketDescription = ""
                },
                title = { Text(ticketDialogTitle, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = ticketDescription,
                        onValueChange = { ticketDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text(ticketDialogHint) },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = colorScheme.surface,
                            focusedContainerColor = colorScheme.surface,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            focusedBorderColor = colorScheme.primary
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val text = ticketDescription.trim()
                            if (text.isEmpty()) {
                                Toast.makeText(context, ticketEmptyMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                val body = text + ticketBodyFooter
                                HelpSupportActions.submitSupportTicket(
                                    context = context,
                                    supportEmail = ticketEmail,
                                    subject = ticketMailSubject,
                                    body = body
                                )
                                ticketDialogVisible = false
                                ticketDescription = ""
                            }
                        }
                    ) {
                        Text(ticketDialogSend)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            ticketDialogVisible = false
                            ticketDescription = ""
                        }
                    ) {
                        Text(cancelLabel)
                    }
                }
            )
        }

        if (offHoursDialogVisible) {
            val cancelLabel = stringResource(R.string.common_cancel)
            AlertDialog(
                onDismissRequest = { offHoursDialogVisible = false },
                title = {
                    Text(
                        supportHoursTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        supportHoursBody,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            offHoursDialogVisible = false
                            WhatsAppSender.openSupportChat(context, "$offHoursWaPrefix\n\n$waPrefill")
                        }
                    ) {
                        Text(supportHoursOpenWa)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { offHoursDialogVisible = false }) {
                        Text(cancelLabel)
                    }
                }
            )
        }
    }
}

@Composable
fun FAQItem(faq: FAQ) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(faq.question, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 15.sp)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text(faq.answer, color = colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun SupportActionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

data class FAQ(val question: String, val answer: String)
