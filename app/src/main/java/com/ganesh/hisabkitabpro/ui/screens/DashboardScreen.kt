@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.domain.support.AppShareActions
import androidx.compose.ui.platform.LocalContext
import com.ganesh.hisabkitabpro.ui.ai_pilot.SuperAIButton
import com.ganesh.hisabkitabpro.ui.auth.ProfileAvatar
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel

@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    onViewTransactionsClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onSuppliersClick: () -> Unit,
    onInvoiceClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onAddEntryClick: () -> Unit,
    onScanBillClick: () -> Unit,
    onBusinessInsightsClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    onShareAppClick: (() -> Unit)? = null
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    val mainGradient = remember(colorScheme.background, colorScheme.surfaceVariant) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.background,
                colorScheme.surfaceVariant
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(mainGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.dashboard_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onBackground
                    )
                    Text(
                        stringResource(R.string.dashboard_subtitle),
                        fontSize = 12.sp,
                        color = colorScheme.onBackground.copy(alpha = 0.72f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onShareAppClick?.invoke() ?: AppShareActions.shareApp(context) }) {
                        Icon(Icons.Default.Share, null, tint = colorScheme.onBackground)
                    }
                    IconButton(onClick = onHelpClick) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = colorScheme.onBackground)
                    }
                    // Theme-aware Authentication Gateway. Inherits colors from
                    // MaterialTheme.colorScheme so it adapts to every theme the
                    // user can pick in Settings (no hardcoded colors here).
                    ProfileAvatar()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DashboardSummaryLayer(dashboardState)

            Spacer(modifier = Modifier.height(12.dp))

            SuperAIButton(
                onClick = onAiAssistantClick,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionItem(stringResource(R.string.dashboard_action_add), Icons.Default.Add, colorScheme.primary, Modifier.weight(1f), onAddEntryClick)
                QuickActionItem(stringResource(R.string.dashboard_action_bill), Icons.AutoMirrored.Filled.Assignment, colorScheme.secondary, Modifier.weight(1f), onInvoiceClick)
                QuickActionItem(stringResource(R.string.dashboard_action_scan), Icons.Default.Camera, colorScheme.tertiary, Modifier.weight(1f), onScanBillClick)
                QuickActionItem(stringResource(R.string.dashboard_action_voice), Icons.Default.Mic, colorScheme.error, Modifier.weight(1f), onAiAssistantClick)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(stringResource(R.string.nav_customers), dashboardState.totalCustomers.toString(), Icons.Default.People, Modifier.weight(1f), onCustomersClick)
                StatCard(stringResource(R.string.nav_suppliers), dashboardState.totalSuppliers.toString(), Icons.Default.Inventory, Modifier.weight(1f), onSuppliersClick)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun QuickActionItem(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.height(100.dp).shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.height(90.dp).shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.92f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            }
        }
    }
}
