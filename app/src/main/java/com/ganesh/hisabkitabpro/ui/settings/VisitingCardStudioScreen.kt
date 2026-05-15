package com.ganesh.hisabkitabpro.ui.settings

import androidx.compose.runtime.Composable
import com.ganesh.hisabkitabpro.ui.businesscard.BusinessCardStudioScreen
import com.ganesh.hisabkitabpro.ui.viewmodel.SettingsViewModel

/**
 * Legacy entry point — kept for backwards compatibility with existing navigation routes
 * and the Settings tile callback. The implementation now delegates to the new
 * procedural [BusinessCardStudioScreen]. The legacy form-based UI, hardcoded template
 * enums and SharedPreferences preset blob have been intentionally removed.
 *
 * The historical [SettingsViewModel] dependency is retained in the signature so
 * existing call-sites do not have to be updated, but it is no longer consumed here —
 * the engine reads the canonical [com.ganesh.hisabkitabpro.domain.model.BusinessProfile]
 * directly through the new [com.ganesh.hisabkitabpro.domain.businesscard.repository.BusinessCardRepository].
 */
@Composable
fun VisitingCardStudioScreen(
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: SettingsViewModel,
    onEditBusinessProfile: () -> Unit = onNavigateBack,
) {
    BusinessCardStudioScreen(
        onNavigateBack = onNavigateBack,
        onEditBusinessProfile = onEditBusinessProfile,
    )
}
