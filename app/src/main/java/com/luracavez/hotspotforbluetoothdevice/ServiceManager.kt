package com.luracavez.hotspotforbluetoothdevice

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent

object ServiceManager {

    fun startService(context: Context) {
        val sharedPrefs = context.getSharedPreferences(SHARED_NAME, MODE_PRIVATE)
        val mode = sharedPrefs.getString(MODE_SHARED_KEY, "")
        var serviceIntent : Intent
        if (mode == "BT") {
            serviceIntent = Intent(context, BtService::class.java)
        } else {
            serviceIntent = Intent(context, BleService::class.java)
        }
        context.startForegroundService(serviceIntent)
    }

}