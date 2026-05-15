package com.ganesh.hisabkitabpro.ui.invoice

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganesh.hisabkitabpro.domain.invoice.InvoiceCalculator
import com.ganesh.hisabkitabpro.domain.invoice.InvoiceNumberGenerator
import com.ganesh.hisabkitabpro.domain.invoice.PdfInvoiceGenerator
import com.ganesh.hisabkitabpro.domain.invoice.QrCodeGenerator
import com.ganesh.hisabkitabpro.domain.model.Invoice
import com.ganesh.hisabkitabpro.domain.model.InvoiceItem
import com.ganesh.hisabkitabpro.ui.viewmodel.CustomerViewModel
import com.ganesh.hisabkitabpro.ui.viewmodel.InvoiceViewModel
import com.ganesh.hisabkitabpro.ui.viewmodel.SettingsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    viewModel: InvoiceViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    customerViewModel: CustomerViewModel = hiltViewModel(),
    customerId: Long = 0L, // Changed to Long
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val businessProfile by settingsViewModel.businessProfile.collectAsState(initial = null)
    val customers by customerViewModel.customers.collectAsState()
    
    // Auto-fetch customer details if ID is provided
    val existingCustomer = remember(customerId, customers) {
        customers.find { it.id == customerId }
    }

    var customerName by remember { mutableStateOf(existingCustomer?.name ?: "") }
    var customerPhone by remember { mutableStateOf(existingCustomer?.phone ?: "") }
    var customerAddress by remember { mutableStateOf("") }
    var gstNumber by remember { mutableStateOf("") }

    // Update fields if customerId changes or data loads
    LaunchedEffect(existingCustomer) {
        existingCustomer?.let {
            customerName = it.name
            customerPhone = it.phone ?: ""
        }
    }

    var selectedTemplate by remember { mutableStateOf(1) }
    val items = remember { mutableStateListOf<InvoiceItem>() }

    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }

    var gstEnabled by remember { mutableStateOf(false) }
    val gstRate = 18.0

    val subtotal = InvoiceCalculator.calculateSubtotal(items)
    val gstAmount = InvoiceCalculator.calculateGst(subtotal, gstRate, gstEnabled)
    val finalAmount = InvoiceCalculator.calculateFinalAmount(subtotal, gstAmount, 0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ultra Invoice Creator", fontWeight = FontWeight.ExtraBold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                // Customer Header (Professional Look)
                if (existingCustomer != null) {
                    item {
                        Card(
                            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(customerName, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(customerPhone, fontSize = 12.sp, color = Color.DarkGray)
                                }
                                Spacer(Modifier.weight(1f))
                                Text("Selected", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    item {
                        Card(modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Bill Details", fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") }, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                item {
                    Text("Additional Info", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(value = customerAddress, onValueChange = { customerAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = gstNumber, onValueChange = { gstNumber = it }, label = { Text("GSTIN (Optional)") }, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp))
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Apply GST (18%)", fontWeight = FontWeight.Bold)
                        Switch(checked = gstEnabled, onCheckedChange = { gstEnabled = it })
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Add Items", fontWeight = FontWeight.Bold, color = Color.Black)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Qty") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(value = itemPrice, onValueChange = { itemPrice = it }, label = { Text("Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                Button(
                                    onClick = {
                                        val q = itemQty.toDoubleOrNull() ?: 0.0
                                        val p = itemPrice.toDoubleOrNull() ?: 0.0
                                        if (itemName.isNotBlank() && q > 0) {
                                            items.add(InvoiceItem(itemName = itemName, quantity = q, rate = p, total = q * p))
                                            itemName = ""; itemQty = ""; itemPrice = ""
                                        }
                                    },
                                    modifier = Modifier.height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                items(items) { item ->
                    Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.itemName, fontWeight = FontWeight.Bold)
                                Text("${item.quantity} x ₹${item.rate}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("₹${item.total}", fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                            IconButton(onClick = { items.remove(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }

            Surface(tonalElevation = 12.dp, shadowElevation = 12.dp, color = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Text("₹${String.format(Locale.getDefault(), "%,.1f", finalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color(0xFF1A237E))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val invoice = Invoice(
                                invoiceId = InvoiceNumberGenerator.generateInvoiceNumber(),
                                customerName = customerName,
                                customerPhone = customerPhone,
                                customerAddress = customerAddress,
                                items = items.toList(),
                                subtotal = subtotal,
                                gstEnabled = gstEnabled,
                                gstPercent = if (gstEnabled) gstRate else 0.0,
                                gstAmount = gstAmount,
                                discount = 0.0,
                                finalAmount = finalAmount,
                                paymentStatus = "PENDING",
                                paymentMethod = "UPI",
                                date = System.currentTimeMillis(),
                                notes = "Professional Bill Generated"
                            )
                            
                            viewModel.saveInvoice(invoice, customerId.toString()) {
                                val qrBitmap = QrCodeGenerator.generateUpiQr(businessProfile?.gstNumber ?: "ganesh@upi", customerName, finalAmount)
                                val pdfFile = PdfInvoiceGenerator.generateProfessionalPdf(
                                    context = context,
                                    invoice = invoice,
                                    businessProfile = businessProfile,
                                    templateId = "template_$selectedTemplate",
                                    qrBitmap = qrBitmap
                                )
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Ultra Invoice"))
                                Toast.makeText(context, "Invoice Shared & Saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                        enabled = items.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share Professional Bill", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
