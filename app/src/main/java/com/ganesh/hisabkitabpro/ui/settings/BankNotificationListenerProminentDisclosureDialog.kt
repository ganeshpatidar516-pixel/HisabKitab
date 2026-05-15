package com.ganesh.hisabkitabpro.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ganesh.hisabkitabpro.R

/**
 * Play policy–aligned **prominent in-app disclosure** before the user enables
 * Bank payment match hints and is sent to Android’s Notification listener settings.
 *
 * Must stay in normal app UI (not only Privacy Policy). Checkbox + explicit
 * acknowledgment reduces mistaken consent.
 */
@Composable
fun BankNotificationListenerProminentDisclosureDialog(
    dialogVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirmContinue: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    if (!dialogVisible) return

    var acknowledged by remember { mutableStateOf(false) }
    LaunchedEffect(dialogVisible) {
        if (dialogVisible) acknowledged = false
    }
    val scroll = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_title),
                        style = typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll),
                ) {
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_intro),
                        style = typography.bodyMedium,
                        color = colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_section_access),
                        style = typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_access_body),
                        style = typography.bodyMedium,
                        color = colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_section_limits),
                        style = typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_limits_body),
                        style = typography.bodyMedium,
                        color = colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_section_control),
                        style = typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_control_body),
                        style = typography.bodyMedium,
                        color = colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenPrivacyPolicy()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.disclosure_bank_listener_privacy_link))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = acknowledged,
                        onCheckedChange = { acknowledged = it },
                    )
                    Text(
                        text = stringResource(R.string.disclosure_bank_listener_checkbox),
                        style = typography.bodyMedium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onConfirmContinue()
                        },
                        enabled = acknowledged,
                    ) {
                        Text(stringResource(R.string.disclosure_bank_listener_continue))
                    }
                }
            }
        }
    }
}
