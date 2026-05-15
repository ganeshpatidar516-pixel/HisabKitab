package com.ganesh.hisabkitabpro.domain.payment

import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToLong

/**
 * Local-only parser for bank/SMS app notifications.
 *
 * Compliance boundaries:
 * - Does not require or use READ_SMS.
 * - Processes only notifications from known banking packages or SMS apps.
 * - Requires credit/received keywords before extracting money.
 * - Returns only structured match hints; raw notification text is not persisted
 *   or sent anywhere.
 */
object BankNotificationParser {

    data class ParsedBankNotification(
        val amountPaise: Long,
        val bankName: String?,
        val accountLastFour: String?,
        val sourcePackage: String
    )

    private val amountPatterns = listOf(
        Pattern.compile(
            "(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            "([0-9][0-9,]*(?:\\.\\d{1,2})?)\\s*(?:rs\\.?|inr)",
            Pattern.CASE_INSENSITIVE
        )
    )

    private val creditKeywords = listOf(
        "credited",
        "credit",
        "received",
        "deposited",
        "payment received",
        "upi received",
        "money received"
    )

    private val debitKeywords = listOf(
        "debited",
        "debit",
        "spent",
        "withdrawn",
        "paid to",
        "sent to",
        "purchase"
    )

    private val lastFourPattern = Pattern.compile(
        "(?:a/c|acct|account|acc|xx|x{2,}|\\*{2,})[\\s.-]*(\\d{4})\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val bankNamePatterns = listOf(
        Pattern.compile("\\b(?:from|in|to)\\s+([A-Z][A-Za-z.& ]{2,30}?\\s+Bank)\\b"),
        Pattern.compile("\\b([A-Z][A-Za-z.& ]{2,30}?\\s+Bank)\\b")
    )

    private val smsPackages = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.miui.mms",
        "com.coloros.mms",
        "com.vivo.messaging"
    )

    private val bankPackages = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",
        "net.one97.paytm",
        "in.org.npci.upiapp",
        "com.csam.icici.bank.imobile",
        "com.icicibank.pockets",
        "com.sbi.SBIFreedomPlus",
        "com.sbi.lotusintouch",
        "com.axis.mobile",
        "com.snapwork.hdfc",
        "com.kotak811",
        "com.myairtelapp",
        "com.freecharge.android",
        "com.mobikwik_new",
        "com.amazon.mShop.android.shopping",
        "com.whatsapp",
        "com.whatsapp.w4b"
    )

    private val bankPackagePrefixes = listOf(
        "com.hdfc",
        "com.icici",
        "com.sbi",
        "com.axis",
        "com.kotak",
        "com.yesbank",
        "com.idfcfirstbank",
        "com.indusind",
        "com.federalbank",
        "com.unionbank",
        "com.canarabank",
        "com.bankofbaroda",
        "com.pnb",
        "com.bhim",
        "com.upi"
    )

    // Precomputed lowercased allowlists — `isAllowedFinancialSource` is hit on
    // every posted notification system-wide, so we keep the hot path branch-free.
    private val smsPackagesLower: Set<String> = smsPackages
        .map { it.lowercase(Locale.ROOT) }
        .toSet()
    private val bankPackagesLower: Set<String> = bankPackages
        .map { it.lowercase(Locale.ROOT) }
        .toSet()
    private val bankPackagePrefixesLower: List<String> = bankPackagePrefixes
        .map { it.lowercase(Locale.ROOT) }

    fun parseNotification(
        packageName: String,
        title: String,
        text: String,
        bigText: String,
        subText: String,
        textLines: List<String> = emptyList()
    ): ParsedBankNotification? {
        val normalizedPackage = packageName.trim()
        if (!isAllowedFinancialSource(normalizedPackage)) return null

        val combined = buildString {
            append(title)
            append(' ')
            append(text)
            append(' ')
            append(bigText)
            append(' ')
            append(subText)
            textLines.forEach {
                append(' ')
                append(it)
            }
        }.replace(Regex("\\s+"), " ").trim()

        if (!looksLikeIncomingPayment(combined)) return null
        val amountPaise = extractAmountPaise(combined) ?: return null

        return ParsedBankNotification(
            amountPaise = amountPaise,
            bankName = extractBankName(combined) ?: bankNameFromPackage(normalizedPackage),
            accountLastFour = extractAccountLastFour(combined),
            sourcePackage = normalizedPackage
        )
    }

    fun toTransactionMatcherInput(
        parsed: ParsedBankNotification
    ): TransactionMatcher.ParsedBankSms {
        return TransactionMatcher.ParsedBankSms(
            amountPaise = parsed.amountPaise,
            accountLastFour = parsed.accountLastFour,
            rawText = "",
            bankName = parsed.bankName,
            sourcePackage = parsed.sourcePackage
        )
    }

    fun isAllowedFinancialSource(packageName: String): Boolean {
        if (packageName.isEmpty()) return false
        val normalized = packageName.lowercase(Locale.ROOT)
        return normalized in smsPackagesLower ||
            normalized in bankPackagesLower ||
            bankPackagePrefixesLower.any { normalized.startsWith(it) }
    }

    private fun looksLikeIncomingPayment(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val hasCreditSignal = creditKeywords.any { lower.contains(it) }
        if (!hasCreditSignal) return false

        val hasDebitSignal = debitKeywords.any { lower.contains(it) }
        return !hasDebitSignal
    }

    private fun extractAmountPaise(text: String): Long? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amount = matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: continue
                if (amount <= 0.0) continue
                return (amount * 100.0).roundToLong()
            }
        }
        return null
    }

    private fun extractAccountLastFour(text: String): String? {
        val matcher = lastFourPattern.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractBankName(text: String): String? {
        for (pattern in bankNamePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)
                    ?.trim()
                    ?.replace(Regex("\\s+"), " ")
                    ?.take(40)
            }
        }
        return null
    }

    private fun bankNameFromPackage(packageName: String): String? {
        val lower = packageName.lowercase(Locale.ROOT)
        return when {
            "hdfc" in lower -> "HDFC Bank"
            "icici" in lower -> "ICICI Bank"
            "sbi" in lower -> "State Bank of India"
            "axis" in lower -> "Axis Bank"
            "kotak" in lower -> "Kotak Bank"
            "yesbank" in lower -> "YES Bank"
            "idfc" in lower -> "IDFC FIRST Bank"
            "paytm" in lower -> "Paytm"
            "phonepe" in lower -> "PhonePe"
            "paisa" in lower -> "Google Pay"
            "bhim" in lower || "npci" in lower -> "BHIM UPI"
            "whatsapp" in lower -> "WhatsApp Payments"
            else -> null
        }
    }
}
