package com.ganesh.hisabkitabpro.domain.businesscard

/** URL presentation for narrow card rails (no network I/O). */
object BusinessCardUrlDisplay {

    fun shorten(url: String, maxLen: Int = 48): String {
        var s = url.trim()
        if (s.startsWith("https://", ignoreCase = true)) s = s.substring(8)
        else if (s.startsWith("http://", ignoreCase = true)) s = s.substring(7)
        if (s.startsWith("www.", ignoreCase = true)) s = s.substring(4)
        return if (s.length <= maxLen) s else s.take(maxLen - 1) + "…"
    }
}
