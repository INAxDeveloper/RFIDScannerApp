package com.example.rfidscanner

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rfidscanner.data.AppDatabase
import com.example.rfidscanner.databinding.ActivityMainDemoBinding
import com.example.rfidscanner.model.RFIDTag
import com.example.rfidscanner.repository.RFIDRepository
import kotlinx.coroutines.launch
import java.util.*

class DemoMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainDemoBinding
    private lateinit var repository: RFIDRepository

    private val tags = mutableListOf<RFIDTag>()
    private lateinit var tagAdapter: TagAdapter

    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize database and repository
        val database = AppDatabase.getInstance(this)
        repository = RFIDRepository(database.rfidTagDao())

        initViews()
        setupRecyclerView()
        simulateBluetoothSetup()
        loadTagsFromDatabase()
    }

    private fun initViews() {
        binding.btnConnect.setOnClickListener {
            if (isConnected) {
                disconnectDevice()
            } else {
                connectToDevice()
            }
        }

        binding.btnTrigger.setOnClickListener {
            if (isConnected) {
                onTriggerEvent()
            } else {
                Toast.makeText(this, "Please connect first", Toast.LENGTH_SHORT).show()
            }
        }

        // Add clear database button functionality
        binding.btnClear.setOnClickListener {
            clearDatabase()
        }
    }

    private fun setupRecyclerView() {
        tagAdapter = TagAdapter(tags) { tag ->
            // Handle item click if needed
        }
        binding.rvTags.layoutManager = LinearLayoutManager(this)
        binding.rvTags.adapter = tagAdapter
    }

    private fun simulateBluetoothSetup() {
        binding.tvStatus.text = "Status: Ready for demo mode"
        Toast.makeText(this, "RFID Scanner Demo - No hardware needed!", Toast.LENGTH_LONG).show()
    }

    private fun loadTagsFromDatabase() {
        lifecycleScope.launch {
            repository.allTags.collect { databaseTags ->
                tags.clear()
                tags.addAll(databaseTags)
                tagAdapter.updateTags(tags)
                updateTagCountDisplay()
            }
        }
    }

    private fun connectToDevice() {
        binding.tvStatus.text = "Status: Connecting to demo device..."
        binding.btnConnect.isEnabled = false

        handler.postDelayed({
            isConnected = true
            binding.tvStatus.text = "Status: Connected (Demo Mode)\nPress TRIGGER or Volume buttons"
            updateUI()

            // Auto-scan every 5 seconds in demo mode
            startAutoDemoMode()
        }, 1500)
    }

    private fun disconnectDevice() {
        isConnected = false
        binding.tvStatus.text = "Status: Disconnected"
        handler.removeCallbacksAndMessages(null)
        updateUI()
    }

    private fun startAutoDemoMode() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isConnected) {
                    onTriggerEvent()
                    handler.postDelayed(this, 5000) // Auto-trigger every 5 seconds
                }
            }
        }, 5000)
    }

    private fun onTriggerEvent() {
        val simulatedTags = generateSimulatedTags()
        saveTagsToDatabase(simulatedTags)

        // Visual feedback
        binding.btnTrigger.text = "SCANNING..."
        handler.postDelayed({
            binding.btnTrigger.text = "TRIGGER SCAN"
        }, 1000)
    }

    private fun generateSimulatedTags(): List<RFIDTag> {
        val newTags = mutableListOf<RFIDTag>()
        val productTypes = arrayOf("CLO", "ELE", "BOO", "FOO", "TOO", "MED", "SPO")

        // Generate random number of tags (2-6)
        val tagCount = (2..6).random()

        repeat(tagCount) {
            val productType = productTypes.random()
            val uniqueId = String.format("%08X", random.nextInt(Int.MAX_VALUE))
            val epc = "E200${productType}$uniqueId"
            val rssi = (-65..-45).random()

            newTags.add(RFIDTag(epc = epc, rssi = rssi))
        }

        return newTags
    }

    private fun saveTagsToDatabase(newTags: List<RFIDTag>) {
        lifecycleScope.launch {
            repository.insertOrUpdateTags(newTags)
            // The database update will automatically trigger the Flow collection in loadTagsFromDatabase()
            Toast.makeText(
                this@DemoMainActivity,
                "Scan: ${newTags.size} tags processed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearDatabase() {
        lifecycleScope.launch {
            repository.deleteAllTags()
            Toast.makeText(this@DemoMainActivity, "Database cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTagCountDisplay() {
        binding.tvTagCount.text = "Total Tags: ${tags.size}"
    }

    // Use volume buttons as trigger
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isConnected) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    onTriggerEvent()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateUI() {
        binding.btnConnect.text = if (isConnected) "Disconnect" else "Connect to Demo"
        binding.btnTrigger.isEnabled = isConnected
        updateTagCountDisplay()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}