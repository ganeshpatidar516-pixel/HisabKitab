package com.ganesh.hisabkitabpro.domain.businesscard

/**
 * Pure colour palette used by every painter. Stored as raw 32-bit ARGB ints so the
 * domain layer does not depend on android.graphics.Color (keeping it test-friendly).
 *
 * All five categories deliberately ship with their own deterministic palette set so
 * variations across an entire 50-card sheet read as a curated collection rather than a
 * random pile. The blueprint index acts as a deterministic seed inside [forVariant].
 */
data class BusinessCardPalette(
    val backgroundArgb: Int,
    val surfaceArgb: Int,
    val accentArgb: Int,
    val accentSecondaryArgb: Int,
    val titleArgb: Int,
    val bodyArgb: Int,
    val muteArgb: Int,
    val hairlineArgb: Int,
) {
    companion object {

        private const val ROYAL_BLACK = 0xFF000000.toInt()
        private const val ROYAL_INK = 0xFF0B0B0D.toInt()
        private const val ROYAL_GOLD = 0xFFD4AF37.toInt()
        private const val ROYAL_GOLD_DEEP = 0xFF8C6A1A.toInt()
        private const val ROYAL_PARCHMENT = 0xFFF5E6B6.toInt()

        private const val CORPORATE_INK = 0xFF111418.toInt()
        private const val CORPORATE_PAPER = 0xFFFAFAFA.toInt()
        private const val CORPORATE_GREY = 0xFF6B7280.toInt()
        private const val CORPORATE_BLUE = 0xFF1E40AF.toInt()
        private const val CORPORATE_BLUE_DEEP = 0xFF0B2A6B.toInt()

        private const val MODERN_VIOLET = 0xFF6D28D9.toInt()
        private const val MODERN_TANGERINE = 0xFFF97316.toInt()
        private const val MODERN_TEAL = 0xFF0EA5A4.toInt()
        private const val MODERN_INK = 0xFF0F172A.toInt()
        private const val MODERN_PAPER = 0xFFF8FAFC.toInt()

        private const val ECO_PAPER = 0xFFF6F1E7.toInt()
        private const val ECO_FOREST = 0xFF2F5D3A.toInt()
        private const val ECO_MOSS = 0xFF6B8E4E.toInt()
        private const val ECO_CLAY = 0xFFB45F2C.toInt()
        private const val ECO_INK = 0xFF1F2421.toInt()

        private const val DIGITAL_NIGHT = 0xFF050816.toInt()
        private const val DIGITAL_NEON_CYAN = 0xFF22D3EE.toInt()
        private const val DIGITAL_NEON_MAGENTA = 0xFFEC4899.toInt()
        private const val DIGITAL_NEON_LIME = 0xFFA3E635.toInt()
        private const val DIGITAL_FOG = 0xFFCBD5F5.toInt()

        fun forVariant(category: BusinessCardCategory, variantIndex: Int): BusinessCardPalette {
            val v = ((variantIndex % 10) + 10) % 10
            return when (category) {
                BusinessCardCategory.ROYAL_SIGNATURE -> royal(v)
                BusinessCardCategory.CORPORATE_ELITE -> corporate(v)
                BusinessCardCategory.MODERN_ABSTRACT -> modern(v)
                BusinessCardCategory.ECO_ORGANIC -> eco(v)
                BusinessCardCategory.DIGITAL_NATIVE -> digital(v)
            }
        }

        private fun royal(v: Int): BusinessCardPalette {
            val bg = if (v % 3 == 0) ROYAL_BLACK else ROYAL_INK
            val accent = if (v % 2 == 0) ROYAL_GOLD else ROYAL_GOLD_DEEP
            val accent2 = if (v % 2 == 0) ROYAL_GOLD_DEEP else ROYAL_GOLD
            val parchment = ROYAL_PARCHMENT
            return BusinessCardPalette(
                backgroundArgb = bg,
                surfaceArgb = if (v >= 5) parchment else bg,
                accentArgb = accent,
                accentSecondaryArgb = accent2,
                titleArgb = if (v >= 5) ROYAL_INK else parchment,
                bodyArgb = if (v >= 5) ROYAL_GOLD_DEEP else 0xFFE6D396.toInt(),
                muteArgb = if (v >= 5) 0xFF806A2A.toInt() else 0xFF8A7A55.toInt(),
                hairlineArgb = accent,
            )
        }

        private fun corporate(v: Int): BusinessCardPalette {
            val isDark = v % 2 == 1
            val accent = if (v % 3 == 0) CORPORATE_BLUE else if (v % 3 == 1) CORPORATE_INK else CORPORATE_BLUE_DEEP
            return BusinessCardPalette(
                backgroundArgb = if (isDark) CORPORATE_INK else CORPORATE_PAPER,
                surfaceArgb = if (isDark) 0xFF1B1F25.toInt() else 0xFFFFFFFF.toInt(),
                accentArgb = accent,
                accentSecondaryArgb = if (isDark) CORPORATE_BLUE else CORPORATE_BLUE_DEEP,
                titleArgb = if (isDark) CORPORATE_PAPER else CORPORATE_INK,
                bodyArgb = if (isDark) 0xFFD8DCE2.toInt() else 0xFF2A2F37.toInt(),
                muteArgb = CORPORATE_GREY,
                hairlineArgb = if (isDark) 0xFF323842.toInt() else 0xFFE5E7EB.toInt(),
            )
        }

        private fun modern(v: Int): BusinessCardPalette {
            val accent = when (v % 3) {
                0 -> MODERN_VIOLET
                1 -> MODERN_TANGERINE
                else -> MODERN_TEAL
            }
            val accent2 = when (v % 3) {
                0 -> MODERN_TEAL
                1 -> MODERN_VIOLET
                else -> MODERN_TANGERINE
            }
            val isDark = (v / 3) % 2 == 1
            return BusinessCardPalette(
                backgroundArgb = if (isDark) MODERN_INK else MODERN_PAPER,
                surfaceArgb = if (isDark) 0xFF111B33.toInt() else 0xFFFFFFFF.toInt(),
                accentArgb = accent,
                accentSecondaryArgb = accent2,
                titleArgb = if (isDark) MODERN_PAPER else MODERN_INK,
                bodyArgb = if (isDark) 0xFFCBD5F5.toInt() else 0xFF334155.toInt(),
                muteArgb = 0xFF94A3B8.toInt(),
                hairlineArgb = if (isDark) 0xFF1E293B.toInt() else 0xFFE2E8F0.toInt(),
            )
        }

        private fun eco(v: Int): BusinessCardPalette {
            val accent = when (v % 3) {
                0 -> ECO_FOREST
                1 -> ECO_MOSS
                else -> ECO_CLAY
            }
            val isInverted = v >= 7
            return BusinessCardPalette(
                backgroundArgb = if (isInverted) ECO_INK else ECO_PAPER,
                surfaceArgb = if (isInverted) 0xFF26302A.toInt() else 0xFFFFFFFF.toInt(),
                accentArgb = accent,
                accentSecondaryArgb = if (v % 2 == 0) ECO_CLAY else ECO_FOREST,
                titleArgb = if (isInverted) ECO_PAPER else ECO_INK,
                bodyArgb = if (isInverted) 0xFFD8D3C2.toInt() else 0xFF3B423E.toInt(),
                muteArgb = if (isInverted) 0xFF98A09A.toInt() else 0xFF7B8278.toInt(),
                hairlineArgb = if (isInverted) 0xFF374138.toInt() else 0xFFD8D3C2.toInt(),
            )
        }

        private fun digital(v: Int): BusinessCardPalette {
            val accent = when (v % 3) {
                0 -> DIGITAL_NEON_CYAN
                1 -> DIGITAL_NEON_MAGENTA
                else -> DIGITAL_NEON_LIME
            }
            val accent2 = when (v % 3) {
                0 -> DIGITAL_NEON_MAGENTA
                1 -> DIGITAL_NEON_LIME
                else -> DIGITAL_NEON_CYAN
            }
            return BusinessCardPalette(
                backgroundArgb = DIGITAL_NIGHT,
                surfaceArgb = 0xFF0B1023.toInt(),
                accentArgb = accent,
                accentSecondaryArgb = accent2,
                titleArgb = DIGITAL_FOG,
                bodyArgb = 0xFF93A3C9.toInt(),
                muteArgb = 0xFF607093.toInt(),
                hairlineArgb = 0xFF1B2240.toInt(),
            )
        }
    }
}
