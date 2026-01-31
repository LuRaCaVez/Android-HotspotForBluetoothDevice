package com.luracavez.hotspotforbluetoothdevice

import android.content.Context
import android.content.Intent

object ServiceManager {
    fun startService(context: Context) {
        val serviceIntent = Intent(context, BleService::class.java)
        context.startForegroundService(serviceIntent)
    }
}