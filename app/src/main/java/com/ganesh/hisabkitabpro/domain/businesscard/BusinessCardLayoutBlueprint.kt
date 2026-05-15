package com.ganesh.hisabkitabpro.domain.businesscard

/**
 * Pure description of the structural layout for a single card variant.
 *
 * The combination of (logoAnchor, textAlignment, accentShape, qrPlacement, seed)
 * is unique across the ten blueprints; combined with five aesthetic categories this
 * produces fifty visually distinct cards without copy-pasting any template.
 */
data class BusinessCardLayoutBlueprint(
    val variantIndex: Int,
    val logoAnchor: LogoAnchor,
    val textAlignment: TextAlignment,
    val accentShape: AccentShape,
    val qrPlacement: QrPlacement,
    val divider: Divider,
    val seed: Int,
) {

    enum class LogoAnchor { TOP_LEFT, TOP_RIGHT, TOP_CENTER, CENTER_LEFT, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER }
    enum class TextAlignment { LEAD_LEFT, LEAD_RIGHT, CENTER }
    enum class AccentShape { DIAGONAL_TR, DIAGONAL_BL, LEFT_BAND, RIGHT_BAND, TOP_BAND, BOTTOM_BAND, CORNER_FRAME, RADIAL_GLOW, FULL_BLEED, HAIRLINE_GRID }
    enum class QrPlacement { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, INSET_RIGHT, INSET_LEFT }
    enum class Divider { NONE, HAIRLINE, GOLDEN, DOTTED }

    companion object {
        /** Returns the deterministic ten-blueprint set. Index is wrapped to 0..9. */
        fun set(): List<BusinessCardLayoutBlueprint> = BLUEPRINTS

        fun at(variantIndex: Int): BusinessCardLayoutBlueprint =
            BLUEPRINTS[((variantIndex % BLUEPRINTS.size) + BLUEPRINTS.size) % BLUEPRINTS.size]

        // Primes are used as seeds so that downstream painters (geometric pattern fields,
        // texture noise, gold-foil grain) have a unique deterministic feed per blueprint.
        private val BLUEPRINTS: List<BusinessCardLayoutBlueprint> = listOf(
            BusinessCardLayoutBlueprint(0, LogoAnchor.TOP_LEFT,    TextAlignment.LEAD_LEFT,  AccentShape.DIAGONAL_TR, QrPlacement.BOTTOM_RIGHT, Divider.HAIRLINE, 7),
            BusinessCardLayoutBlueprint(1, LogoAnchor.TOP_RIGHT,   TextAlignment.LEAD_RIGHT, AccentShape.DIAGONAL_BL, QrPlacement.BOTTOM_LEFT,  Divider.GOLDEN,   13),
            BusinessCardLayoutBlueprint(2, LogoAnchor.TOP_LEFT,    TextAlignment.LEAD_LEFT,  AccentShape.LEFT_BAND,   QrPlacement.BOTTOM_RIGHT, Divider.HAIRLINE, 19),
            BusinessCardLayoutBlueprint(3, LogoAnchor.TOP_CENTER,  TextAlignment.CENTER,     AccentShape.TOP_BAND,    QrPlacement.BOTTOM_RIGHT, Divider.GOLDEN,   23),
            BusinessCardLayoutBlueprint(4, LogoAnchor.BOTTOM_LEFT, TextAlignment.LEAD_RIGHT, AccentShape.RIGHT_BAND,  QrPlacement.TOP_RIGHT,    Divider.DOTTED,   29),
            BusinessCardLayoutBlueprint(5, LogoAnchor.BOTTOM_RIGHT,TextAlignment.LEAD_LEFT,  AccentShape.BOTTOM_BAND, QrPlacement.TOP_LEFT,     Divider.HAIRLINE, 31),
            BusinessCardLayoutBlueprint(6, LogoAnchor.CENTER_LEFT, TextAlignment.LEAD_LEFT,  AccentShape.CORNER_FRAME,QrPlacement.BOTTOM_RIGHT, Divider.GOLDEN,   37),
            BusinessCardLayoutBlueprint(7, LogoAnchor.TOP_LEFT,    TextAlignment.LEAD_LEFT,  AccentShape.HAIRLINE_GRID,QrPlacement.INSET_RIGHT, Divider.NONE,     41),
            BusinessCardLayoutBlueprint(8, LogoAnchor.TOP_RIGHT,   TextAlignment.LEAD_LEFT,  AccentShape.RADIAL_GLOW, QrPlacement.BOTTOM_RIGHT, Divider.HAIRLINE, 43),
            BusinessCardLayoutBlueprint(9, LogoAnchor.CENTER,      TextAlignment.CENTER,     AccentShape.FULL_BLEED,  QrPlacement.INSET_LEFT,   Divider.NONE,     47),
        )
    }
}
