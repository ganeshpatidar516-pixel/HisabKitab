package com.ganesh.hisabkitabpro.ui.screens

import androidx.compose.runtime.Composable
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel

// HomeScreen.kt redirected to DashboardScreen to avoid confusion and conflicting overloads.
//
// LEGACY (not reachable from the live nav graph). Kept under the
// "Preserve Working Systems" directive — R8 strips it from the release AAB
// while it has no callers. If you ever wire it back, audit `MainActivity`
// to ensure it doesn't shadow the live `dashboard` route.
@Deprecated(
    message = "Legacy passthrough wrapper. Use DashboardScreen directly. Not in the live navigation graph.",
    replaceWith = ReplaceWith("DashboardScreen(viewModel, onViewTransactionsClick, onCustomersClick, onSuppliersClick, onInvoiceClick, onAiAssistantClick, onAddEntryClick, onScanBillClick, onBusinessInsightsClick, onHelpClick)"),
    level = DeprecationLevel.WARNING
)
@Composable
fun HomeScreen(
    viewModel: TransactionViewModel,
    onViewTransactionsClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onSuppliersClick: () -> Unit,
    onInvoiceClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onAddEntryClick: () -> Unit,
    onScanBillClick: () -> Unit,
    onBusinessInsightsClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    DashboardScreen(
        viewModel = viewModel,
        onViewTransactionsClick = onViewTransactionsClick,
        onCustomersClick = onCustomersClick,
        onSuppliersClick = onSuppliersClick,
        onInvoiceClick = onInvoiceClick,
        onAiAssistantClick = onAiAssistantClick,
        onAddEntryClick = onAddEntryClick,
        onScanBillClick = onScanBillClick,
        onBusinessInsightsClick = onBusinessInsightsClick,
        onHelpClick = onHelpClick
    )
}
