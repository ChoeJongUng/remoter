// MessageFetcher.kt
package com.heilang.remoter

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.core.content.ContextCompat

data class MessageLogEntry(
    val address: String,
    val body: String,
    val date: String
)
object MessageFetcher {
    // Function to fetch message logs
    fun fetchMessageLogs(context: Context, messageLogs: MutableState<List<MessageLogEntry>>) {
        val messageList = mutableListOf<MessageLogEntry>()

        // Check if permissions are granted to read SMS
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_SMS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("MessageFetcher", "Permission to read SMS not granted")
            return
        }

        // Uri for fetching SMS messages
        val smsUri = Telephony.Sms.CONTENT_URI

        val cursor: Cursor? = context.contentResolver.query(smsUri, null, null, null, null)

        cursor?.use {
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIndex)
                val body = cursor.getString(bodyIndex)
                val date = cursor.getString(dateIndex)

                messageList.add(MessageLogEntry(address, body, date))
            }

            messageLogs.value = messageList
        }
    }
}