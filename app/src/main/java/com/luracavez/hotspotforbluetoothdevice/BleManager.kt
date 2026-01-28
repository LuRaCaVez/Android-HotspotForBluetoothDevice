package com.luracavez.hotspotforbluetoothdevice

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission

private const val LOG_TAG = "BleManager"
private const val LOST_TIMEOUT = 10000L
private const val DEBOUNCER_SECONDS = 4000L

class BleManager(
    private val context: Context,
    private val targetUUID: String,
    private val onStatusChanged: (String) -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(BluetoothManager::class.java)
        manager.adapter
    }

    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    private var isNearConfirmed = false

    private val debounceHandler = Handler(Looper.getMainLooper())

    private fun onDeviceConnected() {
        Log.d(LOG_TAG, "Device connected")
        onStatusChanged("Connected")
        HotspotManager.toggleHotspot(context, true)
    }

    private fun onDeviceDisconnected() {
        Log.d(LOG_TAG, "Device disconnected")
        onStatusChanged("Disconnected")
        HotspotManager.toggleHotspot(context, false)
    }

    private val lostDeviceRunnable = Runnable {
        debounceHandler.removeCallbacks(confirmNearRunnable)
        if (isNearConfirmed) {
            isNearConfirmed = false
            onDeviceDisconnected()
        }
    }

    private val confirmFarRunnable = Runnable {
        if (isNearConfirmed) {
            isNearConfirmed = false
            onDeviceDisconnected()
        }
    }

    private val confirmNearRunnable = Runnable {
        if (!isNearConfirmed) {
            isNearConfirmed = true
            onDeviceConnected()
        }
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi

            Log.d(LOG_TAG, "Found ${device.address} with strength: $rssi dBm")

            if (device?.address?.equals("34:C4:59:86:2F:4E", ignoreCase = true) != true) {
                return
            }

            handler.removeCallbacks(lostDeviceRunnable)
            handler.postDelayed(lostDeviceRunnable, LOST_TIMEOUT)

            if (rssi > -70) {
                debounceHandler.removeCallbacks(confirmFarRunnable)
                if (!isNearConfirmed) {
                    if (!debounceHandler.hasCallbacks(confirmNearRunnable)) {
                        debounceHandler.postDelayed(confirmNearRunnable, DEBOUNCER_SECONDS)
                    }
                }
            } else if (rssi < -95) {
                debounceHandler.removeCallbacks(confirmNearRunnable)
                if (isNearConfirmed) {
                    if (!debounceHandler.hasCallbacks(confirmFarRunnable)) {
                        debounceHandler.postDelayed(confirmFarRunnable, DEBOUNCER_SECONDS)
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        val filter = ScanFilter.Builder()
            //.setServiceUuid(ParcelUuid.fromString(targetUUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()

        bleScanner?.startScan(listOf(filter), settings, scanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        bleScanner?.stopScan(scanCallback)
    }
}