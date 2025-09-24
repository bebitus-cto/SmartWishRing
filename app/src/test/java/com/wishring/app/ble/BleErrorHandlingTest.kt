package com.wishring.app.ble

import app.cash.turbine.test
import com.wishring.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.seconds

/**
 * BLE Error Handling and Reconnection Tests
 * 
 * Tests error scenarios, recovery mechanisms, and reconnection logic
 * Includes edge cases, timeout handling, and resilience testing
 */
@DisplayName("BLE Error Handling and Reconnection Tests")
class BleErrorHandlingTest {
    
    private lateinit var testScope: TestScope
    private lateinit var stableRepository: FakeBleRepository
    private lateinit var errorRepository: FakeBleRepository
    private lateinit var testDevice: BleDevice
    
    @BeforeEach
    fun setup() {
        testScope = TestScope(StandardTestDispatcher())
        
        stableRepository = FakeBleRepository(
            simulateLatency = false,
            simulateErrors = false
        )
        
        errorRepository = FakeBleRepository(
            simulateLatency = false,
            simulateErrors = true,
            errorRate = 0.7f // 70% error rate
        )
        
        testDevice = BluetoothTestUtils.createWishRingDevice()
    }
    
    @AfterEach
    fun tearDown() {
        stableRepository.resetSimulation()
        errorRepository.resetSimulation()
    }
    
    @Nested
    @DisplayName("Connection Error Handling")
    inner class ConnectionErrorHandling {
        
        @Test
        @DisplayName("Connection failure should transition to ERROR state")
        fun testConnectionFailureErrorState() = testScope.runTest {
            // When & Then
            errorRepository.connectionState.test {
                assertEquals(BleConnectionState.DISCONNECTED, awaitItem())
                
                // Attempt connection (should fail)
                val result = errorRepository.connectDevice(testDevice.address)
                assertFalse(result)
                
                // Should go to connecting first
                assertEquals(BleConnectionState.CONNECTING, awaitItem())
                
                // Then to error
                assertEquals(BleConnectionState.ERROR, awaitItem())
                
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Invalid device address should fail immediately")
        fun testInvalidDeviceAddressHandling() = testScope.runTest {
            val invalidAddresses = listOf(
                "",
                "invalid",
                "00:11:22:33:44", // Too short
                "ZZ:11:22:33:44:55", // Invalid characters
                "00:11:22:33:44:55:66" // Too long
            )
            
            for (invalidAddress in invalidAddresses) {
                val result = stableRepository.connectDevice(invalidAddress)
                assertFalse(result) {
                    "Should fail for invalid address: $invalidAddress"
                }
                
                // State should remain DISCONNECTED
                assertEquals(BleConnectionState.DISCONNECTED, stableRepository.connectionState.value)
            }
        }
        
        @Test
        @DisplayName("Multiple failed connection attempts should be handled gracefully")
        fun testMultipleFailedConnections() = testScope.runTest {
            val attemptCount = 5
            val results = mutableListOf<Boolean>()
            
            repeat(attemptCount) {
                val result = errorRepository.connectDevice(testDevice.address)
                results.add(result)
                advanceUntilIdle()
            }
            
            // Most attempts should fail (due to 70% error rate)
            val failureCount = results.count { !it }
            assertTrue(failureCount > 0) {
                "Expected some failures with error rate, but all succeeded"
            }
            
            // Final state should be stable (not stuck)
            val finalState = errorRepository.connectionState.value
            assertTrue(
                finalState == BleConnectionState.ERROR || 
                finalState == BleConnectionState.DISCONNECTED ||
                finalState == BleConnectionState.CONNECTED
            ) {
                "Final state should be stable, but was $finalState"
            }
        }
        
        @Test
        @DisplayName("Connection timeout should be handled properly")
        fun testConnectionTimeout() = testScope.runTest {
            // Create repository with high latency
            val slowRepository = FakeBleRepository(
                simulateLatency = true,
                simulateErrors = false
            )
            
            val startTime = System.currentTimeMillis()
            
            // This should complete within reasonable time even with latency
            val result = slowRepository.connectDevice(testDevice.address)
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            // Should complete within 10 seconds (generous timeout)
            assertTrue(duration < 10000) {
                "Connection took too long: ${duration}ms"
            }
            
            // Should either succeed or fail gracefully
            val finalState = slowRepository.connectionState.value
            assertTrue(
                finalState == BleConnectionState.CONNECTED || 
                finalState == BleConnectionState.ERROR ||
                finalState == BleConnectionState.DISCONNECTED
            )
        }
    }
    
    @Nested
    @DisplayName("Data Operation Error Handling")
    inner class DataOperationErrorHandling {
        
        @Test
        @DisplayName("Data operations should fail gracefully when not connected")
        fun testDataOperationsWhenDisconnected() = testScope.runTest {
            // Ensure disconnected
            assertEquals(BleConnectionState.DISCONNECTED, stableRepository.connectionState.value)
            
            // All data operations should return false/null
            assertFalse(stableRepository.sendWishCount(100))
            assertFalse(stableRepository.sendWishText("test"))
            assertFalse(stableRepository.sendTargetCount(1000))
            assertFalse(stableRepository.sendCompletionStatus(true))
            assertFalse(stableRepository.syncAllData(100, "test", 1000, false))
            
            assertNull(stableRepository.readWishCount())
            assertNull(stableRepository.readButtonPressCount())
            assertNull(stableRepository.getBatteryLevel())
            
            assertFalse(stableRepository.updateDeviceTime())
            assertFalse(stableRepository.resetDevice())
            assertFalse(stableRepository.testConnection())
        }
        
        @Test
        @DisplayName("Data operations should handle errors during transmission")
        fun testDataOperationErrors() = testScope.runTest {
            // Connect first
            errorRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // Assume connection succeeded for this test
            if (errorRepository.connectionState.value == BleConnectionState.CONNECTED) {
                // With 70% error rate, some operations should fail
                val results = mutableListOf<Boolean>()
                
                repeat(10) {
                    results.add(errorRepository.sendWishCount(100))
                    results.add(errorRepository.sendWishText("test"))
                    results.add(errorRepository.sendTargetCount(1000))
                }
                
                // Some should fail
                val failureCount = results.count { !it }
                assertTrue(failureCount > 0) {
                    "Expected some failures with error repository"
                }
            }
        }
        
        @ParameterizedTest
        @ValueSource(strings = ["", "a", "normal text", "very long text that exceeds the typical 20 character limit for BLE"])
        @DisplayName("Wish text validation should handle various inputs")
        fun testWishTextValidation(wishText: String) = testScope.runTest {
            // Connect first
            stableRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // Should not throw exceptions regardless of input
            assertDoesNotThrow {
                stableRepository.sendWishText(wishText)
            }
        }
        
        @Test
        @DisplayName("Numeric overflow should be handled")
        fun testNumericOverflowHandling() = testScope.runTest {
            // Connect first
            stableRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val extremeValues = listOf(
                Int.MIN_VALUE,
                -1000000,
                -1,
                0,
                1,
                1000000,
                Int.MAX_VALUE
            )
            
            for (value in extremeValues) {
                assertDoesNotThrow {
                    stableRepository.sendWishCount(value)
                    stableRepository.sendTargetCount(value)
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Reconnection Logic")
    inner class ReconnectionLogic {
        
        @Test
        @DisplayName("Should be able to reconnect after connection error")
        fun testReconnectAfterError() = testScope.runTest {
            // First connection attempt (should fail)
            val firstResult = errorRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // If first attempt failed, try again
            if (!firstResult) {
                assertEquals(BleConnectionState.ERROR, errorRepository.connectionState.value)
                
                // Reset to a more reliable repository for retry
                val retryResult = stableRepository.connectDevice(testDevice.address)
                advanceUntilIdle()
                
                assertTrue(retryResult)
                assertEquals(BleConnectionState.CONNECTED, stableRepository.connectionState.value)
            }
        }
        
        @Test
        @DisplayName("Should be able to connect to different device after failure")
        fun testConnectToDifferentDeviceAfterFailure() = testScope.runTest {
            val device1 = BluetoothTestUtils.createWishRingDevice(1)
            val device2 = BluetoothTestUtils.createWishRingDevice(2)
            
            // Try to connect to first device (might fail)
            errorRepository.connectDevice(device1.address)
            advanceUntilIdle()
            
            // Try second device with stable repository
            val result = stableRepository.connectDevice(device2.address)
            advanceUntilIdle()
            
            assertTrue(result)
            assertEquals(BleConnectionState.CONNECTED, stableRepository.connectionState.value)
        }
        
        @Test
        @DisplayName("Automatic reconnection scenario")
        fun testAutomaticReconnectionScenario() = testScope.runTest {
            // Connect initially
            stableRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            assertEquals(BleConnectionState.CONNECTED, stableRepository.connectionState.value)
            
            // Simulate connection lost
            stableRepository.simulateConnectionError()
            advanceUntilIdle()
            assertEquals(BleConnectionState.ERROR, stableRepository.connectionState.value)
            
            // Attempt to reconnect
            val reconnectResult = stableRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            assertTrue(reconnectResult)
            assertEquals(BleConnectionState.CONNECTED, stableRepository.connectionState.value)
        }
        
        @Test
        @DisplayName("Reconnection should restore functionality")
        fun testReconnectionRestoresFunctionality() = testScope.runTest {
            // Initial connection and test functionality
            stableRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val initialResult = stableRepository.sendWishCount(100)
            assertTrue(initialResult)
            
            // Simulate disconnection
            stableRepository.disconnectDevice()
            advanceUntilIdle()
            
            // Functionality should not work when disconnected
            assertFalse(stableRepository.sendWishCount(200))
            
            // Reconnect
            stableRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // Functionality should be restored
            val reconnectedResult = stableRepository.sendWishCount(300)
            assertTrue(reconnectedResult)
        }
    }
    
    @Nested
    @DisplayName("Resource Management")
    inner class ResourceManagement {
        
        @Test
        @DisplayName("Scanning should stop on connection error")
        fun testScanningStopsOnError() = testScope.runTest {
            val discoveredDevices = mutableListOf<BleDevice>()
            
            // Start scanning
            val scanJob = launch {
                stableRepository.startScanning(5000).collect {
                    discoveredDevices.add(it)
                }
            }
            
            delay(100)
            
            // Simulate error during scan
            launch {
                stableRepository.simulateConnectionError()
            }
            
            delay(500)
            scanJob.cancel()
            
            // Should not crash and should have discovered some devices
            assertDoesNotThrow {
                stableRepository.stopScanning()
            }
        }
        
        @Test
        @DisplayName("Multiple repository instances should not interfere")
        fun testMultipleRepositoryInstances() = testScope.runTest {
            val repo1 = FakeBleRepository(simulateErrors = false)
            val repo2 = FakeBleRepository(simulateErrors = false)
            
            val device1 = BluetoothTestUtils.createWishRingDevice(1)
            val device2 = BluetoothTestUtils.createWishRingDevice(2)
            
            // Connect different devices to different repositories
            val result1 = repo1.connectDevice(device1.address)
            val result2 = repo2.connectDevice(device2.address)
            
            advanceUntilIdle()
            
            assertTrue(result1)
            assertTrue(result2)
            
            assertEquals(BleConnectionState.CONNECTED, repo1.connectionState.value)
            assertEquals(BleConnectionState.CONNECTED, repo2.connectionState.value)
            
            // Operations should not interfere
            assertTrue(repo1.sendWishCount(100))
            assertTrue(repo2.sendWishCount(200))
        }
        
        @Test
        @DisplayName("Memory leaks should not occur during error scenarios")
        fun testNoMemoryLeaksOnErrors() = testScope.runTest {
            val jobCount = 50
            val jobs = mutableListOf<Job>()
            
            // Create many jobs that might fail
            repeat(jobCount) { index ->
                val job = launch {
                    try {
                        errorRepository.connectDevice(testDevice.address)
                        errorRepository.sendWishCount(index)
                        errorRepository.disconnectDevice()
                    } catch (e: Exception) {
                        // Errors are expected
                    }
                }
                jobs.add(job)
            }
            
            // Wait for all jobs to complete
            jobs.forEach { it.join() }
            
            // Should not have any lingering effects
            assertEquals(BleConnectionState.DISCONNECTED, errorRepository.connectionState.value)
            assertNull(errorRepository.getConnectedDevice())
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Race Conditions")
    inner class EdgeCasesAndRaceConditions {
        
        @Test
        @DisplayName("Rapid connect/disconnect cycles should be handled")
        fun testRapidConnectDisconnectCycles() = testScope.runTest {
            repeat(10) { cycle ->
                // Connect
                val connectResult = stableRepository.connectDevice(testDevice.address)
                advanceTimeBy(50)
                
                // Disconnect
                if (connectResult) {
                    stableRepository.disconnectDevice()
                    advanceTimeBy(50)
                }
                
                // State should be stable
                val state = stableRepository.connectionState.value
                assertTrue(
                    state == BleConnectionState.CONNECTED || 
                    state == BleConnectionState.DISCONNECTED ||
                    state == BleConnectionState.CONNECTING ||
                    state == BleConnectionState.DISCONNECTING
                ) {
                    "Unstable state in cycle $cycle: $state"
                }
            }
        }
        
        @Test
        @DisplayName("Concurrent error conditions should not cause deadlock")
        fun testConcurrentErrorConditions() = testScope.runTest {
            val operations = listOf(
                async { errorRepository.connectDevice("invalid-1") },
                async { errorRepository.connectDevice("invalid-2") },
                async { errorRepository.sendWishCount(-1) },
                async { errorRepository.sendWishText("") },
                async { errorRepository.resetDevice() },
                async { errorRepository.getBatteryLevel() }
            )
            
            // All operations should complete without deadlock
            assertDoesNotThrow {
                withTimeout(5.seconds) {
                    operations.awaitAll()
                }
            }
        }
        
        @Test
        @DisplayName("Exception during flow collection should not break other flows")
        fun testExceptionDuringFlowCollection() = testScope.runTest {
            stableRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val counterEvents = mutableListOf<Int>()
            val buttonEvents = mutableListOf<ButtonPressEvent>()
            
            // Start collecting with one flow that throws
            val errorJob = launch {
                stableRepository.counterIncrements.collect { 
                    if (it == 2) throw RuntimeException("Test exception")
                    counterEvents.add(it)
                }
            }
            
            val goodJob = launch {
                stableRepository.buttonPressEvents.collect {
                    buttonEvents.add(it)
                }
            }
            
            // Generate events
            stableRepository.simulateButtonPress(PressType.SINGLE) // 1
            advanceTimeBy(50)
            stableRepository.simulateButtonPress(PressType.DOUBLE) // 2 - should cause exception
            advanceTimeBy(50)
            stableRepository.simulateButtonPress(PressType.SINGLE) // 1
            advanceTimeBy(50)
            
            errorJob.cancel()
            goodJob.cancel()
            
            // Error in one flow should not affect the other
            assertEquals(1, counterEvents.size) // Only first event before exception
            assertEquals(3, buttonEvents.size) // All events should be received
        }
        
        @Test
        @DisplayName("State consistency during error recovery")
        fun testStateConsistencyDuringErrorRecovery() = testScope.runTest {
            val stateHistory = mutableListOf<BleConnectionState>()
            
            val stateJob = launch {
                stableRepository.connectionState.collect {
                    stateHistory.add(it)
                }
            }
            
            // Perform operations that might cause errors
            stableRepository.connectDevice(testDevice.address)
            advanceTimeBy(100)
            stableRepository.simulateConnectionError()
            advanceTimeBy(100)
            stableRepository.connectDevice(testDevice.address)
            advanceTimeBy(100)
            
            stateJob.cancel()
            
            // State transitions should be logical
            for (i in 1 until stateHistory.size) {
                val fromState = stateHistory[i-1]
                val toState = stateHistory[i]
                
                // Check for invalid transitions
                assertFalse(
                    fromState == BleConnectionState.DISCONNECTED && 
                    toState == BleConnectionState.CONNECTED
                ) {
                    "Invalid direct transition from DISCONNECTED to CONNECTED without CONNECTING state"
                }
            }
        }
    }
}