package com.luracavez.hotspotforbluetoothdevice

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class MyCompanionService : CompanionDeviceService() {

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        super.onDevicePresenceEvent(event)
        val eventType = event.event
        Log.d("CDM", "Presence event: $eventType for association: ${event.associationId}")
        
        when (eventType) {
            DevicePresenceEvent.EVENT_BLE_APPEARED, 
            DevicePresenceEvent.EVENT_BT_CONNECTED,
            DevicePresenceEvent.EVENT_SELF_MANAGED_APPEARED -> {
                handleDeviceIn()
            }
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED, 
            DevicePresenceEvent.EVENT_BT_DISCONNECTED,
            DevicePresenceEvent.EVENT_SELF_MANAGED_DISAPPEARED -> {
                handleDeviceOut()
            }
        }
    }

    /**
     * Legacy callback.
     */
    @Deprecated("Deprecated in Java")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            @Suppress("DEPRECATION")
            super.onDeviceAppeared(associationInfo)
            Log.d("CDM", "Device appeared (Legacy): ${associationInfo.deviceMacAddress}")
            handleDeviceIn()
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
            Log.d("CDM", "Device disappeared (Legacy): ${associationInfo.deviceMacAddress}")
            handleDeviceOut()
        }
    }

    private fun handleDeviceIn() {
        val intent = Intent(this, BLEService::class.java).apply {
            action = BLEService.ACTION_DEVICE_IN_RANGE
        }
        startForegroundService(intent)
    }

    private fun handleDeviceOut() {
        val intent = Intent(this, BLEService::class.java).apply {
            action = BLEService.ACTION_DEVICE_OUT_OF_RANGE
        }
        startForegroundService(intent)
    }
}