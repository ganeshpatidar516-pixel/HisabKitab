package com.ganesh.hisabkitabpro.domain.businessidentity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class GeoHit(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

/**
 * Phase 6 lite: address geocoding + last-known device location. Coordinates stay in [BusinessProfile] only.
 */
object BusinessLocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        val candidates = mutableListOf<Location>()
        for (provider in providers) {
            if (!lm.isProviderEnabled(provider)) continue
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()?.let { candidates += it }
        }
        return candidates.maxByOrNull { it.time }
    }

    suspend fun geocodeAddress(context: Context, query: String, maxResults: Int = 5): List<GeoHit> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 3) return@withContext emptyList()
            if (!Geocoder.isPresent()) return@withContext emptyList()
            val geocoder = Geocoder(context, Locale.getDefault())
            runCatching {
                if (Build.VERSION.SDK_INT >= 33) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocationName(
                            q,
                            maxResults,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (cont.isActive) cont.resume(addresses.toHits())
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(emptyList())
                                }
                            },
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(q, maxResults)?.toHits().orEmpty()
                }
            }.getOrElse { emptyList() }
        }

    private fun List<Address>.toHits(): List<GeoHit> = mapNotNull { it.toHit() }

    private fun Address.toHit(): GeoHit? {
        if (!hasLatitude() || !hasLongitude()) return null
        val lat = latitude
        val lng = longitude
        if (!lat.isFinite() || !lng.isFinite()) return null
        val label = buildAddressLabel(this)
        return GeoHit(latitude = lat, longitude = lng, label = label)
    }

    private fun buildAddressLabel(a: Address): String {
        val lines = (0..a.maxAddressLineIndex).mapNotNull { i ->
            a.getAddressLine(i)?.trim()?.takeIf { it.isNotEmpty() }
        }
        if (lines.isNotEmpty()) return lines.joinToString(", ")
        val parts = listOfNotNull(
            a.featureName?.trim()?.takeIf { it.isNotEmpty() },
            a.subLocality?.trim()?.takeIf { it.isNotEmpty() },
            a.locality?.trim()?.takeIf { it.isNotEmpty() },
            a.adminArea?.trim()?.takeIf { it.isNotEmpty() },
            a.postalCode?.trim()?.takeIf { it.isNotEmpty() },
            a.countryName?.trim()?.takeIf { it.isNotEmpty() },
        ).distinct()
        if (parts.isNotEmpty()) return parts.joinToString(", ")
        return "${a.latitude},${a.longitude}"
    }
}
