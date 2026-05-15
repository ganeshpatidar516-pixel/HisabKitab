package com.ganesh.hisabkitabpro.domain.ledger

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.profile.ProfileBitmapLoader
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    private const val TAG = "InvoicePdfGenerator"

    private fun sharedInvoiceDir(context: Context): File =
        File(context.getExternalFilesDir(null), "shared").apply { mkdirs() }

    private fun legacyInvoiceFile(context: Context, transactionId: Long): File =
        File(context.getExternalFilesDir(null), "INV_${transactionId}.pdf")

    fun getInvoicePdfFile(context: Context, transactionId: Long): File =
        File(sharedInvoiceDir(context), "INV_${transactionId}.pdf")

    /**
     * Backward-compatible resolver:
     * - prefers hardened shared folder
     * - migrates legacy root invoice to shared folder when found
     */
    fun resolveInvoicePdfFile(context: Context, transactionId: Long): File? {
        val secure = getInvoicePdfFile(context, transactionId)
        if (secure.exists()) return secure
        val legacy = legacyInvoiceFile(context, transactionId)
        if (!legacy.exists()) return null
        return runCatching {
            legacy.copyTo(secure, overwrite = true)
            legacy.delete()
            secure
        }.getOrElse {
            Log.w(TAG, "Legacy invoice migration to shared folder failed", it)
            null
        }
    }

    private fun embedBitmap(document: Document, bitmap: Bitmap, maxWidthPt: Float, alignment: HorizontalAlignment) {
        try {
            val os = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 92, os)
            val image = Image(ImageDataFactory.create(os.toByteArray())).setWidth(maxWidthPt)
            document.add(image.setHorizontalAlignment(alignment))
        } catch (e: Exception) {
            Log.w(TAG, "Bitmap embed skipped", e)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    /**
     * Single-page receipt PDF for a ledger transaction. Branding is loaded from
     * [BusinessProfile] (same source as bills); falls back to neutral copy if profile is missing.
     */
    suspend fun generateInvoicePDF(context: Context, customer: Customer, transaction: Transaction): File? {
        val appCtx = context.applicationContext
        val profile = AppDatabase.getDatabase(appCtx).businessProfileDao().getBusinessProfileOnce()

        val fileName = "INV_${transaction.id}.pdf"
        val sharedDir = sharedInvoiceDir(appCtx)
        val file = File(sharedDir, fileName)

        return try {
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            val sdfDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

            val businessTitle = profile?.businessName?.trim()?.takeIf { it.isNotEmpty() } ?: "HisabKitab Pro"

            profile?.logoUrl?.let { ProfileBitmapLoader.loadBitmapForPdfEmbed(appCtx, it) }?.let { bmp ->
                embedBitmap(document, bmp, 72f, HorizontalAlignment.CENTER)
            }

            document.add(
                Paragraph(businessTitle)
                    .setFontSize(20f)
                    .setBold()
                    .setFontColor(ColorConstants.BLACK)
                    .setTextAlignment(TextAlignment.CENTER),
            )

            profile?.ownerName?.trim()?.takeIf { it.isNotEmpty() }?.let { owner ->
                document.add(
                    Paragraph("Proprietor: $owner")
                        .setFontSize(11f)
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setTextAlignment(TextAlignment.CENTER),
                )
            }

            val contactLines = buildList {
                profile?.phone?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Phone: $it") }
                profile?.email?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Email: $it") }
            }
            if (contactLines.isNotEmpty()) {
                document.add(
                    Paragraph(contactLines.joinToString("  •  "))
                        .setFontSize(10f)
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setTextAlignment(TextAlignment.CENTER),
                )
            }

            profile?.address?.trim()?.takeIf { it.isNotEmpty() }?.let { addr ->
                document.add(
                    Paragraph(addr)
                        .setFontSize(9f)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER),
                )
            }

            val taxBits = buildList {
                profile?.gstNumber?.trim()?.takeIf { it.isNotEmpty() }?.let { add("GSTIN: $it") }
                profile?.panNumber?.trim()?.takeIf { it.isNotEmpty() }?.let { add("PAN: $it") }
            }
            if (taxBits.isNotEmpty()) {
                document.add(
                    Paragraph(taxBits.joinToString("  •  "))
                        .setFontSize(9f)
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setTextAlignment(TextAlignment.CENTER),
                )
            }

            profile?.upiId?.trim()?.takeIf { it.isNotEmpty() }?.let { upi ->
                document.add(
                    Paragraph("UPI: $upi")
                        .setFontSize(9f)
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setTextAlignment(TextAlignment.CENTER),
                )
            }

            document.add(
                Paragraph("________________")
                    .setFontSize(12f)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(16f),
            )

            val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()

            table.addCell(Paragraph("Customer Name:").setBold())
            table.addCell(Paragraph(customer.name))

            table.addCell(Paragraph("Phone Number:").setBold())
            table.addCell(Paragraph(customer.phone))

            table.addCell(Paragraph("Transaction Date:").setBold())
            table.addCell(Paragraph(sdfDate.format(Date(transaction.createdAt))))

            table.addCell(Paragraph("Transaction Time:").setBold())
            table.addCell(Paragraph(sdfTime.format(Date(transaction.createdAt))))

            val desc = transaction.note?.trim().orEmpty()
            if (desc.isNotEmpty()) {
                table.addCell(Paragraph("Bill / Note:").setBold())
                table.addCell(Paragraph(desc))
            }

            document.add(table.setMarginBottom(18f))

            document.add(
                Paragraph("Transaction Amount")
                    .setBold()
                    .setFontSize(14f)
                    .setTextAlignment(TextAlignment.RIGHT),
            )
            document.add(
                Paragraph(currencyFormatter.format(transaction.amount / 100.0))
                    .setFontSize(20f)
                    .setBold()
                    .setFontColor(ColorConstants.RED)
                    .setTextAlignment(TextAlignment.RIGHT),
            )

            profile?.qrImagePath?.let { ProfileBitmapLoader.loadBitmapForPdfEmbed(appCtx, it) }?.let { qr ->
                embedBitmap(document, qr, 120f, HorizontalAlignment.CENTER)
            }

            document.add(
                Paragraph("\nThank you for your business!")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY),
            )

            document.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF", e)
            null
        }
    }

    fun sharePdfFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file,
            )
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "HisabKitab Pro — Bill")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(share, "Share bill")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing PDF", e)
        }
    }

    fun openPdfFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val chooser = Intent.createChooser(intent, "Open PDF with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF", e)
        }
    }
}
