package com.luracavez.hotspotforbluetoothdevice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat

private const val LOG_TAG = "BtService"
private const val DEBOUNCER_SECONDS = 4000L

class BtService : Service() {

    private var isRunning = false

    private val debounceHandler = Handler(Looper.getMainLooper())

    private var isConnected = false

    private val disconnectedRunnable = Runnable {
        if (isConnected) {
            isConnected = false
            onDeviceDisconnected()
        }
    }

    private val connectedRunnable = Runnable {
        if (!isConnected) {
            isConnected = true
            onDeviceConnected()
        }
    }

    private fun getTargetMAC(): String {
        val sharedPrefs = getSharedPreferences(SHARED_NAME, MODE_PRIVATE)
        return sharedPrefs.getString(MAC_SHARED_KEY, "") ?: ""
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            checkDeviceAndDoAction(context, action, device)
        }
    }

    private fun onDeviceConnected() {
        Log.d(LOG_TAG, "Device connected")
        updateNotification(CONNECTED_MESSAGE)
        HotspotManager.toggleHotspot(this, true)
    }

    private fun onDeviceDisconnected() {
        Log.d(LOG_TAG, "Device disconnected")
        updateNotification(DISCONNECTED_MESSAGE)
        HotspotManager.toggleHotspot(this, false)
    }

    private fun checkDeviceAndDoAction(context: Context, action: String?, device: BluetoothDevice?) {
        if (device?.address?.equals(getTargetMAC(), ignoreCase = true) == true) {
            if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                debounceHandler.removeCallbacks(disconnectedRunnable)
                if (!isConnected) {
                    if (!debounceHandler.hasCallbacks(connectedRunnable)) {
                        debounceHandler.postDelayed(connectedRunnable, DEBOUNCER_SECONDS)
                    }
                }
            } else if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                debounceHandler.removeCallbacks(connectedRunnable)
                if (isConnected) {
                    if (!debounceHandler.hasCallbacks(disconnectedRunnable)) {
                        debounceHandler.postDelayed(disconnectedRunnable, DEBOUNCER_SECONDS)
                    }
                }
            }
        }
    }

    private fun createNotification(statusText: String) : Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE_BT)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!isRunning) {
            isRunning = true
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            registerReceiver(bluetoothReceiver, filter)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(bluetoothReceiver)
        super.onDestroy()
    }
}