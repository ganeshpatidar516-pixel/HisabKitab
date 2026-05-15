package com.ganesh.hisabkitabpro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganesh.hisabkitabpro.ui.viewmodel.SettingsViewModel

/**
 * HISABKITAB PRO - SECURITY PIN SCREEN (ZERO-ANR PROTOCOL)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPinScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle(initialValue = false)
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    val canUseBiometric = remember { viewModel.canUseBiometricLock() }
    val hasSavedPin = !settings?.securityPinHash.isNullOrBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart App Lock", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Secure your app with PIN + Biometric",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Set a 4-digit PIN and optionally enable fingerprint/face unlock.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable App Lock", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (canUseBiometric) "Lock app with PIN and fingerprint/face"
                            else "Lock app with PIN (biometric not available)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { enable ->
                            if (enable && !hasSavedPin) {
                                errorMessage = "Please save your 4-digit PIN first, then enable App Lock."
                            } else {
                                errorMessage = null
                                viewModel.setBiometricEnabled(enable)
                            }
                        },
                        enabled = true
                    )
                }
            }

            OutlinedTextField(
                value = pin,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() } && it.length <= 4) {
                        pin = it 
                        errorMessage = null
                    }
                },
                label = { Text("Enter PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = confirmPin,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() } && it.length <= 4) {
                        confirmPin = it 
                        errorMessage = null
                    }
                },
                label = { Text("Confirm PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (pin.length != 4) {
                        errorMessage = "PIN must be 4 digits"
                    } else if (pin != confirmPin) {
                        errorMessage = "PINs do not match"
                    } else {
                        viewModel.updatePin(pin)
                        errorMessage = null
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save PIN")
            }
        }
    }
}
