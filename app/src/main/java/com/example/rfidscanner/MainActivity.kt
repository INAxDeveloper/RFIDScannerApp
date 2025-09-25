package com.example.rfidscanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidscanner.model.BluetoothDeviceItem
import com.example.rfidscanner.model.RFIDTag
import java.util.Date
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btnScanDevices: Button
    private lateinit var btnConnect: Button
    private lateinit var etMacAddress: EditText
    private lateinit var tvStatus: TextView
    private lateinit var rvDevices: RecyclerView
    private lateinit var rvTags: RecyclerView

    private val devices = mutableListOf<BluetoothDeviceItem>()
    private val tags = mutableListOf<RFIDTag>()
    private lateinit var deviceAdapter: BluetoothDeviceAdapter
    private lateinit var tagAdapter: TagAdapter

    private var isScanning = false
    private var isConnected = false
    private var selectedDevice: BluetoothDevice? = null

    // Simulated RFID characteristics (assumed for this exercise)
    private val RFID_SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    private val TRIGGER_CHARACTERISTIC_UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
    private val TAGS_CHARACTERISTIC_UUID = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb")
//Service UUID: 00001800-0000-1000-8000-00805f9b34fb (Generic Access)
//
//Trigger Characteristic: 00002a00-0000-1000-8000-00805f9b34fb (Device Name)
//
//Tags Characteristic: 00002a01-0000-1000-8000-00805f9b34fb (Appearance)
    private val handler = Handler(Looper.getMainLooper())
    private val scanRunnable = Runnable { stopDeviceScan() }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        }
                        if (it.name != null) {
                            val deviceItem = BluetoothDeviceItem(
                                device = it,
                                name = it.name ?: "Unknown Device",
                                address = it.address,
                                isPaired = it.bondState == BluetoothDevice.BOND_BONDED
                            )
                            if (devices.none { existing -> existing.address == deviceItem.address }) {
                                devices.add(deviceItem)
                                deviceAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isScanning = false
                    updateUI()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        setupBluetooth()
        setupRecyclerViews()
        checkPermissions()
    }
    private fun initViews() {
        btnScanDevices = findViewById(R.id.btnScanDevices)
        btnConnect = findViewById(R.id.btnConnect)
        etMacAddress = findViewById(R.id.etMacAddress)
        tvStatus = findViewById(R.id.tvStatus)
        rvDevices = findViewById(R.id.rvDevices)
        rvTags = findViewById(R.id.rvTags)

        btnScanDevices.setOnClickListener {
            if (isScanning) {
                stopDeviceScan()
            } else {
                startDeviceScan()
            }
        }

        btnConnect.setOnClickListener {
            if (isConnected) {
                disconnectDevice()
            } else {
                connectToDevice()
            }
        }
    }

    private fun setupBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private fun setupRecyclerViews() {
        deviceAdapter = BluetoothDeviceAdapter(devices) { device ->
            selectedDevice = device.device
            etMacAddress.setText(device.address)
            updateUI()
        }

        tagAdapter = TagAdapter(tags)

        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = deviceAdapter

        rvTags.layoutManager = LinearLayoutManager(this)
        rvTags.adapter = tagAdapter
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BLUETOOTH)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun startDeviceScan() {
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (hasBluetoothPermissions()) {
            devices.clear()
            deviceAdapter.notifyDataSetChanged()

            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            registerReceiver(bluetoothReceiver, filter)

            isScanning = true
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothAdapter.startDiscovery()
            handler.postDelayed(scanRunnable, 10000) // Stop after 10 seconds
            updateUI()
        } else {
            checkPermissions()
        }
    }

    private fun stopDeviceScan() {
        if (isScanning) {
            try {
                unregisterReceiver(bluetoothReceiver)
            } catch (e: Exception) {
                // Receiver was not registered
            }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothAdapter.cancelDiscovery()
            isScanning = false
            handler.removeCallbacks(scanRunnable)
            updateUI()
        }
    }

    private fun connectToDevice() {
        val macAddress = etMacAddress.text.toString().trim()
        if (macAddress.isEmpty() && selectedDevice == null) {
            Toast.makeText(this, "Please select a device or enter MAC address", Toast.LENGTH_SHORT).show()
            return
        }

        // Simulate connection (in real app, this would use BluetoothGATT)
        tvStatus.text = "Status: Connecting..."
        btnConnect.isEnabled = false

        handler.postDelayed({
            isConnected = true
            tvStatus.text = "Status: Connected - Waiting for trigger..."
            updateUI()
            startSimulatedTriggerEvents()
        }, 2000)
    }

    private fun disconnectDevice() {
        isConnected = false
        selectedDevice = null
        tvStatus.text = "Status: Disconnected"
        updateUI()
        handler.removeCallbacksAndMessages(null)
    }

    private fun startSimulatedTriggerEvents() {
        // Simulate trigger events every 3 seconds
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isConnected) {
                    onTriggerEvent()
                    handler.postDelayed(this, 3000)
                }
            }
        }, 3000)
    }

    private fun onTriggerEvent() {
        // Simulate receiving RFID tags when trigger is activated
        val simulatedTags = generateSimulatedTags()
        updateTagList(simulatedTags)
    }

    private fun generateSimulatedTags(): List<RFIDTag> {
        val newTags = mutableListOf<RFIDTag>()

        // Generate 3-8 random tags per trigger
        val tagCount = (3..8).random()
        repeat(tagCount) {
            val epc = "E200${String.format("%012X", (Math.random() * 0xFFFFFFFFFFFFL))}"
            val rssi = (-70..-30).random()

            val existingTag = tags.find { it.epc == epc }
            if (existingTag != null) {
                existingTag.seenCount++
                existingTag.lastSeen = Date()
                newTags.add(existingTag)
            } else {
                newTags.add(RFIDTag(epc = epc, rssi = rssi))
            }
        }

        return newTags
    }

    private fun updateTagList(newTags: List<RFIDTag>) {
        // Update existing tags and add new ones
        newTags.forEach { newTag ->
            val existingIndex = tags.indexOfFirst { it.epc == newTag.epc }
            if (existingIndex != -1) {
                tags[existingIndex] = newTag
            } else {
                tags.add(0, newTag) // Add new tags at the top
            }
        }

        tagAdapter.updateTags(tags)

        // Show toast with scan results
        val newTagCount = newTags.count { tag -> tags.indexOfFirst { it.epc == tag.epc } >= tags.size - newTags.size }
        Toast.makeText(this, "Scan completed: $newTagCount new tags, ${newTags.size} total detected", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        btnScanDevices.text = if (isScanning) "Stop Scan" else "Scan Devices"
        btnConnect.text = if (isConnected) "Disconnect" else "Connect"
        btnConnect.isEnabled = selectedDevice != null || etMacAddress.text.isNotEmpty()
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions denied - some features may not work", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDeviceScan()
        disconnectDevice()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver was not registered
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}