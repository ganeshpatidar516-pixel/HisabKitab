package com.ganesh.hisabkitabpro.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import com.ganesh.hisabkitabpro.MainActivity
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object WhatsAppBillSender {

    private const val TAG = "HK_BillShare"

    private const val PKG_WHATSAPP = "com.whatsapp"
    private const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    /**
     * Android 10+ expects [ClipData] on ACTION_SEND with content [Uri] so receiving apps
     * (WhatsApp, Telegram) get a durable read grant; without it some OEMs throw [SecurityException]
     * at [Context.startActivity] or the target app crashes reading the stream.
     */
    private fun Intent.attachPdfUri(uri: Uri) {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("application/pdf", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun uriForShareablePdf(context: Context, pdfFile: File): Uri? =
        try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile,
            )
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "FileProvider rejected path (outside configured roots?): ${pdfFile.absolutePath}", e)
            null
        }

    /**
     * Starts **any** external handoff (chooser, WhatsApp, mail, etc.) from [MainActivity] while
     * app-lock is enabled: sets [MainActivity.deferAppLockRearmOnNextStop] so [Lifecycle.Event.ON_STOP]
     * does not force [AppLockGate] over the ledger when the user is only in another app briefly.
     *
     * Call this instead of raw [Context.startActivity] for share/export flows.
     */
    fun startShareActivity(context: Context, intent: Intent): Boolean {
        return try {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            (context as? MainActivity)?.deferAppLockRearmOnNextStop = true
            context.startActivity(intent)
            true
        } catch (e: SecurityException) {
            (context as? MainActivity)?.deferAppLockRearmOnNextStop = false
            Log.e(TAG, "startActivity SecurityException (URI grant / policy)", e)
            Toast.makeText(context, "Could not hand off the PDF for sharing.", Toast.LENGTH_LONG).show()
            false
        } catch (e: android.content.ActivityNotFoundException) {
            (context as? MainActivity)?.deferAppLockRearmOnNextStop = false
            Log.e(TAG, "startActivity ActivityNotFoundException", e)
            Toast.makeText(context, "No app found to share this bill.", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean = try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * Digits-only international number for wa.me / api.whatsapp.com / jid (no + prefix).
     * 10-digit numbers are treated as India (+91) to match local ledger defaults.
     */
    fun toInternationalWhatsAppDigits(phoneRaw: String): String? {
        val d = phoneRaw.filter { it.isDigit() }
        if (d.length < 10) return null
        return when {
            d.length == 10 -> "91$d"
            d.length in 11..15 -> d
            d.length > 15 -> d.takeLast(15)
            else -> null
        }
    }

    private fun buildJid(internationalDigits: String): String =
        "$internationalDigits@s.whatsapp.net"

    /**
     * Opens **the customer's chat** with PDF attached (OkCredit-style), without the system contact picker.
     * Uses [Intent.ACTION_SEND] + `jid` extra (WhatsApp / W4B) — this is how media can target a chat on Android.
     * [Intent.ACTION_VIEW] + https URL cannot attach a PDF; use [createWhatsAppApiViewIntent] only as text fallback.
     */
    fun createDirectWhatsAppPdfIntent(context: Context, phoneRaw: String, pdfFile: File): Intent? {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
            return null
        }
        val international = toInternationalWhatsAppDigits(phoneRaw) ?: run {
            Toast.makeText(context, "Customer phone missing — save phone in profile", Toast.LENGTH_LONG).show()
            return null
        }
        val jid = buildJid(international)
        val uri = uriForShareablePdf(context, pdfFile) ?: run {
            Toast.makeText(context, "Cannot share this PDF from its saved location.", Toast.LENGTH_LONG).show()
            return null
        }
        val pm = context.packageManager
        val packages = listOf(PKG_WHATSAPP, PKG_WHATSAPP_BUSINESS).filter { isPackageInstalled(pm, it) }
        if (packages.isEmpty()) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_LONG).show()
            return null
        }

        for (pkg in packages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                setPackage(pkg)
                attachPdfUri(uri)
                putExtra(Intent.EXTRA_TEXT, "HisabKitab Pro — bill")
                putExtra("jid", jid)
            }
            if (intent.resolveActivity(pm) != null) {
                return intent
            }
        }

        for (pkg in packages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                setPackage(pkg)
                attachPdfUri(uri)
                putExtra(Intent.EXTRA_TEXT, "HisabKitab Pro — bill")
            }
            if (intent.resolveActivity(pm) != null) {
                return intent
            }
        }

        Toast.makeText(
            context,
            "Could not open WhatsApp with this PDF — try share chooser",
            Toast.LENGTH_LONG
        ).show()
        return createChooserPdfIntent(context, pdfFile)
    }

    /**
     * [Intent.ACTION_VIEW] — opens WhatsApp (or browser) on the right chat with **text only** (no PDF via URL).
     * Use when SEND+jid is not available; PDF must be shared separately.
     */
    fun createWhatsAppApiViewIntent(context: Context, phoneRaw: String, message: String): Intent? {
        val international = toInternationalWhatsAppDigits(phoneRaw) ?: run {
            Toast.makeText(context, "Customer phone missing — save phone in profile", Toast.LENGTH_LONG).show()
            return null
        }
        val text = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val url = "https://api.whatsapp.com/send?phone=$international&text=$text"
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Share PDF via system chooser — may show contact/app picker. */
    fun createChooserPdfIntent(context: Context, pdfFile: File): Intent? {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
            return null
        }
        val uri = uriForShareablePdf(context, pdfFile) ?: return null
        return try {
            val send = Intent(Intent.ACTION_SEND).apply { attachPdfUri(uri) }
            Intent.createChooser(send, "Share bill").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createChooser failed", e)
            Toast.makeText(context, "Could not build share intent", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /** Opens WhatsApp click-to-chat (text only). */
    fun openChat(context: Context, phoneRaw: String) {
        val international = toInternationalWhatsAppDigits(phoneRaw) ?: return
        val uri = Uri.parse("https://wa.me/$international")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }
}
