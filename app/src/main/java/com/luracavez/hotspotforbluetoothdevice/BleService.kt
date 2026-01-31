package com.luracavez.hotspotforbluetoothdevice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat

class BleService : Service() {
    private lateinit var bleManager: BleManager

    private var isRunning = false

    private fun getTargetUUID(): String {
        val sharedPrefs = getSharedPreferences(SHARED_NAME, MODE_PRIVATE)
        return sharedPrefs.getString(UUID_SHARED_KEY, "") ?: ""
    }

    private fun getTargetMAC(): String {
        val sharedPrefs = getSharedPreferences(SHARED_NAME, MODE_PRIVATE)
        return sharedPrefs.getString(MAC_SHARED_KEY, "") ?: ""
    }

    private fun createNotification(statusText: String) : Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE_BLE)
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(statusText))
    }

    override fun onCreate() {
        super.onCreate()

        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

        startForeground(NOTIFICATION_ID, createNotification(DISCONNECTED_MESSAGE), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (isRunning) {
            bleManager.stopScanning()
        }

        isRunning = true
        bleManager = BleManager(this, getTargetUUID(), getTargetMAC()) { status ->
            updateNotification(status)
        }
        Handler(Looper.getMainLooper()).post {
            bleManager.startScanning()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()

        isRunning = false
        bleManager.stopScanning()
    }
}