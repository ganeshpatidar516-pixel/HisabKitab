package com.ganesh.hisabkitabpro.ui.party

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ganesh.hisabkitabpro.ui.customers.CustomerListScreen
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierListScreen
import com.ganesh.hisabkitabpro.ui.viewmodel.CustomerViewModel
import com.ganesh.hisabkitabpro.ui.viewmodel.PartyViewModel
import kotlinx.coroutines.launch

/**
 * HISABKITAB PRO - UNIFIED PARTY SCREEN
 * The "Sovereign" UI that bridges Customers and Suppliers seamlessly.
 * Features a professional TabLayout + ViewPager layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedPartyScreen(
    partyViewModel: PartyViewModel,
    customerViewModel: CustomerViewModel,
    onCustomerClick: (Long) -> Unit,
    onSupplierClick: (Long) -> Unit,
    onAddCustomerClick: () -> Unit,
    onAddSupplierClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Column {
        // 🏰 THE ULTIMATE NAVIGATION HUB
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.White,
            contentColor = Color(0xFF1A237E), // Navy Blue
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = if (pagerState.currentPage == 0) Color(0xFF1A237E) else Color(0xFF800000)
                    )
                }
            },
            divider = {}
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = { 
                    Text(
                        text = "CUSTOMERS", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (pagerState.currentPage == 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    ) 
                }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { 
                    partyViewModel.setSupplierTab(true)
                    coroutineScope.launch { pagerState.animateScrollToPage(1) } 
                },
                text = { 
                    Text(
                        text = "SUPPLIERS", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (pagerState.currentPage == 1) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    ) 
                }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = true
        ) { page ->
            if (page == 0) {
                // 🛡️ Tab 1: Original "White Card" Customer Page (Untouched)
                CustomerListScreen(
                    viewModel = customerViewModel,
                    onCustomerClick = onCustomerClick,
                    onAddCustomerClick = onAddCustomerClick
                )
            } else {
                // 🏆 Tab 2: New Sovereign Supplier Module
                SupplierListScreen(
                    viewModel = partyViewModel,
                    onSupplierClick = onSupplierClick,
                    onAddSupplierClick = onAddSupplierClick
                )
            }
        }
    }
}
