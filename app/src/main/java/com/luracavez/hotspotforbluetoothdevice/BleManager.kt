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
private const val SCAN_RETRY_DELAY = 10000L
private const val SCAN_RESTART_PERIOD = 3 * 60 * 60 * 1000L //3 hours
private const val CHECK_DISTANCE_PERIOD = 6000L
private const val LOST_TIMEOUT = 60000L
private const val SCAN_REPORT_DELAY = 5000L
private const val RSSI_WINDOW_SIZE = 2

class BleManager(
    private val context: Context,
    private val targetUUID: String,
    private val targetMAC: String,
    private val updateNotification: (String) -> Unit
) {

    private val deviceHandler = Handler(Looper.getMainLooper())

    private val lostDeviceHandler = Handler(Looper.getMainLooper())

    private val scanRestartHandler = Handler(Looper.getMainLooper())

    private val scanRetryHandler = Handler(Looper.getMainLooper())

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
                val distance = calculateDistance(rssiWindow.average().toInt())
                if (distance < 3 && !isDeviceNear) {
                    onDeviceNear()
                } else if (distance > 6 && isDeviceNear) {
                    onDeviceFar()
                }

                val distanceInfo = "Distance: %.2fm".format(distance)
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

    private val restartScanRunnable = object : Runnable {
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override fun run() {
            stopScanning()
            startScanning()
        }
    }

    fun saveRssi(newRssi: Int) {
        rssiWindow.add(newRssi)
        if (rssiWindow.size > RSSI_WINDOW_SIZE) rssiWindow.removeAt(0)
    }

    fun calculateDistance(rssi: Int, measuredPower: Int = -59): Double {
        val pathLossExponent = 3.0
        return 10.0.pow((measuredPower - rssi) / (10 * pathLossExponent))
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
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)

            Log.d(LOG_TAG, "Received %d BLE messages in batch".format(results.count()))

            if (results.count() > 0)
            {
                results.forEach { saveRssi(it.rssi) }
                lostDeviceHandler.removeCallbacks(lostDeviceRunnable)
                lostDeviceHandler.postDelayed(lostDeviceRunnable, LOST_TIMEOUT)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

            Log.e(LOG_TAG, "Scan failed errorCode: %d".format(errorCode))
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        if (targetUUID.isEmpty() && targetMAC.isEmpty()) {
            Log.d(LOG_TAG, "Skip scanning because target is not set.")
            return
        }

        if (bluetoothAdapter?.isEnabled != true)
        {
            Log.d(LOG_TAG, "Bluetooth Adapter is not ready, retry....")
            scanRetryHandler.postDelayed(restartScanRunnable, SCAN_RETRY_DELAY)
            return
        }

        scanRestartHandler.postDelayed(restartScanRunnable, SCAN_RESTART_PERIOD)

        Log.d(LOG_TAG, "Start scanning....")

        val builder = ScanFilter.Builder()
        if (targetUUID.isNotEmpty()) {
            builder.setServiceUuid(ParcelUuid.fromString(targetUUID))
        }
        if (targetMAC.isNotEmpty()) {
            builder.setDeviceAddress(targetMAC)
        }
        val filter = builder.build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(SCAN_REPORT_DELAY)
            .build()

        bleScanner?.startScan(listOf(filter), settings, scanCallback)
        deviceHandler.postDelayed(deviceRunnable, CHECK_DISTANCE_PERIOD)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        Log.d(LOG_TAG, "Stop scanning.")

        scanRetryHandler.removeCallbacks(restartScanRunnable)
        scanRestartHandler.removeCallbacks(restartScanRunnable)
        deviceHandler.removeCallbacks(deviceRunnable)
        lostDeviceHandler.removeCallbacks(lostDeviceRunnable)
        bleScanner?.stopScan(scanCallback)
    }
}