// LocationFetcher.kt
package com.heilang.remoter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.core.content.ContextCompat
import android.util.Log

// Function to fetch the location once and repeat after the previous fetch has completed
fun fetchLocationOnceAndRepeat(
    context: Context,
    locationText: MutableState<String>
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Check if GPS is enabled
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    Log.d("LocationTracking", "GPS Enabled: $isGpsEnabled")

    if (!isGpsEnabled) {
        Log.d("LocationTracking", "GPS is not enabled.")
        locationText.value = "GPS is not enabled."
        return
    }

    // Check if permissions are granted using context
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        // If permission is not granted, return early
        locationText.value = "Permission denied"
        return
    }

    // Request location update once using GPS provider
    locationManager.requestSingleUpdate(
        LocationManager.GPS_PROVIDER,
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Once location is updated, stop listening
                locationManager.removeUpdates(this)

                // Log and update location text with the new location
                locationText.value = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                Log.d("LocationTracking", "Location updated: Latitude: ${location.latitude}, Longitude: ${location.longitude}")

                // Call the function again once the previous fetch is complete
                fetchLocationOnceAndRepeat(context, locationText)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        },
        null // No need for Looper here
    )
}
