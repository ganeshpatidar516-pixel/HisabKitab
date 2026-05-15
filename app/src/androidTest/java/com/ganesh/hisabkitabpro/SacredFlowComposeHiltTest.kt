package com.ganesh.hisabkitabpro

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end sacred smoke on device: Customers → Add customer → list shows name.
 * Second scenario: Settings → Inventory (product hub).
 *
 * Uses **Kakao Compose** (`io.github.kakaocup:compose`) — the same node DSL Kaspresso’s
 * Compose support delegates to; avoids pinning an older `androidx.compose.ui:ui-test` from Kaspresso.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SacredFlowComposeHiltTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("app_lock_enabled", false)
            .apply()
    }

    @Test
    fun sacred_createCustomer_visibleInList() {
        composeRule.waitForIdle()
        runCatching {
            onComposeScreen<HisabMainScreen>(composeRule) {
                guideDone {
                    assertIsDisplayed()
                    performClick()
                }
            }
        }
        composeRule.waitForIdle()

        onComposeScreen<HisabMainScreen>(composeRule) {
            navCustomers {
                assertIsDisplayed()
                performClick()
            }
        }
        composeRule.waitForIdle()

        onComposeScreen<HisabMainScreen>(composeRule) {
            fabAddCustomer {
                assertIsDisplayed()
                performClick()
            }
        }
        composeRule.waitForIdle()

        val name = "UiSacred_${System.currentTimeMillis()}"
        val phone = System.currentTimeMillis().toString().takeLast(10).padStart(10, '9')

        onComposeScreen<HisabMainScreen>(composeRule) {
            addCustomerName {
                assertIsDisplayed()
                performClick()
                performTextInput(name)
            }
            addCustomerPhone {
                assertIsDisplayed()
                performClick()
                performTextInput(phone)
            }
            saveCustomer {
                assertIsDisplayed()
                performClick()
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(name, substring = true).assertExists()
    }

    @Test
    fun sacred_openInventoryFromSettings() {
        composeRule.waitForIdle()
        runCatching {
            onComposeScreen<HisabMainScreen>(composeRule) {
                guideDone {
                    assertIsDisplayed()
                    performClick()
                }
            }
        }
        composeRule.waitForIdle()

        onComposeScreen<HisabMainScreen>(composeRule) {
            navSettings {
                assertIsDisplayed()
                performClick()
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("SETTINGS", substring = false).assertExists()

        onComposeScreen<HisabMainScreen>(composeRule) {
            settingsInventory {
                assertIsDisplayed()
                performClick()
            }
        }
        composeRule.waitForIdle()

        onComposeScreen<HisabMainScreen>(composeRule) {
            inventoryRoot {
                assertIsDisplayed()
            }
        }
    }
}
