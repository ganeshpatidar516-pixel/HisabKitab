package com.ganesh.hisabkitabpro.domain.payroll

import android.util.Log
import com.ganesh.hisabkitabpro.data.local.StaffEntity
import com.ganesh.hisabkitabpro.security.KeyStoreCryptoManager

/**
 * Tiny façade that takes care of selectively encrypting/decrypting the PII
 * fields on [StaffEntity] **at-rest** inside the SQLCipher DB.
 *
 * Why a second layer on top of SQLCipher? Two reasons:
 *  1. Defence-in-depth: even if a future feature exports a per-record JSON
 *     blob (e.g. for shared-khata or audit), the PII stays sealed.
 *  2. Zero-touch backup: cloud-backup uploads the SQLCipher file as-is, so
 *     PII is double-sealed — Drive sees only opaque bytes.
 *
 * Crypto is the existing AES-GCM keystore-sealed key in
 * [KeyStoreCryptoManager], so we never introduce a new secret material path.
 */
object StaffSecureFields {

    private const val TAG = "StaffSecureFields"

    fun encryptForWrite(staff: StaffEntity): StaffEntity {
        val phoneEnc = staff.phone.takeIf { it.isNotBlank() }?.let(::tryEncrypt)
        val emailEnc = currentEmailPlain(staff)?.takeIf { it.isNotBlank() }?.let(::tryEncrypt)
        return staff.copy(
            phoneEnc = phoneEnc ?: staff.phoneEnc,
            emailEnc = emailEnc ?: staff.emailEnc
        )
    }

    /**
     * Resolve the canonical phone number for display:
     *  - Prefer the encrypted column if present and decryptable.
     *  - Fall back to the legacy plaintext column for older records.
     */
    fun decryptedPhone(staff: StaffEntity): String {
        val cipher = staff.phoneEnc
        if (!cipher.isNullOrBlank()) {
            val plain = tryDecrypt(cipher)
            if (plain != null) return plain
        }
        return staff.phone
    }

    fun decryptedEmail(staff: StaffEntity): String {
        val cipher = staff.emailEnc ?: return ""
        return tryDecrypt(cipher).orEmpty()
    }

    private fun currentEmailPlain(staff: StaffEntity): String? = when {
        // Email is not yet a stored plaintext column; surface only when caller
        // re-encrypts an existing ciphertext. This is a no-op for now and
        // exists to keep the API symmetric for the ViewModel layer.
        staff.emailEnc.isNullOrBlank() -> null
        else -> tryDecrypt(staff.emailEnc)
    }

    fun encryptValue(plain: String): String? = plain.takeIf { it.isNotBlank() }?.let(::tryEncrypt)

    fun decryptValue(cipher: String?): String = cipher?.let { tryDecrypt(it) }.orEmpty()

    private fun tryEncrypt(plain: String): String? = try {
        KeyStoreCryptoManager.encrypt(plain)
    } catch (e: Exception) {
        Log.w(TAG, "Encrypt failed: ${e::class.java.simpleName}")
        null
    }

    private fun tryDecrypt(cipher: String): String? = try {
        KeyStoreCryptoManager.decrypt(cipher)
    } catch (e: Exception) {
        Log.w(TAG, "Decrypt failed: ${e::class.java.simpleName}")
        null
    }
}
