package com.luracavez.hotspotforbluetoothdevice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class BleService : Service() {
    private var bleManager: BleManager? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val notificationManager: NotificationManager? by lazy {
        getSystemService(NotificationManager::class.java)
    }

    private fun getTargetUUID(): String {
        val sharedPrefs = getSharedPreferences(SHARED_NAME, MODE_PRIVATE)
        return sharedPrefs.getString(UUID_SHARED_KEY, "") ?: ""
    }

    private fun getTargetMAC(): String {
        val sharedPrefs = getSharedPreferences(SHARED_NAME, MODE_PRIVATE)
        return sharedPrefs.getString(MAC_SHARED_KEY, "") ?: ""
    }

    private fun createNotification(statusText: String) : Notification {
        val stopIntent = Intent(this, BleService::class.java).apply {
            action = ACTION_STOP_FOREGROUND_SERVICE
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE_BLE)
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, NOTIFICATION_STOP_BLE, stopPendingIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        notificationManager?.notify(NOTIFICATION_ID, createNotification(statusText))
    }

    override fun onCreate() {
        super.onCreate()

        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager?.createNotificationChannel(serviceChannel)

        startForeground(NOTIFICATION_ID, createNotification(DISCONNECTED_MESSAGE), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP_FOREGROUND_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (bleManager == null) {
            bleManager = BleManager(
                applicationContext,
                getTargetUUID(),
                getTargetMAC(),
                serviceScope
            ) { status -> updateNotification(status) }
        } else {
            bleManager?.stopScanning()
        }

        bleManager?.startScanning()

        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()

        bleManager?.stopScanning()
        serviceScope.cancel()
        bleManager = null
    }
}