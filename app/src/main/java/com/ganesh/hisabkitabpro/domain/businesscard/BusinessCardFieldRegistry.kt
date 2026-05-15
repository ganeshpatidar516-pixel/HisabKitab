package com.ganesh.hisabkitabpro.domain.businesscard

import com.ganesh.hisabkitabpro.domain.profile.ProfileMapFooter

/**
 * Single registry of every [BusinessCardProfile] field that maps onto the physical card.
 *
 * Lines are emitted in **priority order** so identity-critical data (address, GST, services)
 * appears **before** long social URL tails — every painter uses the same rail via
 * [orderedRailLines], so all **50** card variants (5 aesthetics × 10 blueprints) stay consistent.
 *
 * Optional [extraRailLines] is for future metadata (e.g. JSON) without changing painters.
 */
object BusinessCardFieldRegistry {

    /** Ordered lines for the card body rail (non-null, non-blank only). */
    fun orderedRailLines(
        profile: BusinessCardProfile,
        extraRailLines: List<String> = emptyList(),
    ): List<String> = buildList {
        fun emit(line: String?) {
            line?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        }
        with(profile) {
            emit(businessCategory.takeIf { it.isNotBlank() }?.let { "Category  $it" })
            emit(phone.takeIf { it.isNotBlank() }?.let { "T  $it" })
            emit(email.takeIf { it.isNotBlank() }?.let { "E  $it" })
            emit(address.takeIf { it.isNotBlank() }?.let { "A  $it" })
            emit(gstin.takeIf { it.isNotBlank() }?.let { "GSTIN  $it" })
            emit(pan.takeIf { it.isNotBlank() }?.let { "PAN  $it" })
            emit(upi.takeIf { it.isNotBlank() }?.let { "UPI  $it" })
            emit(operatingHours.takeIf { it.isNotBlank() }?.let { "H  $it" })
            emit(website.takeIf { it.isNotBlank() }?.let { "W  ${BusinessCardUrlDisplay.shorten(it)}" })
            emit(whatsAppBusinessUrl.takeIf { it.isNotBlank() }?.let { "WA  ${BusinessCardUrlDisplay.shorten(it)}" })
            emit(
                ProfileMapFooter.cardLocationRailLine(
                    mapLink = mapLink,
                    latitude = latitude,
                    longitude = longitude,
                    locationLockedAt = locationLockedAt,
                ),
            )
            emit(instagramUrl.takeIf { it.isNotBlank() }?.let { "IG  ${BusinessCardUrlDisplay.shorten(it)}" })
            emit(facebookUrl.takeIf { it.isNotBlank() }?.let { "FB  ${BusinessCardUrlDisplay.shorten(it)}" })
            emit(linkedInUrl.takeIf { it.isNotBlank() }?.let { "LI  ${BusinessCardUrlDisplay.shorten(it)}" })
            emit(youtubeUrl.takeIf { it.isNotBlank() }?.let { "YT  ${BusinessCardUrlDisplay.shorten(it)}" })
            emit(twitterUrl.takeIf { it.isNotBlank() }?.let { "X  ${BusinessCardUrlDisplay.shorten(it)}" })
            emit(googleBusinessProfileUrl.takeIf { it.isNotBlank() }?.let { "GBP  ${BusinessCardUrlDisplay.shorten(it)}" })
            emit(cardCtaText.trim().takeIf { it.isNotEmpty() }?.let { "◇ $it" })
            servicesDescription.trim().lineSequence().forEach { raw ->
                raw.trim().takeIf { it.isNotEmpty() }?.let { emit("· $it") }
            }
        }
        for (extra in extraRailLines) {
            emit(extra)
        }
    }
}
