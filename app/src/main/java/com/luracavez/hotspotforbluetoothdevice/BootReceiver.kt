package com.luracavez.hotspotforbluetoothdevice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val LOG_TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(LOG_TAG, "Action boot completed received")

            val serviceIntent = Intent(context, BleService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}