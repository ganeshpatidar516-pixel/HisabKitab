package com.ganesh.hisabkitabpro.domain.businesscard

import com.ganesh.hisabkitabpro.domain.businessidentity.OperatingHoursCodec
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile

/**
 * Sanitised projection of [BusinessProfile] consumed by the rendering engine.
 *
 * Painters never read the raw [BusinessProfile] directly — this keeps the engine
 * decoupled from Room/Hilt and lets us trim presentational fields in one place.
 */
data class BusinessCardProfile(
    val businessName: String,
    val ownerName: String,
    val tagline: String,
    val servicesDescription: String,
    val cardCtaText: String,
    val phone: String,
    val email: String,
    val website: String,
    val address: String,
    val gstin: String,
    val pan: String,
    val upi: String,
    val businessCategory: String,
    val operatingHours: String,
    val instagramUrl: String,
    val facebookUrl: String,
    val linkedInUrl: String,
    val youtubeUrl: String,
    val twitterUrl: String,
    val whatsAppBusinessUrl: String,
    val googleBusinessProfileUrl: String,
    val mapLink: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLockedAt: Long = 0L,
) {
    val hasIdentity: Boolean
        get() = businessName.isNotBlank() || ownerName.isNotBlank()

    companion object {
        fun from(profile: BusinessProfile?): BusinessCardProfile {
            val safe = profile ?: return EMPTY
            val businessName = safe.businessName.trim()
            val ownerName = safe.ownerName.trim().ifBlank { businessName }
            return BusinessCardProfile(
                businessName = businessName,
                ownerName = ownerName,
                tagline = (safe.tagline ?: "").trim(),
                servicesDescription = (safe.servicesDescription ?: "").trim(),
                cardCtaText = (safe.cardCtaText ?: "").trim(),
                phone = safe.phone.trim(),
                email = safe.email.trim(),
                website = safe.websiteUrl.trim(),
                address = safe.address.trim(),
                gstin = safe.gstNumber.trim(),
                pan = safe.panNumber.trim(),
                upi = safe.upiId.trim(),
                businessCategory = safe.businessCategory.trim(),
                operatingHours = OperatingHoursCodec.formatForDisplay(safe.operatingHours),
                instagramUrl = safe.instagramUrl.trim(),
                facebookUrl = safe.facebookUrl.trim(),
                linkedInUrl = (safe.linkedInUrl ?: "").trim(),
                youtubeUrl = (safe.youtubeUrl ?: "").trim(),
                twitterUrl = (safe.twitterUrl ?: "").trim(),
                whatsAppBusinessUrl = (safe.whatsAppBusinessUrl ?: "").trim(),
                googleBusinessProfileUrl = (safe.googleBusinessProfileUrl ?: "").trim(),
                mapLink = safe.mapLink.trim(),
                latitude = safe.latitude,
                longitude = safe.longitude,
                locationLockedAt = safe.locationLockedAt,
            )
        }

        val EMPTY = BusinessCardProfile(
            businessName = "",
            ownerName = "",
            tagline = "",
            servicesDescription = "",
            cardCtaText = "",
            phone = "",
            email = "",
            website = "",
            address = "",
            gstin = "",
            pan = "",
            upi = "",
            businessCategory = "",
            operatingHours = "",
            instagramUrl = "",
            facebookUrl = "",
            linkedInUrl = "",
            youtubeUrl = "",
            twitterUrl = "",
            whatsAppBusinessUrl = "",
            googleBusinessProfileUrl = "",
            mapLink = "",
            latitude = null,
            longitude = null,
            locationLockedAt = 0L,
        )
    }
}
