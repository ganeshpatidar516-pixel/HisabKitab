package com.ganesh.hisabkitabpro.ui.transactions

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganesh.hisabkitabpro.domain.model.BillItem
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.ledger.InvoicePdfGenerator
import com.ganesh.hisabkitabpro.core.security.PrivacySecureEffect
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import com.ganesh.hisabkitabpro.util.WhatsAppBillSender
import android.util.Log
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

internal data class BillLineUi(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val qty: String = "1",
    val unit: String = "Nos",
    val rate: String = "",
    val mrp: String = "",
    val gstPercent: String = "0",
    val cessPercent: String = "0",
    val rateIncludingTax: Boolean = false,
    val hsnCode: String = "",
    val notes: String = ""
) {
    fun validationMessage(): String? {
        if (name.trim().isEmpty()) return "Item Name required"
        val q = qty.toDoubleOrNull() ?: return "Quantity must be numeric"
        if (q <= 0) return "Quantity must be greater than 0"
        val r = rate.toDoubleOrNull() ?: return "Rate must be numeric"
        if (r <= 0) return "Rate must be greater than 0"
        val gst = gstPercent.toDoubleOrNull() ?: return "GST value is invalid"
        val cess = cessPercent.toDoubleOrNull() ?: return "CESS value is invalid"
        if (gst < 0 || cess < 0) return "GST/CESS cannot be negative"
        return null
    }

    fun toBillItemOrNull(): BillItem? {
        if (validationMessage() != null) return null
        val n = name.trim()
        if (n.isEmpty()) return null
        val q = qty.toDoubleOrNull() ?: return null
        val r = rate.toDoubleOrNull() ?: return null
        if (q <= 0 || r < 0) return null
        val gst = gstPercent.toDoubleOrNull() ?: 0.0
        val cess = cessPercent.toDoubleOrNull() ?: 0.0
        val taxFactor = 1 + ((gst + cess) / 100.0)
        val baseRate = if (rateIncludingTax && taxFactor > 0) r / taxFactor else r
        return BillItem(name = n, quantity = q, price = baseRate)
    }
}

/**
 * Maps HTML brand template id (prefs) and/or Room [AppSettings.invoiceTemplateId] to
 * canvas PDF [com.ganesh.hisabkitabpro.domain.invoice.TemplateRegistry] ids.
 */
internal fun resolvePdfTemplateId(
    settingsInvoiceTemplateId: String?,
    htmlBrandTemplateId: String? = null
): String {
    val fromHtml = when (htmlBrandTemplateId?.trim()) {
        "royal_gold" -> "TEMP_ROYAL"
        "executive_blue" -> "TEMP_GLASS"
        "minimalist" -> "TEMP_MIN_PRO"
        "cyber_dark" -> "TEMP_CYBER"
        "velvet_maroon" -> "TEMP_VELVET"
        "tech_grid" -> "TEMP_TECH"
        "aurora" -> "TEMP_AURORA"
        "carbon" -> "TEMP_CARBON"
        "holo_cyan" -> "TEMP_HOLO"
        "neo_brutal" -> "TEMP_NEO"
        else -> null
    }
    if (fromHtml != null) {
        return com.ganesh.hisabkitabpro.domain.invoice.TemplateRegistry.normalizePdfTemplateId(fromHtml)
    }
    val v = settingsInvoiceTemplateId?.trim().orEmpty()
    val raw = if (v.startsWith("TEMP_")) v else "TEMP_SIMPLE"
    return com.ganesh.hisabkitabpro.domain.invoice.TemplateRegistry.normalizePdfTemplateId(raw)
}

/** Snap app [gstRate] to nearest slab for the line-item GST dropdown (0/5/12/18/28). */
internal fun defaultLineGstPercentForSettings(settingsGstEnabled: Boolean, settingsGstRatePercent: Double): String {
    if (!settingsGstEnabled || settingsGstRatePercent <= 0) return "0"
    val slabs = listOf(0, 5, 12, 18, 28)
    val snap = slabs.minByOrNull { abs(it - settingsGstRatePercent) } ?: 18
    return snap.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenBillEntryScreen(
    customerId: Long,
    customerName: String,
    type: TransactionType,
    businessProfile: BusinessProfile?,
    pdfTemplateId: String,
    /** From app settings: when true, GST is added on bill subtotal and ledger amount matches grand total. */
    settingsGstEnabled: Boolean,
    /** From app settings (e.g. 18.0); used only when [settingsGstEnabled] is true. */
    settingsGstRatePercent: Double,
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    /** Pops this screen and the add-transaction screen so user returns to ledger. */
    onBillFlowComplete: () -> Unit
) {
    PrivacySecureEffect()
    val context = LocalContext.current
    val primaryColor = if (type == TransactionType.CREDIT) Color(0xFFD32F2F) else Color(0xFF2E7D32)
    val lines = remember { mutableStateListOf<BillLineUi>() }
    var showItemEditor by remember { mutableStateOf(true) }
    var editingLineId by remember { mutableStateOf<String?>(null) }
    var lineDraft by remember { mutableStateOf(BillLineUi()) }
    var saving by remember { mutableStateOf(false) }
    val customerPhone by viewModel.customerPhoneForBill.collectAsStateWithLifecycle(initialValue = "")
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val defaultGstSlug = defaultLineGstPercentForSettings(settingsGstEnabled, settingsGstRatePercent)
    val pricing by remember(settingsGstEnabled, settingsGstRatePercent) {
        derivedStateOf {
            val sub = lines.mapNotNull { it.toBillItemOrNull() }.sumOf { it.totalPrice }
            val gst = if (settingsGstEnabled && settingsGstRatePercent > 0) {
                sub * (settingsGstRatePercent / 100.0)
            } else {
                0.0
            }
            Triple(sub, gst, sub + gst)
        }
    }
    val subtotalRupee = pricing.first
    val gstRupee = pricing.second
    val grandTotalRupee = pricing.third

    LaunchedEffect(customerId) {
        viewModel.refreshCustomerPhoneForBill(customerId)
    }

    LaunchedEffect(defaultGstSlug) {
        if (lineDraft.name.isBlank() && lineDraft.rate.isBlank() &&
            lineDraft.gstPercent == "0" && defaultGstSlug != "0"
        ) {
            lineDraft = lineDraft.copy(gstPercent = defaultGstSlug)
        }
    }

    fun lineIndex(id: String): Int = lines.indexOfFirst { it.id == id }

    fun updateLine(id: String, transform: (BillLineUi) -> BillLineUi) {
        val i = lineIndex(id)
        if (i >= 0) lines[i] = transform(lines[i])
    }

    fun parsedItems(): List<BillItem> =
        lines.mapNotNull { it.toBillItemOrNull() }

    fun openEditorFor(line: BillLineUi?) {
        if (line == null) {
            editingLineId = null
            lineDraft = BillLineUi(
                qty = "1",
                gstPercent = defaultGstSlug,
                cessPercent = "0"
            )
        } else {
            editingLineId = line.id
            lineDraft = line
        }
        showItemEditor = true
    }

    fun saveDraft(): String? {
        val msg = lineDraft.validationMessage()
        if (msg != null) return msg
        val existing = editingLineId
        val stored = if (existing == null) {
            val neu = lineDraft.copy(id = UUID.randomUUID().toString())
            lines.add(neu)
            neu
        } else {
            val i = lineIndex(existing)
            val neu = lineDraft.copy(id = existing)
            if (i >= 0) lines[i] = neu
            neu
        }
        ItemQuickMemory.remember(context, stored)
        return null
    }

    fun resetDraftForNext() {
        editingLineId = null
        lineDraft = BillLineUi(
            qty = "1",
            gstPercent = defaultGstSlug,
            cessPercent = "0",
            unit = lineDraft.unit
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New bill", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(customerName, fontSize = 13.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 12.dp, color = Color.White) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    if (settingsGstEnabled && settingsGstRatePercent > 0 && subtotalRupee > 0) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 12.sp, color = Color.DarkGray)
                            Text(currency.format(subtotalRupee), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "GST @${String.format(Locale.US, "%.2f", settingsGstRatePercent)}%",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                            Text(currency.format(gstRupee), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (settingsGstEnabled && settingsGstRatePercent > 0) "Grand total" else "Total",
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                        Text(
                            currency.format(grandTotalRupee),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryColor
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (lines.isEmpty()) return@Button
                            val firstInvalid = lines.withIndex().firstOrNull { it.value.validationMessage() != null }
                            if (firstInvalid != null) {
                                val error = firstInvalid.value.validationMessage() ?: "Invalid item"
                                Toast.makeText(
                                    context,
                                    "Item ${firstInvalid.index + 1}: $error",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                            val items = parsedItems()
                            if (grandTotalRupee <= 0) {
                                Toast.makeText(context, "Total must be greater than zero", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            saving = true
                            viewModel.createBillWithLineItems(
                                customerId = customerId,
                                items = items,
                                businessProfile = businessProfile,
                                pdfTemplateId = pdfTemplateId,
                                extraNote = null,
                                settingsGstEnabled = settingsGstEnabled,
                                settingsGstRatePercent = settingsGstRatePercent
                            ) { result ->
                                saving = false
                                result.onSuccess { r ->
                                    if (!r.pdfReady) {
                                        Log.w("HK_BillShare", "Bill saved but PDF not ready for txnId=${r.transactionId}")
                                        Toast.makeText(
                                            context,
                                            "Bill saved. PDF not ready to share — open ledger and use Share.",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        onBillFlowComplete()
                                        return@onSuccess
                                    }
                                    // Same path as repository final PDF (shared/ + legacy migration); avoids
                                    // FileProvider IllegalArgumentException if only legacy root file existed.
                                    val pdfFile = InvoicePdfGenerator.resolveInvoicePdfFile(context, r.transactionId)
                                        ?: InvoicePdfGenerator.getInvoicePdfFile(context, r.transactionId)
                                            .takeIf { it.exists() && it.length() > 0L }
                                    if (pdfFile == null) {
                                        Log.w("HK_BillShare", "Bill saved but PDF missing for txnId=${r.transactionId}")
                                        Toast.makeText(
                                            context,
                                            "Bill saved. PDF not ready to share — open ledger and use Share.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onBillFlowComplete()
                                        return@onSuccess
                                    }
                                    try {
                                        val direct = WhatsAppBillSender.createDirectWhatsAppPdfIntent(
                                            context,
                                            customerPhone,
                                            pdfFile
                                        )
                                        if (direct != null) {
                                            if (WhatsAppBillSender.startShareActivity(context, direct)) {
                                                viewModel.markBillSentViaWhatsApp(r.transactionId)
                                            }
                                        } else {
                                            WhatsAppBillSender.createChooserPdfIntent(context, pdfFile)?.let { chooser ->
                                                WhatsAppBillSender.startShareActivity(context, chooser)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("HK_BillShare", "Share pipeline failed after save", e)
                                        Toast.makeText(
                                            context,
                                            "Bill saved. Could not open share — try again from ledger.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    Toast.makeText(context, "Bill saved", Toast.LENGTH_SHORT).show()
                                    onBillFlowComplete()
                                }.onFailure { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Save failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = lines.isNotEmpty() && !saving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save & WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (lines.isEmpty()) {
                        Text(
                            "Add at least one item to save and send the bill.",
                            fontSize = 11.sp,
                            color = Color(0xFF757575),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    "Items",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5C4033),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            items(lines, key = { it.id }) { line ->
                BillLineCard(
                    line = line,
                    primaryColor = primaryColor,
                    onEdit = { openEditorFor(line) },
                    onDuplicate = {
                        val i = lineIndex(line.id)
                        val copy = line.copy(id = UUID.randomUUID().toString())
                        if (i >= 0) lines.add(i + 1, copy) else lines.add(copy)
                    },
                    onRemove = {
                        val i = lineIndex(line.id)
                        if (i >= 0) lines.removeAt(i)
                    }
                )
            }
            item {
                TextButton(
                    onClick = { openEditorFor(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = primaryColor)
                    Spacer(Modifier.width(6.dp))
                    Text("Add New Item", color = primaryColor, fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showItemEditor) {
        AddItemDialog(
            draft = lineDraft,
            onDraftChange = { lineDraft = it },
            onDismiss = { showItemEditor = false },
            onSave = {
                val error = saveDraft()
                if (error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                } else {
                    showItemEditor = false
                }
            },
            onSaveAndNext = {
                val error = saveDraft()
                if (error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                } else {
                    resetDraftForNext()
                }
            }
        )
    }
}

/** When GST is still 0/blank, merge HSN edit with optional chapter-based GST suggestion. */
private fun mergeHsnWithSuggestedGst(draft: BillLineUi, newHsn: String): BillLineUi {
    val d = draft.copy(hsnCode = newHsn)
    val sug = HsnGstLookup.suggestGstPercentForHsn(newHsn.trim()) ?: return d
    val gstUnset = draft.gstPercent.isBlank() || draft.gstPercent == "0"
    return if (gstUnset) d.copy(gstPercent = sug) else d
}

/** After item name field loses focus: if GST unset, copy GST/HSN/CESS from saved item memory (exact name). */
private fun tryApplyMemoryGstOnNameBlur(context: Context, draft: BillLineUi): BillLineUi? {
    if (draft.gstPercent.isNotBlank() && draft.gstPercent != "0") return null
    val key = draft.name.trim()
    if (key.length < 2) return null
    val mem = ItemQuickMemory.get(context, key) ?: return null
    val g = mem.gstPercent.ifBlank { "0" }
    if (g == "0") return null
    return draft.copy(
        gstPercent = g,
        hsnCode = if (draft.hsnCode.isBlank()) mem.hsnCode else draft.hsnCode,
        cessPercent = if (draft.cessPercent.isBlank() || draft.cessPercent == "0") {
            mem.cessPercent.ifBlank { "0" }
        } else {
            draft.cessPercent
        }
    )
}

@Composable
private fun BillLineCard(
    line: BillLineUi,
    primaryColor: Color,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(line.name.ifBlank { "Unnamed Item" }, fontSize = 13.sp, color = Color(0xFF37474F), fontWeight = FontWeight.SemiBold)
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF607D8B))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate line", tint = Color(0xFF78909C))
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.Gray)
                    }
                }
            }
            Text(
                "${line.qty} ${line.unit} × ₹${line.rate.ifBlank { "0" }}",
                fontSize = 12.sp,
                color = Color(0xFF546E7A)
            )
            if (line.hsnCode.isNotBlank()) {
                Text("HSN: ${line.hsnCode}", fontSize = 11.sp, color = Color(0xFF78909C))
            }
            val item = line.toBillItemOrNull()
            if (item != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Line total: ${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(item.totalPrice)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    draft: BillLineUi,
    onDraftChange: (BillLineUi) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onSaveAndNext: () -> Unit
) {
    val context = LocalContext.current
    val unitOptions = listOf("Nos", "Kg", "Gram", "Ltr", "Ml", "Meter", "Box", "Piece")
    val gstOptions = listOf("0", "5", "12", "18", "28")
    var unitExpanded by remember { mutableStateOf(false) }
    var gstExpanded by remember { mutableStateOf(false) }
    var itemNameExpanded by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    val stepColor = Color(0xFF5C4033)
    val suggestions = remember(draft.name) { ItemQuickMemory.suggest(context, draft.name) }

    LaunchedEffect(draft.hsnCode) {
        val sug = HsnGstLookup.suggestGstPercentForHsn(draft.hsnCode.trim()) ?: return@LaunchedEffect
        if (draft.gstPercent.isBlank() || draft.gstPercent == "0") {
            if (sug != draft.gstPercent) {
                onDraftChange(draft.copy(gstPercent = sug))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add New Item", fontWeight = FontWeight.Bold)
                Text("HisabKitab Pro Item Builder", fontSize = 11.sp, color = Color(0xFF78909C))
            }
        },
        text = {
            val liveError = draft.validationMessage()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 460.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { }, label = { Text("Basic") }, colors = AssistChipDefaults.assistChipColors(labelColor = stepColor))
                        AssistChip(onClick = { }, label = { Text("Pricing") }, colors = AssistChipDefaults.assistChipColors(labelColor = stepColor))
                        AssistChip(
                            onClick = { showAdvanced = !showAdvanced },
                            label = { Text(if (showAdvanced) "Hide optional" else "Optional fields") },
                            colors = AssistChipDefaults.assistChipColors(labelColor = stepColor)
                        )
                    }
                }
                item {
                    liveError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = itemNameExpanded && suggestions.isNotEmpty(),
                        onExpandedChange = { itemNameExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = {
                                onDraftChange(draft.copy(name = it))
                                itemNameExpanded = true
                            },
                            label = { Text("Item Name * (Required)") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .onFocusChanged { state ->
                                    if (!state.isFocused) {
                                        tryApplyMemoryGstOnNameBlur(context, draft)?.let { onDraftChange(it) }
                                    }
                                },
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = itemNameExpanded && suggestions.isNotEmpty(),
                            onDismissRequest = { itemNameExpanded = false }
                        ) {
                            suggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        val memory = ItemQuickMemory.get(context, suggestion)
                                        onDraftChange(
                                            draft.copy(
                                                name = suggestion,
                                                qty = memory?.qty ?: draft.qty,
                                                unit = memory?.unit ?: draft.unit,
                                                rate = memory?.rate ?: draft.rate,
                                                gstPercent = memory?.gstPercent ?: draft.gstPercent,
                                                cessPercent = memory?.cessPercent ?: draft.cessPercent,
                                                mrp = memory?.mrp ?: draft.mrp,
                                                hsnCode = memory?.hsnCode ?: draft.hsnCode,
                                                rateIncludingTax = memory?.rateIncludingTax ?: draft.rateIncludingTax
                                            )
                                        )
                                        itemNameExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                        OutlinedTextField(
                            value = draft.unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Quantity Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            unitOptions.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        onDraftChange(draft.copy(unit = unit))
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draft.qty,
                            onValueChange = { onDraftChange(draft.copy(qty = it)) },
                            label = { Text("Quantity *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = draft.rate,
                            onValueChange = { onDraftChange(draft.copy(rate = it)) },
                            label = { Text("Rate * (Required)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
                if (showAdvanced) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = draft.mrp,
                                onValueChange = { onDraftChange(draft.copy(mrp = it)) },
                                label = { Text("MRP (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = draft.cessPercent,
                                onValueChange = { onDraftChange(draft.copy(cessPercent = it)) },
                                label = { Text("CESS % (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }
                    item {
                        ExposedDropdownMenuBox(expanded = gstExpanded, onExpandedChange = { gstExpanded = it }) {
                            OutlinedTextField(
                                value = draft.gstPercent,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("GST % (Optional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gstExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = gstExpanded, onDismissRequest = { gstExpanded = false }) {
                                gstOptions.forEach { gst ->
                                    DropdownMenuItem(
                                        text = { Text(gst) },
                                        onClick = {
                                            onDraftChange(draft.copy(gstPercent = gst))
                                            gstExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !draft.rateIncludingTax,
                                onClick = { onDraftChange(draft.copy(rateIncludingTax = false)) },
                                label = { Text("Rate excl. tax") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = draft.rateIncludingTax,
                                onClick = { onDraftChange(draft.copy(rateIncludingTax = true)) },
                                label = { Text("Rate incl. tax") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = draft.hsnCode,
                            onValueChange = { onDraftChange(mergeHsnWithSuggestedGst(draft, it)) },
                            label = { Text("HSN Code (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = draft.notes,
                            onValueChange = { onDraftChange(draft.copy(notes = it)) },
                            label = { Text("Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Text(
                        "Note: Item Name, Quantity, and Rate are required. All other fields are optional.",
                        fontSize = 11.sp,
                        color = Color(0xFF607D8B)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSaveAndNext) { Text("Save & Add Next") }
                TextButton(onClick = onSave) { Text("Save Item") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
