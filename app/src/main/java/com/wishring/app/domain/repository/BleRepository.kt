package com.wishring.app.domain.repository

import com.wishring.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for BLE (Bluetooth Low Energy) operations
 * Manages communication with WISH RING device
 * Simplified to handle only count and battery operations
 */
interface BleRepository {
    
    // Connection state
    val connectionState: StateFlow<BleConnectionState>
    
    /**
     * Start BLE scanning
     * @param timeout Scan timeout in milliseconds (0 for infinite)
     * @return Flow of discovered devices
     */
    fun startScanning(timeout: Long = 10000): Flow<BleDevice>
    
    /**
     * Stop BLE scanning
     */
    suspend fun stopScanning()
    
    /**
     * Connect to BLE device
     * @param deviceAddress Device MAC address
     * @return True if connected successfully
     */
    suspend fun connectDevice(deviceAddress: String): Boolean
    
    /**
     * Disconnect from current device
     */
    suspend fun disconnectDevice()
    
    /**
     * Check if device is connected
     * @return True if connected
     */
    suspend fun isDeviceConnected(): Boolean
    
    /**
     * Get current connected device
     * @return Connected device or null
     */
    suspend fun getConnectedDevice(): BleDevice?
    
    // ====== Wish Count Operations ======
    
    /**
     * Send wish count to device
     * @param count Current wish count
     * @return True if sent successfully
     */
    suspend fun sendWishCount(count: Int): Boolean
    
    /**
     * Send wish text to device
     * @param text Wish text (max 20 chars)
     * @return True if sent successfully
     */
    suspend fun sendWishText(text: String): Boolean
    
    /**
     * Send target count to device
     * @param target Target count
     * @return True if sent successfully
     */
    suspend fun sendTargetCount(target: Int): Boolean
    
    /**
     * Send completion status to device
     * @param isCompleted Completion status
     * @return True if sent successfully
     */
    suspend fun sendCompletionStatus(isCompleted: Boolean): Boolean
    
    /**
     * Sync all data with device
     * @param wishCount Current wish count
     * @param wishText Wish text
     * @param targetCount Target count
     * @param isCompleted Completion status
     * @return True if all data synced successfully
     */
    suspend fun syncAllData(
        wishCount: Int,
        wishText: String,
        targetCount: Int,
        isCompleted: Boolean
    ): Boolean
    
    /**
     * Read wish count from device
     * @return Wish count from device or null
     */
    suspend fun readWishCount(): Int?
    
    /**
     * Read button press count from device
     * @return Button press count or null
     */
    suspend fun readButtonPressCount(): Int?
    
    // ====== Battery Operations ======
    
    /**
     * Get device battery level
     * @return Battery level (0-100) or null
     */
    suspend fun getBatteryLevel(): Int?
    
    /**
     * Get battery level as StateFlow
     * @return StateFlow of battery level
     */
    fun getBatteryLevelFlow(): StateFlow<Int?>
    
    /**
     * Request battery level update from device
     * @return Result of the request
     */
    suspend fun requestBatteryUpdate(): Result<Unit>
    
    // ====== Event Streams ======
    
    /**
     * Button press events from device
     */
    val buttonPressEvents: Flow<ButtonPressEvent>
    
    /**
     * Device notifications
     */
    val notifications: Flow<BleNotification>
    
    /**
     * Counter increment events from MRD SDK (HEART events)
     * Each emission represents a single +1 increment from the ring
     */
    val counterIncrements: Flow<Int>
    
    // ====== Basic Device Operations ======
    
    /**
     * Update device time
     * @return True if time updated successfully
     */
    suspend fun updateDeviceTime(): Boolean
    
    /**
     * Reset device
     * @return True if reset successfully
     */
    suspend fun resetDevice(): Boolean
    
    /**
     * Enable device notifications
     * @return True if enabled successfully
     */
    suspend fun enableNotifications(): Boolean
    
    /**
     * Disable device notifications
     * @return True if disabled successfully
     */
    suspend fun disableNotifications(): Boolean
    
    /**
     * Test device connection
     * @return True if device responds
     */
    suspend fun testConnection(): Boolean
    
    /**
     * Clear all bonded devices
     */
    suspend fun clearBondedDevices()
}

/**
 * BLE Device data class
 */
data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isConnectable: Boolean,
    val isBonded: Boolean
)

/**
 * BLE Connection State enum
 */
enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Extension function to check if BLE is connected
 */
fun BleConnectionState.isConnected(): Boolean {
    return this == BleConnectionState.CONNECTED
}

/**
 * Button press event data class
 */
data class ButtonPressEvent(
    val timestamp: Long,
    val pressCount: Int,
    val pressType: PressType
)

/**
 * Press type enum
 */
enum class PressType {
    SINGLE,
    DOUBLE,
    LONG,
    TRIPLE
}

/**
 * BLE Notification data class
 */
data class BleNotification(
    val type: NotificationType,
    val message: String,
    val timestamp: Long
)

/**
 * Notification type enum
 */
enum class NotificationType {
    BUTTON_PRESS,
    LOW_BATTERY,
    CONNECTION_LOST,
    DATA_SYNC,
    ERROR
}