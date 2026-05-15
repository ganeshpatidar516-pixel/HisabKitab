package com.ganesh.hisabkitabpro.domain.invoice

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.ProfileMapFooter
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Invoice
import com.ganesh.hisabkitabpro.domain.model.InvoiceItem
import com.ganesh.hisabkitabpro.domain.model.TaxType
import com.ganesh.hisabkitabpro.domain.model.UniversalInvoice
import com.ganesh.hisabkitabpro.domain.invoice.TemplateLayoutType

object InvoiceTemplateEngine {

    /** Maps [UniversalInvoice] + line [BillItem]s into the legacy [Invoice] used by PDF / Compose templates. */
    fun toLegacyInvoice(universal: UniversalInvoice): Invoice {
        val items = universal.items.map { bi ->
            InvoiceItem(
                itemName = bi.name,
                quantity = bi.quantity,
                rate = bi.price,
                total = bi.totalPrice
            )
        }
        val hasTax = universal.taxType != TaxType.NONE &&
            universal.taxRate > 0.0 &&
            universal.taxAmount > 0.0
        val noteParts = mutableListOf<String>()
        universal.signatureUrl?.takeIf { it.isNotBlank() }?.let { noteParts += "Signature: $it" }
        if (hasTax) {
            val label = when (universal.taxType) {
                TaxType.GST -> "GST"
                TaxType.VAT -> "VAT"
                TaxType.SALES_TAX -> "Sales tax"
                TaxType.NONE -> ""
            }
            if (label.isNotEmpty()) noteParts += "$label @${universal.taxRate}%"
        }
        val notesStr = noteParts.joinToString("\n").ifBlank { null }
        val taxLineLabel = when {
            !hasTax -> null
            universal.taxType == TaxType.GST -> "GST"
            universal.taxType == TaxType.VAT -> "VAT"
            universal.taxType == TaxType.SALES_TAX -> "Sales tax"
            else -> "Tax"
        }
        return Invoice(
            invoiceId = universal.invoiceNumber,
            customerName = universal.customerName,
            customerPhone = "",
            customerAddress = "",
            items = items,
            subtotal = universal.subTotal,
            gstEnabled = hasTax,
            gstPercent = if (hasTax) universal.taxRate else 0.0,
            gstAmount = if (hasTax) universal.taxAmount else 0.0,
            taxLineLabel = taxLineLabel,
            discount = 0.0,
            finalAmount = universal.grandTotal,
            paymentStatus = "UNPAID",
            paymentMethod = "-",
            date = universal.timestamp,
            notes = notesStr
        )
    }

    /**
     * Renders preview using the same mapping as PDF ([toLegacyInvoice]).
     * [businessProfile] overrides header; if null, [UniversalInvoice.businessName] is used.
     */
    @Composable
    fun RenderUniversalInvoicePreview(
        templateId: String,
        universal: UniversalInvoice,
        businessProfile: BusinessProfile?
    ) {
        val business = businessProfile ?: BusinessProfile(
            userId = "universal_preview",
            businessName = universal.businessName,
            ownerName = "",
            address = "",
            phone = "",
            email = ""
        )
        val customer = Customer(name = universal.customerName, phone = "")
        val legacy = toLegacyInvoice(universal)
        val items = legacy.items
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            ClassicTemplate(business, customer, items, legacy)
        }
    }

    /** Writes PDF via existing pipeline; falls back to [UniversalInvoice.businessName] when profile is null. */
    fun generatePdfFromUniversal(
        context: Context,
        templateId: String,
        universal: UniversalInvoice,
        businessProfile: BusinessProfile?
    ): String {
        val profile = businessProfile ?: BusinessProfile(
            userId = "universal_pdf",
            businessName = universal.businessName,
            ownerName = "",
            address = "",
            phone = "",
            email = ""
        )
        return generatePdf(context, templateId, toLegacyInvoice(universal), profile)
    }

    fun getAllTemplates(): List<InvoiceTemplate> {
        return TemplateRegistry.getAllTemplates()
    }

    fun getTemplateById(templateId: String): InvoiceTemplate {
        return TemplateRegistry.getTemplateById(templateId)
    }

    @Composable
    fun RenderPreview(templateId: String, businessProfile: BusinessProfile? = null) {
        getTemplateById(templateId) // validates id; layout choice matches historical preview (classic body)

        // Mock data for preview (header uses saved [businessProfile] when it has any branding signal)
        val mockBusiness = BusinessProfile(
            userId = "demo_user",
            businessName = "Omni Retailers Ltd", 
            ownerName = "Ganesh Kumar", 
            address = "Tech Hub, Sector 5, Bangalore",
            phone = "+91 9988776655",
            email = "contact@omni.com"
        )
        
        val mockCustomer = Customer(
            id = 101L,
            name = "John Doe (Demo)", 
            phone = "9876543210"
        )
        
        val mockItems = listOf(
            InvoiceItem(
                itemName = "Premium Milk (2L)", 
                quantity = 2.0, 
                rate = 120.0, 
                total = 240.0
            ),
            InvoiceItem(
                itemName = "Organic Eggs (12pk)", 
                quantity = 1.0, 
                rate = 80.0, 
                total = 80.0
            )
        )
        
        val mockInvoice = Invoice(
            invoiceId = "INV-2024-DEMO", 
            customerName = mockCustomer.name,
            customerPhone = mockCustomer.phone ?: "",
            customerAddress = "",
            items = mockItems,
            subtotal = 320.0,
            finalAmount = 320.0,
            paymentStatus = "PAID",
            paymentMethod = "UPI",
            date = System.currentTimeMillis(),
            notes = "Thank you for testing HisabKitab Pro Ultra AI Pilot!"
        )

        val headerProfile = businessProfile?.takeIf { p ->
            p.businessName.isNotBlank() ||
                p.logoUrl.isNotBlank() ||
                p.phone.isNotBlank() ||
                p.address.isNotBlank() ||
                p.qrImagePath.isNotBlank()
        } ?: mockBusiness

        // Same as before: single classic body preview (template id selects settings, not alternate Compose layout)
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            ClassicTemplate(headerProfile, mockCustomer, mockItems, mockInvoice)
        }
    }

    fun generatePdf(context: Context, templateId: String, invoice: Invoice, businessProfile: BusinessProfile?): String {
        return PdfInvoiceGenerator.generateProfessionalPdf(
            context = context,
            invoice = invoice,
            businessProfile = businessProfile,
            templateId = templateId
        ).absolutePath
    }
}

@Composable
fun ClassicTemplate(
    business: BusinessProfile,
    customer: Customer,
    items: List<InvoiceItem>,
    invoice: Invoice,
    showBrandedHeader: Boolean = true,
) {
    val ctx = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showBrandedHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                if (business.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(business.logoUrl).build(),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        business.businessName.ifBlank { "Business" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Black,
                    )
                    if (business.ownerName.isNotBlank()) {
                        Text(business.ownerName, fontSize = 13.sp, color = Color.DarkGray)
                    }
                    if (business.address.isNotBlank()) {
                        Text(business.address, fontSize = 12.sp, color = Color.Gray)
                    }
                    val phone = business.phone.trim()
                    val email = business.email.trim()
                    if (phone.isNotEmpty() || email.isNotEmpty()) {
                        Text(
                            buildString {
                                if (phone.isNotEmpty()) append("Phone: $phone")
                                if (phone.isNotEmpty() && email.isNotEmpty()) append("  •  ")
                                if (email.isNotEmpty()) append("Email: $email")
                            },
                            fontSize = 11.sp,
                            color = Color.Gray,
                        )
                    }
                    if (business.gstNumber.isNotBlank()) {
                        Text("GSTIN: ${business.gstNumber}", fontSize = 11.sp, color = Color.DarkGray)
                    }
                    if (business.panNumber.isNotBlank()) {
                        Text("PAN: ${business.panNumber}", fontSize = 11.sp, color = Color.DarkGray)
                    }
                    if (business.upiId.isNotBlank()) {
                        Text("UPI: ${business.upiId}", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Text(business.businessName, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.Black)
            if (business.address.isNotBlank()) {
                Text(business.address, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(20.dp))
        }
        
        HorizontalDivider(thickness = 2.dp, color = Color.Black)
        Spacer(Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("BILL TO:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(customer.phone ?: "", fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("INVOICE DATE:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                Text(invoice.invoiceId, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Header Row
        Row(modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp)) {
            Text("ITEM", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("QTY", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("RATE", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("TOTAL", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        
        items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(item.itemName, modifier = Modifier.weight(1f), fontSize = 14.sp)
                Text("${item.quantity.toInt()}", modifier = Modifier.width(40.dp), fontSize = 14.sp)
                Text("₹${item.rate}", modifier = Modifier.width(70.dp), fontSize = 14.sp)
                Text("₹${item.total}", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                Text("Total Amount Due:", fontSize = 14.sp, color = Color.Gray)
                Text("₹${invoice.finalAmount}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.Black)
            }
        }

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            if (business.qrImagePath.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(business.qrImagePath).build(),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "SCAN QR TO PAY SECURELY",
                fontSize = 11.sp,
                color = Color.Gray,
            )
            ProfileMapFooter.invoiceLocationCaption(business)?.let { cap ->
                Spacer(Modifier.height(4.dp))
                Text(cap, fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(invoice.notes ?: "", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun ModernTemplate(business: BusinessProfile, customer: Customer, items: List<InvoiceItem>, invoice: Invoice) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A237E)).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(business.businessName, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.White)
                    Text("Professional Invoice", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
                Text("#${invoice.invoiceId.takeLast(4)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("FROM:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Blue)
                Text(business.ownerName, fontWeight = FontWeight.Bold)
                Text(business.address, fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("TO:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Blue)
                Text(customer.name, fontWeight = FontWeight.Bold)
                Text(customer.phone ?: "", fontSize = 11.sp, color = Color.Gray)
            }
        }
        
        Spacer(Modifier.height(24.dp))

        ClassicTemplate(business, customer, items, invoice, showBrandedHeader = false)
    }
}
