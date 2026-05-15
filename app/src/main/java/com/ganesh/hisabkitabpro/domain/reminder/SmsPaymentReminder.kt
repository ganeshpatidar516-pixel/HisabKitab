package com.ganesh.hisabkitabpro.domain.reminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import java.text.NumberFormat
import java.util.Locale

/**
 * Opens the default SMS app with a pre-filled OkCredit-style payment reminder (text only).
 * No WhatsApp — standard [smsto:] + [sms_body] flow for maximum device compatibility.
 */
object SmsPaymentReminder {

    fun openLedgerReminder(
        context: Context,
        customerName: String,
        rawPhone: String,
        netDuePaise: Long,
        businessName: String?,
        upiId: String?,
        currencyFormatter: NumberFormat
    ): Boolean {
        val lc = AppLocaleManager.wrapContext(context)
        val digitsOnly = rawPhone.filter { it.isDigit() }
        val to = when {
            digitsOnly.length >= 10 -> digitsOnly.takeLast(10)
            digitsOnly.isNotEmpty() -> digitsOnly
            else -> {
                Toast.makeText(context, ReminderLocalization.phoneUnavailableText(context), Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (netDuePaise <= 0L) {
            Toast.makeText(
                context,
                lc.getString(R.string.reminder_sms_no_due),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        val amount = currencyFormatter.format(netDuePaise / 100.0)
        val biz = businessName?.trim()?.ifBlank { null } ?: "HisabKitab Pro"
        val payLink = buildUpiPayLink(upiId, netDuePaise, customerName, biz)

        val body = if (payLink != null) {
            lc.getString(R.string.reminder_sms_body_with_pay, biz, customerName, amount, payLink)
        } else {
            lc.getString(R.string.reminder_sms_body_no_pay, biz, customerName, amount)
        }

        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$to")
                putExtra("sms_body", body)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                lc.getString(R.string.reminder_sms_app_missing),
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    /**
     * SMS to supplier: we acknowledge payable (amount we owe them). No UPI collect link.
     */
    fun openSupplierPayableReminder(
        context: Context,
        supplierName: String,
        rawPhone: String,
        payablePaise: Long,
        businessName: String?,
        currencyFormatter: NumberFormat
    ): Boolean {
        val lc = AppLocaleManager.wrapContext(context)
        val digitsOnly = rawPhone.filter { it.isDigit() }
        val to = when {
            digitsOnly.length >= 10 -> digitsOnly.takeLast(10)
            digitsOnly.isNotEmpty() -> digitsOnly
            else -> {
                Toast.makeText(context, ReminderLocalization.phoneUnavailableText(context), Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (payablePaise <= 0L) {
            Toast.makeText(
                context,
                lc.getString(R.string.reminder_sms_no_due),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        val amount = currencyFormatter.format(payablePaise / 100.0)
        val biz = businessName?.trim()?.ifBlank { null } ?: "HisabKitab Pro"
        val body = lc.getString(R.string.reminder_sms_supplier_payable, biz, supplierName, amount)
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$to")
                putExtra("sms_body", body)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                lc.getString(R.string.reminder_sms_app_missing),
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun buildUpiPayLink(
        upiId: String?,
        amountPaise: Long,
        customerName: String,
        payeeName: String
    ): String? {
        if (upiId.isNullOrBlank()) return null
        val amountText = String.format(Locale.US, "%.2f", amountPaise / 100.0)
        val note = Uri.encode("Payment from $customerName")
        return "upi://pay?pa=${Uri.encode(upiId)}&pn=${Uri.encode(payeeName)}&am=$amountText&cu=INR&tn=$note"
    }
}
