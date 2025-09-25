package com.wishring.app.presentation.main

import com.wishring.app.data.repository.BleConnectionState
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.data.model.WishUiState
import com.wishring.app.presentation.home.PageInfo

/**
 * BLE 연결 단계를 나타내는 enum
 */
enum class BlePhase {
    Idle,               // 대기 상태
    Scanning,           // 기기 스캔 중
    DeviceSelected,     // 기기 선택됨 (연결 준비중) ← 새로 추가!
    Connecting,         // 수동 연결 중 (BLE 연결 시도)
    Connected,          // BLE 연결 성공 (아직 초기화 안됨)
    Initializing,       // 연결 후 초기화 작업 중
    ReadingSettings,    // 기기 설정 정보 읽는 중
    WritingTime,        // 시간 동기화 중
    Ready,              // 모든 준비 완료
    AutoConnecting;     // 자동 연결 중
    
    /**
     * 활성 상태인지 (로딩 표시 필요)
     */
    val isActive: Boolean get() = this != Idle && this != Ready
    
    /**
     * 완전히 연결되어 사용 준비가 완료된 상태인지
     */
    val isReady: Boolean get() = this == Ready
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
data class BleCommand(
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

    // 위시 데이터 (HomeViewModel에서 이관)
    val wishHistory: List<WishDayUiState> = emptyList(),
    val todayWish: WishUiState? = null,
    val pageInfo: PageInfo? = null,
    val isWishDataLoading: Boolean = false,
    val wishDataError: String? = null,

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
        get() = scannedDevices.isNotEmpty() && (phase == BlePhase.Idle || phase == BlePhase.Scanning)

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
    
    // ===== HomeViewState 로직 이관 =====
    
    /**
     * 오늘의 등록된 위시 개수를 기반으로 상태 결정
     * @return 위시 개수에 따른 HomeViewState 타입
     */
    fun getHomeViewStateType(actualWishCount: Int? = null): String {
        val wishCount = actualWishCount ?: run {
            // actualWishCount가 없으면 todayWish 존재 여부로 추정
            if (todayWish != null) 1 else 0
        }
        
        return when {
            !isConnected -> "BluetoothDisconnected"
            wishCount == 0 -> "ConnectedNoWishes"
            wishCount in 1..2 -> "ConnectedPartialWishes"
            else -> "ConnectedFullWishes"
        }
    }
    
    /**
     * 위시 등록 프롬프트를 보여줄지 여부
     */
    val showWishRegistrationPrompt: Boolean
        get() = isConnected && (todayWish == null || todayWish.targetCount == 0)
    
    /**
     * 위시 버튼을 보여줄지 여부 (부분 위시 상태)
     */
    val showWishButton: Boolean
        get() {
            val wishCount = todayWish?.let { 1 } ?: 0 // 임시로 1개로 간주
            return isConnected && wishCount in 1..2
        }
    
    /**
     * 완료 애니메이션을 보여줄지 여부
     */
    val showCompletionAnimation: Boolean
        get() = isConnected && todayWish?.isCompleted == true
    
    /**
     * 배터리 경고를 보여줄지 여부
     */
    val showLowBatteryWarning: Boolean
        get() = batteryLevel != null && batteryLevel!! < 20
    
    /**
     * 현재 카운트 (오늘의 위시에서)
     */
    val currentCount: Int
        get() = todayWish?.currentCount ?: 0
        
    /**
     * 목표 카운트 (오늘의 위시에서)
     */
    val targetCount: Int
        get() = todayWish?.targetCount ?: 0
    
    /**
     * 진행률 (0.0-1.0)
     */
    val progress: Float
        get() {
            val wish = todayWish
            return if (wish != null && wish.targetCount > 0) {
                (wish.currentCount.toFloat() / wish.targetCount).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    
    /**
     * 목표 달성 여부
     */
    val isCompleted: Boolean
        get() = todayWish?.isCompleted ?: false
}