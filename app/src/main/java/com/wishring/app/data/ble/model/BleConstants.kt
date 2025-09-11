package com.wishring.app.data.ble.model

import java.util.*

/**
 * BLE constants for WishRing device
 * 
 * WishRing 디바이스 BLE 통신 상수 정의
 */
object BleConstants {
    
    // Service & Characteristic UUIDs
    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val COUNTER_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    val BATTERY_CHAR_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
    val RESET_CHAR_UUID: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    
    // Device identification
    const val DEVICE_NAME = "WishRing"
    const val DEVICE_NAME_PREFIX = "WISH"
    
    // Scan settings
    const val SCAN_TIMEOUT_MS = 10000L      // 10초
    const val CONNECTION_TIMEOUT_MS = 5000L  // 5초
    const val RETRY_ATTEMPTS = 3
    
    // Data parsing
    const val COUNTER_DATA_SIZE = 4         // Int32 = 4 bytes
    const val BATTERY_DATA_SIZE = 1         // UInt8 = 1 byte
    const val RESET_SIGNAL = 0x01.toByte()
    
    // Foreground service
    const val FOREGROUND_SERVICE_ID = 1001
    const val NOTIFICATION_CHANNEL_ID = "wish_ring_ble_channel"
    const val NOTIFICATION_CHANNEL_NAME = "WishRing BLE Service"
}