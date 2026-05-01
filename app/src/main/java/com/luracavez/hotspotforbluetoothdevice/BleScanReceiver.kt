package com.luracavez.hotspotforbluetoothdevice

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val LOG_TAG = "BleScanReceiver"

class BleScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val results = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, ScanResult::class.java)

        Log.d(LOG_TAG, "Received %d BLE messages".format(results?.count()))

        val errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1)
        if (errorCode != -1) {
            Log.e(LOG_TAG, "Error Code: $errorCode")
        }

        results?.let { BleEvents.emit(it) }
    }
}