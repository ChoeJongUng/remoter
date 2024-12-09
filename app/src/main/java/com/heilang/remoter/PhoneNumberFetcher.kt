package com.heilang.remoter

import android.content.Context
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat

object PhoneNumberFetcher {

    fun fetchPhoneNumbers(context: Context): List<String> {
        val phoneNumbers = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

            if (!subscriptionInfoList.isNullOrEmpty()) {
                Log.d("phonenumber","$subscriptionInfoList")
                subscriptionInfoList.forEach { subscriptionInfo ->
                    val displayName = subscriptionInfo.displayName ?: "Unknown SIM"
                    val phoneNumber = subscriptionInfo.number.takeIf { it.isNotEmpty() } ?: "Phone number not available"
                    phoneNumbers.add("$displayName: $phoneNumber")
                }
            } else {
                phoneNumbers.add("No active SIM cards found")
            }
        } else {
            phoneNumbers.add("Permission denied")
        }

        return phoneNumbers
    }
}
