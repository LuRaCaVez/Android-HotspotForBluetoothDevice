package com.luracavez.hotspotforbluetoothdevice

import android.Manifest
import android.app.Activity
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.ComponentName
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val deviceManager: CompanionDeviceManager? by lazy {
        getSystemService(CompanionDeviceManager::class.java)
    }

    private val pairingLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("MainActivity", "Association successful")
            Toast.makeText(this, "Device paired successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d("MainActivity", "All permissions granted")
        } else {
            Toast.makeText(this, "Permissions are required for background monitoring", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // Initial permission check
        checkAndRequestPermissions()

        findViewById<Button>(R.id.btnPairDevice).setOnClickListener {
            if (hasPermissions()) {
                startAssociationProcess()
            } else {
                checkAndRequestPermissions()
            }
        }
        
        findViewById<Button>(R.id.btnSaveAndStartBLE).setOnClickListener {
            if (hasPermissions()) {
                val intent = Intent(this, BLEService::class.java)
                startForegroundService(intent)
            } else {
                checkAndRequestPermissions()
            }
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startAssociationProcess() {
        val manager = deviceManager ?: return
        val deviceFilter = BluetoothDeviceFilter.Builder().build()

        val request = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .build()

        manager.associate(
            request,
            object : CompanionDeviceManager.Callback() {
                override fun onAssociationPending(intentSender: IntentSender) {
                    val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                    pairingLauncher.launch(intentSenderRequest)
                }

                override fun onAssociationCreated(associationInfo: AssociationInfo) {
                    // Enable Boot receiver on pairing success
                    val receiver = ComponentName(this@MainActivity, BootReceiver::class.java)
                    packageManager.setComponentEnabledSetting(
                        receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                            // Use modern API for Android 14+
                            val observeRequest = ObservingDevicePresenceRequest.Builder()
                                .setAssociationId(associationInfo.id)
                                .build()
                            manager.startObservingDevicePresence(observeRequest)
                            Log.d("MainActivity", "Presence observation started (Request API)")
                        } else {
                            // Fallback for Android 13
                            val address = associationInfo.deviceMacAddress
                            if (address != null) {
                                @Suppress("DEPRECATION")
                                manager.startObservingDevicePresence(address.toString())
                                Log.d("MainActivity", "Presence observation started (String API)")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error starting presence observation", e)
                    }
                }

                override fun onFailure(error: CharSequence?) {
                    Log.e("MainActivity", "Association failed: $error")
                }
            },
            null
        )
    }
}