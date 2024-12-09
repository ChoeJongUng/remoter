package com.heilang.remoter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object SmsUtils {

    private const val SMS_PERMISSION_REQUEST_CODE = 123

    // Function to send SMS
    fun sendSms(context: Context, phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(context, "SMS sent successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Request permission at runtime
            requestSmsPermission(context)
        }
    }

    // Function to request SMS permission
    private fun requestSmsPermission(context: Context) {
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Handle permission result
    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }
    // Function to delete SMS based on the phone number
    fun deleteSms(context: Context, phoneNumber: String) {
        try {
            val uri = Uri.parse("content://sms")
            val contentResolver = context.contentResolver

            // Query for SMS with the given phone number
            val cursor = contentResolver.query(
                uri,
                arrayOf("_id"),
                "address = ?",
                arrayOf(phoneNumber),
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getString(it.getColumnIndexOrThrow("_id"))

                    // Delete the SMS using the ID
                    val rowsDeleted = contentResolver.delete(
                        Uri.withAppendedPath(uri, id),
                        null,
                        null
                    )
                    if (rowsDeleted > 0) {
                        Log.d("SmsUtils", "Deleted SMS with ID: $id")
                    }
                }
            }

            Toast.makeText(context, "Deleted SMS logs for $phoneNumber.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SmsUtils", "Failed to delete SMS: ${e.message}", e)
            Toast.makeText(context, "Failed to delete SMS logs: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
