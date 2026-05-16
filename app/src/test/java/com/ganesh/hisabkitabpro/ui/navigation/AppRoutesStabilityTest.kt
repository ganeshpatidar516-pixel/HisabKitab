package com.ganesh.hisabkitabpro.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase-10 — route builders must stay stable for deep links and instrumented tests. */
class AppRoutesStabilityTest {

    @Test
    fun customerLedger_routeFormat() {
        assertEquals("customer_ledger/42", AppRoutes.customerLedger(42L))
    }

    @Test
    fun scanBill_defaultPrefill() {
        assertEquals("scan_bill/0", AppRoutes.scanBill(0L))
    }

    @Test
    fun bottomNav_matchesNavScreen() {
        assertEquals(AppRoutes.Dashboard, NavScreen.Home.route)
        assertEquals(AppRoutes.Customers, NavScreen.Customers.route)
        assertEquals(AppRoutes.Settings, NavScreen.Settings.route)
    }

    @Test
    fun fullBill_includesEncodedSegments() {
        val route = AppRoutes.fullBill(1L, "Test%20User", "CREDIT")
        assertTrue(route.startsWith("full_bill/1/"))
        assertTrue(route.endsWith("/CREDIT"))
    }
}
