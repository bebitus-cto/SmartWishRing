package com.wishring.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for logging BLE events (specifically RESET events)
 * Based on RFP.md specification for BleEventLog table
 */
@Entity(tableName = "ble_event_logs")
data class BleEventLogEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    val timestamp: Long,          // Event occurrence time (System.currentTimeMillis())
    val eventType: String,        // Event type ("RESET" only for now)  
    val value: Int,              // RESET event: count value before reset
    val deviceAddress: String = "",  // Device MAC address (if available)
    val additional: String = ""      // Additional event data (JSON format)
) {
    companion object {
        const val EVENT_TYPE_RESET = "RESET"
        
        /**
         * Create RESET event log entry
         */
        fun createResetEvent(
            timestamp: Long,
            previousCount: Int,
            deviceAddress: String = ""
        ): BleEventLogEntity {
            return BleEventLogEntity(
                timestamp = timestamp,
                eventType = EVENT_TYPE_RESET,
                value = previousCount,
                deviceAddress = deviceAddress
            )
        }
    }
}