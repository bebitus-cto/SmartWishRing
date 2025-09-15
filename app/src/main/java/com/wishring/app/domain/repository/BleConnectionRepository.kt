package com.wishring.app.domain.repository

import com.wishring.app.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for core BLE connection operations
 * Manages basic device communication and data transfer
 */
interface BleConnectionRepository {
    
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
     * Get connection state
     * @return Flow of connection states
     */
    fun getConnectionState(): Flow<BleConnectionState>
    
    /**
     * Get current connected device
     * @return Connected device or null
     */
    suspend fun getConnectedDevice(): BleDevice?
    
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
    
    /**
     * Subscribe to button press events
     * @return Flow of button press events
     */
    fun subscribeToButtonPress(): Flow<ButtonPressEvent>
    
    /**
     * Subscribe to device notifications
     * @return Flow of device notifications
     */
    fun subscribeToNotifications(): Flow<BleNotification>
    
    /**
     * Get device battery level
     * @return Battery level (0-100) or null
     */
    suspend fun getBatteryLevel(): Int?
    
    /**
     * Subscribe to battery level updates
     * @return Flow of battery levels
     */
    fun subscribeToBatteryLevel(): Flow<Int>
    
    /**
     * Get device firmware version
     * @return Firmware version string or null
     */
    suspend fun getFirmwareVersion(): String?
    
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
     * Set device LED color
     * @param color Color in RGB hex format
     * @return True if set successfully
     */
    suspend fun setLedColor(color: String): Boolean
    
    /**
     * Test device connection
     * @return True if device responds
     */
    suspend fun testConnection(): Boolean
    
    /**
     * Find device (activate LED/vibration to locate device)
     * @param enable True to start finding, false to stop
     * @return True if command sent successfully
     */
    suspend fun findDevice(enable: Boolean): Boolean

    /**
     * Clear all bonded devices
     */
    suspend fun clearBondedDevices()
    
    // ====== MRD SDK Counter Integration ======
    
    /**
     * Observable counter increment events from MRD SDK
     * Each emission represents a single +1 increment from the ring
     */
    val counterIncrements: Flow<Int>
    
    /**
     * Request battery level update via MRD SDK
     * Result will be available through subscribeToBatteryLevel()
     */
    suspend fun requestBatteryUpdate(): Result<Unit>
}