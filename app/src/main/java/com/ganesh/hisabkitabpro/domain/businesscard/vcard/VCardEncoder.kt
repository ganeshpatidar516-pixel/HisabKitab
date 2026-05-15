package com.ganesh.hisabkitabpro.domain.businesscard.vcard

import com.ganesh.hisabkitabpro.domain.businessidentity.OperatingHoursCodec
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import java.util.Locale

/**
 * Builds an RFC 6350 vCard 3.0 payload from a [BusinessProfile].
 *
 * The output is intended to be encoded into a QR code so smartphones can trigger an
 * "Add to Contacts" prompt without requiring any companion app. We standardise on
 * vCard 3.0 (rather than 4.0) because 3.0 has the broadest cross-platform parser
 * support, especially on iOS Wallet and stock Android contact apps.
 *
 * All values are escaped per the spec (`\`, `,`, `;`, newline). Lines are folded at
 * 75 octets to remain compliant for high-density payloads.
 */
object VCardEncoder {

    fun encode(profile: BusinessProfile): String {
        val builder = StringBuilder(512)
        builder.append("BEGIN:VCARD\r\n")
        builder.append("VERSION:3.0\r\n")
        builder.append(line("FN", profile.ownerName.ifBlank { profile.businessName }))
        builder.append(line("N", structuredName(profile.ownerName)))
        if (profile.businessName.isNotBlank()) {
            builder.append(line("ORG", escape(profile.businessName)))
        }
        if (profile.phone.isNotBlank()) {
            builder.append(line("TEL;TYPE=CELL,VOICE", escape(profile.phone)))
        }
        if (profile.email.isNotBlank()) {
            builder.append(line("EMAIL;TYPE=INTERNET", escape(profile.email)))
        }
        if (profile.websiteUrl.isNotBlank()) {
            builder.append(line("URL", escape(profile.websiteUrl)))
        }
        val mapTrim = profile.mapLink.trim()
        val websiteTrim = profile.websiteUrl.trim()
        if (mapTrim.isNotBlank() && !mapTrim.equals(websiteTrim, ignoreCase = true)) {
            builder.append(line("URL", escape(mapTrim)))
        }
        if (profile.address.isNotBlank()) {
            builder.append(line("ADR;TYPE=WORK", ";;${escape(profile.address)};;;;"))
        }
        val lat = profile.latitude
        val lon = profile.longitude
        if (lat != null && lon != null && lat.isFinite() && lon.isFinite()) {
            builder.append(
                line(
                    "GEO",
                    String.format(Locale.US, "%.7f;%.7f", lat, lon),
                ),
            )
        }
        val notes = buildList {
            profile.tagline?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
            profile.servicesDescription?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
                add("Services ${raw.replace("\n", " · ")}")
            }
            profile.cardCtaText?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
            if (profile.gstNumber.isNotBlank()) add("GSTIN ${profile.gstNumber}")
            if (profile.panNumber.isNotBlank()) add("PAN ${profile.panNumber}")
            if (profile.upiId.isNotBlank()) add("UPI ${profile.upiId}")
            if (profile.businessCategory.isNotBlank()) add("Category ${profile.businessCategory}")
            if (profile.operatingHours.isNotBlank()) {
                add("Hours ${OperatingHoursCodec.formatForDisplay(profile.operatingHours)}")
            }
            if (profile.instagramUrl.isNotBlank()) add("Instagram ${profile.instagramUrl}")
            if (profile.facebookUrl.isNotBlank()) add("Facebook ${profile.facebookUrl}")
            profile.linkedInUrl?.takeIf { it.isNotBlank() }?.let { add("LinkedIn $it") }
            profile.youtubeUrl?.takeIf { it.isNotBlank() }?.let { add("YouTube $it") }
            profile.twitterUrl?.takeIf { it.isNotBlank() }?.let { add("X $it") }
            profile.whatsAppBusinessUrl?.takeIf { it.isNotBlank() }?.let { add("WhatsApp $it") }
            profile.googleBusinessProfileUrl?.takeIf { it.isNotBlank() }?.let { add("Google Business $it") }
        }
        if (notes.isNotEmpty()) {
            builder.append(line("NOTE", escape(notes.joinToString(" • "))))
        }
        builder.append("END:VCARD\r\n")
        return foldLines(builder.toString())
    }

    private fun structuredName(displayName: String): String {
        if (displayName.isBlank()) return ";;;;"
        val parts = displayName.trim().split(' ', limit = 2)
        val family = if (parts.size == 2) escape(parts[1]) else ""
        val given = escape(parts[0])
        return "$family;$given;;;"
    }

    private fun line(field: String, value: String): String = "$field:$value\r\n"

    private fun escape(input: String): String = buildString(input.length + 8) {
        for (ch in input) {
            when (ch) {
                '\\' -> append("\\\\")
                ',' -> append("\\,")
                ';' -> append("\\;")
                '\n' -> append("\\n")
                '\r' -> Unit
                else -> append(ch)
            }
        }
    }

    /** Fold long lines at 75 octets per RFC 6350 §3.2. */
    private fun foldLines(payload: String): String {
        val out = StringBuilder(payload.length + 32)
        for (rawLine in payload.split("\r\n")) {
            if (rawLine.isEmpty()) continue
            val bytes = rawLine.toByteArray(Charsets.UTF_8)
            if (bytes.size <= 75) {
                out.append(rawLine).append("\r\n")
            } else {
                var index = 0
                var first = true
                while (index < bytes.size) {
                    val end = minOf(index + if (first) 75 else 74, bytes.size)
                    val chunk = String(bytes.copyOfRange(index, end), Charsets.UTF_8)
                    if (!first) out.append(' ')
                    out.append(chunk).append("\r\n")
                    first = false
                    index = end
                }
            }
        }
        return out.toString()
    }
}
