package com.luracavez.hotspotforbluetoothdevice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_PACKAGE_REPLACED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            Log.d("BootReceiver", "Boot completed. Starting service.")

            val serviceIntent = Intent(context, BLEService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}