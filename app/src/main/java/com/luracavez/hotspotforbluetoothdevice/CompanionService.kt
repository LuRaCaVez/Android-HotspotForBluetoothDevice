package com.luracavez.hotspotforbluetoothdevice

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Collections

private const val LOG_TAG = "CompanionService"

class CompanionService : CompanionDeviceService() {

    companion object {
        private val activeDeviceIds = Collections.synchronizedSet(mutableSetOf<String>())

        var active = false
    }

    /**
     * Modern callback
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        super.onDevicePresenceEvent(event)
        val id = event.associationId.toString()
        
        when (event.event) {
            DevicePresenceEvent.EVENT_BLE_APPEARED, 
            DevicePresenceEvent.EVENT_BT_CONNECTED,
            DevicePresenceEvent.EVENT_SELF_MANAGED_APPEARED -> {
                Log.d(LOG_TAG, "Device entered range: $id")
                activeDeviceIds.add(id)
            }
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED, 
            DevicePresenceEvent.EVENT_BT_DISCONNECTED,
            DevicePresenceEvent.EVENT_SELF_MANAGED_DISAPPEARED -> {
                Log.d(LOG_TAG, "Device left range: $id")
                activeDeviceIds.remove(id)
            }
        }
        updateHotspotState()
    }

    /**
     * Legacy callback
     */
    @Deprecated("Deprecated in Java")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            @Suppress("DEPRECATION")
            super.onDeviceAppeared(associationInfo)
            val id = associationInfo.id.toString()
            Log.d(LOG_TAG, "Device entered range (Legacy): $id")
            activeDeviceIds.add(id)
            updateHotspotState()
        }
    }

    /**
     * Legacy callback
     */
    @Deprecated("Deprecated in Java")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            @Suppress("DEPRECATION")
            super.onDeviceDisappeared(associationInfo)
            val id = associationInfo.id.toString()
            Log.d(LOG_TAG, "Device left range (Legacy): $id")
            activeDeviceIds.remove(id)
            updateHotspotState()
        }
    }

    private fun updateHotspotState() {
        val shouldBeEnabled = activeDeviceIds.isNotEmpty()
        Log.d(LOG_TAG, "Active device count: ${activeDeviceIds.size}. Hotspot active: $shouldBeEnabled")
        if (active)
        {
            HotspotManager.toggleHotspot(this, shouldBeEnabled)
        }
    }
}