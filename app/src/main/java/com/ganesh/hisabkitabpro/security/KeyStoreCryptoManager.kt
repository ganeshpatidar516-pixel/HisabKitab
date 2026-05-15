package com.ganesh.hisabkitabpro.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SQLCipher passphrase never lives in plaintext on disk: it is sealed with an **AES-GCM key
 * inside Android Keystore** (TEE / StrongBox when available). BiometricPrompt can be layered
 * *above* DB open in a future UX without rotating the SQLCipher key — existing DB files stay valid.
 */
object KeyStoreCryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val KEY_ALIAS = "hisabkitab_keystore_master_v1"
    private const val PREFS_NAME = "secure_key_material"
    private const val PREF_DB_KEY = "sqlcipher_key_v1"
    private const val PREF_DB_KEY_LEGACY_FLAG = "sqlcipher_legacy_seeded_v1"

    // Legacy passphrase is reconstructed at runtime for one-time compatibility unlock only.
    // Keep exact bytes stable so existing encrypted databases continue to open.
    private val LEGACY_DB_KEY_PARTS = charArrayOf(
        'h', 'i', 's', 'a', 'b', 'k', 'i', 't', 'a', 'b', '_',
        'u', 'l', 't', 'r', 'a', '_', 's', 'e', 'c', 'u', 'r', 'e', '_',
        'b', 'l', 'u', 'e', 'p', 'r', 'i', 'n', 't', '_', '2', '0', '2', '4'
    )

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encrypted, 0, payload, iv.size, encrypted.size)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(encryptedData: String): String {
        val payload = Base64.decode(encryptedData, Base64.NO_WRAP)
        require(payload.size > 12) { "Encrypted payload is invalid." }
        val iv = payload.copyOfRange(0, 12)
        val body = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        val clear = cipher.doFinal(body)
        return String(clear, StandardCharsets.UTF_8)
    }

    /**
     * SQLCipher key source:
     * - Existing installs: preserves legacy key by sealing it once in keystore.
     * - New installs: creates high-entropy random key and seals it in keystore.
     */
    fun getOrCreateDatabasePassphrase(context: Context): CharArray {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingCipherText = prefs.getString(PREF_DB_KEY, null)
        if (!existingCipherText.isNullOrBlank()) {
            return decrypt(existingCipherText).toCharArray()
        }

        val databaseExists = appContext.getDatabasePath(AppDatabase.DATABASE_NAME).exists()
        val useLegacyForCompatibility = databaseExists && !prefs.getBoolean(PREF_DB_KEY_LEGACY_FLAG, false)
        val initial = if (useLegacyForCompatibility) legacyDbPassphrase() else randomPassphrase()
        prefs.edit()
            .putString(PREF_DB_KEY, encrypt(initial))
            .putBoolean(PREF_DB_KEY_LEGACY_FLAG, true)
            .apply()
        return initial.toCharArray()
    }

    private fun randomPassphrase(lengthBytes: Int = 48): String {
        val entropy = ByteArray(lengthBytes)
        SecureRandom().nextBytes(entropy)
        return Base64.encodeToString(entropy, Base64.NO_WRAP)
    }

    private fun legacyDbPassphrase(): String = LEGACY_DB_KEY_PARTS.concatToString()

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        if (Build.VERSION.SDK_INT >= 28) {
            try {
                val strong = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                strong.init(buildAesGcmSpec(preferStrongBox = true))
                return strong.generateKey()
            } catch (_: Exception) {
                // StrongBox not available on this device — fall back to TEE-backed keystore.
            }
        }
        val tee = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        tee.init(buildAesGcmSpec(preferStrongBox = false))
        return tee.generateKey()
    }

    private fun buildAesGcmSpec(preferStrongBox: Boolean): KeyGenParameterSpec {
        val specBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            specBuilder.setUnlockedDeviceRequired(false)
        }
        if (Build.VERSION.SDK_INT >= 28 && preferStrongBox) {
            specBuilder.setIsStrongBoxBacked(true)
        }
        return specBuilder.build()
    }
}
