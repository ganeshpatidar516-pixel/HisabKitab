package com.ganesh.hisabkitabpro.domain.invoice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.LogoRenderFit
import com.ganesh.hisabkitabpro.domain.profile.ProfileBitmapLoader
import com.ganesh.hisabkitabpro.domain.profile.ProfileMapFooter
import com.ganesh.hisabkitabpro.domain.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfInvoiceGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 50f
    private const val ITEM_ROW_HEIGHT = 25f
    /** Y threshold — start a new page before rows would collide with totals/footer. */
    private const val ITEMS_PAGE_BREAK_Y = 520f
    private const val TOTALS_PAGE_BREAK_Y = 620f
    private const val MAX_PDF_PAGES = 24

    /**
     * Thrown when line items cannot fit within [MAX_PDF_PAGES] (prevents silent truncation).
     */
    class InvoicePdfLayoutException(
        val totalItems: Int,
        val renderedItems: Int,
    ) : Exception("Invoice has too many line items to render ($renderedItems of $totalItems on PDF)")

    /**
     * [Block 7: Integration Layer] - स्मार्ट इनवॉइस जनरेटर (V3: Layout Precision Update).
     * Header: logo + identity; payment QR sits bottom-left above the scan instruction.
     * P1: multi-page line items — no silent drop at a fixed Y.
     */
    fun generateProfessionalPdf(
        context: Context,
        invoice: Invoice,
        businessProfile: BusinessProfile?,
        templateId: String = "template_1",
        qrBitmap: Bitmap? = null
    ): File {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val template = TemplateRegistry.getTemplateById(TemplateRegistry.normalizePdfTemplateId(templateId))
        val headerColor = if (template.layoutType == TemplateLayoutType.MODERN) {
            0xFF1A237E.toInt()
        } else {
            0xFF000000.toInt()
        }

        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        var canvas = page.canvas

        var y = drawBusinessHeader(context, canvas, paint, businessProfile, headerColor)

        paint.color = 0xFF000000.toInt()
        y += 40f
        paint.textSize = 12f
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        canvas.drawText("Invoice ID: ${invoice.invoiceId}", MARGIN, y, paint)
        canvas.drawText("Date: ${sdf.format(Date(invoice.date))}", 400f, y, paint)

        y += 40f
        canvas.drawLine(MARGIN, y, 550f, y, paint)

        y += 30f
        paint.isFakeBoldText = true
        canvas.drawText("BILL TO:", MARGIN, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        canvas.drawText(invoice.customerName, MARGIN, y, paint)
        y += 15f
        canvas.drawText("Phone: ${invoice.customerPhone}", MARGIN, y, paint)
        if (invoice.customerAddress.isNotEmpty()) {
            y += 15f
            canvas.drawText(invoice.customerAddress, MARGIN, y, paint)
        }

        y += 40f
        y = drawItemsTableHeader(canvas, paint, y)

        var itemIndex = 0
        val items = invoice.items
        while (itemIndex < items.size) {
            if (y > ITEMS_PAGE_BREAK_Y) {
                document.finishPage(page)
                pageNumber++
                if (pageNumber > MAX_PDF_PAGES) {
                    document.close()
                    throw InvoicePdfLayoutException(items.size, itemIndex)
                }
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
                y = 72f
                paint.textSize = 10f
                paint.color = 0xFF666666.toInt()
                canvas.drawText("Invoice ${invoice.invoiceId} (continued)", MARGIN, y, paint)
                y += 22f
                paint.color = 0xFF000000.toInt()
                y = drawItemsTableHeader(canvas, paint, y)
            }
            val item = items[itemIndex]
            canvas.drawText(item.itemName, MARGIN, y, paint)
            canvas.drawText(item.quantity.toString(), 300f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.rate), 380f, y, paint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.total), 480f, y, paint)
            y += ITEM_ROW_HEIGHT
            itemIndex++
        }

        if (y > TOTALS_PAGE_BREAK_Y) {
            document.finishPage(page)
            pageNumber++
            if (pageNumber > MAX_PDF_PAGES) {
                document.close()
                throw InvoicePdfLayoutException(items.size, items.size)
            }
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            canvas = page.canvas
            y = 72f
        }

        y += 10f
        canvas.drawLine(MARGIN, y, 550f, y, paint)

        y += 30f
        canvas.drawText("Subtotal:", 350f, y, paint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", invoice.subtotal)}", 480f, y, paint)

        if (invoice.gstEnabled && invoice.gstAmount > 0) {
            y += 20f
            val taxLabel = invoice.taxLineLabel ?: "GST"
            canvas.drawText("$taxLabel (${invoice.gstPercent}%):", 350f, y, paint)
            canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", invoice.gstAmount)}", 480f, y, paint)
        }

        if (invoice.discount > 0) {
            y += 20f
            canvas.drawText("Discount:", 350f, y, paint)
            canvas.drawText("-₹${String.format(Locale.getDefault(), "%.2f", invoice.discount)}", 480f, y, paint)
        }

        y += 35f
        paint.isFakeBoldText = true
        paint.textSize = 14f
        canvas.drawText("GRAND TOTAL:", 300f, y, paint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", invoice.finalAmount)}", 480f, y, paint)

        val footerQrSize = 88
        val qrTop = y + 28f
        var scanTextBaseline = y + 36f
        val qrSrcFooter = qrBitmap ?: businessProfile?.qrImagePath?.let {
            ProfileBitmapLoader.loadBitmapForPdfEmbed(context, it)
        }
        val qrFooterOwned = qrBitmap == null && qrSrcFooter != null
        qrSrcFooter?.let { src ->
            val scaledQr = Bitmap.createScaledBitmap(src, footerQrSize, footerQrSize, true)
            canvas.drawBitmap(scaledQr, MARGIN, qrTop, null)
            scanTextBaseline = qrTop + footerQrSize + 10f
            if (scaledQr != src && !scaledQr.isRecycled) scaledQr.recycle()
            if (qrFooterOwned && !src.isRecycled) src.recycle()
        }

        paint.textSize = 8f
        paint.isFakeBoldText = false
        paint.color = 0xFF666666.toInt()
        canvas.drawText("SCAN QR TO PAY SECURELY", MARGIN, scanTextBaseline, paint)
        scanTextBaseline += 12f
        ProfileMapFooter.invoiceLocationCaption(businessProfile)?.let { cap ->
            canvas.drawText(cap, MARGIN, scanTextBaseline, paint)
            scanTextBaseline += 12f
        }

        val signTop = (scanTextBaseline + 10f).coerceAtLeast(y + 56f)
        businessProfile?.signatureImagePath?.let { ProfileBitmapLoader.loadBitmapForPdfEmbed(context, it) }?.let { sig ->
            if (signTop + 44f < 810f) {
                LogoRenderFit.drawWithinRect(canvas, sig, RectF(400f, signTop, 545f, signTop + 44f))
            }
            if (!sig.isRecycled) sig.recycle()
        }

        document.finishPage(page)
        val file = File(
            com.ganesh.hisabkitabpro.core.storage.AppStoragePaths.invoicesCacheDir(context),
            "Invoice_${invoice.invoiceId}.pdf",
        )
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    private fun drawBusinessHeader(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        businessProfile: BusinessProfile?,
        headerColor: Int,
    ): Float {
        var y = 78f
        val logo = businessProfile?.logoUrl?.let { ProfileBitmapLoader.loadBitmapForPdfEmbed(context, it) }
        val logoSlot = RectF(MARGIN, y - 18f, MARGIN + 64f, y - 18f + 64f)
        var hadLogo = false
        logo?.let {
            LogoRenderFit.drawWithinRect(canvas, it, logoSlot)
            hadLogo = true
            if (!it.isRecycled) it.recycle()
        }
        val textLeft = if (hadLogo) logoSlot.right + 12f else MARGIN

        paint.color = headerColor
        paint.textSize = 22f
        paint.isFakeBoldText = true
        val bName = businessProfile?.businessName?.trim()?.uppercase(Locale.getDefault())?.takeIf { it.isNotEmpty() }
            ?: "HISABKITAB PRO"
        canvas.drawText(bName, textLeft, y, paint)

        paint.isFakeBoldText = false
        paint.textSize = 10f
        paint.color = 0xFF666666.toInt()

        businessProfile?.let { profile ->
            y += 22f
            profile.ownerName.trim().takeIf { it.isNotEmpty() }?.let {
                canvas.drawText(it, textLeft, y, paint)
                y += 14f
            }
            if (profile.address.isNotEmpty()) {
                canvas.drawText(profile.address, textLeft, y, paint)
                y += 14f
            }
            val phone = profile.phone.trim()
            val email = profile.email.trim()
            if (phone.isNotEmpty() || email.isNotEmpty()) {
                val contactInfo = buildString {
                    if (phone.isNotEmpty()) append("Phone: $phone")
                    if (phone.isNotEmpty() && email.isNotEmpty()) append("  |  ")
                    if (email.isNotEmpty()) append("Email: $email")
                }
                canvas.drawText(contactInfo, textLeft, y, paint)
                y += 14f
            }
            profile.websiteUrl.trim().takeIf { it.isNotEmpty() }?.let {
                canvas.drawText(it, textLeft, y, paint)
                y += 14f
            }
            profile.upiId.trim().takeIf { it.isNotEmpty() }?.let {
                canvas.drawText("UPI: $it", textLeft, y, paint)
                y += 14f
            }
            if (profile.gstNumber.isNotEmpty()) {
                canvas.drawText("GSTIN: ${profile.gstNumber}", textLeft, y, paint)
                y += 14f
            }
            profile.panNumber.trim().takeIf { it.isNotEmpty() }?.let {
                canvas.drawText("PAN: $it", textLeft, y, paint)
            }
        } ?: run {
            y += 20f
        }
        return y
    }

    private fun drawItemsTableHeader(canvas: Canvas, paint: Paint, y: Float): Float {
        var rowY = y
        paint.isFakeBoldText = true
        paint.textSize = 11f
        paint.color = 0xFF000000.toInt()
        canvas.drawText("ITEM NAME", MARGIN, rowY, paint)
        canvas.drawText("QTY", 300f, rowY, paint)
        canvas.drawText("RATE", 380f, rowY, paint)
        canvas.drawText("TOTAL", 480f, rowY, paint)
        rowY += 10f
        canvas.drawLine(MARGIN, rowY, 550f, rowY, paint)
        rowY += 30f
        paint.isFakeBoldText = false
        return rowY
    }
}
