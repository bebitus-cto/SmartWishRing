package com.wishring.app.domain.repository

import com.wishring.app.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for BLE (Bluetooth Low Energy) operations
 * Manages communication with WISH RING device
 */
interface BleRepository {
    
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
     * Set device vibration pattern
     * @param pattern Vibration pattern
     * @return True if set successfully
     */
    suspend fun setVibrationPattern(pattern: VibrationPattern): Boolean
    
    /**
     * Test device connection
     * @return True if device responds
     */
    suspend fun testConnection(): Boolean
    
    /**
     * Get device info
     * @return Device information
     */
    suspend fun getDeviceInfo(): DeviceInfo?
    
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
    
    // ====== MRD SDK 확장 기능들 ======
    
    /**
     * 시스템 관련 기능
     */
    
    /**
     * Get device system information
     * @param type System info type (battery, firmware, etc.)
     * @return System information or null
     */
    suspend fun getSystemInfo(type: SystemInfoType): String?
    
    /**
     * Set device screen brightness
     * @param brightness Brightness level (0-100)
     * @return True if set successfully
     */
    suspend fun setScreenBrightness(brightness: Int): Boolean
    
    /**
     * Reset device to factory settings
     * @return True if reset successfully  
     */
    suspend fun factoryReset(): Boolean
    
    /**
     * 사용자 정보 관리
     */
    
    /**
     * Set user profile information
     * @param userProfile User profile data
     * @return True if set successfully
     */
    suspend fun setUserProfile(userProfile: UserProfile): Boolean
    
    /**
     * Get user profile information
     * @return User profile or null
     */
    suspend fun getUserProfile(): UserProfile?
    
    /**
     * Set sport target (daily step goal)
     * @param steps Target step count
     * @return True if set successfully
     */
    suspend fun setSportTarget(steps: Int): Boolean
    
    /**
     * Get sport target
     * @return Target step count or null
     */
    suspend fun getSportTarget(): Int?
    
    /**
     * 건강 데이터 조회
     */
    
    /**
     * Get latest heart rate data
     * @return Heart rate data or null
     */
    suspend fun getLatestHeartRate(): HeartRateData?
    
    /**
     * Get heart rate history
     * @param days Number of days to retrieve
     * @return List of heart rate data
     */
    suspend fun getHeartRateHistory(days: Int = 7): List<HeartRateData>
    
    /**
     * Get latest blood pressure data
     * @return Blood pressure data or null
     */
    suspend fun getLatestBloodPressure(): BloodPressureData?
    
    /**
     * Get sleep data
     * @param date Specific date to retrieve (null for latest)
     * @return Sleep data or null
     */
    suspend fun getSleepData(date: String? = null): SleepData?
    
    /**
     * Get step data
     * @param date Specific date to retrieve (null for today)
     * @return Step data or null
     */
    suspend fun getStepData(date: String? = null): StepData?
    
    /**
     * Get temperature data
     * @return Temperature data or null
     */
    suspend fun getTemperatureData(): TemperatureData?
    
    /**
     * Get ECG data
     * @return ECG data or null
     */
    suspend fun getEcgData(): EcgData?
    
    /**
     * Get blood oxygen data
     * @return Blood oxygen data or null
     */
    suspend fun getBloodOxygenData(): BloodOxygenData?
    
    /**
     * 실시간 측정 제어
     */
    
    /**
     * Start real-time heart rate measurement
     * @return Flow of real-time heart rate data
     */
    fun startRealTimeHeartRate(): Flow<HeartRateData>
    
    /**
     * Stop real-time heart rate measurement
     * @return True if stopped successfully
     */
    suspend fun stopRealTimeHeartRate(): Boolean
    
    /**
     * Start real-time ECG measurement
     * @return Flow of real-time ECG data
     */
    fun startRealTimeEcg(): Flow<EcgData>
    
    /**
     * Stop real-time ECG measurement
     * @return True if stopped successfully
     */
    suspend fun stopRealTimeEcg(): Boolean
    
    /**
     * Start real-time blood pressure measurement
     * @return Flow of real-time blood pressure data
     */
    fun startRealTimeBloodPressure(): Flow<BloodPressureData>
    
    /**
     * Stop real-time blood pressure measurement
     * @return True if stopped successfully
     */
    suspend fun stopRealTimeBloodPressure(): Boolean
    
    /**
     * 앱 알림 및 기기 제어
     */
    
    /**
     * Send app notification to device
     * @param notification Notification data
     * @return True if sent successfully
     */
    suspend fun sendAppNotification(notification: AppNotification): Boolean
    
    /**
     * Set device language
     * @param language Language code
     * @return True if set successfully
     */
    suspend fun setDeviceLanguage(language: String): Boolean
    
    /**
     * Set unit preferences (metric/imperial)
     * @param units Unit preferences
     * @return True if set successfully
     */
    suspend fun setUnitPreferences(units: UnitPreferences): Boolean
    
    /**
     * Set raise wrist to wake feature
     * @param enabled Enable/disable feature
     * @return True if set successfully
     */
    suspend fun setRaiseWristWake(enabled: Boolean): Boolean
    
    /**
     * Set automatic heart rate monitoring
     * @param enabled Enable/disable feature
     * @param interval Monitoring interval in minutes
     * @return True if set successfully
     */
    suspend fun setAutoHeartRateMonitoring(enabled: Boolean, interval: Int = 30): Boolean
    
    /**
     * 실시간 데이터 스트림
     */
    
    /**
     * Subscribe to all health data updates
     * @return Flow of health data updates
     */
    fun subscribeToHealthDataUpdates(): Flow<HealthDataUpdate>
    
    /**
     * Subscribe to device status updates
     * @return Flow of device status
     */
    fun subscribeToDeviceStatus(): Flow<DeviceStatus>
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

/**
 * Vibration pattern enum
 */
enum class VibrationPattern {
    SHORT,
    LONG,
    DOUBLE,
    TRIPLE,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * Device information data class
 */
data class DeviceInfo(
    val name: String,
    val address: String,
    val firmwareVersion: String,
    val hardwareVersion: String,
    val batteryLevel: Int,
    val serialNumber: String?,
    val manufacturer: String?
)