package com.ganesh.hisabkitabpro.core.storage

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.repository.local.BusinessProfileDao
import java.io.File

/**
 * One-time migration from broad FileProvider roots to narrow subfolders (Phase 9).
 * Updates [business_profile] absolute paths when media files move.
 */
object FileProviderStorageMigration {

    private const val TAG = "HK_FileProviderMigr"
    const val PREF_STORAGE_LAYOUT_V2 = "hk_storage_file_provider_layout_v2"

    suspend fun runIfNeeded(context: Context, db: AppDatabase) {
        val prefs = context.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_STORAGE_LAYOUT_V2, false)) return
        val app = context.applicationContext
        try {
            migrateMediaFiles(app)
            migrateCachePdfs(app)
            updateBusinessProfilePaths(app, db.businessProfileDao())
            prefs.edit().putBoolean(PREF_STORAGE_LAYOUT_V2, true).commit()
            Log.i(TAG, "storage_layout_v2 applied")
        } catch (t: Throwable) {
            Log.e(TAG, "storage_layout_v2 failed — will retry on next cold start", t)
        }
    }

    private fun migrateMediaFiles(context: Context) {
        val legacyQr = File(context.filesDir, "business_qr.png")
        val legacyLogo = File(context.filesDir, "business_logo.png")
        val legacySig = File(context.filesDir, "business_signature.png")
        val legacyProcessed = File(context.filesDir, "business_qr_processed.png")
        moveIfExists(legacyQr, AppStoragePaths.businessQrFile(context))
        moveIfExists(legacyLogo, AppStoragePaths.businessLogoFile(context))
        moveIfExists(legacySig, AppStoragePaths.businessSignatureFile(context))
        moveIfExists(legacyProcessed, File(AppStoragePaths.mediaQrDir(context), "business_qr_processed.png"))
    }

    private fun migrateCachePdfs(context: Context) {
        val root = context.cacheDir.listFiles() ?: return
        for (child in root) {
            if (!child.isFile) continue
            val name = child.name
            when {
                name.startsWith("Invoice_") && name.endsWith(".pdf") ->
                    moveIfExists(child, File(AppStoragePaths.invoicesCacheDir(context), name))
                name.startsWith("HisabKitab_Premium_") && name.endsWith(".pdf") ->
                    moveIfExists(child, File(AppStoragePaths.invoicesCacheDir(context), name))
                name.startsWith("invoice_") && name.endsWith(".pdf") ->
                    moveIfExists(child, File(AppStoragePaths.invoicesCacheDir(context), name))
                name.contains("_Supplier_Statement_", ignoreCase = true) && name.endsWith(".pdf") ->
                    moveIfExists(child, File(AppStoragePaths.exportsCacheDir(context), name))
                name.startsWith("Statement_") && name.endsWith(".pdf") ->
                    moveIfExists(child, File(AppStoragePaths.exportsCacheDir(context), name))
                name.startsWith("reminder_history_") &&
                    (name.endsWith(".pdf") || name.endsWith(".csv")) ->
                    moveIfExists(child, File(AppStoragePaths.exportsCacheDir(context), name))
                name.startsWith("supplier_") && name.endsWith("_ledger.csv") ->
                    moveIfExists(child, File(AppStoragePaths.exportsCacheDir(context), name))
                name.contains("_Statement_") && name.endsWith(".pdf") ->
                    moveIfExists(child, File(AppStoragePaths.exportsCacheDir(context), name))
                name.startsWith("ocr_scan_") && name.endsWith(".jpg") ->
                    moveIfExists(child, File(AppStoragePaths.ocrCacheDir(context), name))
                name.startsWith("Poster_") && name.endsWith(".png") ->
                    moveIfExists(child, File(AppStoragePaths.exportsCacheDir(context), name))
            }
        }
    }

    private suspend fun updateBusinessProfilePaths(context: Context, dao: BusinessProfileDao) {
        val profile = dao.getBusinessProfileOnce() ?: return
        val oldQr = File(context.filesDir, "business_qr.png").absolutePath
        val oldLogo = File(context.filesDir, "business_logo.png").absolutePath
        val oldSig = File(context.filesDir, "business_signature.png").absolutePath
        val newQrFile = AppStoragePaths.businessQrFile(context)
        val newLogoFile = AppStoragePaths.businessLogoFile(context)
        val newSigFile = AppStoragePaths.businessSignatureFile(context)

        var qrPath = profile.qrImagePath
        if (profile.qrImagePath.isNotBlank() &&
            (profile.qrImagePath == oldQr || profile.qrImagePath.endsWith("/business_qr.png")) &&
            newQrFile.exists()
        ) {
            qrPath = newQrFile.absolutePath
        }

        var logoPath = profile.logoUrl
        if (profile.logoUrl.isNotBlank() &&
            (profile.logoUrl == oldLogo || profile.logoUrl.endsWith("/business_logo.png")) &&
            newLogoFile.exists()
        ) {
            logoPath = newLogoFile.absolutePath
        }

        var sigPath = profile.signatureImagePath
        if (profile.signatureImagePath.isNotBlank() &&
            (profile.signatureImagePath == oldSig ||
                profile.signatureImagePath.endsWith("/business_signature.png")) &&
            newSigFile.exists()
        ) {
            sigPath = newSigFile.absolutePath
        }

        if (qrPath != profile.qrImagePath ||
            logoPath != profile.logoUrl ||
            sigPath != profile.signatureImagePath
        ) {
            dao.updateProfile(
                profile.copy(
                    qrImagePath = qrPath,
                    logoUrl = logoPath,
                    signatureImagePath = sigPath,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun moveIfExists(from: File, to: File) {
        if (!from.exists()) return
        to.parentFile?.mkdirs()
        if (to.exists() && from.absolutePath != to.absolutePath) {
            if (to.length() > 0L) {
                runCatching { from.delete() }
            }
            return
        }
        if (from.absolutePath == to.absolutePath) return
        val moved = from.renameTo(to)
        if (!moved) {
            runCatching {
                from.copyTo(to, overwrite = true)
            }.onSuccess {
                if (to.exists() && to.length() > 0L) {
                    runCatching { from.delete() }
                }
            }
        }
    }
}
