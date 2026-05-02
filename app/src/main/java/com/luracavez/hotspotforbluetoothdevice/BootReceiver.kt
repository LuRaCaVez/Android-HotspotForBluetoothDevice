package com.luracavez.hotspotforbluetoothdevice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

private const val LOG_TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "android.intent.action.REBOOT") {

            Log.d(LOG_TAG, "Action boot received: $action")

            val workRequest = OneTimeWorkRequestBuilder<BleWorkWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}