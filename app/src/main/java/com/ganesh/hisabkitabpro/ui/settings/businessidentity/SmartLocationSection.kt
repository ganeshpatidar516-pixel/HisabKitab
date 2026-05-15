package com.ganesh.hisabkitabpro.ui.settings.businessidentity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganesh.hisabkitabpro.domain.businessidentity.BusinessLocationHelper
import com.ganesh.hisabkitabpro.domain.businessidentity.GeoHit
import com.ganesh.hisabkitabpro.domain.profile.LiveLocationEngine
import kotlinx.coroutines.launch

/**
 * Location: compact summary, maps shortcuts, address search in an expandable panel.
 * Same persistence path as legacy lat/lng + lock.
 */
@Composable
fun SmartLocationSection(
    latitudeText: String,
    longitudeText: String,
    mapLink: String,
    locationLockedAt: Long,
    onLatitudeTextChange: (String) -> Unit,
    onLongitudeTextChange: (String) -> Unit,
    onApplyCoordinates: (Double, Double) -> Unit,
    onLockFromFields: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val applyCoords = rememberUpdatedState(onApplyCoordinates)
    var addressQuery by remember { mutableStateOf("") }
    var geoHits by remember { mutableStateOf<List<GeoHit>>(emptyList()) }
    var geoBusy by remember { mutableStateOf(false) }
    var showManualCoords by remember { mutableStateOf(false) }
    var toolsExpanded by remember { mutableStateOf(false) }

    val lat = latitudeText.toDoubleOrNull()
    val lng = longitudeText.toDoubleOrNull()

    fun applyDeviceLocation() {
        val loc = BusinessLocationHelper.getBestLastKnownLocation(context)
        if (loc != null) {
            applyCoords.value(loc.latitude, loc.longitude)
            Toast.makeText(context, "Location applied.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "No recent GPS fix. Enable location or search by address.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            applyDeviceLocation()
        } else {
            Toast.makeText(context, "Location permission is needed for current position.", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    val locked = LiveLocationEngine.isLocked(
        latitude = lat,
        longitude = lng,
        lockedAt = locationLockedAt,
    )

    fun openExternalMaps() {
        val uri = when {
            lat != null && lng != null ->
                Uri.parse("geo:$lat,$lng?q=$lat,$lng")
            mapLink.startsWith("http") -> Uri.parse(mapLink)
            else -> null
        }
        if (uri == null) {
            Toast.makeText(context, "Set a location first.", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure {
            Toast.makeText(context, "Could not open Maps.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Used for map links on bills and cards. Saved with Business Profile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = {
                if (!BusinessLocationHelper.hasLocationPermission(context)) {
                    requestLocationPermission()
                } else {
                    applyDeviceLocation()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Use current location")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { openExternalMaps() },
                enabled = (lat != null && lng != null) || mapLink.startsWith("http"),
                modifier = Modifier.weight(1f),
            ) {
                Text("Open in Maps")
            }
            TextButton(onClick = { toolsExpanded = !toolsExpanded }) {
                Text(if (toolsExpanded) "Less" else "Refine")
            }
        }

        Text(
            text = if (locked) "Locked" else "Not locked",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(
            visible = toolsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = addressQuery,
                        onValueChange = { addressQuery = it },
                        label = { Text("Search address") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                geoBusy = true
                                geoHits = BusinessLocationHelper.geocodeAddress(context, addressQuery, maxResults = 5)
                                geoBusy = false
                                if (geoHits.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "No matches. Add city or PIN (needs network).",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        enabled = !geoBusy && addressQuery.trim().length >= 3,
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }

                if (geoHits.isNotEmpty()) {
                    Text("Results", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        geoHits.forEach { hit ->
                            ListItem(
                                headlineContent = { Text(hit.label, style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier.clickable {
                                    applyCoords.value(hit.latitude, hit.longitude)
                                    geoHits = emptyList()
                                    addressQuery = ""
                                    Toast.makeText(context, "Location applied.", Toast.LENGTH_SHORT).show()
                                },
                            )
                        }
                    }
                }

                TextButton(onClick = { showManualCoords = !showManualCoords }) {
                    Text(if (showManualCoords) "Hide coordinates" else "Coordinates (advanced)")
                }

                if (showManualCoords) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = latitudeText,
                            onValueChange = onLatitudeTextChange,
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = longitudeText,
                            onValueChange = onLongitudeTextChange,
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(
                        onClick = onLockFromFields,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Lock from coordinates")
                    }
                }

                if (mapLink.isNotBlank()) {
                    Text(
                        mapLink,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
