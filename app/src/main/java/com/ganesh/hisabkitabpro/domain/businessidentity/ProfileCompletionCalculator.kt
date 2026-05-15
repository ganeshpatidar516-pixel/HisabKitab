package com.ganesh.hisabkitabpro.domain.businessidentity

import android.util.Patterns
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.GstGatekeeper
import java.io.File

/** True when a saved path/URI likely points to media (file on disk or content/file URI). */
private fun profileMediaPathPresent(path: String): Boolean {
    val t = path.trim()
    if (t.isEmpty()) return false
    if (t.startsWith("content://", ignoreCase = true)) return true
    if (t.startsWith("file://", ignoreCase = true)) return true
    return File(t).exists()
}

/**
 * Lightweight profile strength (0–100) for onboarding psychology; does not affect billing.
 */
object ProfileCompletionCalculator {

    fun percent(profile: BusinessProfile?, gstEnabled: Boolean): Int {
        if (profile == null) return 0
        var done = 0
        var total = 16

        fun hit(condition: Boolean) {
            if (condition) done++
        }

        hit(profile.businessName.trim().length >= 2)
        hit(profile.ownerName.trim().length >= 2)
        hit(profile.phone.trim().length >= 10)
        hit(profile.email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(profile.email.trim()).matches())
        hit(profile.address.isNotBlank())
        if (gstEnabled) {
            val g = GstGatekeeper.normalize(profile.gstNumber.trim())
            hit(g.isNotBlank() && GstGatekeeper.isValid(g))
        } else {
            hit(true)
        }
        hit(profile.panNumber.isNotBlank())
        hit(profile.upiId.isNotBlank())
        hit(profile.qrImagePath.isNotBlank() && profileMediaPathPresent(profile.qrImagePath))
        hit(profile.logoUrl.isNotBlank() && profileMediaPathPresent(profile.logoUrl))
        hit(profile.businessCategory.isNotBlank())
        hit(!profile.tagline.isNullOrBlank())
        hit(!profile.servicesDescription.isNullOrBlank() || !profile.cardCtaText.isNullOrBlank())
        hit(profile.operatingHours.isNotBlank())
        hit(
            listOf(
                profile.websiteUrl,
                profile.instagramUrl,
                profile.facebookUrl,
                profile.linkedInUrl.orEmpty(),
                profile.youtubeUrl.orEmpty(),
                profile.twitterUrl.orEmpty(),
                profile.whatsAppBusinessUrl.orEmpty(),
                profile.googleBusinessProfileUrl.orEmpty(),
            ).any { it.isNotBlank() },
        )
        hit(profile.latitude != null && profile.longitude != null && profile.mapLink.isNotBlank())
        hit(profile.signatureImagePath.isNotBlank() && profileMediaPathPresent(profile.signatureImagePath))

        return ((100.0 * done) / total).toInt().coerceIn(0, 100)
    }
}
