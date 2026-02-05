package com.luracavez.hotspotforbluetoothdevice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
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
import kotlin.math.pow

private const val LOG_TAG = "BleManager"
private const val CHECK_DISTANCE_PERIOD = 3000L
private const val LOST_TIMEOUT = 60000L
private const val RSSI_WINDOW_SIZE = 10

class BleManager(
    private val context: Context,
    private val targetUUID: String,
    private val targetMAC: String,
    private val updateNotification: (String) -> Unit
) {

    private val deviceHandler =  Handler(Looper.getMainLooper())

    private val lostDeviceHandler =  Handler(Looper.getMainLooper())

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(BluetoothManager::class.java)
        manager.adapter
    }

    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    private val rssiWindow = mutableListOf<Int>()

    private var isDeviceNear = false

    private val deviceRunnable = object : Runnable {
        override fun run() {
            if (rssiWindow.isNotEmpty()) {
                val averageRssi = rssiWindow.average().toInt()
                val distance = calculateDistance(averageRssi)
                val distanceInfo = "Distance: %.2fm".format(distance)
                val isNear = isNear(distance)
                if (isNear == true && !isDeviceNear) {
                    onDeviceNear()
                } else if (isNear == false && isDeviceNear) {
                    onDeviceFar()
                }
                if (isDeviceNear) {
                    updateNotification("$CONNECTED_MESSAGE - $distanceInfo")
                } else {
                    updateNotification("$DISCONNECTED_MESSAGE - $distanceInfo")
                }
            }
            deviceHandler.postDelayed(this, CHECK_DISTANCE_PERIOD)
        }
    }

    private val lostDeviceRunnable = Runnable {
        if (isDeviceNear) {
            onDeviceFar()
        } else {
            updateNotification(DISCONNECTED_MESSAGE)
        }
        rssiWindow.removeAll { true }
    }

    fun saveRssi(newRssi: Int) {
        rssiWindow.add(newRssi)
        if (rssiWindow.size > RSSI_WINDOW_SIZE) rssiWindow.removeAt(0)
    }

    fun calculateDistance(rssi: Int, measuredPower: Int = -59): Double {
        val pathLossExponent = 3.0
        return 10.0.pow((measuredPower - rssi) / (10 * pathLossExponent))
    }

    fun isNear(distance: Double): Boolean? {
        if (distance < 3) {
            return true
        } else if (distance > 6) {
            return false
        }
        return null
    }

    fun onDeviceFar() {
        Log.d(LOG_TAG, "Device disconnected")
        isDeviceNear = false
        updateNotification(DISCONNECTED_MESSAGE)
        HotspotManager.toggleHotspot(context, false)
    }

    fun onDeviceNear() {
        Log.d(LOG_TAG, "Device connected")
        isDeviceNear = true
        updateNotification(CONNECTED_MESSAGE)
        HotspotManager.toggleHotspot(context, true)
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            saveRssi(result.rssi)

            lostDeviceHandler.removeCallbacks(lostDeviceRunnable)
            lostDeviceHandler.postDelayed(lostDeviceRunnable, LOST_TIMEOUT)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        val builder = ScanFilter.Builder()
        if (targetUUID.isNotEmpty()) {
            builder.setServiceUuid(ParcelUuid.fromString(targetUUID))
        }
        if (targetMAC.isNotEmpty()) {
            builder.setDeviceAddress(targetMAC)
        }
        val filter = builder.build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()

        bleScanner?.startScan(listOf(filter), settings, scanCallback)
        deviceHandler.postDelayed(deviceRunnable, CHECK_DISTANCE_PERIOD)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        bleScanner?.stopScan(scanCallback)
        deviceHandler.removeCallbacks(deviceRunnable)
        lostDeviceHandler.removeCallbacks(lostDeviceRunnable)
    }
}