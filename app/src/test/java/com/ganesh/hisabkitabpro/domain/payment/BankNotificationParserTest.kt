package com.ganesh.hisabkitabpro.domain.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankNotificationParserTest {

    @Test
    fun parsesIncomingBankCreditNotification() {
        val parsed = BankNotificationParser.parseNotification(
            packageName = "com.snapwork.hdfc",
            title = "HDFC Bank",
            text = "Rs. 1,250.50 credited to A/c XX1234",
            bigText = "",
            subText = ""
        )

        assertNotNull(parsed)
        assertEquals(125050L, parsed!!.amountPaise)
        assertEquals("1234", parsed.accountLastFour)
        assertEquals("HDFC Bank", parsed.bankName)
    }

    @Test
    fun parsesIncomingSmsAppPaymentAlertWithoutReadSms() {
        val parsed = BankNotificationParser.parseNotification(
            packageName = "com.google.android.apps.messaging",
            title = "VK-ICICIB",
            text = "",
            bigText = "INR 500 received in ICICI Bank account XX7777 via UPI.",
            subText = ""
        )

        assertNotNull(parsed)
        assertEquals(50000L, parsed!!.amountPaise)
        assertEquals("7777", parsed.accountLastFour)
        assertEquals("ICICI Bank", parsed.bankName)
    }

    @Test
    fun rejectsDebitNotifications() {
        val parsed = BankNotificationParser.parseNotification(
            packageName = "com.axis.mobile",
            title = "Axis Bank",
            text = "Rs 900 debited from A/c XX1111 for purchase",
            bigText = "",
            subText = ""
        )

        assertNull(parsed)
    }

    @Test
    fun rejectsUnrelatedPackagesEvenIfTextLooksFinancial() {
        val parsed = BankNotificationParser.parseNotification(
            packageName = "com.social.random",
            title = "Sale",
            text = "Rs 1000 credited today",
            bigText = "",
            subText = ""
        )

        assertNull(parsed)
    }

    @Test
    fun isAllowedFinancialSourceAllowsSmsAndBankPackages() {
        assertTrue(BankNotificationParser.isAllowedFinancialSource("com.google.android.apps.messaging"))
        assertTrue(BankNotificationParser.isAllowedFinancialSource("com.snapwork.hdfc"))
        assertTrue(BankNotificationParser.isAllowedFinancialSource("com.PHONEPE.app"))
    }

    @Test
    fun isAllowedFinancialSourceRejectsRandomAndEmptyPackages() {
        assertFalse(BankNotificationParser.isAllowedFinancialSource(""))
        assertFalse(BankNotificationParser.isAllowedFinancialSource("com.social.random"))
        assertFalse(BankNotificationParser.isAllowedFinancialSource("org.mozilla.firefox"))
    }

    @Test
    fun rejectsCreditTextThatAlsoLooksLikeDebitOrPurchase() {
        // Realistic mixed-language wallet notification — must not be misread as credit.
        val parsed = BankNotificationParser.parseNotification(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "You spent Rs 250 — purchase debited from your linked account",
            bigText = "",
            subText = ""
        )
        assertNull(parsed)
    }

    @Test
    fun handlesAmountSuffixForm() {
        // Some banks format as "1,500.00 INR credited" rather than "INR 1,500.00 credited".
        val parsed = BankNotificationParser.parseNotification(
            packageName = "com.snapwork.hdfc",
            title = "HDFC Bank",
            text = "1,500.00 INR credited to your A/c XX2222",
            bigText = "",
            subText = ""
        )
        assertNotNull(parsed)
        assertEquals(150000L, parsed!!.amountPaise)
        assertEquals("2222", parsed.accountLastFour)
    }

    @Test
    fun rejectsZeroAndMissingAmount() {
        val zero = BankNotificationParser.parseNotification(
            packageName = "com.snapwork.hdfc",
            title = "HDFC Bank",
            text = "Rs 0 credited — promotional",
            bigText = "",
            subText = ""
        )
        assertNull(zero)

        val noAmount = BankNotificationParser.parseNotification(
            packageName = "com.snapwork.hdfc",
            title = "HDFC Bank",
            text = "Funds credited successfully",
            bigText = "",
            subText = ""
        )
        assertNull(noAmount)
    }
}
