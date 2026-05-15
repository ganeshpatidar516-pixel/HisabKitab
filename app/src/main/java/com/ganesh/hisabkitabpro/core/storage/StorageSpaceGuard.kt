package com.ganesh.hisabkitabpro.core.storage

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * Low-storage preflight for writes (PDF, OCR import, DB snapshot).
 */
object StorageSpaceGuard {

    private const val DEFAULT_MIN_FREE_MB = 32L

    fun hasMinFreeSpace(context: Context, minFreeMb: Long = DEFAULT_MIN_FREE_MB): Boolean {
        val dir = context.filesDir ?: return true
        return hasMinFreeSpace(dir, minFreeMb)
    }

    fun hasMinFreeSpace(targetDir: File, minFreeMb: Long = DEFAULT_MIN_FREE_MB): Boolean {
        return runCatching {
            val stat = StatFs(targetDir.absolutePath)
            val available = stat.availableBytes
            available >= minFreeMb * 1024L * 1024L
        }.getOrDefault(true)
    }
}
