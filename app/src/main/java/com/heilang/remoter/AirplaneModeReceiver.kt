package com.heilang.remoter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

class AirplaneModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            val isAirplaneModeEnabled = intent.getBooleanExtra("state", false)
            if (!isAirplaneModeEnabled) {
                val currentTime = System.currentTimeMillis()
                saveTimeToPreferences(context, currentTime)
                Log.d("AirplaneModeReceiver", "Airplane mode disabled at: $currentTime")
            }
        }
    }

    private fun saveTimeToPreferences(context: Context, time: Long) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("AirplaneModeDisabledTime", time).apply()
        Log.d("AirplaneModeReceiver", "Time saved to preferences: $time")
    }
}
