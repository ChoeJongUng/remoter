package com.heilang.remoter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.heilang.remoter.ui.theme.RemoterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permission
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // Permission granted
                Log.d("LocationTracking", "Location permission granted.")
                setContent {
                    RemoterTheme {
                        LocationTrackingHandler()
                    }
                }
            } else {
                // Permission denied
                Log.d("LocationTracking", "Location permission denied.")
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show()
            }
        }

        // Request permissions on launch
        requestPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
}

@Composable
fun LocationTrackingHandler() {
    val context = LocalContext.current
    val locationText = remember { mutableStateOf("Fetching location...") }
    val callLogs = remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    LaunchedEffect(Unit) {
        // Ensure permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LocationTracking", "Location permission granted.")
            try {
                // Start fetching the location and repeat after each completion
                fetchLocationOnceAndRepeat(context, locationText)
            } catch (e: SecurityException) {
                locationText.value = "Permission denied"
            }
        } else {
            Log.d("LocationTracking", "Location permission denied.")
            locationText.value = "Permission denied"
        }
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Start periodic fetching of call logs and location every 10 seconds
            val runnable = object : Runnable {
                override fun run() {
                    // Fetch call logs
                    CallLogFetcher.fetchCallLogs(context, callLogs)
                    // Schedule next fetch after 10 seconds
                    handler.postDelayed(this, 10000)
                }
            }
            // Start the first fetch
            handler.post(runnable)
        } else {
            Toast.makeText(context, "Permissions are denied.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Display location
            Text(
                text = "Location: ${locationText.value}",
                modifier = Modifier.padding(8.dp)
            )

            // LazyColumn to display all call logs
            LazyColumn {
                items(callLogs.value) { callLog ->
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "Number: ${callLog.number}")
                        Text(text = "Type: ${callLog.type}")
                        Text(text = "Date: ${callLog.date}")
                    }
                }
            }
        }
    }
}

// Function to fetch the location once and repeat after the previous fetch has completed
private fun fetchLocationOnceAndRepeat(
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

@Preview(showBackground = true)
@Composable
fun LocationTrackingHandlerPreview() {
    RemoterTheme {
        Text("Preview content here")
    }
}
