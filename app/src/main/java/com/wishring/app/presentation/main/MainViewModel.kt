package com.wishring.app.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishring.app.data.model.ConnectedDevice
import com.wishring.app.data.repository.BleConnectionState
import com.wishring.app.data.repository.PreferencesRepository
import com.wishring.app.data.repository.WishCountRepository
import com.wishring.app.data.ble.model.BleConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val wishCountRepository: WishCountRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {


    // BLE 통합 상태 관리
    private val _bleUiState = MutableStateFlow(BleUiState())
    val bleUiState = _bleUiState.asStateFlow()


    init {
        loadTodayCount()
    }

    private fun loadTodayCount() {
        viewModelScope.launch {
            try {
                val todayWishCount = wishCountRepository.getTodayWishCount()
                val count = todayWishCount?.targetCount ?: 0
                _bleUiState.value = _bleUiState.value.copy(buttonCount = count)
                Log.i(WR_EVENT, "[MainViewModel] 오늘 카운트 로드: $count")
            } catch (e: Exception) {
                Log.e(WR_EVENT, "[MainViewModel] 카운트 로드 실패", e)
            }
        }
    }

    fun updateBatteryLevel(batteryLevel: Int) {
        _bleUiState.value = _bleUiState.value.copy(
            batteryLevel = batteryLevel,
        )
        Log.i(WR_EVENT, "[MainViewModel] 배터리 업데이트 - 레벨: ${batteryLevel}%")
    }

    fun updateButtonCount(count: Int) {
        _bleUiState.value = _bleUiState.value.copy(buttonCount = count)
        Log.i(WR_EVENT, "[MainViewModel] 버튼 카운트 업데이트: $count")
    }

    fun updateConnectionState(connected: Boolean) {
        val currentState = _bleUiState.value

        _bleUiState.value = currentState.copy(
            connectionState = if (connected) BleConnectionState.CONNECTED
            else BleConnectionState.DISCONNECTED,
            phase = if (connected) BlePhase.Idle else currentState.phase,
            scannedDevices = if (connected) emptyList() else currentState.scannedDevices
        )

        Log.i(WR_EVENT, "[MainViewModel] 연결 상태 변경: $connected")

        if (connected) {
            Log.i(WR_EVENT, "[MainViewModel] 연결 성공 - 기기 목록 및 스캔 상태 정리")
        }
    }

    fun addScannedDevice(
        name: String,
        address: String,
        rssi: Int,
        serviceUuids: List<String> = emptyList()
    ) {

        if (validateWishRingDevice(serviceUuids)) {
            Log.i(WR_EVENT, "[MainViewModel] ✅ WISH RING 기기 확인됨 (Service UUID 검증)")

            val currentState = _bleUiState.value
            val devices = currentState.scannedDevices.toMutableList()

            // 중복 제거
            devices.removeAll { it.address == address }
            devices.add(DeviceInfo(name, address, rssi))

            // RSSI 강한 순으로 정렬
            val sortedDevices = devices.sortedByDescending { it.rssi }

            // BleUiState 업데이트
            _bleUiState.value = currentState.copy(
                scannedDevices = sortedDevices
            )

            Log.i(WR_EVENT, "[MainViewModel] 현재 WISH RING 기기 목록 크기: ${sortedDevices.size}")
            Log.i(
                WR_EVENT,
                "[MainViewModel] shouldShowDevicePicker 조건: devices=${sortedDevices.isNotEmpty()}, phase=${currentState.phase == BlePhase.Idle}"
            )
            Log.i(
                WR_EVENT,
                "[MainViewModel] → shouldShowDevicePicker = ${sortedDevices.isNotEmpty() && currentState.phase == BlePhase.Idle}"
            )
        } else {
            // 무시 - 너무 많은 로그 생성
        }

        Log.i(WR_EVENT, "[MainViewModel] ================================================")
    }

    private fun validateWishRingDevice(serviceUuids: List<String>): Boolean {
        // Service UUID로 H13 기기 식별
        val hasValidServiceUuid = serviceUuids.any { uuid ->
            uuid.equals(BleConstants.SERVICE_UUID.toString(), ignoreCase = true)
        }

        Log.i(WR_EVENT, "[MainViewModel] Service UUID 검증:")
        Log.i(WR_EVENT, "[MainViewModel] - 찾는 UUID: ${BleConstants.SERVICE_UUID}")
        Log.i(WR_EVENT, "[MainViewModel] - 기기의 UUIDs: $serviceUuids")
        Log.i(WR_EVENT, "[MainViewModel] - 검증 결과: ${if (hasValidServiceUuid) "✅ 일치" else "❌ 불일치"}")

        return hasValidServiceUuid
    }

    fun clearScannedDevices() {
        _bleUiState.value = _bleUiState.value.copy(
            scannedDevices = emptyList(),
            phase = BlePhase.Idle
        )
        Log.i(WR_EVENT, "[MainViewModel] 기기 목록 초기화")
    }

    fun dismissDevicePicker() {
        _bleUiState.value = _bleUiState.value.copy(
            scannedDevices = emptyList(),
            phase = BlePhase.Idle
        )
        Log.i(WR_EVENT, "[MainViewModel] 기기 선택 다이얼로그 닫기")
    }

    // ===== 자동 연결 기능 =====

    fun tryAutoConnect() {
        val currentState = _bleUiState.value

        if (currentState.autoConnectAttempted || currentState.phase == BlePhase.AutoConnecting || currentState.isConnected) {
            Log.i(WR_EVENT, "[MainViewModel] 자동 연결 스킵 - 이미 시도했거나 연결 중이거나 연결됨")
            return
        }

        viewModelScope.launch {
            try {
                // 자동 연결이 활성화되어 있는지 확인
                if (!preferencesRepository.isAutoConnectEnabled()) {
                    Log.i(WR_EVENT, "[MainViewModel] 자동 연결이 비활성화되어 있음")
                    _bleUiState.value = currentState.copy(
                        autoConnectAttempted = true,
                        autoConnectResult = AutoConnectResult.NotAttempted
                    )
                    return@launch
                }

                // 저장된 기기 정보 확인
                val lastDevice = preferencesRepository.getLastConnectedDevice()
                if (lastDevice == null) {
                    Log.i(WR_EVENT, "[MainViewModel] 저장된 기기 정보가 없음")
                    _bleUiState.value = currentState.copy(
                        autoConnectAttempted = true,
                        autoConnectResult = AutoConnectResult.NotAttempted
                    )
                    return@launch
                }

                // 최근 연결인지 확인 (7일 이내)
                if (!lastDevice.isRecentConnection()) {
                    Log.i(
                        WR_EVENT,
                        "[MainViewModel] 마지막 연결이 오래됨: ${lastDevice.getFormattedLastConnectedTime()}"
                    )
                    _bleUiState.value = currentState.copy(
                        autoConnectAttempted = true,
                        autoConnectResult = AutoConnectResult.Failed("마지막 연결이 너무 오래되었습니다")
                    )
                    return@launch
                }

                Log.i(
                    WR_EVENT,
                    "[MainViewModel] 자동 연결 시작: ${lastDevice.name} (${lastDevice.address})"
                )

                // 자동 연결 단계 시작
                _bleUiState.value = currentState.copy(
                    phase = BlePhase.AutoConnecting,
                    autoConnectAttempted = true,
                    connectionStartTime = System.currentTimeMillis()
                )

                // MainActivity에서 상태 변화를 관찰하여 실제 연결 수행
                // 연결 결과는 updateConnectionState()로 받음

            } catch (e: Exception) {
                Log.e(WR_EVENT, "[MainViewModel] 자동 연결 중 오류", e)
                _bleUiState.value = currentState.copy(
                    phase = BlePhase.Idle,
                    autoConnectAttempted = true,
                    autoConnectResult = AutoConnectResult.Failed("연결 오류: ${e.message}")
                )
            }
        }
    }

    /**
     * 연결 성공 시 기기 정보 저장
     */
    suspend fun onDeviceConnected(deviceAddress: String, deviceName: String) {
        Log.i(WR_EVENT, "[MainViewModel] 기기 연결 성공: $deviceName ($deviceAddress)")

        try {
            // 기존 기기 정보가 있으면 업데이트, 없으면 새로 생성
            val existingDevice = preferencesRepository.getLastConnectedDevice()
            val device = if (existingDevice?.address == deviceAddress) {
                existingDevice.copy(
                    name = deviceName, // 이름 업데이트
                    lastConnectedTime = System.currentTimeMillis(),
                    connectionCount = existingDevice.connectionCount + 1
                )
            } else {
                ConnectedDevice.create(deviceAddress, deviceName)
            }

            preferencesRepository.saveConnectedDevice(device)
            Log.i(WR_EVENT, "[MainViewModel] 기기 정보 저장 완료")

        } catch (e: Exception) {
            Log.e(WR_EVENT, "[MainViewModel] 기기 정보 저장 실패", e)
        }
    }

    fun resetAutoConnectState() {
        _bleUiState.value = _bleUiState.value.copy(
            phase = if (_bleUiState.value.phase == BlePhase.AutoConnecting) BlePhase.Idle else _bleUiState.value.phase,
            autoConnectAttempted = false,
            autoConnectResult = null
        )
        Log.i(WR_EVENT, "[MainViewModel] 자동 연결 상태 초기화")
    }


    // ===== BLE 스캔 관련 메서드 =====

    fun actuallyStartBleScan() {
        // 스캔 상태로 변경
        _bleUiState.value = _bleUiState.value.copy(
            phase = BlePhase.Scanning,
            scannedDevices = emptyList() // 새 스캔 시작 시 기존 목록 클리어
        )

        Log.i(WR_EVENT, "[MainViewModel] BLE 스캔 상태로 변경됨")
        // MainActivity가 이 상태를 관찰하여 Classic Discovery를 시작함
    }

    fun stopBleScan() {
        Log.i(WR_EVENT, "[MainViewModel] BLE 스캔 중지 요청")
        // 스캔 중지 시 Idle 상태로 변경
        _bleUiState.value = _bleUiState.value.copy(phase = BlePhase.Idle)
        // MainActivity가 이 상태를 관찰하여 Discovery를 중지함
    }

    fun onDiscoveryFinished() {
        val currentState = _bleUiState.value
        val hasDevices = currentState.scannedDevices.isNotEmpty()

        Log.i(WR_EVENT, "[MainViewModel] Discovery 완료됨")
        Log.i(WR_EVENT, "[MainViewModel] - 발견된 WISH RING 기기 수: ${currentState.scannedDevices.size}")

        // Discovery 완료 시 Idle 상태로 변경
        _bleUiState.value = currentState.copy(phase = BlePhase.Idle)

        // 기기가 발견되었으면 다이얼로그 표시 조건 확인
        if (hasDevices) {
            Log.i(WR_EVENT, "[MainViewModel] ✅ 기기 발견됨 - 다이얼로그 표시 가능")
            Log.i(
                WR_EVENT,
                "[MainViewModel] shouldShowDevicePicker = ${_bleUiState.value.shouldShowDevicePicker}"
            )

            // 디버그: 다이얼로그가 표시되어야 하는데 표시되지 않는 경우
            if (!_bleUiState.value.shouldShowDevicePicker) {
                Log.e(WR_EVENT, "[MainViewModel] ⚠️ 다이얼로그가 표시되어야 하는데 표시되지 않음!")
                Log.e(WR_EVENT, "[MainViewModel] - phase: ${_bleUiState.value.phase}")
                Log.e(
                    WR_EVENT,
                    "[MainViewModel] - scannedDevices: ${_bleUiState.value.scannedDevices}"
                )
            }
        } else {
            Log.i(WR_EVENT, "[MainViewModel] ❌ 발견된 기기 없음")
        }
    }


    /**
     * BLE 스캔 시작 (외부 호출용)
     */
    fun startBleScan() {
        actuallyStartBleScan()
    }

    companion object {
        private const val WR_EVENT = "WR_EVENT"
    }
}