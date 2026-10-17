package bizora.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class UserCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Calculates distance in meters between two geographical coordinates using Haversine formula.
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    /**
     * Safely queries the device's current location via LocationManager.
     * Checks GPS and Network providers and falls back to last known location.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): UserCoordinates? {
        if (!hasLocationPermission(context)) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // 1. Try last known location first for immediate responsiveness
        var bestLocation: Location? = null
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        // If last known location is fresh (within last 5 minutes), use it immediately
        if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 5 * 60 * 1000) {
            return UserCoordinates(
                latitude = bestLocation.latitude,
                longitude = bestLocation.longitude,
                accuracy = bestLocation.accuracy
            )
        }

        // 2. Request a single fresh location update
        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(
                            UserCoordinates(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy
                            )
                        )
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }

                if (provider != null) {
                    locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } else if (bestLocation != null) {
                    continuation.resume(
                        UserCoordinates(
                            latitude = bestLocation.latitude,
                            longitude = bestLocation.longitude,
                            accuracy = bestLocation.accuracy
                        )
                    )
                    return@suspendCancellableCoroutine
                } else {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    if (bestLocation != null) {
                        continuation.resume(
                            UserCoordinates(
                                latitude = bestLocation.latitude,
                                longitude = bestLocation.longitude,
                                accuracy = bestLocation.accuracy
                            )
                        )
                    } else {
                        continuation.resume(null)
                    }
                }
            }

            continuation.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun getAddressFromCoordinates(context: Context, latitude: Double, longitude: Double): String? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    address.getAddressLine(0) ?: listOfNotNull(
                        address.featureName,
                        address.thoroughfare,
                        address.subLocality,
                        address.locality,
                        address.adminArea
                    ).joinToString(", ")
                } else null
            } catch (e: Exception) {
                null
            }
        }

    suspend fun getCoordinatesFromAddress(context: Context, addressString: String): UserCoordinates? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocationName(addressString, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    UserCoordinates(
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
}
