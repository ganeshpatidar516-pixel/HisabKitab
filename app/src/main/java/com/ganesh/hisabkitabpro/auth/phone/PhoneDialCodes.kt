package com.ganesh.hisabkitabpro.auth.phone

/**
 * Curated dial codes for the sign-in sheet (no extra dependencies).
 * Defaults to **India +91** so existing UX stays familiar.
 */
data class PhoneDialOption(
    val displayLabel: String,
    val e164Prefix: String,
    val nationalMinLen: Int,
    val nationalMaxLen: Int,
    val digitHint: String,
) {
    fun isValidNational(nationalDigits: String): Boolean {
        val n = nationalDigits.length
        return n in nationalMinLen..nationalMaxLen
    }
}

object PhoneDialCodes {
    val default: PhoneDialOption get() = options.first()

    val options: List<PhoneDialOption> = listOf(
        PhoneDialOption("India (+91)", "+91", 10, 10, "10-digit mobile"),
        PhoneDialOption("United States (+1)", "+1", 10, 10, "10-digit number"),
        PhoneDialOption("United Arab Emirates (+971)", "+971", 9, 9, "9-digit mobile"),
        PhoneDialOption("Saudi Arabia (+966)", "+966", 9, 9, "9-digit mobile"),
        PhoneDialOption("United Kingdom (+44)", "+44", 10, 10, "10-digit mobile"),
        PhoneDialOption("Pakistan (+92)", "+92", 10, 10, "10-digit mobile"),
        PhoneDialOption("Bangladesh (+880)", "+880", 10, 10, "10-digit mobile"),
        PhoneDialOption("Nepal (+977)", "+977", 10, 10, "10-digit mobile"),
        PhoneDialOption("Singapore (+65)", "+65", 8, 8, "8-digit mobile"),
    )
}
