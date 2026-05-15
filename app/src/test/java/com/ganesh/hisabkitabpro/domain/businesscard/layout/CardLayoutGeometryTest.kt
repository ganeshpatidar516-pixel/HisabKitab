package com.ganesh.hisabkitabpro.domain.businesscard.layout

import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardLayoutBlueprint
import com.ganesh.hisabkitabpro.domain.businesscard.GoldenRatioGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardLayoutGeometryTest {

    private val cardW = 1000f
    /** Taller than default φ card so top-logo + QR tests do not hit vertical safe-rect collapse. */
    private val cardH = 1200f
    private val bounds = LayoutRect.fromLtrb(0f, 0f, cardW, cardH)
    private val padding = GoldenRatioGrid.compute(cardW, cardH).safePadding

    @Test
    fun qrSideLength_respectsFloorAndCap() {
        val q = CardLayoutGeometry.qrSideLength(cardW, cardH)
        assertTrue(q >= cardW * CardLayoutGeometry.QR_MIN_FRACTION_OF_WIDTH * 0.99f)
        assertTrue(q <= kotlin.math.min(cardW, cardH) * CardLayoutGeometry.QR_MAX_FRACTION_OF_MIN_EDGE * 1.01f)
    }

    @Test
    fun logoSideLength_matchesFraction() {
        assertEquals(cardW * CardLayoutGeometry.LOGO_FRACTION_OF_WIDTH, CardLayoutGeometry.logoSideLength(cardW), 0.01f)
    }

    @Test
    fun textSafeRect_leadLeft_clearsLeftInsetQr() {
        val bp = BusinessCardLayoutBlueprint.at(9).copy(
            textAlignment = BusinessCardLayoutBlueprint.TextAlignment.LEAD_LEFT,
        )
        require(bp.qrPlacement == BusinessCardLayoutBlueprint.QrPlacement.INSET_LEFT)
        val qrSlot = LayoutRect.fromLtrb(
            bounds.left + padding,
            bounds.centerY() - 100f,
            bounds.left + padding + 200f,
            bounds.centerY() + 100f,
        )
        val r = CardLayoutGeometry.computeTextSafeRect(bounds, padding, logoSlot = null, qrSlot = qrSlot, blueprint = bp)
        assertTrue("text clears QR on the left", r.left >= qrSlot.right + padding * 0.4f)
    }

    @Test
    fun textSafeRect_leadRight_clearsRightInsetQr() {
        val bp = BusinessCardLayoutBlueprint.at(7).copy(
            textAlignment = BusinessCardLayoutBlueprint.TextAlignment.LEAD_RIGHT,
        )
        require(bp.qrPlacement == BusinessCardLayoutBlueprint.QrPlacement.INSET_RIGHT)
        val qrSlot = LayoutRect.fromLtrb(
            bounds.right - padding - 200f,
            bounds.centerY() - 100f,
            bounds.right - padding,
            bounds.centerY() + 100f,
        )
        val r = CardLayoutGeometry.computeTextSafeRect(bounds, padding, null, qrSlot, bp)
        assertTrue("text clears QR on the right", r.right <= qrSlot.left - padding * 0.4f)
    }

    @Test
    fun textSafeRect_topLogo_pushesTopDown() {
        val bp = BusinessCardLayoutBlueprint.at(0)
        require(bp.logoAnchor == BusinessCardLayoutBlueprint.LogoAnchor.TOP_LEFT)
        val side = 180f
        // Logo must extend below the padded text band start (bounds.top + padding) or the composer will not inset.
        val logo = LayoutRect.fromLtrb(padding, padding, padding + side, padding + side)
        val r = CardLayoutGeometry.computeTextSafeRect(bounds, padding, logo, qrSlot = null, blueprint = bp)
        assertTrue(r.top >= logo.bottom + padding * 0.4f)
    }

    @Test
    fun textSafeRect_centerLeftLogo_pushesTextRight() {
        val bp = BusinessCardLayoutBlueprint.at(6)
        require(bp.logoAnchor == BusinessCardLayoutBlueprint.LogoAnchor.CENTER_LEFT)
        val logo = LayoutRect.fromLtrb(
            bounds.left + padding,
            bounds.centerY() - 90f,
            bounds.left + padding + 180f,
            bounds.centerY() + 90f,
        )
        val r = CardLayoutGeometry.computeTextSafeRect(bounds, padding, logo, null, bp)
        assertTrue(r.left >= logo.right + padding * 0.35f)
    }

    @Test
    fun layoutRect_intersects_detectsOverlap() {
        val a = LayoutRect.fromLtrb(0f, 0f, 10f, 10f)
        val b = LayoutRect.fromLtrb(5f, 5f, 15f, 15f)
        val c = LayoutRect.fromLtrb(20f, 20f, 30f, 30f)
        assertTrue(a.intersects(b))
        assertFalse(a.intersects(c))
    }

    @Test
    fun allTenBlueprints_textSafeRect_disjointFromLogoAndQrSlots() {
        val blueprints = BusinessCardLayoutBlueprint.set()
        assertEquals(10, blueprints.size)
        val cardW = 1000f
        val cardH = (cardW / GoldenRatioGrid.PHI).toFloat()
        val bounds = LayoutRect.fromLtrb(0f, 0f, cardW, cardH)
        val padding = GoldenRatioGrid.compute(cardW, cardH).safePadding
        val logoSide = CardLayoutGeometry.logoSideLength(cardW)
        val qrSide = CardLayoutGeometry.qrSideLength(cardW, cardH)
        val eps = 0.5f
        for (bp in blueprints) {
            val logo = CardLayoutGeometry.logoSlot(bounds, bp.logoAnchor, padding, logoSide)
            val qr = CardLayoutGeometry.qrSlot(bounds, bp.qrPlacement, padding, qrSide)
            assertTrue(
                "variant ${bp.variantIndex} logo inside card",
                logo.left >= bounds.left - eps && logo.top >= bounds.top - eps &&
                    logo.right <= bounds.right + eps && logo.bottom <= bounds.bottom + eps,
            )
            assertTrue(
                "variant ${bp.variantIndex} qr inside card",
                qr.left >= bounds.left - eps && qr.top >= bounds.top - eps &&
                    qr.right <= bounds.right + eps && qr.bottom <= bounds.bottom + eps,
            )
            val text = CardLayoutGeometry.computeTextSafeRect(bounds, padding, logo, qr, bp)
            assertFalse(
                "variant ${bp.variantIndex} (${bp.logoAnchor}, ${bp.qrPlacement}, ${bp.textAlignment}): text ∩ logo",
                text.intersects(logo),
            )
            assertFalse(
                "variant ${bp.variantIndex} (${bp.logoAnchor}, ${bp.qrPlacement}, ${bp.textAlignment}): text ∩ qr",
                text.intersects(qr),
            )
            assertTrue(
                "variant ${bp.variantIndex} text band has area",
                text.width() > 2f && text.height() > 2f,
            )
        }
    }

    @Test
    fun textSafeRect_horizontalCollapse_resetsToPaddedBounds() {
        val bp = BusinessCardLayoutBlueprint.at(0).copy(textAlignment = BusinessCardLayoutBlueprint.TextAlignment.LEAD_LEFT)
        val hugeQr = LayoutRect.fromLtrb(bounds.left, bounds.top, bounds.right, bounds.bottom)
        val r = CardLayoutGeometry.computeTextSafeRect(bounds, padding, null, hugeQr, bp)
        assertEquals(bounds.left + padding, r.left, 0.5f)
        assertEquals(bounds.right - padding, r.right, 0.5f)
    }
}
