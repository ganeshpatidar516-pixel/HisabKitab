package com.ganesh.hisabkitabpro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ganesh.hisabkitabpro.R

sealed class NavScreen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : NavScreen("dashboard", R.string.nav_home, Icons.Default.Home)
    object Customers : NavScreen("customers", R.string.nav_customers, Icons.Default.People)
    object Suppliers : NavScreen("suppliers", R.string.nav_suppliers, Icons.Default.Inventory)
    object AI : NavScreen("ai_assistant", R.string.nav_ai_assistant, Icons.Default.AutoAwesome)
    object Settings : NavScreen("settings", R.string.nav_settings, Icons.Default.Settings)
}

val bottomNavItems = listOf(
    NavScreen.Home,
    NavScreen.Customers,
    NavScreen.Suppliers,
    NavScreen.AI,
    NavScreen.Settings
)
