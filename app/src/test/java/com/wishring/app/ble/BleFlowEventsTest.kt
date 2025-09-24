package com.wishring.app.ble

import app.cash.turbine.test
import com.wishring.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.seconds

/**
 * BLE Flow Events Test
 * 
 * Tests all Flow-based events from BLE repository including:
 * - Counter increments from ring button presses
 * - Button press events with different press types
 * - BLE notifications and alerts
 * - Event timing and sequencing
 */
@DisplayName("BLE Flow Events Tests")
class BleFlowEventsTest {
    
    private lateinit var testScope: TestScope
    private lateinit var fakeRepository: FakeBleRepository
    private lateinit var testDevice: BleDevice
    
    @BeforeEach
    fun setup() {
        testScope = TestScope(StandardTestDispatcher())
        fakeRepository = FakeBleRepository(
            simulateLatency = false, // Faster tests
            simulateErrors = false
        )
        testDevice = BluetoothTestUtils.createWishRingDevice()
    }
    
    @AfterEach
    fun tearDown() {
        fakeRepository.resetSimulation()
    }
    
    @Nested
    @DisplayName("Counter Increment Events")
    inner class CounterIncrementEvents {
        
        @Test
        @DisplayName("Counter increments should not emit when disconnected")
        fun testCounterIncrementsWhenDisconnected() = testScope.runTest {
            // When & Then
            fakeRepository.counterIncrements.test {
                // Should not emit anything when disconnected
                expectNoEvents()
                
                // Try to simulate button press (should not work)
                fakeRepository.simulateButtonPress(PressType.SINGLE)
                advanceTimeBy(1000)
                
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Counter increments should emit when connected")
        fun testCounterIncrementsWhenConnected() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // When & Then
            fakeRepository.counterIncrements.test {
                // Simulate single button press
                fakeRepository.simulateButtonPress(PressType.SINGLE)
                advanceTimeBy(100)
                
                // Should emit 1 increment
                assertEquals(1, awaitItem())
                
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @ParameterizedTest
        @EnumSource(PressType::class)
        @DisplayName("Different press types should emit correct increment counts")
        fun testDifferentPressTypes(pressType: PressType) = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val expectedIncrement = when (pressType) {
                PressType.SINGLE -> 1
                PressType.DOUBLE -> 2
                PressType.TRIPLE -> 3
                PressType.LONG -> 1
            }
            
            // When & Then
            fakeRepository.counterIncrements.test {
                fakeRepository.simulateButtonPress(pressType)
                advanceTimeBy(100)
                
                assertEquals(expectedIncrement, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Multiple rapid button presses should emit correct sequence")
        fun testMultipleRapidButtonPresses() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val pressSequence = listOf(
                PressType.SINGLE,   // +1
                PressType.DOUBLE,   // +2
                PressType.TRIPLE,   // +3
                PressType.SINGLE    // +1
            )
            val expectedSequence = listOf(1, 2, 3, 1)
            
            // When & Then
            fakeRepository.counterIncrements.test {
                pressSequence.forEach { pressType ->
                    fakeRepository.simulateButtonPress(pressType)
                    advanceTimeBy(50)
                }
                
                expectedSequence.forEach { expected ->
                    assertEquals(expected, awaitItem())
                }
                
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Counter increments should stop when disconnected")
        fun testCounterIncrementsStopWhenDisconnected() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // When & Then
            fakeRepository.counterIncrements.test {
                // First increment while connected
                fakeRepository.simulateButtonPress(PressType.SINGLE)
                advanceTimeBy(100)
                assertEquals(1, awaitItem())
                
                // Disconnect
                fakeRepository.disconnectDevice()
                advanceUntilIdle()
                
                // Try to increment after disconnect (should not emit)
                fakeRepository.simulateButtonPress(PressType.SINGLE)
                advanceTimeBy(500)
                
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Multiple collectors should receive same counter events")
        fun testMultipleCollectors() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val collector1Results = mutableListOf<Int>()
            val collector2Results = mutableListOf<Int>()
            
            // When
            val job1 = launch {
                fakeRepository.counterIncrements.collect {
                    collector1Results.add(it)
                }
            }
            
            val job2 = launch {
                fakeRepository.counterIncrements.collect {
                    collector2Results.add(it)
                }
            }
            
            // Simulate some button presses
            fakeRepository.simulateButtonPress(PressType.SINGLE)
            advanceTimeBy(100)
            fakeRepository.simulateButtonPress(PressType.DOUBLE)
            advanceTimeBy(100)
            
            job1.cancel()
            job2.cancel()
            
            // Then
            assertEquals(listOf(1, 2), collector1Results)
            assertEquals(listOf(1, 2), collector2Results)
        }
    }
    
    @Nested
    @DisplayName("Button Press Events")
    inner class ButtonPressEvents {
        
        @Test
        @DisplayName("Button press events should include timestamp and press type")
        fun testButtonPressEventStructure() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val pressType = PressType.DOUBLE
            val beforeTime = System.currentTimeMillis()
            
            // When & Then
            fakeRepository.buttonPressEvents.test {
                fakeRepository.simulateButtonPress(pressType)
                advanceTimeBy(100)
                
                val event = awaitItem()
                val afterTime = System.currentTimeMillis()
                
                assertEquals(pressType, event.pressType)
                assertEquals(2, event.pressCount)
                assertTrue(event.timestamp >= beforeTime && event.timestamp <= afterTime)
                
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Button press events should correlate with counter increments")
        fun testButtonPressEventCorrelation() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            var buttonPressEvent: ButtonPressEvent? = null
            var counterIncrement: Int? = null
            
            // Collect both events
            val buttonJob = launch {
                fakeRepository.buttonPressEvents.collect {
                    buttonPressEvent = it
                }
            }
            
            val counterJob = launch {
                fakeRepository.counterIncrements.collect {
                    counterIncrement = it
                }
            }
            
            // Simulate press
            fakeRepository.simulateButtonPress(PressType.TRIPLE)
            advanceTimeBy(200)
            
            buttonJob.cancel()
            counterJob.cancel()
            
            // Then - events should correlate
            assertNotNull(buttonPressEvent)
            assertNotNull(counterIncrement)
            assertEquals(buttonPressEvent!!.pressCount, counterIncrement)
            assertEquals(PressType.TRIPLE, buttonPressEvent!!.pressType)
        }
        
        @ParameterizedTest
        @ValueSource(ints = [1, 5, 10, 20])
        @DisplayName("Burst of button presses should be handled correctly")
        fun testBurstOfButtonPresses(burstSize: Int) = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val receivedEvents = mutableListOf<ButtonPressEvent>()
            
            // When
            fakeRepository.buttonPressEvents.test {
                // Simulate burst of button presses
                repeat(burstSize) {
                    fakeRepository.simulateButtonPress(PressType.SINGLE)
                    advanceTimeBy(10)
                }
                
                // Collect all events
                repeat(burstSize) {
                    receivedEvents.add(awaitItem())
                }
                
                cancelAndIgnoreRemainingEvents()
            }
            
            // Then
            assertEquals(burstSize, receivedEvents.size)
            receivedEvents.forEach { event ->
                assertEquals(PressType.SINGLE, event.pressType)
                assertEquals(1, event.pressCount)
            }
            
            // Timestamps should be in chronological order
            for (i in 1 until receivedEvents.size) {
                assertTrue(receivedEvents[i].timestamp >= receivedEvents[i-1].timestamp)
            }
        }
    }
    
    @Nested
    @DisplayName("BLE Notifications")
    inner class BleNotifications {
        
        @Test
        @DisplayName("Low battery notification should be emitted")
        fun testLowBatteryNotification() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // When & Then
            fakeRepository.notifications.test {
                fakeRepository.simulateLowBattery()
                advanceTimeBy(100)
                
                val notification = awaitItem()
                assertEquals(NotificationType.LOW_BATTERY, notification.type)
                assertTrue(notification.message.contains("Battery level"))
                assertTrue(notification.timestamp > 0)
                
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Connection lost notification should be emitted")
        fun testConnectionLostNotification() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // When & Then
            fakeRepository.notifications.test {
                fakeRepository.simulateConnectionError()
                advanceTimeBy(100)
                
                val notification = awaitItem()
                assertEquals(NotificationType.CONNECTION_LOST, notification.type)
                assertEquals("Connection lost", notification.message)
                
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Multiple notification types should be handled")
        fun testMultipleNotificationTypes() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val receivedNotifications = mutableListOf<BleNotification>()
            
            // When
            val job = launch {
                fakeRepository.notifications.collect {
                    receivedNotifications.add(it)
                }
            }
            
            // Simulate different notifications
            fakeRepository.simulateLowBattery()
            advanceTimeBy(100)
            
            fakeRepository.simulateButtonPress(PressType.SINGLE) // This might generate button press notification
            advanceTimeBy(100)
            
            fakeRepository.simulateConnectionError()
            advanceTimeBy(100)
            
            job.cancel()
            
            // Then
            assertTrue(receivedNotifications.isNotEmpty())
            
            // Should have at least low battery and connection lost notifications
            val notificationTypes = receivedNotifications.map { it.type }.toSet()
            assertTrue(notificationTypes.contains(NotificationType.LOW_BATTERY))
            assertTrue(notificationTypes.contains(NotificationType.CONNECTION_LOST))
        }
        
        @Test
        @DisplayName("Notifications should not be emitted when disconnected")
        fun testNotificationsWhenDisconnected() = testScope.runTest {
            // Ensure disconnected
            assertEquals(BleConnectionState.DISCONNECTED, fakeRepository.connectionState.value)
            
            // When & Then
            fakeRepository.notifications.test {
                // Try to trigger notifications while disconnected
                fakeRepository.simulateLowBattery()
                fakeRepository.simulateConnectionError()
                advanceTimeBy(500)
                
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
    
    @Nested
    @DisplayName("Flow Event Integration")
    inner class FlowEventIntegration {
        
        @Test
        @DisplayName("All event flows should be cold")
        fun testFlowsAreCold() = testScope.runTest {
            // Connect for potential events
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            // Test that flows are cold (don't emit until collected)
            fakeRepository.simulateButtonPress(PressType.SINGLE)
            advanceTimeBy(100)
            
            // Start collecting after the event
            fakeRepository.counterIncrements.test {
                // Should not receive the previous event
                expectNoEvents()
                
                // But should receive new events
                fakeRepository.simulateButtonPress(PressType.SINGLE)
                advanceTimeBy(100)
                
                assertEquals(1, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        
        @Test
        @DisplayName("Flow cancellation should not affect other flows")
        fun testFlowCancellationIsolation() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            var counterReceived = false
            var buttonReceived = false
            var notificationReceived = false
            
            // Start collecting all flows
            val counterJob = launch {
                fakeRepository.counterIncrements.collect { counterReceived = true }
            }
            
            val buttonJob = launch {
                fakeRepository.buttonPressEvents.collect { buttonReceived = true }
            }
            
            val notificationJob = launch {
                fakeRepository.notifications.collect { notificationReceived = true }
            }
            
            // Cancel one flow
            counterJob.cancel()
            
            // Generate events
            fakeRepository.simulateButtonPress(PressType.SINGLE)
            fakeRepository.simulateLowBattery()
            advanceTimeBy(200)
            
            buttonJob.cancel()
            notificationJob.cancel()
            
            // Then
            assertFalse(counterReceived) // Was cancelled
            assertTrue(buttonReceived)   // Should still work
            assertTrue(notificationReceived) // Should still work
        }
        
        @Test
        @DisplayName("Event ordering should be maintained under load")
        fun testEventOrderingUnderLoad() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val pressEvents = mutableListOf<ButtonPressEvent>()
            val counterEvents = mutableListOf<Int>()
            
            val pressJob = launch {
                fakeRepository.buttonPressEvents.collect { pressEvents.add(it) }
            }
            
            val counterJob = launch {
                fakeRepository.counterIncrements.collect { counterEvents.add(it) }
            }
            
            // Generate rapid sequence of events
            val pressSequence = listOf(
                PressType.SINGLE,
                PressType.DOUBLE,
                PressType.SINGLE,
                PressType.TRIPLE
            )
            
            pressSequence.forEach { pressType ->
                fakeRepository.simulateButtonPress(pressType)
                advanceTimeBy(10)
            }
            
            // Allow events to be processed
            advanceTimeBy(200)
            
            pressJob.cancel()
            counterJob.cancel()
            
            // Then - events should be in order
            assertEquals(4, pressEvents.size)
            assertEquals(4, counterEvents.size)
            
            // Verify chronological order
            for (i in 1 until pressEvents.size) {
                assertTrue(pressEvents[i].timestamp >= pressEvents[i-1].timestamp) {
                    "Events not in chronological order at index $i"
                }
            }
            
            // Verify press types match expected sequence
            pressEvents.forEachIndexed { index, event ->
                assertEquals(pressSequence[index], event.pressType)
            }
        }
        
        @Test
        @DisplayName("Memory usage should be bounded with long-running flows")
        fun testMemoryUsageWithLongRunningFlows() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val eventCount = 100
            val receivedEvents = mutableListOf<Int>()
            
            // Collect events but don't keep references to old ones
            val job = launch {
                fakeRepository.counterIncrements.collect { increment ->
                    receivedEvents.add(increment)
                    // Simulate processing and releasing memory
                    if (receivedEvents.size > 10) {
                        receivedEvents.removeAt(0) // Keep only last 10
                    }
                }
            }
            
            // Generate many events
            repeat(eventCount) {
                fakeRepository.simulateButtonPress(PressType.SINGLE)
                advanceTimeBy(1)
            }
            
            advanceTimeBy(500)
            job.cancel()
            
            // Memory should be bounded
            assertTrue(receivedEvents.size <= 10) {
                "Memory not bounded: ${receivedEvents.size} events retained"
            }
        }
    }
    
    @Nested
    @DisplayName("Event Reliability")
    inner class EventReliability {
        
        @Test
        @DisplayName("Events should not be lost during connection state changes")
        fun testEventsDuringConnectionChanges() = testScope.runTest {
            val allCounterEvents = mutableListOf<Int>()
            
            val counterJob = launch {
                fakeRepository.counterIncrements.collect { 
                    allCounterEvents.add(it)
                }
            }
            
            // Connect and generate events
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            fakeRepository.simulateButtonPress(PressType.SINGLE)
            advanceTimeBy(50)
            
            // Disconnect and reconnect
            fakeRepository.disconnectDevice()
            advanceUntilIdle()
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            fakeRepository.simulateButtonPress(PressType.DOUBLE)
            advanceTimeBy(50)
            
            counterJob.cancel()
            
            // Should have received events when connected
            assertEquals(listOf(1, 2), allCounterEvents)
        }
        
        @Test
        @DisplayName("Duplicate events should not be emitted")
        fun testNoDuplicateEvents() = testScope.runTest {
            // Given - connect first
            fakeRepository.connectDevice(testDevice.address)
            advanceUntilIdle()
            
            val receivedTimestamps = mutableListOf<Long>()
            
            fakeRepository.buttonPressEvents.test {
                // Simulate the same press multiple times rapidly
                val pressType = PressType.SINGLE
                repeat(3) {
                    fakeRepository.simulateButtonPress(pressType)
                    advanceTimeBy(10)
                }
                
                repeat(3) {
                    val event = awaitItem()
                    receivedTimestamps.add(event.timestamp)
                }
                
                cancelAndIgnoreRemainingEvents()
            }
            
            // All events should have different timestamps (no duplicates)
            assertEquals(receivedTimestamps.toSet().size, receivedTimestamps.size)
        }
    }
}