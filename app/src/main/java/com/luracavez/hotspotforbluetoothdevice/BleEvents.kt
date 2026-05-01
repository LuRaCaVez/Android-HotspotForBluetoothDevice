package com.luracavez.hotspotforbluetoothdevice

import android.bluetooth.le.ScanResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object BleEvents {
    private val _scanResults = MutableSharedFlow<List<ScanResult>>(extraBufferCapacity = 10)
    val scanResults = _scanResults.asSharedFlow()

    fun emit(results: List<ScanResult>) {
        _scanResults.tryEmit(results)
    }
}
