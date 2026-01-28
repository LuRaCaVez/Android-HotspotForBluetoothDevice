package com.luracavez.hotspotforbluetoothdevice

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        val editUUID = findViewById<EditText>(R.id.editUUID)
        val btnStart = findViewById<Button>(R.id.btnSaveAndStart)
        val sharedPrefs = getSharedPreferences("HotspotConfig", MODE_PRIVATE)

        editUUID.setText(sharedPrefs.getString("target_uuid", ""))

        btnStart.setOnClickListener {
            try {
                val bleUUID = editUUID.text.toString().trim()
                ParcelUuid.fromString(bleUUID)
                sharedPrefs.edit { putString("target_uuid", bleUUID) }
                checkPermissionsAndStart()
            } catch (e: Exception) {
                Toast.makeText(this, "UUID not valid!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        permissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        permissions.add(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)

        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
        } else {
            startService()
            finish()
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, BleService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}