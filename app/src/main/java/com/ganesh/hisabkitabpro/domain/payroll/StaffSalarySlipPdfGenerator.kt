package com.ganesh.hisabkitabpro.domain.payroll

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.local.StaffEntity
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.LogoRenderFit
import com.ganesh.hisabkitabpro.domain.profile.ProfileBitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a single-page A4 salary slip PDF.
 *
 * Theme is intentionally template-agnostic: same neutral header used by every
 * business profile, with the merchant's brand colour applied to accent strips.
 * No theme-engine flags are read or modified — this generator only consumes
 * the [BusinessProfile] for branding text and (optionally) the logo.
 */
class StaffSalarySlipPdfGenerator(private val context: Context) {

    suspend fun generate(
        staff: StaffEntity,
        result: StaffPayrollEngine.PayrollResult,
        periodLabel: String,
        currencySymbol: String = "\u20B9"
    ): File = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val profile = db.businessProfileDao().getBusinessProfileOnce()

        val width = 595
        val height = 842
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create())
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        try {
            drawHeader(canvas, paint, profile, width, context)

            var y = 160f
            val margin = 40f

            paint.color = ACCENT
            paint.textSize = 18f
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText("SALARY SLIP", margin, y, paint)

            paint.textSize = 11f
            paint.color = MUTED
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Period: $periodLabel", margin, y + 18f, paint)
            canvas.drawText(
                "Issued: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}",
                width - margin - 160f,
                y + 18f,
                paint
            )

            y += 50f
            drawKeyValueGrid(
                canvas = canvas,
                paint = paint,
                left = margin,
                right = width - margin,
                top = y,
                rows = listOf(
                    "Staff Name" to staff.name,
                    "Designation" to staff.designation.ifBlank { staff.role },
                    "Joining Date" to formatDate(staff.joiningDate),
                    "Salary Type" to staff.salaryType.replace('_', ' '),
                    "Workdays / Week" to staff.workdaysPerWeek.toString(),
                    "Base Salary" to formatMoney(result.baseSalaryPaise, currencySymbol)
                )
            )
            y += 6 * ROW_HEIGHT + 16f

            paint.color = ACCENT
            paint.textSize = 13f
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText("Attendance Summary", margin, y, paint)
            y += 18f
            drawKeyValueGrid(
                canvas = canvas,
                paint = paint,
                left = margin,
                right = width - margin,
                top = y,
                rows = listOf(
                    "Calendar Days" to result.totalDays.toString(),
                    "Present" to result.presentCount.toString(),
                    "Half Day" to result.halfDayCount.toString(),
                    "Absent" to result.absentCount.toString(),
                    "Leave" to result.leaveCount.toString(),
                    "Effective Days" to formatDays(result.effectiveDays)
                )
            )
            y += 6 * ROW_HEIGHT + 16f

            paint.color = ACCENT
            paint.textSize = 13f
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText("Earnings & Adjustments", margin, y, paint)
            y += 18f
            val breakdownRows = listOf(
                "Earned (attendance)" to formatMoney(result.earnedPaise, currencySymbol),
                "Loss of Pay" to "- " + formatMoney(result.lossOfPayPaise, currencySymbol),
                "Bonuses" to "+ " + formatMoney(result.bonusesPaise, currencySymbol),
                "Advances Taken" to "- " + formatMoney(result.advancesPaise, currencySymbol),
                "Deductions" to "- " + formatMoney(result.deductionsPaise, currencySymbol),
                "Salary Paid" to "- " + formatMoney(result.salaryPaidPaise, currencySymbol)
            )
            drawKeyValueGrid(
                canvas = canvas,
                paint = paint,
                left = margin,
                right = width - margin,
                top = y,
                rows = breakdownRows
            )
            y += breakdownRows.size * ROW_HEIGHT + 20f

            // Net payable callout
            val callout = Paint(paint).apply {
                color = ACCENT
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(margin, y, width - margin, y + 64f, 14f, 14f, callout)
            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            canvas.drawText("NET PAYABLE", margin + 18f, y + 26f, paint)
            paint.textSize = 22f
            canvas.drawText(
                formatMoney(result.netPayablePaise, currencySymbol),
                margin + 18f,
                y + 52f,
                paint
            )

            // Inline breakdown formula — keeps the math auditable without
            // relying on the recipient to recompute totals manually.
            paint.color = MUTED
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f
            val formula = "Net = Earned + Bonus − Advance − Deduction − Salary Paid"
            canvas.drawText(formula, margin, y + 78f, paint)
            val numericFormula = listOf(
                formatMoney(result.earnedPaise, currencySymbol),
                "+ " + formatMoney(result.bonusesPaise, currencySymbol),
                "- " + formatMoney(result.advancesPaise, currencySymbol),
                "- " + formatMoney(result.deductionsPaise, currencySymbol),
                "- " + formatMoney(result.salaryPaidPaise, currencySymbol),
                "= " + formatMoney(result.netPayablePaise, currencySymbol)
            ).joinToString(separator = "  ")
            canvas.drawText(numericFormula, margin, y + 92f, paint)

            // Footer / brand strip
            paint.color = MUTED
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            val footer1 = "Generated by ${profile?.businessName ?: "HisabKitab Pro"} \u2022 hisabkitab.pro"
            canvas.drawText(footer1, margin, (height - 30).toFloat(), paint)
            canvas.drawText(
                "This is a system-generated document and does not require a signature.",
                margin,
                (height - 16).toFloat(),
                paint
            )
        } catch (e: Exception) {
            paint.color = Color.RED
            canvas.drawText("Slip render error: ${e::class.java.simpleName}", 50f, 50f, paint)
        }

        pdf.finishPage(page)

        val outDir = File(context.cacheDir, "salary_slips").apply { mkdirs() }
        val file = File(
            outDir,
            "SalarySlip_${staff.name.replace(Regex("[^A-Za-z0-9]"), "_")}_${result.periodStartMillis}.pdf"
        )
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        file
    }

    private fun drawHeader(
        canvas: Canvas,
        paint: Paint,
        profile: BusinessProfile?,
        width: Int,
        context: Context,
    ) {
        val headerPaint = Paint(paint).apply {
            color = ACCENT
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 90f, headerPaint)

        // Logo (file / URI)
        profile?.logoUrl?.let { ProfileBitmapLoader.loadBitmapForPdfEmbed(context, it) }?.let { raw ->
            val dst = RectF(30f, 18f, 30f + 56f, 18f + 56f)
            LogoRenderFit.drawWithinRect(canvas, raw, dst)
            if (!raw.isRecycled) raw.recycle()
        }

        paint.color = Color.WHITE
        paint.typeface = Typeface.create("serif", Typeface.BOLD)
        paint.textSize = 22f
        canvas.drawText(profile?.businessName?.uppercase() ?: "HISABKITAB PRO", 100f, 42f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        paint.color = Color.WHITE
        val sub = listOfNotNull(
            profile?.address?.takeIf { it.isNotBlank() },
            profile?.phone?.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
            profile?.email?.takeIf { it.isNotBlank() }
        ).joinToString(separator = "  \u2022  ")
        if (sub.isNotBlank()) canvas.drawText(sub, 100f, 62f, paint)
    }

    private fun drawKeyValueGrid(
        canvas: Canvas,
        paint: Paint,
        left: Float,
        right: Float,
        top: Float,
        rows: List<Pair<String, String>>
    ) {
        var y = top + ROW_HEIGHT - 6f
        rows.forEachIndexed { index, (k, v) ->
            if (index % 2 == 0) {
                val stripe = Paint(paint).apply {
                    color = Color.parseColor("#F6F6F8")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(left, y - ROW_HEIGHT + 6f, right, y + 6f, stripe)
            }
            paint.style = Paint.Style.FILL
            paint.color = MUTED
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 11f
            canvas.drawText(k, left + 10f, y, paint)

            paint.color = Color.BLACK
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            paint.textSize = 12f
            val valWidth = paint.measureText(v)
            canvas.drawText(v, right - valWidth - 10f, y, paint)

            y += ROW_HEIGHT
        }
    }

    private fun formatMoney(paise: Long, symbol: String): String {
        val sign = if (paise < 0) "-" else ""
        val abs = Math.abs(paise)
        val rupees = abs / 100
        val rem = abs % 100
        return "$sign$symbol ${"%,d".format(rupees)}.${"%02d".format(rem)}"
    }

    private fun formatDays(days: Double): String =
        if (days % 1.0 == 0.0) "%.0f".format(days) else "%.1f".format(days)

    private fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return "—"
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
    }

    companion object {
        private const val ROW_HEIGHT = 22f
        private val ACCENT = Color.parseColor("#1F2A44")
        private val MUTED = Color.parseColor("#5C6478")
    }
}
