package com.ganesh.hisabkitabpro.domain.businessidentity

import android.net.Uri
import android.util.Patterns
import java.util.Locale
import java.util.regex.Pattern

/**
 * Phase 5: website vs email detection, light URL normalization, social URL cleanup.
 * All optional fields: blank stays blank; invalid input is rejected at save with a clear message.
 */
object BusinessIdentityInputNormalizer {

    enum class WebsiteInputKind {
        Blank,
        LikelyEmail,
        LikelyUrl,
        Other,
    }

    fun classifyWebsite(raw: String): WebsiteInputKind {
        val s = raw.trim()
        if (s.isEmpty()) return WebsiteInputKind.Blank
        if (Patterns.EMAIL_ADDRESS.matcher(s).matches()) return WebsiteInputKind.LikelyEmail
        val lowered = s.lowercase(Locale.ROOT)
        if (lowered.startsWith("mailto:")) return WebsiteInputKind.LikelyEmail
        if (s.contains("@") && !s.contains("://") && s.length <= 120) {
            val domainPart = s.substringAfter("@", "")
            if (domainPart.contains(".") && domainPart.length >= 3 && !domainPart.contains("/")) {
                return WebsiteInputKind.LikelyEmail
            }
        }
        if (looksLikeWebUrl(s)) return WebsiteInputKind.LikelyUrl
        return WebsiteInputKind.Other
    }

    /**
     * Returns null if [raw] is blank or cannot be normalized into a plausible http(s) URL.
     */
    fun normalizeWebsite(raw: String): String? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        if (classifyWebsite(s) == WebsiteInputKind.LikelyEmail) return null
        var t = s
        if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) {
            return t
        }
        if (t.startsWith("www.", ignoreCase = true)) {
            return "https://$t"
        }
        if (WEB_HOST_PATTERN.matcher(t).matches()) {
            return "https://$t"
        }
        return null
    }

    fun isAcceptableWebsiteForSave(raw: String): Boolean {
        val s = raw.trim()
        if (s.isEmpty()) return true
        if (classifyWebsite(s) == WebsiteInputKind.LikelyEmail) return false
        val normalized = normalizeWebsite(s) ?: return false
        val uri = Uri.parse(normalized)
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host
        return (scheme == "http" || scheme == "https") &&
            !host.isNullOrBlank() &&
            host.contains(".")
    }

    fun normalizeInstagram(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
            return s
        }
        val lowered = s.lowercase(Locale.ROOT)
        if (lowered.contains("instagram.com") || lowered.contains("instagr.am")) {
            val rest = s.trimStart('/')
            return "https://$rest"
        }
        val handle = s.removePrefix("@").substringBefore("?").trim()
        if (handle.isEmpty()) return ""
        if (handle.contains("/") || handle.contains(".")) {
            return "https://$handle"
        }
        return "https://instagram.com/$handle"
    }

    fun normalizeFacebook(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
            return s
        }
        val slug = s.removePrefix("@").substringBefore("?").trim()
        if (slug.isEmpty()) return ""
        if (slug.contains(".") || slug.contains("/")) {
            return if (slug.startsWith("facebook.", ignoreCase = true) || slug.startsWith("fb.", ignoreCase = true)) {
                "https://$slug"
            } else {
                "https://facebook.com/$slug"
            }
        }
        return "https://facebook.com/$slug"
    }

    fun normalizeLinkedIn(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) return s
        val t = s.removePrefix("@").substringBefore("?").trim()
        if (t.isEmpty()) return ""
        val low = t.lowercase(Locale.ROOT)
        if (low.contains("linkedin.com")) return ensureHttpsUrl(t)
        return "https://www.linkedin.com/in/$t"
    }

    fun normalizeYoutube(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) return s
        val t = s.substringBefore("?").trim()
        val low = t.lowercase(Locale.ROOT)
        if (low.contains("youtube.com") || low.contains("youtu.be")) return ensureHttpsUrl(t)
        return "https://www.youtube.com/@$t"
    }

    fun normalizeTwitter(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) return s
        val t = s.removePrefix("@").substringBefore("?").trim()
        if (t.isEmpty()) return ""
        val low = t.lowercase(Locale.ROOT)
        if (low.contains("twitter.com") || low.contains("x.com")) return ensureHttpsUrl(t)
        return "https://x.com/$t"
    }

    fun normalizeWhatsAppBusiness(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) return s
        val digits = s.filter { it.isDigit() }
        if (digits.length >= 10) return "https://wa.me/$digits"
        return ""
    }

    fun normalizeGoogleBusinessProfile(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) return s
        val t = s.trimStart('/')
        val low = t.lowercase(Locale.ROOT)
        if (low.contains("google.com") || low.contains("goo.gl") || low.contains("maps.app.goo.gl") ||
            low.contains("business.google")
        ) {
            return ensureHttpsUrl(t)
        }
        return "https://$t"
    }

    private fun ensureHttpsUrl(raw: String): String {
        val t = raw.trim().trimStart('/')
        if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) return t
        return "https://$t"
    }

    private fun looksLikeWebUrl(s: String): Boolean {
        if (s.contains("://")) return true
        if (s.startsWith("www.", ignoreCase = true)) return true
        return WEB_HOST_PATTERN.matcher(s).matches()
    }

    private val WEB_HOST_PATTERN: Pattern = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+(/.*)?$",
    )
}
