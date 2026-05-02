package com.luracavez.hotspotforbluetoothdevice

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class BleWorkWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val serviceIntent = Intent(applicationContext, BleService::class.java)
        applicationContext.startForegroundService(serviceIntent)
        return Result.success()
    }
}