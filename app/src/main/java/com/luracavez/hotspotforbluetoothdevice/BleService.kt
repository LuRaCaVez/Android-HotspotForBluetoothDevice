package com.luracavez.hotspotforbluetoothdevice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BLEService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "ProximityServiceChannel"
        
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_TOGGLE_ON = "ACTION_TOGGLE_ON"
        const val ACTION_TOGGLE_OFF = "ACTION_TOGGLE_OFF"
        const val ACTION_DEVICE_IN_RANGE = "ACTION_DEVICE_IN_RANGE"
        const val ACTION_DEVICE_OUT_OF_RANGE = "ACTION_DEVICE_OUT_OF_RANGE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceWithNotification()
    }

    private fun startForegroundServiceWithNotification() {
        val stopIntent = Intent(this, BLEService::class.java).apply { action = ACTION_STOP_SERVICE }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val onIntent = Intent(this, BLEService::class.java).apply { action = ACTION_TOGGLE_ON }
        val onPendingIntent = PendingIntent.getService(this, 1, onIntent, PendingIntent.FLAG_IMMUTABLE)

        val offIntent = Intent(this, BLEService::class.java).apply { action = ACTION_TOGGLE_OFF }
        val offPendingIntent = PendingIntent.getService(this, 2, offIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_add, getString(R.string.action_on), onPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, getString(R.string.action_off), offPendingIntent)
            .build()

        // Android 14+ requires FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Proximity Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("BLEService", "Service started with action: $action")

        when (action) {
            ACTION_DEVICE_IN_RANGE, ACTION_TOGGLE_ON -> {
                Log.d("BLEService", "Enabling hotspot.")
                HotspotManager.toggleHotspot(this, true)
            }
            ACTION_DEVICE_OUT_OF_RANGE, ACTION_TOGGLE_OFF -> {
                Log.d("BLEService", "Disabling hotspot.")
                HotspotManager.toggleHotspot(this, false)
            }
            ACTION_STOP_SERVICE -> {
                Log.d("BLEService", "Stopping service.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}