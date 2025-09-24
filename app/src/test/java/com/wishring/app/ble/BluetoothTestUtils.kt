package com.wishring.app.ble

import app.cash.turbine.test
import com.wishring.app.data.repository.*
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.TestScope
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Bluetooth Testing Utilities
 * 
 * Provides common utilities and helpers for Bluetooth-related testing
 * Including device creation, flow testing, and assertion helpers
 */
object BluetoothTestUtils {
    
    // ===== Test Device Creation =====
    
    /**
     * Create a test BLE device with customizable properties
     */
    fun createTestDevice(
        name: String = "WISH RING Test",
        address: String = generateMacAddress(),
        rssi: Int = Random.nextInt(-90, -30),
        isConnectable: Boolean = true,
        isBonded: Boolean = false
    ): BleDevice {
        return BleDevice(
            name = name,
            address = address,
            rssi = rssi,
            isConnectable = isConnectable,
            isBonded = isBonded
        )
    }
    
    /**
     * Create multiple test devices
     */
    fun createTestDevices(count: Int): List<BleDevice> {
        return (1..count).map { index ->
            createTestDevice(
                name = "WISH RING $index",
                address = generateMacAddress(),
                rssi = Random.nextInt(-90, -30)
            )
        }
    }
    
    /**
     * Create a test device that appears as WISH RING device
     */
    fun createWishRingDevice(
        deviceId: Int = 1,
        batteryLevel: Int = Random.nextInt(20, 100),
        isBonded: Boolean = false
    ): BleDevice {
        return BleDevice(
            name = "WISH RING ${String.format("%03d", deviceId)}",
            address = generateMacAddress(),
            rssi = Random.nextInt(-60, -30), // Good signal strength
            isConnectable = true,
            isBonded = isBonded
        )
    }
    
    // ===== Test Event Creation =====
    
    /**
     * Create a test button press event
     */
    fun createButtonPressEvent(
        pressType: PressType = PressType.SINGLE,
        timestamp: Long = System.currentTimeMillis()
    ): ButtonPressEvent {
        val pressCount = when (pressType) {
            PressType.SINGLE -> 1
            PressType.DOUBLE -> 2
            PressType.TRIPLE -> 3
            PressType.LONG -> 1
        }
        
        return ButtonPressEvent(
            timestamp = timestamp,
            pressCount = pressCount,
            pressType = pressType
        )
    }
    
    /**
     * Create a test BLE notification
     */
    fun createNotification(
        type: NotificationType = NotificationType.BUTTON_PRESS,
        message: String = "Test notification",
        timestamp: Long = System.currentTimeMillis()
    ): BleNotification {
        return BleNotification(
            type = type,
            message = message,
            timestamp = timestamp
        )
    }
    
    // ===== Flow Testing Helpers =====
    
    /**
     * Test a StateFlow for expected state transitions
     */
    suspend fun <T> StateFlow<T>.testStateTransitions(
        testScope: TestScope,
        expectedStates: List<T>,
        timeout: Duration = 5.seconds
    ) {
        test(timeout = timeout) {
            expectedStates.forEach { expectedState ->
                val actualState = awaitItem()
                assert(actualState == expectedState) {
                    "Expected state: $expectedState, but got: $actualState"
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    /**
     * Test a Flow for specific emissions within a timeout
     */
    suspend fun <T> Flow<T>.testEmissions(
        expectedItems: List<T>,
        timeout: Duration = 5.seconds,
        exactMatch: Boolean = true
    ) {
        test(timeout = timeout) {
            if (exactMatch) {
                expectedItems.forEach { expected ->
                    val actual = awaitItem()
                    assert(actual == expected) {
                        "Expected: $expected, but got: $actual"
                    }
                }
                awaitComplete()
            } else {
                val receivedItems = mutableListOf<T>()
                while (receivedItems.size < expectedItems.size) {
                    receivedItems.add(awaitItem())
                }
                
                assert(receivedItems.containsAll(expectedItems)) {
                    "Expected items: $expectedItems not found in: $receivedItems"
                }
            }
        }
    }
    
    /**
     * Test that a flow emits no events within a given timeout
     */
    suspend fun <T> Flow<T>.testNoEmissions(timeout: Duration = 1.seconds) {
        test(timeout = timeout) {
            expectNoEvents()
        }
    }
    
    /**
     * Test that a flow emits at least one event within timeout
     */
    suspend fun <T> Flow<T>.testHasEmissions(timeout: Duration = 5.seconds): T {
        var result: T? = null
        test(timeout = timeout) {
            result = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return result!!
    }
    
    // ===== Connection Testing Helpers =====
    
    /**
     * Simulate a connection sequence with state transitions
     */
    suspend fun simulateConnectionSequence(
        repository: BleRepository,
        deviceAddress: String,
        shouldSucceed: Boolean = true
    ): List<BleConnectionState> {
        val states = mutableListOf<BleConnectionState>()
        
        repository.connectionState.test {
            // Initial state
            states.add(awaitItem())
            
            // Start connection
            repository.connectDevice(deviceAddress)
            
            // Connecting state
            val connectingState = awaitItem()
            states.add(connectingState)
            
            if (shouldSucceed) {
                // Connected state
                val connectedState = awaitItem()
                states.add(connectedState)
            } else {
                // Error state
                val errorState = awaitItem()
                states.add(errorState)
            }
            
            cancelAndIgnoreRemainingEvents()
        }
        
        return states
    }
    
    /**
     * Test device scanning with timeout
     */
    suspend fun testDeviceScanning(
        repository: BleRepository,
        timeout: Long = 5000L,
        expectedDeviceCount: Int? = null
    ): List<BleDevice> {
        val discoveredDevices = mutableListOf<BleDevice>()
        
        repository.startScanning(timeout).test {
            while (true) {
                try {
                    val device = awaitItem()
                    discoveredDevices.add(device)
                    
                    if (expectedDeviceCount != null && discoveredDevices.size >= expectedDeviceCount) {
                        repository.stopScanning()
                        break
                    }
                } catch (e: Exception) {
                    break
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
        
        return discoveredDevices
    }
    
    // ===== Assertion Helpers =====
    
    /**
     * Assert that a BLE device matches expected criteria
     */
    fun assertDeviceMatches(
        device: BleDevice,
        expectedName: String? = null,
        expectedAddress: String? = null,
        minRssi: Int? = null,
        maxRssi: Int? = null,
        shouldBeConnectable: Boolean? = null,
        shouldBeBonded: Boolean? = null
    ) {
        expectedName?.let { 
            assert(device.name == it) { "Expected name: $it, got: ${device.name}" }
        }
        
        expectedAddress?.let {
            assert(device.address == it) { "Expected address: $it, got: ${device.address}" }
        }
        
        minRssi?.let {
            assert(device.rssi >= it) { "RSSI ${device.rssi} is below minimum $it" }
        }
        
        maxRssi?.let {
            assert(device.rssi <= it) { "RSSI ${device.rssi} is above maximum $it" }
        }
        
        shouldBeConnectable?.let {
            assert(device.isConnectable == it) { "Expected connectable: $it, got: ${device.isConnectable}" }
        }
        
        shouldBeBonded?.let {
            assert(device.isBonded == it) { "Expected bonded: $it, got: ${device.isBonded}" }
        }
    }
    
    /**
     * Assert that connection state is as expected
     */
    fun assertConnectionState(
        actual: BleConnectionState,
        expected: BleConnectionState
    ) {
        assert(actual == expected) {
            "Expected connection state: $expected, but got: $actual"
        }
    }
    
    /**
     * Assert that battery level is within valid range
     */
    fun assertValidBatteryLevel(batteryLevel: Int?) {
        assert(batteryLevel != null) { "Battery level should not be null" }
        assert(batteryLevel!! in 0..100) { "Battery level $batteryLevel is not in valid range 0-100" }
    }
    
    /**
     * Assert that wish text meets device constraints
     */
    fun assertValidWishText(wishText: String, maxLength: Int = 20) {
        assert(wishText.length <= maxLength) { 
            "Wish text length ${wishText.length} exceeds maximum $maxLength" 
        }
    }
    
    /**
     * Assert that target count is within valid range
     */
    fun assertValidTargetCount(targetCount: Int, minCount: Int = 1, maxCount: Int = 9999) {
        assert(targetCount in minCount..maxCount) {
            "Target count $targetCount is not in valid range $minCount-$maxCount"
        }
    }
    
    // ===== Mock Helpers =====
    
    /**
     * Create a mock BLE repository with default behaviors
     */
    fun createMockBleRepository(
        initialConnectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
        shouldConnectSucceed: Boolean = true
    ): BleRepository = mockk {
        // Add default mock behaviors as needed
    }
    
    // ===== Timing Helpers =====
    
    /**
     * Wait for condition with timeout
     */
    suspend fun waitForCondition(
        timeout: Duration = 5.seconds,
        interval: Duration = Duration.parse("100ms"),
        condition: suspend () -> Boolean
    ) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = timeout.inWholeMilliseconds
        
        while (!condition() && (System.currentTimeMillis() - startTime) < timeoutMs) {
            delay(interval.inWholeMilliseconds)
        }
        
        assert(condition()) { "Condition not met within $timeout" }
    }
    
    /**
     * Measure execution time of a suspend function
     */
    suspend fun <T> measureTime(block: suspend () -> T): Pair<T, Duration> {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        return result to Duration.parse("${endTime - startTime}ms")
    }
    
    // ===== Random Data Generators =====
    
    /**
     * Generate a random MAC address
     */
    fun generateMacAddress(): String {
        return (1..6).joinToString(":") { 
            Random.nextInt(0, 256).toString(16).padStart(2, '0').uppercase()
        }
    }
    
    /**
     * Generate random wish text
     */
    fun generateWishText(maxLength: Int = 20): String {
        val wishes = listOf(
            "Success in career",
            "Good health",
            "Happy family",
            "Financial freedom",
            "Inner peace",
            "Travel the world",
            "Learn new skills",
            "Help others",
            "Find true love",
            "Achieve goals"
        )
        
        val wish = wishes.random()
        return if (wish.length > maxLength) wish.take(maxLength) else wish
    }
    
    /**
     * Generate random target count
     */
    fun generateTargetCount(): Int = Random.nextInt(100, 5000)
    
    /**
     * Generate random button press sequence
     */
    fun generateButtonPressSequence(length: Int = 5): List<PressType> {
        return (1..length).map {
            PressType.values().random()
        }
    }
    
    // ===== Test Data Validation =====
    
    /**
     * Validate that test data is consistent
     */
    fun validateTestData(
        device: BleDevice,
        connectionState: BleConnectionState,
        batteryLevel: Int?
    ) {
        // Validate device
        assert(device.name.isNotBlank()) { "Device name should not be blank" }
        assert(device.address.matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$"))) {
            "Invalid MAC address format: ${device.address}"
        }
        assert(device.rssi in -100..0) { "Invalid RSSI value: ${device.rssi}" }
        
        // Validate connection state consistency
        if (connectionState == BleConnectionState.CONNECTED) {
            batteryLevel?.let { assertValidBatteryLevel(it) }
        } else {
            assert(batteryLevel == null || batteryLevel == 0) {
                "Battery level should be null or 0 when not connected"
            }
        }
    }
    
    // ===== Performance Testing Helpers =====
    
    /**
     * Test operation performance
     */
    suspend fun testOperationPerformance(
        operation: suspend () -> Unit,
        expectedMaxTime: Duration,
        operationName: String = "Operation"
    ) {
        val (_, duration) = measureTime { operation() }
        assert(duration <= expectedMaxTime) {
            "$operationName took $duration, expected max $expectedMaxTime"
        }
    }
    
    /**
     * Test concurrent operations
     */
    suspend fun testConcurrentOperations(
        operations: List<suspend () -> Unit>,
        maxConcurrency: Int = operations.size
    ) {
        assert(operations.isNotEmpty()) { "No operations provided" }
        assert(maxConcurrency > 0) { "Max concurrency must be positive" }
        
        operations.chunked(maxConcurrency).forEach { chunk ->
            chunk.map { operation ->
                kotlinx.coroutines.async { operation() }
            }.forEach { 
                it.await() 
            }
        }
    }
}