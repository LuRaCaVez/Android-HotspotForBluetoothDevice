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
import androidx.core.content.edit
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        val editUUID = findViewById<EditText>(R.id.editUUID)
        val editMAC = findViewById<EditText>(R.id.editMAC)
        val btnStartBLE = findViewById<Button>(R.id.btnSaveAndStartBLE)
        val btnViewLogs = findViewById<Button>(R.id.btnViewLogs)
        val sharedPrefs = getSharedPreferences(SHARED_NAME, MODE_PRIVATE)

        editUUID.setText(sharedPrefs.getString(UUID_SHARED_KEY, ""))
        editMAC.setText(sharedPrefs.getString(MAC_SHARED_KEY, ""))

        btnStartBLE.setOnClickListener {
            val mac = editMAC.text.toString().trim()
            val uuid = editUUID.text.toString().trim()
            if (mac.isEmpty() && uuid.isEmpty()) {
                Toast.makeText(this, "One of MAC or UUID must be filled!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mac.isNotEmpty() && mac.length != 17) {
                Toast.makeText(this, "MAC not valid!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (uuid.isNotEmpty()) {
                try {
                    ParcelUuid.fromString(uuid)
                } catch (e: Exception) {
                    Toast.makeText(this, "UUID not valid!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            sharedPrefs.edit { putString(UUID_SHARED_KEY, uuid) }
            sharedPrefs.edit { putString(MAC_SHARED_KEY, mac) }
            checkPermissionsAndStart()
        }

        btnViewLogs.setOnClickListener {
            startActivity(Intent(this, LogViewerActivity::class.java))
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
            ServiceManager.startService(this)
            finish()
        }
    }
}