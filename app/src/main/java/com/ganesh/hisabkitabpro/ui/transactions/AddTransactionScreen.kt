package com.ganesh.hisabkitabpro.ui.transactions

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.storage.TxnBillAttachmentStore
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import java.util.Locale

/**
 * HISABKITAB PRO - 📱 9. FRONTEND RULES (STRICT)
 * - Amount displayed in Rupees, sent in Paise.
 * - Ultra Pro Max Voice & OCR Integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    customerId: Long,
    customerName: String,
    type: TransactionType,
    viewModel: TransactionViewModel,
    onTransactionAdded: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onOpenScanBill: () -> Unit = {},
    onOpenFullBill: () -> Unit = {}
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val saveFailedMessage = stringResource(R.string.common_save_failed)
    var amountText by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    var showOcrLowConfidenceBanner by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var pendingBillImageUri by remember { mutableStateOf<String?>(null) }

    val primaryColor = if (type == TransactionType.CREDIT) Color(0xFFD32F2F) else Color(0xFF2E7D32)
    val headerText = if (type == TransactionType.CREDIT) "Giving ₹" else "Receiving ₹"

    // 🎙️ Voice Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            // Simple extraction: look for numbers in spoken text
            val numberRegex = Regex("""\d+""")
            val match = numberRegex.find(spokenText)
            if (match != null) {
                amountText = match.value
                Toast.makeText(context, "Voice Detected: ₹${match.value}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not hear amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load initial data
    LaunchedEffect(customerId) {
        viewModel.loadCustomerData(customerId)
    }

    val scanPrefill by viewModel.pendingScanPrefill.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(scanPrefill) {
        val data = scanPrefill ?: return@LaunchedEffect
        amountText = data.amountKeypadText
        showOcrLowConfidenceBanner = data.lowConfidenceAmount
        if (data.note.isNotBlank()) {
            note = if (note.isBlank()) data.note else "${note.trim()} · ${data.note}"
        }
        pendingBillImageUri = data.billImageUri
        viewModel.consumePendingScanPrefill()
        Toast.makeText(
            context,
            if (data.lowConfidenceAmount) {
                context.getString(R.string.ocr_prefill_toast_low_confidence)
            } else {
                context.getString(R.string.ocr_prefill_toast_ok)
            },
            Toast.LENGTH_SHORT,
        ).show()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(customerName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(headerText, fontSize = 12.sp, color = primaryColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 💰 Amount Entry Card
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Enter Amount (Rupees)",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                    )
                    if (showOcrLowConfidenceBanner) {
                        Text(
                            text = stringResource(R.string.ocr_low_confidence_banner),
                            fontSize = 13.sp,
                            color = Color(0xFFB06000),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = amountText,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add Note (optional)") },
                        leadingIcon = { Icon(Icons.Default.Notes, null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 🤖 10. ULTRA FEATURES - Voice & OCR Icons
            Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                    onClick = { 
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say amount like 'Five hundred'")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Voice search not supported", Toast.LENGTH_SHORT).show()
                        }
                    },
                    label = { Text("Voice") },
                    leadingIcon = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = colorScheme.primary,
                        leadingIconContentColor = colorScheme.primary,
                    )
                )
                AssistChip(
                    onClick = { onOpenScanBill() },
                    label = { Text("Scan Bill") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.dashboard_action_scan),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = colorScheme.primary,
                        leadingIconContentColor = colorScheme.primary,
                    )
                )
            }

            Spacer(Modifier.weight(1f))

            // ⌨️ Keypad & Confirm
            Surface(
                color = colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 🧾 4. BILL SYSTEM — full flow (PDF + optional payment reminder)
                        Button(
                            onClick = onOpenFullBill,
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.surfaceVariant,
                                contentColor = colorScheme.onSurface,
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create Bill")
                        }
                        
                        // 🔥 3. TRANSACTION SYSTEM Button
                        Button(
                            onClick = {
                                if (saving) return@Button
                                val rupees = amountText.toDoubleOrNull() ?: 0.0
                                val paise = (rupees * 100).toLong()
                                if (paise > 0) {
                                    saving = true
                                    viewModel.addTransaction(
                                        customerId = customerId,
                                        amountPaise = paise,
                                        type = type,
                                        note = note,
                                        onComplete = { transactionId ->
                                            saving = false
                                            pendingBillImageUri?.let { uri ->
                                                TxnBillAttachmentStore.attachAfterTransactionSave(
                                                    context,
                                                    uri,
                                                    transactionId,
                                                )
                                            }
                                            pendingBillImageUri = null
                                            onTransactionAdded()
                                        },
                                        onError = {
                                            saving = false
                                            scope.launch {
                                                snackbarHostState.showSnackbar(saveFailedMessage)
                                            }
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            enabled = amountText != "0" && !saving,
                        ) {
                            if (saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Confirm ${type.name}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Numeric Keypad
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "DEL")
                    Column {
                        keys.chunked(3).forEach { rowKeys ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowKeys.forEach { key ->
                                    KeyItem(key, Modifier.weight(1f)) {
                                        amountText = updateAmount(amountText, key)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun KeyItem(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .height(56.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text == "DEL") {
            Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = Color.Red)
        } else {
            Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun updateAmount(current: String, key: String): String {
    if (key == "DEL") return if (current.length > 1) current.dropLast(1) else "0"
    if (current == "0") return if (key == ".") "0." else key
    if (current.contains(".") && key == ".") return current
    if (current.length > 9) return current
    return current + key
}
