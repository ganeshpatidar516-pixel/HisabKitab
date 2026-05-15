package com.ganesh.hisabkitabpro.domain.businesscard.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardContactLineBuilder
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardLayoutBlueprint
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardPalette
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardTypography
import com.ganesh.hisabkitabpro.domain.businesscard.GoldenRatioGrid
import com.ganesh.hisabkitabpro.domain.businesscard.LogoAspectClassifier
import com.ganesh.hisabkitabpro.domain.businesscard.layout.CardLayoutGeometry
import com.ganesh.hisabkitabpro.domain.businesscard.layout.LayoutRect

/**
 * Centralised "after the background and accent are drawn" composer that lays out the
 * canonical card content — logo well, brand title, owner subtitle, contact rail and the
 * vCard QR. Painters call this once they have established their aesthetic.
 *
 * Keeping this in one place guarantees that every category respects the same hierarchy
 * (brand → person → contacts → qr) which is what gives a 50-card sheet the disciplined,
 * curated feel we promised.
 *
 * **Two-pass layout:** pass 1 measures total vertical extent for a content scale; pass 2
 * applies a safe content scale derived from the measured overflow so the full stack
 * (including every registry rail line) fits inside the clipped text region without
 * clipping.
 */
internal object CardLayoutCompositor {

    private const val CONTENT_SCALE_FLOOR = 0.40f
    // Binary-searching scale is too expensive for per-tile scrolling; we instead derive
    // a deterministic ratio from a single measurement pass.

    private fun GoldenRatioGrid.Metrics.withContentScale(scale: Float): GoldenRatioGrid.Metrics = copy(
        titleSizePx = (titleSizePx * scale).coerceAtLeast(4.8f),
        subtitleSizePx = (subtitleSizePx * scale).coerceAtLeast(4.4f),
        bodySizePx = (bodySizePx * scale).coerceAtLeast(4f),
        microSizePx = (microSizePx * scale).coerceAtLeast(3.6f),
        baselineRhythm = (baselineRhythm * scale).coerceAtLeast(3f),
    )

    private data class ContactRailSolve(
        val bodyPaint: Paint,
        val chunks: List<String>,
        val lineHeight: Float,
    )

    fun composeBody(
        canvas: Canvas,
        bounds: RectF,
        blueprint: BusinessCardLayoutBlueprint,
        palette: BusinessCardPalette,
        typography: BusinessCardTypography,
        profile: BusinessCardProfile,
        logo: Bitmap?,
        qr: Bitmap?,
        logoShape: LogoAspectClassifier.LogoShape,
    ) {
        val metrics = GoldenRatioGrid.compute(bounds.width(), bounds.height())
        val padding = metrics.safePadding

        val logoSize = CardLayoutGeometry.logoSideLength(bounds.width())
        val qrSize = CardLayoutGeometry.qrSideLength(bounds.width(), bounds.height())

        // Defence-in-depth: drop already-recycled bitmaps before slot planning so a
        // mid-frame race with the lifecycle cleanup can never reach the native canvas
        // with a freed pointer (which would otherwise throw `Canvas: trying to use a
        // recycled bitmap` and tear down the entire engine).
        val safeLogo = logo?.takeIf { !it.isRecycled }
        val safeQr = qr?.takeIf { !it.isRecycled }

        val logoSlot = if (safeLogo != null) {
            CardLayoutGeometry.logoSlot(bounds.toLayoutRect(), blueprint.logoAnchor, padding, logoSize).toRectF()
        } else null

        val qrSlot = if (safeQr != null) {
            CardLayoutGeometry.qrSlot(bounds.toLayoutRect(), blueprint.qrPlacement, padding, qrSize).toRectF()
        } else null

        if (logoSlot != null && safeLogo != null) {
            val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            PainterToolkit.drawLogo(canvas, logoPaint, safeLogo, logoSlot, logoShape)
        }
        if (qrSlot != null && safeQr != null) {
            val qrSurface = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                PainterToolkit.softShadow(this, radius = bounds.width() * 0.012f, dy = bounds.width() * 0.004f, color = 0x33000000)
            }
            PainterToolkit.drawQr(canvas, safeQr, qrSlot, qrSurface)
        }

        val textRegion = CardLayoutGeometry.computeTextSafeRect(
            bounds = bounds.toLayoutRect(),
            padding = padding,
            logoSlot = logoSlot?.toLayoutRect(),
            qrSlot = qrSlot?.toLayoutRect(),
            blueprint = blueprint,
        ).toRectF()
        val clipCount = canvas.save()
        canvas.clipRect(
            textRegion.left,
            textRegion.top,
            textRegion.right,
            textRegion.bottom,
        )
        drawTextStack(canvas, textRegion, blueprint, palette, typography, profile)
        canvas.restoreToCount(clipCount)
        if (qrSlot != null && qr != null) {
            drawQrScanCaption(canvas, bounds, qrSlot, padding, palette)
        }
    }

    /** Micro CTA under the QR so the block reads as intentional brand chrome, not a pasted asset. */
    private fun drawQrScanCaption(
        canvas: Canvas,
        bounds: RectF,
        qrSlot: RectF,
        padding: Float,
        palette: BusinessCardPalette,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.muteArgb
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = (bounds.width() * 0.026f).coerceIn(7f, 14f)
            textAlign = Paint.Align.CENTER
            isSubpixelText = true
        }
        val label = "SCAN · SAVE CONTACT"
        val baseline = (qrSlot.bottom + paint.textSize * 0.75f).coerceAtMost(bounds.bottom - padding * 0.35f)
        if (baseline > qrSlot.bottom && baseline < bounds.bottom) {
            canvas.drawText(label, qrSlot.centerX(), baseline, paint)
        }
    }

    /** Bottom Y of ink after the full stack at [rowMetrics] (no drawing). */
    private fun measureStackBottomY(
        region: RectF,
        blueprint: BusinessCardLayoutBlueprint,
        palette: BusinessCardPalette,
        typography: BusinessCardTypography,
        profile: BusinessCardProfile,
        rowMetrics: GoldenRatioGrid.Metrics,
    ): Float {
        val yAfterHeader = advanceHeaderBlockY(
            region = region,
            blueprint = blueprint,
            palette = palette,
            typography = typography,
            profile = profile,
            rowMetrics = rowMetrics,
            bodyPaintForSpacing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.bodyArgb
                typeface = PainterToolkit.typeface(typography.bodyFont, typography.bodyWeight)
                textSize = rowMetrics.bodySizePx
            },
        )
        val contactLines = BusinessCardContactLineBuilder.lines(profile)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.bodyArgb
            typeface = PainterToolkit.typeface(typography.bodyFont, typography.bodyWeight)
            textSize = rowMetrics.bodySizePx
            letterSpacing = typography.bodyLetterSpacingEm
        }
        val available = (region.bottom - yAfterHeader - bodyPaint.textSize * 0.12f).coerceAtLeast(0f)
        val solved = solveContactRail(contactLines, region.width(), available, rowMetrics.bodySizePx, typography, palette)
            ?: return yAfterHeader + bodyPaint.textSize * 0.2f
        return yAfterHeader + solved.chunks.size * solved.lineHeight + solved.bodyPaint.fontMetrics.descent * 0.5f
    }

    /**
     * Walks title → tagline → owner → divider spacing; returns Y after that block (next baseline
     * anchor for the contact rail). [bodyPaintForSpacing] supplies textSize for divider gaps.
     */
    private fun advanceHeaderBlockY(
        region: RectF,
        blueprint: BusinessCardLayoutBlueprint,
        palette: BusinessCardPalette,
        typography: BusinessCardTypography,
        profile: BusinessCardProfile,
        rowMetrics: GoldenRatioGrid.Metrics,
        bodyPaintForSpacing: Paint,
    ): Float {
        val titleText = profile.businessName.ifBlank { profile.ownerName }.uppercase()
        val ownerText = profile.ownerName
        val taglineText = profile.tagline.trim()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.titleArgb
            typeface = PainterToolkit.typeface(typography.titleFont, typography.titleWeight)
            textSize = rowMetrics.titleSizePx
            letterSpacing = typography.titleLetterSpacingEm
        }
        val titleSize = PainterToolkit.fitFontSize(
            paint = titlePaint,
            text = titleText,
            maxWidth = region.width(),
            startSize = rowMetrics.titleSizePx,
            minSize = rowMetrics.titleSizePx * 0.55f,
        )
        titlePaint.textSize = titleSize
        val titleFontMetrics = titlePaint.fontMetrics

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.bodyArgb
            typeface = PainterToolkit.typeface(typography.bodyFont, typography.titleWeight)
            textSize = rowMetrics.subtitleSizePx
            letterSpacing = typography.bodyLetterSpacingEm
        }

        var y = region.top - titleFontMetrics.ascent
        y += titleSize * typography.titleLineHeightFactor

        if (taglineText.isNotEmpty()) {
            val taglinePaint = Paint(subtitlePaint).apply {
                textSize = subtitlePaint.textSize * 0.92f
                color = palette.muteArgb
            }
            val tagFit = PainterToolkit.fitFontSize(
                paint = taglinePaint,
                text = taglineText,
                maxWidth = region.width(),
                startSize = taglinePaint.textSize,
                minSize = taglinePaint.textSize * 0.55f,
            )
            taglinePaint.textSize = tagFit
            y += taglinePaint.textSize * 0.35f + taglinePaint.textSize * typography.bodyLineHeightFactor
        }

        if (ownerText.isNotBlank() && !ownerText.equals(titleText, ignoreCase = true)) {
            y += subtitlePaint.textSize * 0.1f + subtitlePaint.textSize * typography.bodyLineHeightFactor
        }

        // Hairline is drawn at ~y but does not advance the layout baseline (same as [drawTextStack]).
        if (blueprint.divider != BusinessCardLayoutBlueprint.Divider.NONE) {
            y += bodyPaintForSpacing.textSize * 0.6f
        } else {
            y += bodyPaintForSpacing.textSize * 0.2f
        }
        return y
    }

    private fun solveContactRail(
        contactLines: List<String>,
        regionWidth: Float,
        available: Float,
        bodyStartSize: Float,
        typography: BusinessCardTypography,
        palette: BusinessCardPalette,
    ): ContactRailSolve? {
        if (contactLines.isEmpty() || available <= 1f) return null
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.bodyArgb
            typeface = PainterToolkit.typeface(typography.bodyFont, typography.bodyWeight)
            letterSpacing = typography.bodyLetterSpacingEm
            textSize = bodyStartSize
        }
        var bodySize = bodyStartSize
        var lineHeightFactor = typography.bodyLineHeightFactor
        val minBody = 4.8f
        var chunks: List<String> = emptyList()
        var iter = 0
        while (iter++ < 72) {
            bodyPaint.textSize = bodySize
            chunks = flattenWrappedLines(bodyPaint, contactLines, regionWidth)
            val lh = bodyPaint.textSize * lineHeightFactor
            val needed = chunks.size * lh
            if (needed <= available || (bodySize <= minBody && lineHeightFactor <= 1.04f)) break
            when {
                bodySize > minBody -> bodySize *= 0.91f
                lineHeightFactor > 1.04f -> lineHeightFactor = (lineHeightFactor - 0.028f).coerceAtLeast(1.04f)
                else -> break
            }
        }
        bodyPaint.textSize = bodySize
        chunks = flattenWrappedLines(bodyPaint, contactLines, regionWidth)
        var lh = bodyPaint.textSize * lineHeightFactor
        var safety = 0
        while (chunks.isNotEmpty() && chunks.size * lh > available && bodyPaint.textSize > 3.35f && safety++ < 64) {
            bodyPaint.textSize *= 0.88f
            chunks = flattenWrappedLines(bodyPaint, contactLines, regionWidth)
            lh = bodyPaint.textSize * lineHeightFactor
        }
        safety = 0
        while (chunks.isNotEmpty() && chunks.size * lh > available && lineHeightFactor > 1.03f && safety++ < 32) {
            lineHeightFactor = (lineHeightFactor - 0.025f).coerceAtLeast(1.03f)
            lh = bodyPaint.textSize * lineHeightFactor
        }
        var fitGuard = 0
        while (chunks.isNotEmpty() && chunks.size * lh > available && fitGuard++ < 8) {
            bodyPaint.textSize = (available / chunks.size / lineHeightFactor).coerceIn(3.0f, bodyPaint.textSize)
            chunks = flattenWrappedLines(bodyPaint, contactLines, regionWidth)
            lh = bodyPaint.textSize * lineHeightFactor
        }
        return ContactRailSolve(bodyPaint, chunks, lh)
    }

    private fun drawTextStack(
        canvas: Canvas,
        region: RectF,
        blueprint: BusinessCardLayoutBlueprint,
        palette: BusinessCardPalette,
        typography: BusinessCardTypography,
        profile: BusinessCardProfile,
    ) {
        val baseRowMetrics = GoldenRatioGrid.compute(region.width().coerceAtLeast(1f), region.height().coerceAtLeast(1f))

        // —— Pass 1: measure at full scale —— //
        val measuredBottom = measureStackBottomY(
            region, blueprint, palette, typography, profile, baseRowMetrics,
        )

        val contentScale = if (!measuredBottom.isFinite() || measuredBottom <= region.bottom) {
            1f
        } else {
            // Scale = targetHeight / measuredHeight
            val measuredHeight = (measuredBottom - region.top).coerceAtLeast(1f)
            val targetHeight = (region.bottom - region.top).coerceAtLeast(1f)
            (targetHeight / measuredHeight).coerceIn(CONTENT_SCALE_FLOOR, 1f)
        }

        val rowMetrics = baseRowMetrics.withContentScale(contentScale)

        val titleText = profile.businessName.ifBlank { profile.ownerName }.uppercase()
        val ownerText = profile.ownerName
        val taglineText = profile.tagline.trim()
        val contactLines = BusinessCardContactLineBuilder.lines(profile)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.titleArgb
            typeface = PainterToolkit.typeface(typography.titleFont, typography.titleWeight)
            textSize = rowMetrics.titleSizePx
            letterSpacing = typography.titleLetterSpacingEm
        }
        val titleSize = PainterToolkit.fitFontSize(
            paint = titlePaint,
            text = titleText,
            maxWidth = region.width(),
            startSize = rowMetrics.titleSizePx,
            minSize = rowMetrics.titleSizePx * 0.55f,
        )
        titlePaint.textSize = titleSize
        val titleFontMetrics = titlePaint.fontMetrics

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.bodyArgb
            typeface = PainterToolkit.typeface(typography.bodyFont, typography.titleWeight)
            textSize = rowMetrics.subtitleSizePx
            letterSpacing = typography.bodyLetterSpacingEm
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.bodyArgb
            typeface = PainterToolkit.typeface(typography.bodyFont, typography.bodyWeight)
            textSize = rowMetrics.bodySizePx
            letterSpacing = typography.bodyLetterSpacingEm
        }

        val rightX = region.right
        val centerX = region.centerX()
        val leftX = region.left

        var y = region.top - titleFontMetrics.ascent
        y = PainterToolkit.drawTextLine(canvas, titlePaint, titleText, leftX, y, titleSize * typography.titleLineHeightFactor, blueprint.textAlignment, rightX, centerX)

        if (taglineText.isNotEmpty()) {
            val taglinePaint = Paint(subtitlePaint).apply {
                textSize = subtitlePaint.textSize * 0.92f
                color = palette.muteArgb
            }
            val tagFit = PainterToolkit.fitFontSize(
                paint = taglinePaint,
                text = taglineText,
                maxWidth = region.width(),
                startSize = taglinePaint.textSize,
                minSize = taglinePaint.textSize * 0.55f,
            )
            taglinePaint.textSize = tagFit
            y = PainterToolkit.drawTextLine(
                canvas, taglinePaint, taglineText, leftX, y + taglinePaint.textSize * 0.35f,
                taglinePaint.textSize * typography.bodyLineHeightFactor, blueprint.textAlignment, rightX, centerX,
            )
        }

        if (ownerText.isNotBlank() && !ownerText.equals(titleText, ignoreCase = true)) {
            y = PainterToolkit.drawTextLine(
                canvas, subtitlePaint, ownerText, leftX, y + subtitlePaint.textSize * 0.1f,
                subtitlePaint.textSize * typography.bodyLineHeightFactor, blueprint.textAlignment, rightX, centerX,
            )
        }

        when (blueprint.divider) {
            BusinessCardLayoutBlueprint.Divider.HAIRLINE -> drawHairline(canvas, region, y, palette.hairlineArgb, dotted = false)
            BusinessCardLayoutBlueprint.Divider.GOLDEN -> drawHairline(canvas, region, y, palette.accentArgb, dotted = false)
            BusinessCardLayoutBlueprint.Divider.DOTTED -> drawHairline(canvas, region, y, palette.muteArgb, dotted = true)
            BusinessCardLayoutBlueprint.Divider.NONE -> Unit
        }
        if (blueprint.divider != BusinessCardLayoutBlueprint.Divider.NONE) {
            y += bodyPaint.textSize * 0.6f
        } else {
            y += bodyPaint.textSize * 0.2f
        }

        val available = (region.bottom - y - bodyPaint.textSize * 0.12f).coerceAtLeast(0f)
        val solved = solveContactRail(contactLines, region.width(), available, rowMetrics.bodySizePx, typography, palette)
        if (solved != null) {
            for (chunk in solved.chunks) {
                y = PainterToolkit.drawTextLine(
                    canvas, solved.bodyPaint, chunk, leftX, y, solved.lineHeight, blueprint.textAlignment, rightX, centerX,
                )
            }
        }
    }

    private fun flattenWrappedLines(paint: Paint, lines: List<String>, maxWidth: Float): List<String> {
        if (lines.isEmpty()) return emptyList()
        val out = ArrayList<String>(lines.size * 3)
        for (line in lines) {
            out.addAll(PainterToolkit.wrapText(paint, line, maxWidth, maxLines = 96))
        }
        return out
    }

    private fun drawHairline(canvas: Canvas, region: RectF, y: Float, color: Int, dotted: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = region.width() * 0.0035f
            if (dotted) {
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(strokeWidth * 2f, strokeWidth * 3f), 0f)
            }
        }
        canvas.drawLine(region.left, y + region.width() * 0.005f, region.right, y + region.width() * 0.005f, paint)
    }

    private fun RectF.toLayoutRect() = LayoutRect(left, top, right, bottom)

    private fun LayoutRect.toRectF() = RectF(left, top, right, bottom)
}
