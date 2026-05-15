package com.ganesh.hisabkitabpro.security

import android.util.Base64

object SecurityUtils {

    fun encrypt(data: String): String {
        return KeyStoreCryptoManager.encrypt(data)
    }

    fun decrypt(encryptedData: String): String {
        return KeyStoreCryptoManager.decrypt(encryptedData)
    }
    
    /**
     * Generate a unique hash for a transaction to prevent duplicates.
     */
    fun generateTransactionHash(upiId: String, amount: String?, timestamp: Long): String {
        val raw = "$upiId|$amount|$timestamp"
        return Base64.encodeToString(raw.toByteArray(), Base64.NO_WRAP)
    }
}
