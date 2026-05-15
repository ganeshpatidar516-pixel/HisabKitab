package com.ganesh.hisabkitabpro.domain.businesscard

/**
 * Immutable description of one of the fifty cards. Carries everything a painter or
 * exporter needs to render deterministically.
 */
data class BusinessCardVariation(
    val id: String,
    val displayName: String,
    val category: BusinessCardCategory,
    val blueprint: BusinessCardLayoutBlueprint,
    val palette: BusinessCardPalette,
    val typography: BusinessCardTypography,
) {
    val variantIndex: Int get() = blueprint.variantIndex
    val seed: Int get() = blueprint.seed * (category.ordinal + 1)
}
