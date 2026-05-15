package com.ganesh.hisabkitabpro.domain.payment

import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.model.Bill
import com.ganesh.hisabkitabpro.domain.model.Customer
import java.util.regex.Pattern
import kotlinx.coroutines.flow.first

/**
 * Parses bank/UPI SMS text and finds a best-effort pending [Bill] match.
 * Does not post ledger entries — caller surfaces a confirm step only.
 */
object TransactionMatcher {

    private val amountPattern = Pattern.compile(
        "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val lastFourPattern = Pattern.compile(
        "(?:a/c|acct|account|XX|xx|\\*+)[\\s-]*(\\d{4})\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val upiLastFour = Pattern.compile(
        "(\\d{4})\\s*(?:credited|received|debited)",
        Pattern.CASE_INSENSITIVE
    )

    data class ParsedBankSms(
        val amountPaise: Long,
        val accountLastFour: String?,
        val rawText: String,
        val bankName: String? = null,
        val sourcePackage: String? = null
    )

    fun parseSms(title: String, text: String): ParsedBankSms? {
        val combined = "$title $text".trim()
        val amountMatcher = amountPattern.matcher(combined)
        if (!amountMatcher.find()) return null
        val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val rupees = amountStr.toDoubleOrNull() ?: return null
        val paise = (rupees * 100.0).toLong()

        var last4: String? = null
        val m1 = lastFourPattern.matcher(combined)
        if (m1.find()) last4 = m1.group(1)
        if (last4 == null) {
            val m2 = upiLastFour.matcher(combined)
            if (m2.find()) last4 = m2.group(1)
        }
        return ParsedBankSms(amountPaise = paise, accountLastFour = last4, rawText = combined)
    }

    /**
     * Prefer exact amount match on pending bill; if [accountLastFour] is present, narrow by customer phone suffix.
     */
    suspend fun findPendingBillMatch(db: AppDatabase, parsed: ParsedBankSms): Bill? {
        val pending = db.billDao().getPendingBills()
        if (pending.isEmpty()) return null

        val exact = pending.firstOrNull { it.totalAmount == parsed.amountPaise }
        if (exact != null) return exact

        val suffix = parsed.accountLastFour ?: return null
        val customers: List<Customer> = db.customerDao().getAllCustomers().first()
        val candidateIds = customers
            .filter { c ->
                !c.isDeleted && c.phone.filter { ch -> ch.isDigit() }.endsWith(suffix)
            }
            .map { it.id }
            .toSet()
        if (candidateIds.isEmpty()) return null

        return pending.firstOrNull { it.customerId in candidateIds && it.totalAmount == parsed.amountPaise }
            ?: pending.firstOrNull { it.customerId in candidateIds }
    }
}
