package com.ganesh.hisabkitabpro.domain.businesscard

/**
 * The procedural engine. Emits the canonical 50-card master sheet (5 categories × 10
 * blueprints). The output is fully deterministic for a given input — there is no
 * randomness, so re-running the engine for the same business profile always yields the
 * same sheet (important for PDF export and "regenerate" flows).
 *
 * The engine deliberately performs *no* I/O and *no* Android calls so it can be exercised
 * from JVM unit tests.
 */
object MultiTemplateGenerator {

    const val VARIATIONS_PER_CATEGORY: Int = 10

    val totalVariations: Int
        get() = BusinessCardCategory.values().size * VARIATIONS_PER_CATEGORY

    fun generate(): List<BusinessCardVariation> {
        val blueprints = BusinessCardLayoutBlueprint.set()
        val out = ArrayList<BusinessCardVariation>(totalVariations)
        for (category in BusinessCardCategory.values()) {
            val typography = BusinessCardTypography.forCategory(category)
            for (blueprint in blueprints) {
                val palette = BusinessCardPalette.forVariant(category, blueprint.variantIndex)
                out += BusinessCardVariation(
                    id = idFor(category, blueprint.variantIndex),
                    displayName = displayName(category, blueprint.variantIndex),
                    category = category,
                    blueprint = blueprint,
                    palette = palette,
                    typography = typography,
                )
            }
        }
        return out
    }

    fun byCategory(category: BusinessCardCategory): List<BusinessCardVariation> =
        generate().filter { it.category == category }

    fun idFor(category: BusinessCardCategory, variantIndex: Int): String =
        "${category.name.lowercase()}-${(variantIndex % VARIATIONS_PER_CATEGORY).toString().padStart(2, '0')}"

    private fun displayName(category: BusinessCardCategory, variantIndex: Int): String =
        "${category.displayName} ${(variantIndex % VARIATIONS_PER_CATEGORY) + 1}"
}
