@file:Suppress("DEPRECATION")

package com.ganesh.hisabkitabpro.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ganesh.hisabkitabpro.domain.model.InvoiceItem
import com.ganesh.hisabkitabpro.utils.InvoicePdfGenerator
import java.text.NumberFormat
import java.util.*

/**
 * LEGACY standalone invoice creator (not reachable from the live nav graph).
 * The live invoice flow is `ui/invoice/InvoiceEditorScreen` +
 * `ui/invoice/PdfPreviewScreen`. Preserved under the "Preserve Working
 * Systems" directive — R8 strips it from the release AAB while it has no
 * callers. Note this file imports `utils.InvoicePdfGenerator` which is a
 * separate legacy utility from the live `domain.ledger.InvoicePdfGenerator`.
 */
@Deprecated(
    message = "Legacy invoice flow. Use ui.invoice.InvoiceEditorScreen + PdfPreviewScreen instead. Not in the live navigation graph.",
    level = DeprecationLevel.WARNING
)
@Composable
fun InvoiceScreen(
    onGenerateInvoice: (List<InvoiceItem>) -> Unit
) {

    val context = LocalContext.current

    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    val items = remember { mutableStateListOf<InvoiceItem>() }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    val totalAmount = items.sumOf { it.total }

    val isValidInput =
        itemName.isNotBlank() &&
                quantity.isNotBlank() &&
                price.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Create Invoice",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {

                val qty = quantity.toDoubleOrNull() ?: return@Button
                val pr = price.toDoubleOrNull() ?: return@Button

                items.add(
                    InvoiceItem(
                        itemName = itemName.trim(),
                        quantity = qty,
                        rate = pr,
                        total = qty * pr
                    )
                )

                itemName = ""
                quantity = ""
                price = ""
            },
            enabled = isValidInput,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Item")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {

            items(items) { item ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Column {

                        Text(item.itemName)

                        Text(
                            "Qty: ${item.quantity} × ${currencyFormatter.format(item.rate)}"
                        )
                    }

                    Text(
                        currencyFormatter.format(item.total)
                    )
                }

                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Total: ${currencyFormatter.format(totalAmount)}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {

                val file =
                    InvoicePdfGenerator.generateInvoicePdf(
                        context,
                        items
                    )

                val uri =
                    FileProvider.getUriForFile(
                        context,
                        context.packageName + ".provider",
                        file
                    )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(
                    Intent.createChooser(intent, "Share Invoice")
                )

                onGenerateInvoice(items.toList())

            },
            enabled = items.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate & Share Invoice")
        }
    }
}