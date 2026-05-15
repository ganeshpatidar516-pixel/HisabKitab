package com.ganesh.hisabkitabpro.ui.customers

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.ui.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    viewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onCustomerAdded: (String) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showContactsDisclosureDialog by remember { mutableStateOf(false) }

    // Launcher for picking a specific phone number (More reliable)
    val phonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            contactUri?.let { uri ->
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        
                        if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: ""
                        if (numberIndex >= 0) {
                            val rawNumber = cursor.getString(numberIndex) ?: ""
                            // Extract last 10 digits for a clean Indian mobile number
                            val digitsOnly = rawNumber.replace(Regex("[^0-9]"), "")
                            phone = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly
                        }
                    }
                }
            }
        }
    }

    // Permission Launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            phonePickerLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Permission Required to access Contacts", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ADD NEW CUSTOMER",
                        fontWeight = FontWeight.Black,
                        color = colorScheme.primary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name", color = colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = colorScheme.primary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sacred_add_customer_name"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    cursorColor = colorScheme.primary
                )
            )

            // Phone Field
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 10) phone = it },
                label = { Text("Phone Number", color = colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Phone, null, tint = colorScheme.primary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sacred_add_customer_phone"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    cursorColor = colorScheme.primary
                )
            )

            // Import Button
            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        phonePickerLauncher.launch(intent)
                    } else {
                        showContactsDisclosureDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContactPhone, null)
                Spacer(Modifier.width(12.dp))
                Text("IMPORT FROM CONTACTS", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.length >= 10) {
                        viewModel.addCustomer(name, phone)
                        onCustomerAdded(name)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("sacred_add_customer_save"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank() && phone.length >= 10
            ) {
                Text("SAVE CUSTOMER", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    if (showContactsDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showContactsDisclosureDialog = false },
            title = { Text(stringResource(R.string.permission_contacts_title)) },
            text = { Text(stringResource(R.string.permission_contacts_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showContactsDisclosureDialog = false
                        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showContactsDisclosureDialog = false }) { Text("Cancel") }
            }
        )
    }
}
