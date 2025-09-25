package com.example.rfidscanner.model

import android.bluetooth.BluetoothDevice

data class BluetoothDeviceItem(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    var isPaired: Boolean = false
)