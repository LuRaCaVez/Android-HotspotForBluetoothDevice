package com.luracavez.hotspotforbluetoothdevice

import android.Manifest
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val LOG_TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val deviceManager: CompanionDeviceManager? by lazy {
        getSystemService(CompanionDeviceManager::class.java)
    }

    private lateinit var deviceAdapter: DeviceAdapter

    private val pairingLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val association: AssociationInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                result.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION)
            }
            
            val deviceName = association?.displayName ?: "Device"
            Log.d(LOG_TAG, "Association successful: $deviceName")
            Toast.makeText(this, "$deviceName paired successfully!", Toast.LENGTH_SHORT).show()
            loadAssociations()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (getMissingPermissions().isEmpty()) {
            Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions are required for the app to function.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        setupRecyclerView()

        findViewById<Button>(R.id.btnPairDevice).setOnClickListener {
            handleAction { startAssociationProcess() }
        }
        
        findViewById<Button>(R.id.btnSaveAndStartBLE).setOnClickListener {
            handleAction {
                startMonitoringService()
                Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 750)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAssociations()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvDevices)
        deviceAdapter = DeviceAdapter(emptyList()) { association ->
            disassociateDevice(association)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = deviceAdapter
    }

    private fun loadAssociations() {
        val manager = deviceManager ?: return
        deviceAdapter.update(manager.myAssociations)
    }

    private fun disassociateDevice(association: AssociationInfo) {
        val manager = deviceManager ?: return
        try {
            manager.disassociate(association.id)
            Toast.makeText(this, "Device removed", Toast.LENGTH_SHORT).show()
            loadAssociations()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error disassociating", e)
        }
    }

    private fun handleAction(onReady: () -> Unit) {
        val missing = getMissingPermissions()
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
            return
        }

        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Please allow 'Modify system settings' for Hotspot control", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
            return
        }

        onReady()
    }

    private fun getMissingPermissions(): List<String> {
        val required = mutableListOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startAssociationProcess() {
        val manager = deviceManager ?: return
        
        val request = AssociationRequest.Builder()
            .addDeviceFilter(BluetoothDeviceFilter.Builder().build())
            .addDeviceFilter(BluetoothLeDeviceFilter.Builder().build())
            .build()

        manager.associate(
            request,
            object : CompanionDeviceManager.Callback() {
                override fun onAssociationPending(intentSender: IntentSender) {
                    val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                    pairingLauncher.launch(intentSenderRequest)
                }

                override fun onAssociationCreated(associationInfo: AssociationInfo) {
                    val address = associationInfo.deviceMacAddress?.toString()
                    
                    if (address != null) {
                        manager.myAssociations.filter { 
                            it.deviceMacAddress?.toString() == address && it.id != associationInfo.id 
                        }.forEach { old ->
                            Log.d(LOG_TAG, "Removing duplicate association: ${old.id}")
                            manager.disassociate(old.id)
                        }
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                            val observeRequest = ObservingDevicePresenceRequest.Builder()
                                .setAssociationId(associationInfo.id)
                                .build()
                            manager.startObservingDevicePresence(observeRequest)
                        } else {
                            if (address != null) {
                                @Suppress("DEPRECATION")
                                manager.startObservingDevicePresence(address)
                            }
                        }
                        Log.d(LOG_TAG, "Started observing: $address ${associationInfo.id}")
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Failed to observe ${associationInfo.id}", e)
                    }

                    loadAssociations()
                }

                override fun onFailure(error: CharSequence?) {
                    Log.e(LOG_TAG, "Association failed: $error")
                }
            },
            null
        )
    }

    private fun startMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java)
        startForegroundService(intent)
    }

    private inner class DeviceAdapter(
        private var devices: List<AssociationInfo>,
        private val onDelete: (AssociationInfo) -> Unit
    ) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.deviceName)
            val address: TextView = view.findViewById(R.id.deviceAddress)
            val deleteBtn: ImageButton = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            holder.name.text = device.displayName ?: "Unknown Device"
            holder.address.text = device.deviceMacAddress?.toString() ?: "No Address"
            holder.deleteBtn.setOnClickListener { onDelete(device) }
        }

        override fun getItemCount() = devices.size

        fun update(newDevices: List<AssociationInfo>) {
            devices = newDevices
            notifyDataSetChanged()
        }
    }
}