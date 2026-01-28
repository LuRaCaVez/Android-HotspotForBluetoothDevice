package com.luracavez.hotspotforbluetoothdevice

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

private const val LOG_TAG = "HotspotManager"

object HotspotManager {

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

            Log.d(LOG_TAG, "Stop signal sent successfully!")

        } catch (e: Exception) {
            Log.e(LOG_TAG, "Stop failed: ${e.message}")
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
                Log.d(LOG_TAG, "Method called: ${method.name}")
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

            Log.d(LOG_TAG, "Success! Check your status bar.")

        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed: ${e.message}")
        }
    }

    fun toggleHotspot(context: Context, enabled: Boolean) {
        val wifiManager = context.getSystemService(WifiManager::class.java)

        try {
            val methods = wifiManager.javaClass.declaredMethods

            val getWifiApState = methods.find { it.name == "getWifiApState" }
            var isAlreadyEnabled = !enabled
            if (getWifiApState != null) {
                val currentState = getWifiApState.invoke(wifiManager) as Int
                // State: 11 = Off, 13 = On
                isAlreadyEnabled = currentState == 13
                Log.d(LOG_TAG, "WiFiAp state received")
            } else {
                Log.e(LOG_TAG, "Method getWifiApState not found")
            }

            if (isAlreadyEnabled != enabled) {
                if (enabled) {
                    startTetheringReflection(context)
                } else {
                    stopTetheringReflection(context)
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error: ${e.message}")
        }
    }
}