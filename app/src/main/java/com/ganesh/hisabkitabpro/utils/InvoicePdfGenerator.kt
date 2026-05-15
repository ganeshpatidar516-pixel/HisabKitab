package com.ganesh.hisabkitabpro.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.ganesh.hisabkitabpro.domain.model.InvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * LEGACY / DEAD-PAIRED — DO NOT USE FOR NEW CODE.
 *
 * Used only by the deprecated [com.ganesh.hisabkitabpro.ui.screens.InvoiceScreen],
 * which is itself unreachable from the live navigation graph. Confirmed stripped
 * from release AABs by R8 (see `app/build/outputs/mapping/release/usage.txt`).
 *
 * The LIVE invoice PDF generator for the customer-ledger flow is
 * [com.ganesh.hisabkitabpro.domain.ledger.InvoicePdfGenerator] — use that.
 *
 * Kept here only as a reference for the old standalone Invoice screen until
 * a future cleanup pass formally removes both files together.
 */
@Deprecated(
    message = "Legacy InvoicePdfGenerator paired with the deprecated ui.screens.InvoiceScreen. " +
        "Use com.ganesh.hisabkitabpro.domain.ledger.InvoicePdfGenerator for the live ledger flow.",
    replaceWith = ReplaceWith("com.ganesh.hisabkitabpro.domain.ledger.InvoicePdfGenerator"),
    level = DeprecationLevel.WARNING
)
object InvoicePdfGenerator {

    fun generateInvoicePdf(
        context: Context,
        items: List<InvoiceItem>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }

        val normalPaint = Paint().apply {
            textSize = 16f
        }

        var y = 60f

        // Title
        canvas.drawText("HisabKitab Invoice", 180f, y, titlePaint)
        y += 40f

        // Date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = dateFormat.format(Date())
        canvas.drawText("Date: $date", 50f, y, normalPaint)
        y += 40f

        // Table Headers
        canvas.drawText("Item", 50f, y, headerPaint)
        canvas.drawText("Qty", 300f, y, headerPaint)
        canvas.drawText("Price", 360f, y, headerPaint)
        canvas.drawText("Total", 460f, y, headerPaint)
        y += 20f

        canvas.drawLine(50f, y, 550f, y, normalPaint)
        y += 30f

        var grandTotal = 0.0
        items.forEach { item ->
            canvas.drawText(item.itemName, 50f, y, normalPaint)
            canvas.drawText(item.quantity.toString(), 300f, y, normalPaint)
            canvas.drawText("₹${item.rate}", 360f, y, normalPaint)
            canvas.drawText("₹${item.total}", 460f, y, normalPaint)
            grandTotal += item.total
            y += 30f
        }

        y += 20f
        canvas.drawLine(50f, y, 550f, y, normalPaint)
        y += 40f

        canvas.drawText("Grand Total: ₹$grandTotal", 350f, y, headerPaint)

        document.finishPage(page)
        val file = File(
            com.ganesh.hisabkitabpro.core.storage.AppStoragePaths.invoicesCacheDir(context),
            "invoice_${System.currentTimeMillis()}.pdf",
        )
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }
}
