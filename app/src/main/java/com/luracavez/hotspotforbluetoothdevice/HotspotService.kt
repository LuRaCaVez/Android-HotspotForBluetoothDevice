package com.luracavez.hotspotforbluetoothdevice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat

class HotspotService : Service() {

    private val channelId = "HotspotServiceChannel"

    private fun getTargetMac(): String? {
        val sharedPrefs = getSharedPreferences("HotspotConfig", MODE_PRIVATE)
        return sharedPrefs.getString("target_mac", null)
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            checkDeviceAndDoAction(context, action, device)
        }
    }

    private fun checkDeviceAndDoAction(context: Context, action: String?, device: BluetoothDevice?) {
        val targetMac = getTargetMac()
        if (device?.address?.equals(targetMac, ignoreCase = true) == true) {
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> toggleHotspot(context, true)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> toggleHotspot(context, false)
            }
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelId,
            "Hotspot Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    fun stopTetheringReflection(context: Context) {
        try {
            // 1. Get the TetheringManager service
            val tetheringManager = context.getSystemService("tethering")

            // 2. Find the stopTethering method
            // In Android 11-13, the signature is usually stopTethering(int type)
            val stopMethod = tetheringManager.javaClass.getDeclaredMethod(
                "stopTethering",
                Int::class.javaPrimitiveType
            )

            stopMethod.isAccessible = true

            // 3. Invoke it with 0 (TETHERING_WIFI)
            stopMethod.invoke(tetheringManager, 0)

            Log.d("Hotspot", "Stop signal sent successfully!")

        } catch (e: Exception) {
            Log.e("Hotspot", "Stop failed: ${e.message}")
        }
    }

    fun startTetheringReflection(context: Context) {
        try {
            // 1. Get the actual TetheringManager service (it's hidden but accessible by name)
            val tetheringManager = context.getSystemService("tethering")

            // 2. Get the new callback interface (In Android 11+, this IS an interface)
            val callbackClass = Class.forName($$"android.net.TetheringManager$StartTetheringCallback")

            // 3. Create the Proxy (This will work because in TetheringManager, it's an interface)
            val proxyCallback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, _ ->
                Log.d("Hotspot", "Method called: ${method.name}")
                null
            }

            // 4. Use the TetheringRequest Builder (Hidden)
            val requestClass = Class.forName($$"android.net.TetheringManager$TetheringRequest")
            val builderClass = Class.forName($$"android.net.TetheringManager$TetheringRequest$Builder")
            val builderInstance = builderClass.getConstructor(Int::class.javaPrimitiveType).newInstance(0) // 0 = WIFI
            val requestInstance = builderClass.getDeclaredMethod("build").invoke(builderInstance)

            // 5. Invoke startTethering(request, executor, callback)
            val startMethod = tetheringManager.javaClass.getDeclaredMethod(
                "startTethering",
                requestClass,
                java.util.concurrent.Executor::class.java,
                callbackClass
            )

            startMethod.isAccessible = true
            startMethod.invoke(tetheringManager, requestInstance, context.mainExecutor, proxyCallback)

            Log.d("Hotspot", "Success! Check your status bar.")

        } catch (e: Exception) {
            Log.e("Hotspot", "Failed: ${e.message}")
        }
    }

    private fun toggleHotspot(context: Context, enabled: Boolean) {
        val wifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        try {
            val methods = wifiManager.javaClass.declaredMethods

            val getWifiApState = methods.find { it.name == "getWifiApState" }
            var isAlreadyEnabled = !enabled
            if (getWifiApState != null) {
                val currentState = getWifiApState.invoke(wifiManager) as Int
                // State: 11 = Off, 13 = On
                isAlreadyEnabled = currentState == 13
                Log.d("HotspotService", "WiFiAp state received")
            } else {
                Log.e("HotspotService", "Method getWifiApState not found")
            }

            if (isAlreadyEnabled != enabled) {
                if (enabled) {
                    startTetheringReflection(context)
                } else {
                    stopTetheringReflection(context)
                }
            }
        } catch (e: Exception) {
            Log.e("HotspotService", "Error: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hotspot Automation")
            .setContentText("Monitoring Bluetooth MAC")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            checkDeviceAndDoAction(this, action, device)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}