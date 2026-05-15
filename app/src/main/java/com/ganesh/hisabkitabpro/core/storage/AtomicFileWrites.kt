package com.ganesh.hisabkitabpro.core.storage

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Write to a temp sibling file, then rename — avoids half-written PDFs on interruption.
 */
object AtomicFileWrites {

    private const val TAG = "AtomicFileWrites"

    fun writeAtomically(
        context: Context,
        dest: File,
        minFreeMb: Long = 8L,
        writeTemp: (File) -> Unit,
    ): Boolean {
        if (!StorageSpaceGuard.hasMinFreeSpace(context, minFreeMb)) {
            Log.w(TAG, "Skipped atomic write — low storage for ${dest.name}")
            return false
        }
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "${dest.name}.tmp")
        if (tmp.exists()) tmp.delete()
        return try {
            writeTemp(tmp)
            if (!tmp.exists() || tmp.length() <= 0L) {
                tmp.delete()
                false
            } else {
                if (dest.exists()) dest.delete()
                val renamed = tmp.renameTo(dest)
                if (!renamed) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                dest.exists() && dest.length() > 0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Atomic write failed for ${dest.name}", e)
            if (tmp.exists()) tmp.delete()
            false
        }
    }
}
