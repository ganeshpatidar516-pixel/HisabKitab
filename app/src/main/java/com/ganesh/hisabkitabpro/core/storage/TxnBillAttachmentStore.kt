package com.ganesh.hisabkitabpro.core.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

/**
 * P2 — persists customer OCR bill images on device (no Room migration).
 * Files live under `files/txn_bill_attachments/{transactionId}.jpg`.
 */
object TxnBillAttachmentStore {

    private const val TAG = "TxnBillAttachment"
    private const val DIR = "txn_bill_attachments"

    fun attachAfterTransactionSave(context: Context, sourceUriString: String?, transactionId: Long) {
        if (sourceUriString.isNullOrBlank() || transactionId <= 0L) return
        val dest = fileForTransaction(context, transactionId) ?: return
        runCatching {
            val uri = Uri.parse(sourceUriString)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return
            OcrBillAttachmentUri.deleteOwnedFileIfPossible(context, sourceUriString)
            Log.i(TAG, "attached bill image txnId=$transactionId")
        }.onFailure {
            Log.w(TAG, "attach bill image failed txnId=$transactionId", it)
        }
    }

    fun fileForTransaction(context: Context, transactionId: Long): File? {
        if (transactionId <= 0L) return null
        val dir = File(context.filesDir, DIR)
        if (!dir.exists() && !dir.mkdirs()) return null
        return File(dir, "$transactionId.jpg")
    }

    fun deleteForTransaction(context: Context, transactionId: Long) {
        fileForTransaction(context, transactionId)?.deleteQuietly()
    }

    fun wipeAll(context: Context) {
        File(context.filesDir, DIR).deleteRecursivelyQuietly()
    }

    private fun File.deleteQuietly() {
        runCatching { delete() }
    }

    private fun File.deleteRecursivelyQuietly() {
        runCatching { deleteRecursively() }
    }
}
