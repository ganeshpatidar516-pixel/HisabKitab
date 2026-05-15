package com.ganesh.hisabkitabpro.commandos.sync

import android.content.SharedPreferences
import android.util.Base64
import com.ganesh.hisabkitabpro.security.SecurityUtils
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistentOfflineCommandJournal @Inject constructor(
    private val prefs: SharedPreferences
) : OfflineCommandJournal {
    override fun enqueue(entry: OfflineCommandEntry) {
        val current = readAll().toMutableList()
        current.add(entry)
        writeAll(current)
    }

    override fun peek(limit: Int): List<OfflineCommandEntry> {
        return readAll().take(limit)
    }

    override fun remove(id: String): Boolean {
        val current = readAll()
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return false
        writeAll(updated)
        return true
    }

    override fun size(): Int = readAll().size

    private fun readAll(): List<OfflineCommandEntry> {
        val raw = prefs.getString(KEY_OFFLINE_COMMAND_JOURNAL, null) ?: return emptyList()
        return try {
            raw.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { decodeEntry(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeAll(entries: List<OfflineCommandEntry>) {
        val payload = entries.joinToString(separator = "\n") { encodeEntry(it) }
        prefs.edit().putString(KEY_OFFLINE_COMMAND_JOURNAL, payload).apply()
    }

    private fun encodeEntry(entry: OfflineCommandEntry): String {
        return listOf(
            entry.id,
            b64(encryptSafe(entry.rawCommand)),
            b64(encryptSafe(entry.locale)),
            entry.idempotencyKey,
            entry.createdAt.toString()
        ).joinToString("|")
    }

    private fun decodeEntry(line: String): OfflineCommandEntry? {
        val parts = line.split("|")
        if (parts.size != 5) return null
        return OfflineCommandEntry(
            id = parts[0],
            rawCommand = decryptSafe(fromB64(parts[1])),
            locale = decryptSafe(fromB64(parts[2])),
            idempotencyKey = parts[3],
            createdAt = parts[4].toLongOrNull() ?: return null
        )
    }

    private fun encryptSafe(value: String): String {
        return try {
            "enc:${SecurityUtils.encrypt(value)}"
        } catch (_: Throwable) {
            "plain:$value"
        }
    }

    private fun decryptSafe(value: String): String {
        return when {
            value.startsWith("enc:") -> {
                val payload = value.removePrefix("enc:")
                try {
                    SecurityUtils.decrypt(payload)
                } catch (_: Throwable) {
                    ""
                }
            }
            value.startsWith("plain:") -> value.removePrefix("plain:")
            else -> value
        }
    }

    private fun b64(value: String): String {
        return try {
            Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun fromB64(value: String): String {
        return try {
            String(Base64.decode(value, Base64.NO_WRAP), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            String(java.util.Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
        }
    }

    companion object {
        private const val KEY_OFFLINE_COMMAND_JOURNAL = "super_command_offline_journal_v1"
    }
}
