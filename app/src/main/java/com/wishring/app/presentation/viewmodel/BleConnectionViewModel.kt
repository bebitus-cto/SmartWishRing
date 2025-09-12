package com.wishring.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishring.app.data.local.dao.BleEventDao
import com.wishring.app.data.local.entity.BleEventLogEntity
import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.domain.repository.BleDevice
import com.wishring.app.domain.repository.BleRepository
import com.wishring.app.domain.repository.isConnected
import com.wishring.app.domain.model.ResetEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Extension function to get status text for BleConnectionState enum
 */
private fun BleConnectionState.statusText(): String {
    return when (this) {
        BleConnectionState.DISCONNECTED -> "연결 안됨"
        BleConnectionState.CONNECTING -> "연결 중..."
        BleConnectionState.CONNECTED -> "연결됨"
        BleConnectionState.DISCONNECTING -> "연결 해제 중..."
        BleConnectionState.ERROR -> "연결 오류"
    }
}

/**
 * ViewModel for managing BLE connection state and events
 * Handles connection lifecycle, battery monitoring, and reset event logging
 */
@HiltViewModel
class BleConnectionViewModel @Inject constructor(
    private val bleRepository: BleRepository,
    private val bleEventDao: BleEventDao
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(BleConnectionUiState())
    val uiState: StateFlow<BleConnectionUiState> = _uiState.asStateFlow()

    // Effects for one-time events
    private val _effect = Channel<BleConnectionEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeBleConnection()
        observeBatteryLevel()
        observeResetEvents()
    }

    /**
     * Handle UI events
     */
    fun onEvent(event: BleConnectionEvent) {
        when (event) {
            is BleConnectionEvent.StartConnection -> startConnection(event.deviceAddress)
            is BleConnectionEvent.StopConnection -> stopConnection()
            is BleConnectionEvent.RefreshBattery -> refreshBattery()
            is BleConnectionEvent.RetryConnection -> retryConnection(event.deviceAddress)
        }
    }

    private fun startConnection(deviceAddress: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                if (deviceAddress != null) {
                    val success = bleRepository.connectDevice(deviceAddress)
                    if (success) {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.send(BleConnectionEffect.ShowToast("디바이스 연결 시작"))
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = "연결 실패"
                            ) 
                        }
                        _effect.send(BleConnectionEffect.ShowError("디바이스 연결에 실패했습니다"))
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "디바이스 주소가 없습니다"
                        ) 
                    }
                    _effect.send(BleConnectionEffect.ShowError("연결할 디바이스를 찾을 수 없습니다"))
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message
                    ) 
                }
                _effect.send(BleConnectionEffect.ShowError("연결 실패: ${e.message}"))
            }
        }
    }

    private fun stopConnection() {
        viewModelScope.launch {
            try {
                bleRepository.disconnectDevice()
                _effect.send(BleConnectionEffect.ShowToast("연결이 종료되었습니다"))
            } catch (e: Exception) {
                _effect.send(BleConnectionEffect.ShowError("연결 종료 실패: ${e.message}"))
            }
        }
    }

    private fun refreshBattery() {
        viewModelScope.launch {
            bleRepository.requestBatteryUpdate()
                .onFailure { error ->
                    _effect.send(BleConnectionEffect.ShowError("배터리 정보 요청 실패: ${error.message}"))
                }
        }
    }

    private fun retryConnection(deviceAddress: String?) {
        viewModelScope.launch {
            stopConnection()
            kotlinx.coroutines.delay(1000) // Wait 1 second
            startConnection(deviceAddress)
        }
    }

    /**
     * Start scanning for devices
     */
    fun startScanning() {
        viewModelScope.launch {
            bleRepository.startScanning().collect { device ->
                _effect.send(BleConnectionEffect.DeviceFound(device))
            }
        }
    }

    /**
     * Stop scanning for devices
     */
    fun stopScanning() {
        viewModelScope.launch {
            bleRepository.stopScanning()
        }
    }

    /**
     * Observe BLE connection state changes
     */
    private fun observeBleConnection() {
        viewModelScope.launch {
            bleRepository.getConnectionState().collect { connectionState ->
                _uiState.update { 
                    it.copy(connectionState = connectionState)
                }
                
                // Send effects based on connection state changes
                when (connectionState) {
                    BleConnectionState.CONNECTED -> {
                        // Get connected device info
                        val connectedDevice = bleRepository.getConnectedDevice()
                        if (connectedDevice != null) {
                            _effect.send(BleConnectionEffect.ConnectionSuccess(connectedDevice))
                        }
                    }
                    BleConnectionState.ERROR -> {
                        _effect.send(BleConnectionEffect.ShowError("연결 오류가 발생했습니다"))
                    }
                    else -> {
                        // Handle other states if needed
                    }
                }
            }
        }
    }

    /**
     * Observe battery level changes
     */
    private fun observeBatteryLevel() {
        viewModelScope.launch {
            bleRepository.subscribeToBatteryLevel().collect { batteryLevel ->
                _uiState.update { 
                    it.copy(batteryLevel = batteryLevel)
                }
                
                // Warn if battery is low
                if (batteryLevel <= 15) {
                    _effect.send(BleConnectionEffect.LowBatteryWarning(batteryLevel))
                }
            }
        }
    }

    /**
     * Observe reset events and log them to database
     */
    private fun observeResetEvents() {
        viewModelScope.launch {
            // Subscribe to button press events which include reset information
            bleRepository.subscribeToButtonPress().collect { buttonEvent ->
                // Convert button press to reset event if needed
                // This is a placeholder - actual implementation depends on MRD SDK
                val resetEvent = ResetEvent(
                    timestamp = buttonEvent.timestamp,
                    previousCount = buttonEvent.pressCount,
                    deviceInfo = "Device reset event"
                )
                logResetEvent(resetEvent)
                _effect.send(BleConnectionEffect.ResetEventReceived(resetEvent))
            }
        }
    }

    /**
     * Log reset event to database
     */
    private suspend fun logResetEvent(resetEvent: ResetEvent) {
        try {
            val eventLogEntity = BleEventLogEntity.createResetEvent(
                timestamp = resetEvent.timestamp,
                previousCount = resetEvent.previousCount,
                deviceAddress = resetEvent.deviceInfo
            )
            
            bleEventDao.insertEventLog(eventLogEntity)
            
        } catch (e: Exception) {
            _effect.send(BleConnectionEffect.ShowError("리셋 이벤트 로깅 실패: ${e.message}"))
        }
    }
}

/**
 * UI State for BLE connection
 */
data class BleConnectionUiState(
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val batteryLevel: Int = 100,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isConnected: Boolean
        get() = connectionState.isConnected()
    
    val statusText: String
        get() = connectionState.statusText()
    
    val batteryStatusColor: BatteryStatus
        get() = when {
            batteryLevel > 50 -> BatteryStatus.GOOD
            batteryLevel > 15 -> BatteryStatus.MEDIUM
            else -> BatteryStatus.LOW
        }
}

/**
 * UI Events for BLE connection
 */
sealed class BleConnectionEvent {
    data class StartConnection(val deviceAddress: String?) : BleConnectionEvent()
    object StopConnection : BleConnectionEvent()
    object RefreshBattery : BleConnectionEvent()
    data class RetryConnection(val deviceAddress: String?) : BleConnectionEvent()
}

/**
 * UI Effects for BLE connection
 */
sealed class BleConnectionEffect {
    data class ShowToast(val message: String) : BleConnectionEffect()
    data class ShowError(val message: String) : BleConnectionEffect()
    data class ConnectionSuccess(val device: BleDevice) : BleConnectionEffect()
    data class DeviceFound(val device: BleDevice) : BleConnectionEffect()
    data class LowBatteryWarning(val level: Int) : BleConnectionEffect()
    data class ResetEventReceived(val resetEvent: ResetEvent) : BleConnectionEffect()
}

/**
 * Battery status for UI coloring
 */
enum class BatteryStatus {
    GOOD, MEDIUM, LOW
}