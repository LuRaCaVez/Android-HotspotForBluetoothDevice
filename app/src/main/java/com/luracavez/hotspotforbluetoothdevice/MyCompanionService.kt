package com.luracavez.hotspotforbluetoothdevice

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Collections

class MyCompanionService : CompanionDeviceService() {

    companion object {
        private const val TAG = "MyCompanionService"
        // Thread-safe set to track which device IDs are currently in range
        private val activeDeviceIds = Collections.synchronizedSet(mutableSetOf<String>())
    }

    /**
     * Modern callback for Android 14 (API 34) and higher.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        super.onDevicePresenceEvent(event)
        val id = event.associationId.toString()
        
        when (event.event) {
            DevicePresenceEvent.EVENT_BLE_APPEARED, 
            DevicePresenceEvent.EVENT_BT_CONNECTED,
            DevicePresenceEvent.EVENT_SELF_MANAGED_APPEARED -> {
                Log.d(TAG, "Device in range: $id")
                activeDeviceIds.add(id)
            }
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED, 
            DevicePresenceEvent.EVENT_BT_DISCONNECTED,
            DevicePresenceEvent.EVENT_SELF_MANAGED_DISAPPEARED -> {
                Log.d(TAG, "Device out of range: $id")
                activeDeviceIds.remove(id)
            }
        }
        updateHotspotState()
    }

    /**
     * Legacy callback for Android 13 (API 33).
     */
    @Deprecated("Deprecated in Java")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            super.onDeviceAppeared(associationInfo)
            val id = associationInfo.id.toString()
            Log.d(TAG, "Device in range (Legacy): $id")
            activeDeviceIds.add(id)
            updateHotspotState()
        }
    }

    /**
     * Legacy callback for Android 13 (API 33).
     */
    @Deprecated("Deprecated in Java")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            super.onDeviceDisappeared(associationInfo)
            val id = associationInfo.id.toString()
            Log.d(TAG, "Device out of range (Legacy): $id")
            activeDeviceIds.remove(id)
            updateHotspotState()
        }
    }

    private fun updateHotspotState() {
        // Toggle Hotspot ON if at least one device is present, OFF if none.
        val shouldBeEnabled = activeDeviceIds.isNotEmpty()
        Log.d(TAG, "Active devices: ${activeDeviceIds.size}. Hotspot should be enabled: $shouldBeEnabled")
        HotspotManager.toggleHotspot(this, shouldBeEnabled)
    }
}