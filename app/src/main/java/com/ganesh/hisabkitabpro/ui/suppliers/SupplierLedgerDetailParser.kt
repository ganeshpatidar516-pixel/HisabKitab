package com.ganesh.hisabkitabpro.ui.suppliers

import android.net.Uri

/**
 * Parses supplier audit [detail] lines built by [PartyViewModel.recordSupplierEntry].
 */
data class ParsedSupplierLedgerLine(
    val amountPaise: Long,
    val balanceAfterPaise: Long?,
    val tagRaw: String,
    val noteDisplay: String,
    val dueAtMillis: Long?,
    val billImageUri: String
) {
    val showTag: Boolean
        get() {
            val t = tagRaw.trim()
            if (t.isEmpty()) return false
            if (t.equals("general", ignoreCase = true)) return false
            if (t.equals("#general", ignoreCase = true)) return false
            return true
        }
}

object SupplierLedgerDetailParser {

    fun parse(detail: String?): ParsedSupplierLedgerLine {
        if (detail.isNullOrBlank()) {
            return ParsedSupplierLedgerLine(0L, null, "", "", null, "")
        }
        val amountPaise = extractLeadingLong(detail, "amountPaise=") ?: 0L
        val balanceAfter = extractLeadingLong(detail, "balanceAfter=")

        val billUri = if (detail.contains(",billImageUri=")) {
            detail.substringAfterLast(",billImageUri=", "")
        } else {
            ""
        }
        val withoutBill = if (detail.contains(",billImageUri=")) {
            detail.substringBeforeLast(",billImageUri=")
        } else {
            detail
        }

        val dueAt = if (withoutBill.contains(",dueAt=")) {
            val tail = withoutBill.substringAfter(",dueAt=", "")
            tail.takeWhile { it.isDigit() || it == '-' }.toLongOrNull()
        } else {
            null
        }

        val core = if (withoutBill.contains(",dueAt=")) {
            withoutBill.substringBefore(",dueAt=")
        } else {
            withoutBill
        }

        val noteEncoded = if (core.contains(",note=")) {
            core.substringAfter(",note=", "")
        } else {
            ""
        }
        val tagEncoded = if (core.contains(",tag=") && core.contains(",note=")) {
            core.substringAfter(",tag=", "").substringBefore(",note=")
        } else if (core.contains(",tag=")) {
            core.substringAfter(",tag=", "")
        } else {
            ""
        }

        val noteDecoded = safeDecode(noteEncoded)
        val tagDecoded = safeDecode(tagEncoded)

        return ParsedSupplierLedgerLine(
            amountPaise = amountPaise,
            balanceAfterPaise = balanceAfter,
            tagRaw = tagDecoded,
            noteDisplay = sanitizeNoteForDisplay(noteDecoded),
            dueAtMillis = dueAt?.takeIf { it > 0L },
            billImageUri = billUri.trim()
        )
    }

    private fun extractLeadingLong(detail: String, key: String): Long? {
        val idx = detail.indexOf(key)
        if (idx == -1) return null
        val tail = detail.substring(idx + key.length)
        val num = tail.takeWhile { it.isDigit() || it == '-' }
        return num.toLongOrNull()
    }

    private fun safeDecode(s: String): String {
        return try {
            Uri.decode(s.trim())
        } catch (_: Exception) {
            s.trim()
        }
    }

    fun sanitizeNoteForDisplay(note: String): String {
        if (note.isBlank()) return ""
        var t = note
        t = t.replace(Regex("""(?i)\s*billImageUri\s*=\s*\S+"""), "")
        t = t.replace(Regex("""(?i)\s*dueAt\s*=\s*\d+"""), "")
        t = t.replace(Regex("""(?i)\s*tag\s*=\s*\S+"""), "")
        return t.trim()
    }
}
