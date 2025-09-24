package com.wishring.app.presentation.main

import com.wishring.app.data.repository.BleConnectionState

/**
 * BLE 연결 단계를 나타내는 enum
 */
enum class BlePhase {
    Idle,           // 대기 상태
    Scanning,       // 기기 스캔 중
    Connecting,     // 수동 연결 중
    AutoConnecting; // 자동 연결 중
    
    /**
     * 활성 상태인지 (로딩 표시 필요)
     */
    val isActive: Boolean get() = this != Idle
}

/**
 * 자동 연결 결과
 */
sealed class AutoConnectResult {
    data class Success(val deviceName: String) : AutoConnectResult()
    data class Failed(val reason: String) : AutoConnectResult()
    object NotAttempted : AutoConnectResult()
}

/**
 * 기기 정보
 */
data class DeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int
)

/**
 * BLE 관련 모든 상태를 통합 관리하는 UI State
 * HomeViewState 패턴을 참조하여 설계
 */
data class BleUiState(
    // 기본 연결 상태
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val phase: BlePhase = BlePhase.Idle,
    
    // 기기 정보
    val connectedDevice: DeviceInfo? = null,
    val scannedDevices: List<DeviceInfo> = emptyList(),
    
    // 자동 연결 관련
    val autoConnectAttempted: Boolean = false,
    val autoConnectResult: AutoConnectResult? = null,
    
    // 배터리 및 카운트 정보
    val batteryLevel: Int? = null,
    val buttonCount: Int = 0,
    
    // 기타
    val errorMessage: String? = null,
    val lastScanTime: Long = 0L,
    val connectionStartTime: Long? = null
) {
    
    // ===== Computed Properties =====
    
    /**
     * 연결된 상태인지
     */
    val isConnected: Boolean 
        get() = connectionState == BleConnectionState.CONNECTED
    
    /**
     * 연결 중인지
     */
    val isConnecting: Boolean 
        get() = connectionState == BleConnectionState.CONNECTING
    
    /**
     * 기기 선택 다이얼로그를 표시할지
     */
    val shouldShowDevicePicker: Boolean 
        get() = scannedDevices.isNotEmpty() && phase == BlePhase.Idle
    
    /**
     * 로딩 표시할지 (스캔, 연결 중 등)
     */
    val shouldShowLoading: Boolean 
        get() = phase.isActive || isConnecting
    
    /**
     * 자동 연결 중인지
     */
    val isAutoConnecting: Boolean
        get() = phase == BlePhase.AutoConnecting
    
    /**
     * 현재 상태에 따른 버튼 텍스트
     */
    fun getButtonText(): String = when {
        phase == BlePhase.Scanning -> "기기 검색 중..."
        phase == BlePhase.Connecting -> "연결 중..."
        phase == BlePhase.AutoConnecting -> "자동 연결 중..."
        isConnecting -> "연결 중..."
        isConnected -> "연결됨"
        else -> "WISH RING 연결하기"
    }
    
    /**
     * 버튼 활성화 여부
     */
    fun isButtonEnabled(): Boolean = 
        phase == BlePhase.Idle && !isConnecting && !isConnected
    
    /**
     * 연결 시도 지속 시간 (초)
     */
    val connectionAttemptDurationSeconds: Int
        get() = connectionStartTime?.let { startTime ->
            ((System.currentTimeMillis() - startTime) / 1000).toInt()
        } ?: 0
    
    /**
     * 연결 시도 타임아웃 여부 (30초)
     */
    val isConnectionAttemptTimedOut: Boolean
        get() = (phase == BlePhase.Connecting || phase == BlePhase.AutoConnecting) && 
                connectionAttemptDurationSeconds >= 30
}