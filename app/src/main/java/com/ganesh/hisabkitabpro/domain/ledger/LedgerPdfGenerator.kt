package com.ganesh.hisabkitabpro.domain.ledger

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.StatementPdfBranding
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * HISABKITAB PRO - 📄 ULTRA-PRO MAX PDF GENERATOR
 * Generates Professional, High-Resolution Ledger Statements.
 * Updated for "CRASH-PROOF" (Enterprise Level).
 */
object LedgerPdfGenerator {

    fun generateLedgerPdf(
        context: Context, 
        customer: Customer, 
        transactions: List<Transaction>,
        businessProfile: BusinessProfile?,
        startDate: Long? = null,
        endDate: Long? = null
    ): File? {
        val startMs = System.currentTimeMillis()
        // Use applicationContext for safety
        val appContext = context.applicationContext
        val document = PdfDocument()
        
        return try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val currencyFormatter = try {
                NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            } catch (e: Exception) {
                NumberFormat.getCurrencyInstance(Locale.US) // Fallback
            }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            val margin = 50f
            var y = 60f
            val titleLeft = StatementPdfBranding.titleStartXAfterBranding(
                context = appContext,
                canvas = canvas,
                profile = businessProfile,
                margin = margin,
                titleBaselineY = y,
            )

            // 👑 1. PROFESSIONAL BRANDING
            val businessName = businessProfile?.businessName?.trim().takeUnless { it.isNullOrEmpty() } ?: "HisabKitab Pro Business"
            val businessAddress = businessProfile?.address?.trim().takeUnless { it.isNullOrEmpty() }
            val businessPhone = businessProfile?.phone?.trim().takeUnless { it.isNullOrEmpty() } ?: "N/A"
            val businessGstin = businessProfile?.gstNumber?.trim().takeUnless { it.isNullOrEmpty() } ?: "N/A"

            paint.color = Color.parseColor("#B8860B")
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(businessName, titleLeft, y, paint)
            
            y += 25f
            paint.textSize = 12f
            paint.color = Color.DKGRAY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Powered by HisabKitab Pro", 50f, y, paint)

            // 🏢 2. BUSINESS DETAILS
            y += 40f
            paint.textSize = 10f
            paint.color = Color.BLACK
            paint.typeface = Typeface.DEFAULT
            
            businessProfile?.let {
                canvas.drawText("Business: $businessName", 50f, y, paint)
                y += 15f
                businessAddress?.let { address ->
                    canvas.drawText(address, 50f, y, paint)
                    y += 15f
                }
                canvas.drawText("Phone: $businessPhone | GSTIN: $businessGstin", 50f, y, paint)
            } ?: run {
                canvas.drawText("Business: $businessName", 50f, y, paint)
                y += 15f
                canvas.drawText("Phone: $businessPhone | GSTIN: $businessGstin", 50f, y, paint)
            }

            // 👤 3. CUSTOMER DETAILS
            y += 40f
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CUSTOMER STATEMENT", 50f, y, paint)
            
            y += 25f
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Name: ${customer.name}", 50f, y, paint)
            canvas.drawText("Date Range: ${if (startDate != null) sdf.format(Date(startDate)) else "Beginning"} - ${if (endDate != null) sdf.format(Date(endDate)) else "Today"}", 300f, y, paint)
            
            y += 18f
            canvas.drawText("Phone: ${customer.phone ?: "N/A"}", 50f, y, paint)

            val filteredTransactions = if (startDate != null && endDate != null) {
                transactions.filter { it.createdAt in startDate..endDate }
            } else {
                transactions
            }

            val openingBalancePaise = if (startDate != null) {
                transactions.filter { it.createdAt < startDate && !it.isDeleted && it.status == "SUCCESS" }
                    .sumOf { 
                        if (it.type == TransactionType.CREDIT || it.type == TransactionType.INVOICE) it.amount else -it.amount 
                    }
            } else 0L

            y += 30f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Opening Balance: ${currencyFormatter.format(openingBalancePaise / 100.0)}", 50f, y, paint)

            // 📝 5. DATA TABLE
            y += 30f
            paint.color = Color.LTGRAY
            canvas.drawRect(50f, y - 20f, 545f, y + 10f, paint)
            
            paint.color = Color.BLACK
            canvas.drawText("Date", 60f, y, paint)
            canvas.drawText("Details", 150f, y, paint)
            canvas.drawText("Type", 350f, y, paint)
            canvas.drawText("Amount", 460f, y, paint)
            
            y += 30f
            paint.typeface = Typeface.DEFAULT
            
            filteredTransactions.take(25).forEach { tx -> // Simple limit for single page stability
                if (y > 780f) return@forEach
                
                canvas.drawText(sdf.format(Date(tx.createdAt)), 60f, y, paint)
                val details = if (tx.type == TransactionType.INVOICE) "Bill #${tx.id}" else (tx.note ?: "Manual Entry")
                canvas.drawText(details.take(20), 150f, y, paint)
                
                val isGiven = tx.type == TransactionType.CREDIT || tx.type == TransactionType.INVOICE
                paint.color = if (isGiven) Color.parseColor("#D32F2F") else Color.parseColor("#388E3C")
                canvas.drawText(if (isGiven) "GIVEN" else "RECEIVED", 350f, y, paint)
                
                canvas.drawText(currencyFormatter.format(tx.amount / 100.0), 460f, y, paint)
                
                paint.color = Color.BLACK
                y += 25f
            }

            y += 20f
            canvas.drawLine(50f, y, 545f, y, paint)
            
            y += 30f
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val closingBalancePaise = openingBalancePaise + filteredTransactions.sumOf { 
                if (it.type == TransactionType.CREDIT || it.type == TransactionType.INVOICE) it.amount else -it.amount 
            }
            
            canvas.drawText("Closing Balance: ${currencyFormatter.format(closingBalancePaise / 100.0)}", 50f, y, paint)

            document.finishPage(page)
            
            val fileName = "Statement_${customer.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(
                com.ganesh.hisabkitabpro.core.storage.AppStoragePaths.exportsCacheDir(appContext),
                fileName,
            )
            
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            Log.i("LedgerPdfGenerator", "PDF generated in ${System.currentTimeMillis() - startMs} ms")
            file
        } catch (e: Exception) {
            Log.e("LedgerPdfGenerator", "CRASH_PROOF_FALLBACK: PDF Generation failed", e)
            null
        } finally {
            document.close()
        }
    }
}
