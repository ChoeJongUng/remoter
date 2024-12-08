package com.heilang.remoter

import android.annotation.SuppressLint
import android.content.Context
import android.provider.CallLog
import androidx.compose.runtime.MutableState

data class CallLogEntry(val number: String, val type: String, val date: String)

object CallLogFetcher {

    // Function to fetch all call logs and update the callLogs state
    @SuppressLint("Range")
    fun fetchCallLogs(context: Context, callLogsState: MutableState<List<CallLogEntry>>) {
        val resolver = context.contentResolver

        // Query for the call log
        val uri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )

        val sortOrder = CallLog.Calls.DATE + " DESC" // Sort by most recent

        val cursor = resolver.query(uri, projection, null, null, sortOrder)

        val newCallLogs = mutableListOf<CallLogEntry>()

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val callNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                val callType = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE))
                val callDate = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))

                // Format the call type (incoming, outgoing, missed)
                val callTypeStr = when (callType) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    else -> "Unknown"
                }

                // Format the date
                val formattedDate = java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(java.util.Date(callDate))

                // Add the call log to the list
                newCallLogs.add(CallLogEntry(callNumber, callTypeStr, formattedDate))
            }
        }

        cursor?.close()

        // Update the state with all the call logs
        callLogsState.value = newCallLogs
    }
}
