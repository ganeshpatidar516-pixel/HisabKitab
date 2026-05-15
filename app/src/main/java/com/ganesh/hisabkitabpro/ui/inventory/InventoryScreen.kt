package com.ganesh.hisabkitabpro.ui.inventory

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.data.local.ProductEntity
import com.ganesh.hisabkitabpro.domain.inventory.InventoryScanNormalizer
import com.ganesh.hisabkitabpro.ui.viewmodel.InventorySummaryUiState
import com.ganesh.hisabkitabpro.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val search by viewModel.searchQuery.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val scannedCode by viewModel.scannedCode.collectAsStateWithLifecycle()
    var editorProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var createMode by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var scannerOpen by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.testTag("sacred_inventory_root"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inventory_screen_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { scannerOpen = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan product")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { createMode = true; editorProduct = null }) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InventorySummaryRow(summary = summary)

            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search products, category, barcode or SKU") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = { scannerOpen = true },
                    label = { Text("Scan") },
                    icon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = null) }
                )
                SuggestionChip(
                    onClick = { viewModel.setSearchQuery("low stock") },
                    label = { Text("Low stock: ${summary.lowStockCount}") },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) }
                )
                scannedCode?.let { code ->
                    AssistChip(
                        onClick = { viewModel.setSearchQuery(code) },
                        label = { Text("Last scan: $code") }
                    )
                }
            }

            scanMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (products.isEmpty()) {
                Text(
                    text = if (search.isBlank()) stringResource(R.string.inventory_empty_state) else "No matching products.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { p ->
                        ProductInventoryCard(
                            product = p,
                            onEdit = { editorProduct = p; createMode = false },
                            onDelete = { deleteTarget = p },
                            onAdjustStock = { delta -> viewModel.adjustStock(p.id, delta) }
                        )
                    }
                }
            }
        }
    }

    if (createMode || editorProduct != null) {
        ProductEditorDialog(
            existing = editorProduct,
            allProducts = products,
            scannedCode = scannedCode,
            validate = viewModel::validateProductDraft,
            onDismiss = { createMode = false; editorProduct = null },
            onSave = { product ->
                if (editorProduct == null) viewModel.addProduct(product) else viewModel.updateProduct(product)
                createMode = false
                editorProduct = null
            }
        )
    }

    if (scannerOpen) {
        InventoryScannerSheet(
            onDismiss = { scannerOpen = false },
            onCodeScanned = { raw ->
                val normalized = InventoryScanNormalizer.normalize(raw)
                viewModel.handleScannedRawValue(raw) { resolved ->
                    if (resolved != null) {
                        editorProduct = resolved
                        createMode = false
                        scanMessage = "Matched ${resolved.name} from code $normalized"
                    } else {
                        editorProduct = null
                        createMode = true
                        scanMessage = "New code scanned: $normalized. Add product details."
                    }
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete product?") },
            text = { Text("Remove '${target.name}' from inventory.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProduct(target)
                        deleteTarget = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun InventorySummaryRow(summary: InventorySummaryUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InventoryMetricCard(
            modifier = Modifier.weight(1f),
            title = "Products",
            value = summary.productCount.toString(),
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
        )
        InventoryMetricCard(
            modifier = Modifier.weight(1f),
            title = "Sale Value",
            value = "₹${String.format("%.0f", summary.saleValue)}",
            icon = { Icon(Icons.Default.Bolt, contentDescription = null) }
        )
        InventoryMetricCard(
            modifier = Modifier.weight(1f),
            title = "Low",
            value = summary.lowStockCount.toString(),
            icon = { Icon(Icons.Default.Warning, contentDescription = null) }
        )
    }
}

@Composable
private fun InventoryMetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                icon()
                Text(title, style = MaterialTheme.typography.labelSmall)
            }
            Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProductInventoryCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Double) -> Unit
) {
    val lowStock = product.stockQuantity <= product.minStockLevel
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (lowStock) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name.ifBlank { "Unnamed product" }, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.inventory_line_stock, product.stockQuantity, product.unit, product.sellingPrice),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (lowStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val codeLine = listOfNotNull(
                        product.barcode?.takeIf { it.isNotBlank() }?.let { "Barcode: $it" },
                        product.sku?.takeIf { it.isNotBlank() }?.let { "SKU: $it" },
                        product.category?.takeIf { it.isNotBlank() }?.let { "Category: $it" }
                    ).joinToString(" • ")
                    if (codeLine.isNotBlank()) {
                        Text(
                            codeLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (lowStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAdjustStock(1.0) }) { Text("+1") }
                Button(onClick = { onAdjustStock(-1.0) }) { Text("-1") }
                if (lowStock) {
                    Spacer(Modifier.width(4.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Low stock") },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    existing: ProductEntity?,
    allProducts: List<ProductEntity>,
    scannedCode: String?,
    validate: (name: String, stock: String, sellingPrice: String, purchasePrice: String, minStock: String) -> String?,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var stock by remember(existing?.id) { mutableStateOf(existing?.stockQuantity?.toString() ?: "0") }
    var price by remember(existing?.id) { mutableStateOf(existing?.sellingPrice?.toString() ?: "0") }
    var purchasePrice by remember(existing?.id) { mutableStateOf(existing?.purchasePrice?.toString() ?: "0") }
    var minStock by remember(existing?.id) { mutableStateOf(existing?.minStockLevel?.toString() ?: "5") }
    var unit by remember(existing?.id) { mutableStateOf(existing?.unit ?: "Pcs") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category.orEmpty()) }
    var barcode by remember(existing?.id, scannedCode) { mutableStateOf(existing?.barcode ?: scannedCode.orEmpty()) }
    var sku by remember(existing?.id) { mutableStateOf(existing?.sku.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Product" else "Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; error = null },
                    label = { Text("Category") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it; error = null },
                    label = { Text("Stock") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it; error = null },
                        label = { Text("Purchase") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it; error = null },
                        label = { Text("Selling") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                OutlinedTextField(
                    value = minStock,
                    onValueChange = { minStock = it; error = null },
                    label = { Text("Low stock alert level") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it; error = null },
                    label = { Text("Unit (Pcs/Kg/Ltr)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it.trim(); error = null },
                    label = { Text("Barcode / QR code") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it.trim(); error = null },
                    label = { Text("SKU (optional)") },
                    singleLine = true
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeName = name.trim()
                    val safeCategory = category.trim().ifBlank { null }
                    val safeUnit = unit.trim().ifBlank { "Pcs" }
                    val stockVal = stock.toDoubleOrNull()
                    val priceVal = price.toDoubleOrNull()
                    val purchaseVal = purchasePrice.toDoubleOrNull()
                    val minStockVal = minStock.toDoubleOrNull()
                    val validation = validate(safeName, stock, price, purchasePrice, minStock)
                    if (validation != null) {
                        error = validation
                        return@TextButton
                    }
                    val duplicate = allProducts.any {
                        it.id != existing?.id && it.name.equals(safeName, ignoreCase = true)
                    }
                    if (duplicate) {
                        error = "Product with same name already exists."
                        return@TextButton
                    }
                    val now = System.currentTimeMillis()
                    onSave(
                        (existing ?: ProductEntity(
                            id = now.toString(),
                            name = safeName,
                            category = safeCategory,
                            purchasePrice = purchaseVal ?: 0.0,
                            sellingPrice = priceVal ?: 0.0,
                            stockQuantity = stockVal ?: 0.0,
                            unit = safeUnit,
                            minStockLevel = minStockVal ?: 5.0,
                            barcode = barcode.trim().ifBlank { null },
                            sku = sku.trim().ifBlank { null },
                            createdAt = now,
                            updatedAt = now
                        )).copy(
                            name = safeName,
                            category = safeCategory,
                            unit = safeUnit,
                            stockQuantity = stockVal ?: 0.0,
                            sellingPrice = priceVal ?: 0.0,
                            purchasePrice = purchaseVal ?: existing?.purchasePrice ?: 0.0,
                            minStockLevel = minStockVal ?: 5.0,
                            barcode = barcode.trim().ifBlank { null },
                            sku = sku.trim().ifBlank { null },
                            updatedAt = now
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
