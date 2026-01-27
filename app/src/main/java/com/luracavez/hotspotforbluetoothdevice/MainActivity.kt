package com.luracavez.hotspotforbluetoothdevice

import android.Manifest
import android.content.Intent
import android.os.Bundle
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

        val editMac = findViewById<EditText>(R.id.editMacAddress)
        val btnStart = findViewById<Button>(R.id.btnSaveAndStart)
        val sharedPrefs = getSharedPreferences("HotspotConfig", MODE_PRIVATE)

        editMac.setText(sharedPrefs.getString("target_mac", ""))

        btnStart.setOnClickListener {
            val mac = editMac.text.toString().trim()
            if (mac.length == 17) {
                sharedPrefs.edit { putString("target_mac", mac) }

                checkPermissionsAndStart()
            } else {
                Toast.makeText(this, "MAC not valid: it must be 17 chars length", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)

        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
        } else {
            startService()
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, HotspotService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        finish()
    }
}