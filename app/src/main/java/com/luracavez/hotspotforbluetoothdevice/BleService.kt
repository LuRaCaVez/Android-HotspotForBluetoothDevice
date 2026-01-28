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

private const val CHANNEL_ID = "BleServiceChannel"
private const val NOTIFICATION_ID = 1

class BleService : Service() {
    private lateinit var bleManager: BleManager

    private var isRunning = false

    private fun getTargetUUID(): String? {
        val sharedPrefs = getSharedPreferences("HotspotConfig", MODE_PRIVATE)
        return sharedPrefs.getString("target_uuid", null)
    }

    private fun createNotification(statusText: String) : Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Monitor Active")
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
            "Ble Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

        startForeground(NOTIFICATION_ID, createNotification("Disconnected"), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val bleUUID = getTargetUUID()
        if (bleUUID != null && !isRunning) {
            isRunning = true
            bleManager = BleManager(this, bleUUID) { status ->
                updateNotification(status)
            }
            Handler(Looper.getMainLooper()).post {
                bleManager.startScanning()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()

        bleManager.stopScanning()
    }
}