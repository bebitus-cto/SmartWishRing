package com.wishring.app.data.ble.model

import java.util.*

/**
 * BLE constants for WishRing device
 * 
 * WishRing 디바이스 BLE 통신 상수 정의
 */
object BleConstants {
    
    // Service & Characteristic UUIDs
    val SERVICE_UUID: UUID = UUID.fromString("f000efe0-0451-4000-0000-00000000b000")
    val WRITE_CHAR_UUID: UUID = UUID.fromString("f000efe1-0451-4000-0000-00000000b000")
    val COUNTER_CHAR_UUID: UUID = UUID.fromString("f000efe3-0451-4000-0000-00000000b000")
    val BATTERY_CHAR_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
    val RESET_CHAR_UUID: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    
    // Device identification (removed fake WishRing constants)
    // Scan settings
    const val SCAN_TIMEOUT_MS = 10000L      // 10초
    const val CONNECTION_TIMEOUT_MS = 5000L  // 5초
    const val RETRY_ATTEMPTS = 3
    
    // Data parsing
    const val COUNTER_DATA_SIZE = 4         // Int32 = 4 bytes
    const val BATTERY_DATA_SIZE = 1         // UInt8 = 1 byte
    const val RESET_SIGNAL = 0x01.toByte()
    
    // MRD SDK Commands (for reference, actual commands use MRD SDK)
    val BATTERY_COMMAND = byteArrayOf(0x02, 0x01)  // Legacy command, now using SystemEnum.battery
    
    // Foreground service
    const val FOREGROUND_SERVICE_ID = 1001
    const val NOTIFICATION_CHANNEL_ID = "wish_ring_ble_channel"
    const val NOTIFICATION_CHANNEL_NAME = "WishRing BLE Service"
}