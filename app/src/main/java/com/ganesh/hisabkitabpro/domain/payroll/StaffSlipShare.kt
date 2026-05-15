package com.ganesh.hisabkitabpro.domain.payroll

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Tiny helper that wraps FileProvider + Intent plumbing for salary-slip PDFs.
 *
 * Kept in the staff domain package so it never reaches into invoice / ledger
 * code paths. Failures are swallowed (logged only) — the caller already has
 * the generated file path on disk and can fall back to it.
 */
object StaffSlipShare {

    private const val TAG = "StaffSlipShare"
    private const val MIME = "application/pdf"

    private fun authority(context: Context): String = "${context.packageName}.provider"

    private fun toUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    fun share(context: Context, file: File, subject: String = "Salary Slip"): Boolean = try {
        val uri = toUri(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share Salary Slip").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to share salary slip", e)
        false
    }

    fun open(context: Context, file: File): Boolean = try {
        val uri = toUri(context, file)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(view, "Open Salary Slip").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open salary slip", e)
        false
    }
}
