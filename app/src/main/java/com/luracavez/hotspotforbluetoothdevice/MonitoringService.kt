package com.luracavez.hotspotforbluetoothdevice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MonitoringService : Service() {

    companion object {
        private const val TAG = "MonitoringService"
        private const val CHANNEL_ID = "monitoring_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_MONITORING = "com.luracavez.hotspotforbluetoothdevice.STOP_MONITORING"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        showForegroundNotification()
        return START_STICKY
    }

    private fun showForegroundNotification() {
        val stopIntent = Intent(this, MonitoringService::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Stopping all device monitoring...")
        val manager = getSystemService(CompanionDeviceManager::class.java)
        if (manager != null) {
            for (association in manager.myAssociations) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        val observeRequest = ObservingDevicePresenceRequest.Builder()
                            .setAssociationId(association.id)
                            .build()
                        manager.stopObservingDevicePresence(observeRequest)
                    } else {
                        val address = association.deviceMacAddress
                        if (address != null) {
                            @Suppress("DEPRECATION")
                            manager.stopObservingDevicePresence(address.toString())
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop observing ${association.displayName}", e)
                }
            }
        }
        
        MyCompanionService.clearActiveDevices()
        HotspotManager.toggleHotspot(this, false)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Device Monitoring Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}