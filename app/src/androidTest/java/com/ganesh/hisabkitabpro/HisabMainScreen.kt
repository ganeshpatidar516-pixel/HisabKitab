package com.ganesh.hisabkitabpro

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

/**
 * Kakao Compose screen map (same DSL stack Kaspresso wraps for Compose).
 * Tags are stable `testTag` hooks on production UI (no behavior change).
 */
class HisabMainScreen(
    semanticsProvider: SemanticsNodeInteractionsProvider
) : ComposeScreen<HisabMainScreen>(semanticsProvider) {

    val guideDone: KNode = child { hasTestTag("sacred_guide_done") }
    val navCustomers: KNode = child { hasTestTag("bottom_nav_customers") }
    val navSettings: KNode = child { hasTestTag("bottom_nav_settings") }
    val fabAddCustomer: KNode = child { hasTestTag("sacred_fab_add_customer") }
    val addCustomerName: KNode = child { hasTestTag("sacred_add_customer_name") }
    val addCustomerPhone: KNode = child { hasTestTag("sacred_add_customer_phone") }
    val saveCustomer: KNode = child { hasTestTag("sacred_add_customer_save") }
    val settingsInventory: KNode = child { hasTestTag("sacred_settings_inventory") }
    val inventoryRoot: KNode = child { hasTestTag("sacred_inventory_root") }
}
