package com.example.rfidscanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "rfid_tags")
data class RFIDTag(
    @PrimaryKey
    val epc: String,
    var rssi: Int? = null,
    var seenCount: Int = 1,
    val firstSeen: Date = Date(),
    var lastSeen: Date = Date()
)