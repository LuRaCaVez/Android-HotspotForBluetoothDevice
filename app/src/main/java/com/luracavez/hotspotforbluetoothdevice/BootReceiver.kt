package com.luracavez.hotspotforbluetoothdevice

import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, restoring associations...")
            val manager = context.getSystemService(CompanionDeviceManager::class.java) ?: return
            
            // Restore presence observation for all existing associations
            for (association in manager.myAssociations) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        val observeRequest = ObservingDevicePresenceRequest.Builder()
                            .setAssociationId(association.id)
                            .build()
                        manager.startObservingDevicePresence(observeRequest)
                    } else {
                        val address = association.deviceMacAddress
                        if (address != null) {
                            @Suppress("DEPRECATION")
                            manager.startObservingDevicePresence(address.toString())
                        }
                    }
                    Log.d("BootReceiver", "Started observing: ${association.displayName}")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to observe ${association.displayName}", e)
                }
            }
        }
    }
}