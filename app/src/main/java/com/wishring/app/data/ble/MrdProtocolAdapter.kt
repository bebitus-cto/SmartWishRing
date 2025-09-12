package com.wishring.app.data.ble

import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.domain.repository.BleDevice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Local data class for MRD SDK reset events
 */
data class ResetEvent(
    val timestamp: Long,
    val previousCount: Int
)

/**
 * Connection state for MRD SDK
 */
sealed class MrdConnectionState {
    object DISCONNECTED : MrdConnectionState()
    object CONNECTING : MrdConnectionState()
    data class CONNECTED(val deviceInfo: String = "") : MrdConnectionState()
    data class ERROR(val message: String) : MrdConnectionState()
}

/**
 * Adapter that converts MRD SDK data types to Domain layer models
 * This maintains clean separation between data and domain layers
 */
class MrdProtocolAdapter @Inject constructor() {
    
    /**
     * Convert MRD connection state to domain connection state
     */
    fun convertConnectionState(mrdState: MrdConnectionState): BleConnectionState {
        return when (mrdState) {
            is MrdConnectionState.DISCONNECTED -> BleConnectionState.DISCONNECTED
            is MrdConnectionState.CONNECTING -> BleConnectionState.CONNECTING
            is MrdConnectionState.CONNECTED -> BleConnectionState.CONNECTED
            is MrdConnectionState.ERROR -> BleConnectionState.ERROR
        }
    }
    
    /**
     * Convert MRD connection state Flow to domain Flow
     */
    fun convertConnectionStateFlow(mrdStateFlow: Flow<MrdConnectionState>): Flow<BleConnectionState> {
        return mrdStateFlow.map { convertConnectionState(it) }
    }
    
    /**
     * Convert MRD reset event to domain model
     */
    fun convertResetEvent(resetEvent: ResetEvent): com.wishring.app.domain.model.ResetEvent {
        return com.wishring.app.domain.model.ResetEvent(
            timestamp = resetEvent.timestamp,
            previousCount = resetEvent.previousCount,
            deviceInfo = "WISH RING"
        )
    }
    
    /**
     * Convert MRD reset event Flow to domain Flow
     */
    fun convertResetEventFlow(resetFlow: Flow<ResetEvent>): Flow<com.wishring.app.domain.model.ResetEvent> {
        return resetFlow.map { convertResetEvent(it) }
    }
    
    /**
     * Validate counter increment value
     * MRD SDK might send bulk counts, we need individual increments
     */
    fun processCounterIncrement(rawCount: Int, lastCount: Int = 0): List<Int> {
        val increment = rawCount - lastCount
        return if (increment > 0) {
            // Return list of individual +1 increments
            List(increment) { 1 }
        } else {
            emptyList()
        }
    }
    
    /**
     * Validate and clamp battery level to valid range
     */
    fun processBatteryLevel(rawLevel: Int): Int {
        return rawLevel.coerceIn(0, 100)
    }
    
    /**
     * Convert MRD device info to BLE device model
     */
    fun convertDeviceInfo(deviceInfo: String): BleDevice {
        return BleDevice(
            name = "WISH RING",
            address = deviceInfo,
            rssi = -50, // Mock RSSI value
            isConnectable = true,
            isBonded = false
        )
    }
}