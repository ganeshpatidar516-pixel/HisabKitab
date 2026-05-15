package com.ganesh.hisabkitabpro.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey
    val userId: String = "default_user",
    val businessName: String = "",
    val ownerName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val gstNumber: String = "",
    val panNumber: String = "", // Added PAN Support
    val upiId: String = "",
    val qrImagePath: String = "",
    val logoUrl: String = "",
    val businessCategory: String = "",
    val operatingHours: String = "",
    val websiteUrl: String = "",
    val instagramUrl: String = "",
    val facebookUrl: String = "",
    /** Nullable for Gson/API compatibility; treat as empty when null. */
    val linkedInUrl: String? = null,
    val youtubeUrl: String? = null,
    val twitterUrl: String? = null,
    val whatsAppBusinessUrl: String? = null,
    val googleBusinessProfileUrl: String? = null,
    val signatureImagePath: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLockedAt: Long = 0L,
    val mapLink: String = "",
    /** Short line under business name on cards and marketing surfaces. */
    val tagline: String? = null,
    /** Services or product lines; each line may wrap on the business card rail. */
    val servicesDescription: String? = null,
    /** Short call-to-action (e.g. "Call for free estimate") on cards. */
    val cardCtaText: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
