package com.ganesh.hisabkitabpro.core.storage

import android.content.Context
import java.io.File

/**
 * Canonical on-device paths for FileProvider-narrowed sharing (Phase 9).
 * All new writes should use these; [FileProviderStorageMigration] moves legacy root files once.
 */
object AppStoragePaths {

    fun mediaRoot(context: Context): File = File(context.filesDir, "media").apply { mkdirs() }

    fun mediaQrDir(context: Context): File = File(mediaRoot(context), "qr").apply { mkdirs() }

    fun mediaLogoDir(context: Context): File = File(mediaRoot(context), "logo").apply { mkdirs() }

    fun mediaSignaturesDir(context: Context): File = File(mediaRoot(context), "signatures").apply { mkdirs() }

    fun businessQrFile(context: Context): File = File(mediaQrDir(context), "business_qr.png")

    fun businessLogoFile(context: Context): File = File(mediaLogoDir(context), "business_logo.png")

    fun businessSignatureFile(context: Context): File = File(mediaSignaturesDir(context), "business_signature.png")

    fun invoicesCacheDir(context: Context): File = File(context.cacheDir, "invoices").apply { mkdirs() }

    fun exportsCacheDir(context: Context): File = File(context.cacheDir, "exports").apply { mkdirs() }

    fun ocrCacheDir(context: Context): File = File(context.cacheDir, "ocr").apply { mkdirs() }
}
