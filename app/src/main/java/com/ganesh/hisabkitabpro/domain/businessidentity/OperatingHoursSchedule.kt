package com.ganesh.hisabkitabpro.domain.businessidentity

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Structured weekly hours stored in [com.ganesh.hisabkitabpro.domain.model.BusinessProfile.operatingHours]
 * as JSON (v=1). Any other string is treated as legacy free text.
 */
data class WeeklyHoursV1(
    @SerializedName("v")
    val version: Int = 1,
    @SerializedName("weekly")
    val weekly: List<DaySlot> = emptyList(),
)

data class DaySlot(
    @SerializedName("d")
    val day: String,
    @SerializedName("closed")
    val closed: Boolean = false,
    @SerializedName("from")
    val from: String = "",
    @SerializedName("to")
    val to: String = "",
)

object OperatingHoursCodec {

    private val gson = Gson()
    val dayOrder: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    private val dayLabel = mapOf(
        "MON" to "Mon",
        "TUE" to "Tue",
        "WED" to "Wed",
        "THU" to "Thu",
        "FRI" to "Fri",
        "SAT" to "Sat",
        "SUN" to "Sun",
    )

    private val TIME_PATTERN = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun isStructuredJson(raw: String): Boolean {
        val s = raw.trim()
        if (!s.startsWith("{")) return false
        return runCatching {
            val w = gson.fromJson(s, WeeklyHoursV1::class.java)
            w != null && w.version == 1 && w.weekly.isNotEmpty()
        }.getOrDefault(false)
    }

    fun parse(raw: String): WeeklyHoursV1? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        val w = runCatching { gson.fromJson(s, WeeklyHoursV1::class.java) }
            .getOrNull()
            ?.takeIf { it.version == 1 && it.weekly.isNotEmpty() }
            ?: return null
        return mergeFullWeek(w)
    }

    private fun mergeFullWeek(parsed: WeeklyHoursV1): WeeklyHoursV1 {
        val by = parsed.weekly.associateBy { it.day.uppercase() }
        return WeeklyHoursV1(
            version = 1,
            weekly = dayOrder.map { d ->
                val s = by[d]
                if (s == null) {
                    DaySlot(day = d, closed = true, from = "", to = "")
                } else {
                    DaySlot(day = d, closed = s.closed, from = s.from, to = s.to)
                }
            },
        )
    }

    fun toJson(schedule: WeeklyHoursV1): String = gson.toJson(schedule.copy(version = 1))

    fun defaultWeekly(): WeeklyHoursV1 = WeeklyHoursV1(
        version = 1,
        weekly = dayOrder.map { d ->
            DaySlot(
                day = d,
                closed = d == "SUN",
                from = if (d == "SUN") "" else "09:00",
                to = if (d == "SUN") "" else "21:00",
            )
        },
    )

    /** For cards, vCard, and PDF-style surfaces: readable line(s) from stored value. */
    fun formatForDisplay(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""
        val parsed = parse(s) ?: return s
        return formatWeeklyHuman(parsed)
    }

    private data class WeekSegment(
        val days: MutableList<String>,
        val closed: Boolean,
        val from: String,
        val to: String,
    )

    private fun buildWeeklySegments(schedule: WeeklyHoursV1): List<WeekSegment> {
        val byDay = schedule.weekly.associateBy { it.day }
        val slots = dayOrder.map { d -> d to (byDay[d] ?: DaySlot(day = d, closed = true, from = "", to = "")) }
        val segments = mutableListOf<WeekSegment>()
        for ((code, slot) in slots) {
            val closed = slot.closed || slot.from.isBlank() || slot.to.isBlank()
            val from = slot.from.trim()
            val to = slot.to.trim()
            val last = segments.lastOrNull()
            val canMerge = last != null && last.closed == closed &&
                (closed || (last.from == from && last.to == to))
            if (canMerge) {
                segments.last().days.add(code)
            } else {
                segments += WeekSegment(mutableListOf(code), closed, from, to)
            }
        }
        return segments
    }

    fun formatWeeklyHuman(schedule: WeeklyHoursV1): String {
        return buildWeeklySegments(schedule).joinToString("; ") { seg ->
            val rangeLabel = when {
                seg.days.size == 1 -> dayLabel[seg.days.first()] ?: seg.days.first()
                else -> "${dayLabel[seg.days.first()]}–${dayLabel[seg.days.last()]}"
            }
            when {
                seg.closed -> "$rangeLabel closed"
                else -> "$rangeLabel ${seg.from}–${seg.to}"
            }
        }
    }

    /** One-line friendly preview (12-hour clock) for compact UI, e.g. Mon–Sat · 9 AM – 9 PM · Sun closed. */
    fun formatWeeklyAmPmSummary(schedule: WeeklyHoursV1): String {
        return buildWeeklySegments(schedule).joinToString(" · ") { seg ->
            val rangeLabel = when {
                seg.days.size == 1 -> dayLabel[seg.days.first()] ?: seg.days.first()
                else -> "${dayLabel[seg.days.first()]}–${dayLabel[seg.days.last()]}"
            }
            when {
                seg.closed -> "$rangeLabel closed"
                else -> "$rangeLabel ${formatHhmmAmPm(seg.from)}–${formatHhmmAmPm(seg.to)}"
            }
        }
    }

    private fun formatHhmmAmPm(hhmm: String): String {
        val t = hhmm.trim()
        if (!TIME_PATTERN.matches(t)) return t
        val parts = t.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val suffix = when {
            h == 0 || h < 12 -> "AM"
            else -> "PM"
        }
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return if (m == 0) "$h12 $suffix" else "$h12:${"%02d".format(m)} $suffix"
    }

    /** Loose HH:mm validation */
    fun isValidTimeSlot(from: String, to: String): Boolean {
        if (!TIME_PATTERN.matches(from.trim()) || !TIME_PATTERN.matches(to.trim())) return false
        val a = from.trim().split(":").map { it.toInt() }
        val b = to.trim().split(":").map { it.toInt() }
        val start = a[0] * 60 + a[1]
        val end = b[0] * 60 + b[1]
        return end > start
    }
}
