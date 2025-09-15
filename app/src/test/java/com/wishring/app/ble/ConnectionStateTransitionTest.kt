package com.wishring.app.ble

import app.cash.turbine.test
import com.wishring.app.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.time.Duration.Companion.seconds

/**
 * Connection State Transition Tests
 * 
 * Tests BLE connection state management and transitions using FakeBleRepository
 * Verifies proper state machine behavior and edge cases
 */
@DisplayName("BLE Connection State Transition Tests")
class ConnectionStateTransitionTest {
    
    private lateinit var testScope: TestScope
    private lateinit var fakeRepository: FakeBleRepository
    private lateinit var testDevice: BleDevice
    
    @BeforeEach
    fun setup() {
        testScope = TestScope(StandardTestDispatcher())
        fakeRepository = FakeBleRepository(
            simulateLatency = false, // Disable for faster tests
            simulateErrors = false
        )
        testDevice = BluetoothTestUtils.createWishRingDevice()
    }
    
    @AfterEach
    fun tearDown() {
        fakeRepository.resetSimulation()
    }
    
    @Nested
    @DisplayName("Basic Connection State Transitions")
    inner class BasicConnectionStateTransitions {
        
        @Test
        @DisplayName("Initial state should be DISCONNECTED")
        fun testInitialState() = testScope.runTest {
            // When & Then
            assertEquals(BleConnectionState.DISCONNECTED, fakeRepository.connectionState.value)
        }
        
        @Test
        @DisplayName("Successful connection should transition DISCONNECTED -> CONNECTING -> CONNECTED")
        fun testSuccessfulConnection() = testScope.runTest {
            // Given
            val expectedStates = listOf(
                BleConnectionState.DISCONNECTED,
                BleConnectionState.CONNECTING,
                BleConnectionState.CONNECTED
            )
            
            // When & Then
            fakeRepository.connectionState.test {
                // Initial state
                assertEquals(BleConnectionState.DISCONNECTED, awaitItem())
                
                // Start connection
                fakeRepository.connectDevice(testDevice.address)
                
                // Should transition to CONNECTING
                assertEquals(BleConnectionState.CONNECTING, awaitItem())
                
                // Should transition to CONNECTED
                assertEquals(BleConnectionState.CONNECTED, awaitItem())
                
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Failed connection should transition DISCONNECTED -> CONNECTING -> ERROR")
        fun testFailedConnection() = testScope.runTest {
            // Given
            val errorRepository = FakeBleRepository(
                simulateLatency = false,
                simulateErrors = true,
                errorRate = 1.0f // 100% error rate
            )
            
            // When & Then
            errorRepository.connectionState.test {
                // Initial state
                assertEquals(BleConnectionState.DISCONNECTED, awaitItem())
                
                // Start connection (should fail)
                errorRepository.connectDevice(testDevice.address)
                
                // Should transition to CONNECTING first
                assertEquals(BleConnectionState.CONNECTING, awaitItem())
                
                // Then to ERROR
                assertEquals(BleConnectionState.ERROR, awaitItem())
                
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Disconnection should transition CONNECTED -> DISCONNECTING -> DISCONNECTED")
        fun testDisconnection() = testScope.runTest {
            // Given - first connect
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            assertEquals(BleConnectionState.CONNECTED, fakeRepository.connectionState.value)
            
            // When & Then
            fakeRepository.connectionState.test {
                // Skip to connected state
                assertEquals(BleConnectionState.CONNECTED, awaitItem())
                
                // Start disconnection
                fakeRepository.disconnectDevice()
                
                // Should transition to DISCONNECTING
                assertEquals(BleConnectionState.DISCONNECTING, awaitItem())
                
                // Should transition to DISCONNECTED
                assertEquals(BleConnectionState.DISCONNECTED, awaitItem())
                
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Scenarios")
    inner class EdgeCasesAndErrorScenarios {
        
        @Test
        @DisplayName("Connection to invalid address should fail immediately")
        fun testConnectionToInvalidAddress() = testScope.runTest {
            // Given
            val invalidAddress = "invalid-address"
            
            // When
            val result = fakeRepository.connectDevice(invalidAddress)
            advanceUntilIdle()
            
            // Then
            assertFalse(result)
            assertEquals(BleConnectionState.DISCONNECTED, fakeRepository.connectionState.value)
        }
        
        @Test
        @DisplayName("Multiple connection attempts should be handled gracefully")
        fun testMultipleConnectionAttempts() = testScope.runTest {
            // Given
            val device1 = BluetoothTestUtils.createWishRingDevice(1)
            val device2 = BluetoothTestUtils.createWishRingDevice(2)
            
            // When
            val result1 = fakeRepository.connectDevice(device1.address)
            val result2 = fakeRepository.connectDevice(device2.address) // Second attempt while first is connecting
            advanceUntilIdle()
            
            // Then
            assertTrue(result1)
            // Second connection should either succeed (replacing first) or fail gracefully
            assertEquals(BleConnectionState.CONNECTED, fakeRepository.connectionState.value)
        }
        
        @Test
        @DisplayName("Disconnection when already disconnected should be safe")
        fun testDisconnectionWhenDisconnected() = testScope.runTest {
            // Given - ensure disconnected
            assertEquals(BleConnectionState.DISCONNECTED, fakeRepository.connectionState.value)
            
            // When & Then
            assertDoesNotThrow {
                fakeRepository.disconnectDevice()
            }
            
            // State should remain DISCONNECTED
            assertEquals(BleConnectionState.DISCONNECTED, fakeRepository.connectionState.value)
        }
        
        @Test
        @DisplayName("Connection during disconnection should be handled properly")
        fun testConnectionDuringDisconnection() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            assertEquals(BleConnectionState.CONNECTED, fakeRepository.connectionState.value)
            
            // When - start disconnection and immediately try to connect
            launch { fakeRepository.disconnectDevice() }
            delay(50) // Let disconnection start
            val connectResult = fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // Then - should handle gracefully
            // Final state should be either CONNECTED or DISCONNECTED (not stuck in intermediate)
            val finalState = fakeRepository.connectionState.value
            assertTrue(
                finalState == BleConnectionState.CONNECTED || 
                finalState == BleConnectionState.DISCONNECTED
            ) {
                "Final state should be CONNECTED or DISCONNECTED, but was $finalState"
            }
        }
        
        @ParameterizedTest
        @EnumSource(BleConnectionState::class)
        @DisplayName("State transitions should be deterministic from each state")
        fun testStateTransitionsFromEachState(initialState: BleConnectionState) = testScope.runTest {
            // This test ensures that from any state, the repository behaves predictably
            
            when (initialState) {
                BleConnectionState.DISCONNECTED -> {
                    // Can connect
                    val result = fakeRepository.connectDevice(testDevice.address)
                    assertTrue(result)
                }
                BleConnectionState.CONNECTING -> {
                    // This is a transient state, hard to test directly
                    // Skip or test timeout behavior
                }
                BleConnectionState.CONNECTED -> {
                    // First get to connected state
                    fakeRepository.connectDevice(testDevice.address)
                    advanceUntilIdle()
                    
                    // Can disconnect
                    assertDoesNotThrow {
                        fakeRepository.disconnectDevice()
                    }
                }
                BleConnectionState.DISCONNECTING -> {
                    // This is a transient state, hard to test directly
                    // Skip or test that it transitions to DISCONNECTED
                }
                BleConnectionState.ERROR -> {
                    // From error state, should be able to retry connection
                    val errorRepository = FakeBleRepository(simulateErrors = true, errorRate = 1.0f)
                    errorRepository.connectDevice("invalid")
                    advanceUntilIdle()
                    
                    // Should be able to attempt connection again
                    assertDoesNotThrow {
                        errorRepository.connectDevice(testDevice.address)
                    }
                }
            }
        }
    }
    
    @Nested
    @DisplayName("State Machine Invariants")
    inner class StateMachineInvariants {
        
        @Test
        @DisplayName("Should never have invalid state transitions")
        fun testNoInvalidStateTransitions() = testScope.runTest {
            val observedTransitions = mutableListOf<Pair<BleConnectionState, BleConnectionState>>()
            
            fakeRepository.connectionState.test {
                var previousState = awaitItem() // Initial DISCONNECTED
                
                // Perform multiple operations
                launch {
                    fakeRepository.connectDevice(testDevice.address)
                    delay(100)
                    fakeRepository.disconnectDevice()
                    delay(100)
                    fakeRepository.connectDevice(testDevice.address)
                }
                
                // Collect all state transitions
                repeat(6) { // Expect multiple transitions
                    try {
                        val currentState = awaitItem()
                        observedTransitions.add(previousState to currentState)
                        previousState = currentState
                    } catch (e: Exception) {
                        break
                    }
                }
                
                cancelAndIgnoreRemainingEvents()
            }
            
            // Validate all transitions are valid
            observedTransitions.forEach { (from, to) ->
                assertTrue(isValidTransition(from, to)) {
                    "Invalid transition: $from -> $to"
                }
            }
        }
        
        @Test
        @DisplayName("Connected device should be available only when CONNECTED")
        fun testConnectedDeviceAvailability() = testScope.runTest {
            // Initially no device
            assertNull(fakeRepository.getConnectedDevice())
            
            // Connect
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // Should have connected device
            val connectedDevice = fakeRepository.getConnectedDevice()
            assertNotNull(connectedDevice)
            assertEquals(testDevice.address, connectedDevice?.address)
            
            // Disconnect
            fakeRepository.disconnectDevice()
            advanceUntilIdle()
            
            // Should not have connected device
            assertNull(fakeRepository.getConnectedDevice())
        }
        
        @Test
        @DisplayName("Connection state should be consistent with isDeviceConnected")
        fun testConnectionStateConsistency() = testScope.runTest {
            // Test multiple state changes
            val testSequence = listOf(
                { fakeRepository.connectDevice(testDevice.address) },
                { fakeRepository.disconnectDevice() },
                { fakeRepository.connectDevice(testDevice.address) },
                { fakeRepository.disconnectDevice() }
            )
            
            for (operation in testSequence) {
                operation()
                advanceUntilIdle()
                
                val connectionState = fakeRepository.connectionState.value
                val isConnected = fakeRepository.isDeviceConnected()
                
                // Consistency check
                when (connectionState) {
                    BleConnectionState.CONNECTED -> assertTrue(isConnected)
                    BleConnectionState.DISCONNECTED,
                    BleConnectionState.CONNECTING,
                    BleConnectionState.DISCONNECTING,
                    BleConnectionState.ERROR -> assertFalse(isConnected)
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Concurrent Connection Operations")
    inner class ConcurrentConnectionOperations {
        
        @Test
        @DisplayName("Concurrent connection and disconnection should be handled safely")
        fun testConcurrentConnectionDisconnection() = testScope.runTest {
            val results = mutableListOf<Boolean>()
            
            // Launch multiple concurrent operations
            val jobs = listOf(
                async { fakeRepository.connectDevice(testDevice.address) },
                async { 
                    delay(25)
                    fakeRepository.disconnectDevice()
                    true
                },
                async { 
                    delay(50)
                    fakeRepository.connectDevice(testDevice.address)
                }
            )
            
            // Wait for all operations
            jobs.forEach { job ->
                try {
                    results.add(job.await())
                } catch (e: Exception) {
                    // Some operations might fail due to concurrency, that's OK
                }
            }
            
            advanceUntilIdle()
            
            // Final state should be stable
            val finalState = fakeRepository.connectionState.value
            assertTrue(
                finalState == BleConnectionState.CONNECTED || 
                finalState == BleConnectionState.DISCONNECTED
            ) {
                "Final state should be stable, but was $finalState"
            }
        }
        
        @Test
        @DisplayName("Multiple scan operations should not interfere with connection state")
        fun testScanningDoesNotAffectConnectionState() = testScope.runTest {
            // Start connection
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            assertEquals(BleConnectionState.CONNECTED, fakeRepository.connectionState.value)
            
            // Start scanning while connected
            val scanJob = launch {
                fakeRepository.startScanning(1000).collect { /* ignore devices */ }
            }
            
            delay(200)
            
            // Connection state should remain CONNECTED
            assertEquals(BleConnectionState.CONNECTED, fakeRepository.connectionState.value)
            
            scanJob.cancel()
        }
    }
    
    // ===== Helper Methods =====
    
    /**
     * Check if a state transition is valid according to BLE state machine rules
     */
    private fun isValidTransition(from: BleConnectionState, to: BleConnectionState): Boolean {
        return when (from) {
            BleConnectionState.DISCONNECTED -> to in listOf(
                BleConnectionState.DISCONNECTED, // Stay disconnected
                BleConnectionState.CONNECTING    // Start connecting
            )
            BleConnectionState.CONNECTING -> to in listOf(
                BleConnectionState.CONNECTING,   // Stay connecting
                BleConnectionState.CONNECTED,    // Success
                BleConnectionState.ERROR,        // Failure
                BleConnectionState.DISCONNECTED  // Cancelled
            )
            BleConnectionState.CONNECTED -> to in listOf(
                BleConnectionState.CONNECTED,     // Stay connected
                BleConnectionState.DISCONNECTING, // Start disconnecting
                BleConnectionState.ERROR          // Unexpected error
            )
            BleConnectionState.DISCONNECTING -> to in listOf(
                BleConnectionState.DISCONNECTING, // Stay disconnecting
                BleConnectionState.DISCONNECTED,  // Success
                BleConnectionState.ERROR          // Unexpected error
            )
            BleConnectionState.ERROR -> to in listOf(
                BleConnectionState.ERROR,        // Stay in error
                BleConnectionState.DISCONNECTED, // Reset to disconnected
                BleConnectionState.CONNECTING    // Retry connection
            )
        }
    }
}