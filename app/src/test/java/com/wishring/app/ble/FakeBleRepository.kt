package com.wishring.app.ble

import com.wishring.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

/**
 * Fake BLE Repository for integration testing
 * 
 * Simulates real BLE behavior without requiring hardware
 * Useful for integration tests that need predictable BLE interactions
 */
class FakeBleRepository(
    private val simulateLatency: Boolean = true,
    private val simulateErrors: Boolean = false,
    private val errorRate: Float = 0.1f // 10% error rate when enabled
) : BleRepository {
    
    // Internal state
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    private val _buttonPressEvents = MutableSharedFlow<ButtonPressEvent>()
    private val _notifications = MutableSharedFlow<BleNotification>()
    private val _counterIncrements = MutableSharedFlow<Int>()
    
    // Simulated device data
    private var connectedDevice: BleDevice? = null
    private var deviceWishCount: Int = 0
    private var deviceWishText: String = ""
    private var deviceTargetCount: Int = 1000
    private var deviceIsCompleted: Boolean = false
    private var deviceBatteryLevel: Int = 85
    private var deviceTime: Long = System.currentTimeMillis()
    
    // Available devices for scanning
    private val availableDevices = listOf(
        BleDevice("WISH RING 001", "00:11:22:33:44:55", -45, true, false),
        BleDevice("WISH RING 002", "66:77:88:99:AA:BB", -60, true, false),
        BleDevice("WISH RING 003", "CC:DD:EE:FF:00:11", -75, true, true)
    )
    
    // Simulation control
    private var isScanning = false
    private val scanJob = MutableStateFlow<Job?>(null)
    
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    override val buttonPressEvents: Flow<ButtonPressEvent> = _buttonPressEvents.asSharedFlow()
    override val notifications: Flow<BleNotification> = _notifications.asSharedFlow()
    override val counterIncrements: Flow<Int> = _counterIncrements.asSharedFlow()
    
    // ===== Connection Management =====
    
    override fun startScanning(timeout: Long): Flow<BleDevice> = flow {
        if (shouldSimulateError()) {
            throw RuntimeException("Simulated scan error")
        }
        
        isScanning = true
        _connectionState.value = BleConnectionState.DISCONNECTED
        
        try {
            val scanTimeout = if (timeout <= 0) Long.MAX_VALUE else timeout
            val startTime = System.currentTimeMillis()
            
            for (device in availableDevices) {
                if (!isScanning) break
                if (System.currentTimeMillis() - startTime > scanTimeout) break
                
                delay(if (simulateLatency) Random.nextLong(500, 1500) else 100)
                emit(device)
            }
        } finally {
            isScanning = false
        }
    }
    
    override suspend fun stopScanning() {
        isScanning = false
        scanJob.value?.cancel()
        scanJob.value = null
    }
    
    override suspend fun connectDevice(deviceAddress: String): Boolean {
        if (shouldSimulateError()) return false
        
        val device = availableDevices.find { it.address == deviceAddress }
            ?: return false
        
        _connectionState.value = BleConnectionState.CONNECTING
        
        if (simulateLatency) delay(Random.nextLong(1000, 3000))
        
        return if (Random.nextFloat() > 0.1f || !simulateErrors) { // 90% success rate
            connectedDevice = device
            _connectionState.value = BleConnectionState.CONNECTED
            _batteryLevel.value = deviceBatteryLevel
            
            // Start simulating counter increments when connected
            startCounterSimulation()
            
            true
        } else {
            _connectionState.value = BleConnectionState.ERROR
            false
        }
    }
    
    override suspend fun disconnectDevice() {
        if (simulateLatency) delay(Random.nextLong(200, 800))
        
        _connectionState.value = BleConnectionState.DISCONNECTING
        delay(100)
        
        connectedDevice = null
        _connectionState.value = BleConnectionState.DISCONNECTED
        _batteryLevel.value = null
    }
    
    override suspend fun isDeviceConnected(): Boolean {
        return _connectionState.value == BleConnectionState.CONNECTED
    }
    
    override suspend fun getConnectedDevice(): BleDevice? {
        return if (isDeviceConnected()) connectedDevice else null
    }
    
    // ===== Data Synchronization =====
    
    override suspend fun sendWishCount(count: Int): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(100, 300))
        
        deviceWishCount = count
        return true
    }
    
    override suspend fun sendWishText(text: String): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(150, 400))
        
        deviceWishText = text.take(20) // Simulate device limitation
        return true
    }
    
    override suspend fun sendTargetCount(target: Int): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(100, 300))
        
        deviceTargetCount = target
        return true
    }
    
    override suspend fun sendCompletionStatus(isCompleted: Boolean): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(100, 300))
        
        deviceIsCompleted = isCompleted
        return true
    }
    
    override suspend fun syncAllData(
        wishCount: Int,
        wishText: String,
        targetCount: Int,
        isCompleted: Boolean
    ): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(500, 1200))
        
        deviceWishCount = wishCount
        deviceWishText = wishText.take(20)
        deviceTargetCount = targetCount
        deviceIsCompleted = isCompleted
        
        return true
    }
    
    override suspend fun readWishCount(): Int? {
        if (!isDeviceConnected()) return null
        if (shouldSimulateError()) return null
        
        if (simulateLatency) delay(Random.nextLong(100, 300))
        
        return deviceWishCount
    }
    
    override suspend fun readButtonPressCount(): Int? {
        if (!isDeviceConnected()) return null
        if (shouldSimulateError()) return null
        
        if (simulateLatency) delay(Random.nextLong(100, 300))
        
        return Random.nextInt(0, 50) // Simulate some button presses
    }
    
    // ===== Battery Operations =====
    
    override suspend fun getBatteryLevel(): Int? {
        if (!isDeviceConnected()) return null
        if (shouldSimulateError()) return null
        
        if (simulateLatency) delay(Random.nextLong(100, 200))
        
        return deviceBatteryLevel
    }
    
    override fun getBatteryLevelFlow(): StateFlow<Int?> = _batteryLevel.asStateFlow()
    
    override suspend fun requestBatteryUpdate(): Result<Unit> {
        if (!isDeviceConnected()) {
            return Result.failure(IllegalStateException("Device not connected"))
        }
        
        if (shouldSimulateError()) {
            return Result.failure(RuntimeException("Simulated battery update error"))
        }
        
        if (simulateLatency) delay(Random.nextLong(200, 500))
        
        // Simulate battery drain over time
        deviceBatteryLevel = (deviceBatteryLevel - Random.nextInt(0, 3)).coerceAtLeast(0)
        _batteryLevel.value = deviceBatteryLevel
        
        return Result.success(Unit)
    }
    
    // ===== Basic Device Operations =====
    
    override suspend fun updateDeviceTime(): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(200, 500))
        
        deviceTime = System.currentTimeMillis()
        return true
    }
    
    override suspend fun resetDevice(): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(1000, 2000))
        
        // Reset device state
        deviceWishCount = 0
        deviceWishText = ""
        deviceTargetCount = 1000
        deviceIsCompleted = false
        deviceBatteryLevel = 100
        
        return true
    }
    
    override suspend fun enableNotifications(): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(100, 300))
        
        // Start notification simulation
        startNotificationSimulation()
        
        return true
    }
    
    override suspend fun disableNotifications(): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(100, 300))
        
        return true
    }
    
    override suspend fun testConnection(): Boolean {
        if (!isDeviceConnected()) return false
        if (shouldSimulateError()) return false
        
        if (simulateLatency) delay(Random.nextLong(50, 150))
        
        return true
    }
    
    override suspend fun clearBondedDevices() {
        if (simulateLatency) delay(Random.nextLong(500, 1000))
        
        // Simulate clearing bonded devices
        // In real implementation, this would affect the bonded status of devices
    }
    
    // ===== Simulation Methods =====
    
    /**
     * Start simulating counter increments from the device
     */
    private fun startCounterSimulation() {
        if (!isDeviceConnected()) return
        
        GlobalScope.launch {
            while (isDeviceConnected()) {
                delay(Random.nextLong(5000, 15000)) // Random button press every 5-15 seconds
                
                if (isDeviceConnected()) {
                    val increment = Random.nextInt(1, 4) // 1-3 increments
                    _counterIncrements.emit(increment)
                    
                    // Also emit button press event
                    _buttonPressEvents.emit(
                        ButtonPressEvent(
                            timestamp = System.currentTimeMillis(),
                            pressCount = increment,
                            pressType = when (increment) {
                                1 -> PressType.SINGLE
                                2 -> PressType.DOUBLE
                                3 -> PressType.TRIPLE
                                else -> PressType.LONG
                            }
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Start simulating notifications from the device
     */
    private fun startNotificationSimulation() {
        if (!isDeviceConnected()) return
        
        GlobalScope.launch {
            delay(2000) // Initial delay
            
            while (isDeviceConnected()) {
                delay(Random.nextLong(10000, 30000)) // Random notification every 10-30 seconds
                
                if (isDeviceConnected()) {
                    val notificationType = when (Random.nextInt(4)) {
                        0 -> NotificationType.BUTTON_PRESS
                        1 -> NotificationType.LOW_BATTERY
                        2 -> NotificationType.DATA_SYNC
                        else -> NotificationType.CONNECTION_LOST
                    }
                    
                    _notifications.emit(
                        BleNotification(
                            type = notificationType,
                            message = "Simulated ${notificationType.name} notification",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Simulate button press from device
     */
    suspend fun simulateButtonPress(pressType: PressType = PressType.SINGLE) {
        if (!isDeviceConnected()) return
        
        val pressCount = when (pressType) {
            PressType.SINGLE -> 1
            PressType.DOUBLE -> 2
            PressType.TRIPLE -> 3
            PressType.LONG -> 1
        }
        
        _counterIncrements.emit(pressCount)
        _buttonPressEvents.emit(
            ButtonPressEvent(
                timestamp = System.currentTimeMillis(),
                pressCount = pressCount,
                pressType = pressType
            )
        )
    }
    
    /**
     * Simulate low battery notification
     */
    suspend fun simulateLowBattery() {
        if (!isDeviceConnected()) return
        
        deviceBatteryLevel = Random.nextInt(5, 15) // Low battery level
        _batteryLevel.value = deviceBatteryLevel
        
        _notifications.emit(
            BleNotification(
                type = NotificationType.LOW_BATTERY,
                message = "Battery level: $deviceBatteryLevel%",
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Simulate connection error
     */
    suspend fun simulateConnectionError() {
        _connectionState.value = BleConnectionState.ERROR
        _notifications.emit(
            BleNotification(
                type = NotificationType.CONNECTION_LOST,
                message = "Connection lost",
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    // ===== Helper Methods =====
    
    private fun shouldSimulateError(): Boolean {
        return simulateErrors && Random.nextFloat() < errorRate
    }
    
    /**
     * Get current device state (for testing purposes)
     */
    fun getDeviceState(): DeviceState {
        return DeviceState(
            wishCount = deviceWishCount,
            wishText = deviceWishText,
            targetCount = deviceTargetCount,
            isCompleted = deviceIsCompleted,
            batteryLevel = deviceBatteryLevel,
            deviceTime = deviceTime
        )
    }
    
    /**
     * Set device state (for testing purposes)
     */
    fun setDeviceState(state: DeviceState) {
        deviceWishCount = state.wishCount
        deviceWishText = state.wishText
        deviceTargetCount = state.targetCount
        deviceIsCompleted = state.isCompleted
        deviceBatteryLevel = state.batteryLevel
        deviceTime = state.deviceTime
        
        if (isDeviceConnected()) {
            _batteryLevel.value = deviceBatteryLevel
        }
    }
    
    /**
     * Reset all simulation state
     */
    fun resetSimulation() {
        _connectionState.value = BleConnectionState.DISCONNECTED
        _batteryLevel.value = null
        connectedDevice = null
        deviceWishCount = 0
        deviceWishText = ""
        deviceTargetCount = 1000
        deviceIsCompleted = false
        deviceBatteryLevel = 85
        deviceTime = System.currentTimeMillis()
        isScanning = false
    }
}

/**
 * Device state data class for testing
 */
data class DeviceState(
    val wishCount: Int = 0,
    val wishText: String = "",
    val targetCount: Int = 1000,
    val isCompleted: Boolean = false,
    val batteryLevel: Int = 85,
    val deviceTime: Long = System.currentTimeMillis()
)