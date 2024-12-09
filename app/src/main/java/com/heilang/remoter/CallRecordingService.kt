package com.heilang.remoter

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException

class CallRecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var telephonyCallback: MyTelephonyCallback

    override fun onCreate() {
        super.onCreate()
        Log.d("CallRecorder", "Service started")

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = MyTelephonyCallback()
            telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback)
        } else {
            Log.e("CallRecorder", "TelephonyCallback requires API 31 or higher. Falling back to PhoneStateListener if needed.")
        }

        // Start the service in the foreground
        createNotificationChannel()
        startForeground(1, getNotification())
    }

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.S)
    private inner class MyTelephonyCallback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (!isRecording) {
                        Log.d("CallRecorder", "Call started, recording...")
                        startRecording()
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (isRecording) {
                        Log.d("CallRecorder", "Call ended, stopping recording...")
                        stopRecording()
                    }
                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.d("CallRecorder", "Phone is ringing")
                }
            }
        }
    }

    private fun startRecording() {
        try {
            val fileName = "CallRecording_${System.currentTimeMillis()}.3gp"
            val outputFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION // Set the mode to communication
            audioManager.isSpeakerphoneOn = true
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
                isRecording = true
            }
            Log.d("CallRecorder", "Recording started: ${outputFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("CallRecorder", "Error starting recording: ${e.message}", e)
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Log.d("CallRecorder", "Recording stopped")
        } catch (e: Exception) {
            Log.e("CallRecorder", "Error stopping recording: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "call_recording_channel",
                "Call Recording Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("NewApi")
    private fun getNotification(): Notification {
        return Notification.Builder(this, "call_recording_channel")
            .setContentTitle("Call Recording Active")
            .setContentText("Recording calls in the background.")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecording()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback)
        }
        super.onDestroy()
    }
}
