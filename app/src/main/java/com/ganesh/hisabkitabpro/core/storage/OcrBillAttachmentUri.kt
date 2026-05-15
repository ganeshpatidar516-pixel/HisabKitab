package com.ganesh.hisabkitabpro.core.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Prefer FileProvider [content://] URIs for bill attachments (supplier OCR prefill).
 * Parses and deletes underlying cache files when safe.
 */
object OcrBillAttachmentUri {

    fun fromCacheFile(context: Context, file: File): String {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file,
        )
        return uri.toString()
    }

    fun deleteOwnedFileIfPossible(context: Context, uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        when (uri.scheme) {
            "file" -> File(uri.path.orEmpty()).deleteQuietly()
            "content" -> {
                if (uri.authority == "${context.packageName}.provider") {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
            }
        }
    }

    private fun File.deleteQuietly() {
        runCatching { delete() }
    }
}
