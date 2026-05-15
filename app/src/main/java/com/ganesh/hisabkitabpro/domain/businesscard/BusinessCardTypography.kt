package com.ganesh.hisabkitabpro.domain.businesscard

/**
 * Procedural typography description per category. These values are framework-agnostic
 * so the same descriptor drives both Compose previews and the off-screen Android
 * Canvas exporter (consistent metrics across PNG/PDF output).
 *
 * Letter spacing is expressed in EM units; line height as a multiplier of font size.
 */
data class BusinessCardTypography(
    val titleFont: FontFamilyToken,
    val bodyFont: FontFamilyToken,
    val titleWeight: FontWeightToken,
    val bodyWeight: FontWeightToken,
    val titleLetterSpacingEm: Float,
    val bodyLetterSpacingEm: Float,
    val titleLineHeightFactor: Float,
    val bodyLineHeightFactor: Float,
) {

    enum class FontFamilyToken { SERIF, SANS_SERIF, MONOSPACE }
    enum class FontWeightToken { THIN, LIGHT, REGULAR, MEDIUM, SEMI_BOLD, BOLD, BLACK }

    companion object {
        fun forCategory(category: BusinessCardCategory): BusinessCardTypography = when (category) {
            BusinessCardCategory.ROYAL_SIGNATURE -> BusinessCardTypography(
                titleFont = FontFamilyToken.SERIF,
                bodyFont = FontFamilyToken.SERIF,
                titleWeight = FontWeightToken.BOLD,
                bodyWeight = FontWeightToken.REGULAR,
                titleLetterSpacingEm = 0.16f,
                bodyLetterSpacingEm = 0.04f,
                titleLineHeightFactor = 1.10f,
                bodyLineHeightFactor = 1.32f,
            )
            BusinessCardCategory.CORPORATE_ELITE -> BusinessCardTypography(
                titleFont = FontFamilyToken.SANS_SERIF,
                bodyFont = FontFamilyToken.SANS_SERIF,
                titleWeight = FontWeightToken.SEMI_BOLD,
                bodyWeight = FontWeightToken.REGULAR,
                titleLetterSpacingEm = -0.01f,
                bodyLetterSpacingEm = 0.0f,
                titleLineHeightFactor = 1.05f,
                bodyLineHeightFactor = 1.30f,
            )
            BusinessCardCategory.MODERN_ABSTRACT -> BusinessCardTypography(
                titleFont = FontFamilyToken.SANS_SERIF,
                bodyFont = FontFamilyToken.SANS_SERIF,
                titleWeight = FontWeightToken.BLACK,
                bodyWeight = FontWeightToken.MEDIUM,
                titleLetterSpacingEm = -0.02f,
                bodyLetterSpacingEm = 0.02f,
                titleLineHeightFactor = 1.00f,
                bodyLineHeightFactor = 1.28f,
            )
            BusinessCardCategory.ECO_ORGANIC -> BusinessCardTypography(
                titleFont = FontFamilyToken.SERIF,
                bodyFont = FontFamilyToken.SANS_SERIF,
                titleWeight = FontWeightToken.MEDIUM,
                bodyWeight = FontWeightToken.LIGHT,
                titleLetterSpacingEm = 0.04f,
                bodyLetterSpacingEm = 0.02f,
                titleLineHeightFactor = 1.18f,
                bodyLineHeightFactor = 1.36f,
            )
            BusinessCardCategory.DIGITAL_NATIVE -> BusinessCardTypography(
                titleFont = FontFamilyToken.MONOSPACE,
                bodyFont = FontFamilyToken.MONOSPACE,
                titleWeight = FontWeightToken.BOLD,
                bodyWeight = FontWeightToken.REGULAR,
                titleLetterSpacingEm = 0.08f,
                bodyLetterSpacingEm = 0.04f,
                titleLineHeightFactor = 1.10f,
                bodyLineHeightFactor = 1.30f,
            )
        }
    }
}
