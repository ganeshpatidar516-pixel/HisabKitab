package com.ganesh.hisabkitabpro.domain.payment

import java.io.IOException
import java.util.Locale

/**
 * Parses a simple bank/UPI CSV export into normalized rows for reconciliation.
 * Supports both single-amount columns and separate credit/debit columns.
 */
object BankStatementCsvParser {

    data class StatementRow(
        val postedAtRaw: String,
        val description: String,
        val amountPaise: Long,
        val reference: String?,
        val rowNumber: Int
    )

    fun parse(csv: String): Result<List<StatementRow>> = runCatching {
        val lines = csv
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.size < 2) {
            throw IOException("Statement CSV must have header + at least one row")
        }

        val header = parseCsvLine(lines.first())
        val dateIdx = header.indexOfFirstHeader("date", "txn date", "transaction date", "posted date")
        val descIdx = header.indexOfFirstHeader("description", "narration", "remarks", "particulars")
        val amountIdx = header.indexOfFirstHeader("amount", "txn amount")
        val creditIdx = header.indexOfFirstHeader("credit", "deposit")
        val debitIdx = header.indexOfFirstHeader("debit", "withdrawal")
        val refIdx = header.indexOfFirstHeader("reference", "utr", "txn id", "transaction id", "rrn")

        if (dateIdx < 0 || descIdx < 0 || (amountIdx < 0 && creditIdx < 0 && debitIdx < 0)) {
            throw IOException("CSV header missing required columns (date/description/amount)")
        }

        val rows = mutableListOf<StatementRow>()
        for ((offset, line) in lines.drop(1).withIndex()) {
            val rowNumber = offset + 2
            val cols = parseCsvLine(line)
            val postedAtRaw = cols.getOrNull(dateIdx).orEmpty().trim()
            val description = cols.getOrNull(descIdx).orEmpty().trim()
            if (postedAtRaw.isBlank() || description.isBlank()) continue

            val paise = when {
                amountIdx >= 0 -> parseAmountPaise(cols.getOrNull(amountIdx).orEmpty())
                else -> {
                    val credit = if (creditIdx >= 0) parseAmountPaise(cols.getOrNull(creditIdx).orEmpty()) else 0L
                    val debit = if (debitIdx >= 0) parseAmountPaise(cols.getOrNull(debitIdx).orEmpty()) else 0L
                    credit - debit
                }
            }
            if (paise == 0L) continue

            val reference = cols.getOrNull(refIdx)?.trim()?.ifBlank { null }
            rows += StatementRow(
                postedAtRaw = postedAtRaw,
                description = description,
                amountPaise = paise,
                reference = reference,
                rowNumber = rowNumber
            )
        }

        if (rows.isEmpty()) {
            throw IOException("No usable rows found in statement CSV")
        }
        rows
    }

    private fun parseAmountPaise(raw: String): Long {
        val cleaned = raw
            .replace(",", "")
            .replace("₹", "")
            .replace("INR", "", ignoreCase = true)
            .trim()
        if (cleaned.isBlank()) return 0L
        val negative = cleaned.startsWith("-")
        val numeric = cleaned.removePrefix("-")
        val rupees = numeric.toDoubleOrNull() ?: return 0L
        val paise = (rupees * 100.0).toLong()
        return if (negative) -paise else paise
    }

    private fun List<String>.indexOfFirstHeader(vararg accepted: String): Int {
        val acceptedSet = accepted.map { it.trim().lowercase(Locale.getDefault()) }.toSet()
        return indexOfFirst { col ->
            val value = col.trim().lowercase(Locale.getDefault())
            value in acceptedSet
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        line.forEach { ch ->
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) current.append(ch) else {
                        result += current.toString()
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        result += current.toString()
        return result
    }
}
