package com.ganesh.hisabkitabpro.commandos.sync

import java.security.MessageDigest

object IdempotencyKeyFactory {
    fun from(rawCommand: String, locale: String, atMillis: Long): String {
        val payload = "$rawCommand|$locale|$atMillis"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
