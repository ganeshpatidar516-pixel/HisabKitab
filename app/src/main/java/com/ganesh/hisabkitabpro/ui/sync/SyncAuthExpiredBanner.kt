package com.ganesh.hisabkitabpro.ui.sync

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.domain.sync.SyncHealthMonitor

/**
 * Non-blocking banner when cloud sync paused due to expired Firebase session.
 * Local ledger remains fully usable.
 */
@Composable
fun SyncAuthExpiredBanner(
    onOpenCloudSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val health by SyncHealthMonitor.state.collectAsStateWithLifecycle()
    if (health.workerPauseReason != SyncHealthMonitor.WorkerPauseReason.AUTH_EXPIRED) return

    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sync_auth_expired_banner_message),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onOpenCloudSettings) {
                Text(stringResource(R.string.sync_auth_expired_banner_action))
            }
        }
    }
}
