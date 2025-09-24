package com.wishring.app.presentation.home

import com.wishring.app.data.model.WishUiState
import com.wishring.app.data.model.DailyRecord
import com.wishring.app.data.repository.BleConnectionState
import com.wishring.app.data.repository.StreakInfo
import com.wishring.app.core.util.Constants

/**
 * ViewState for Home screen
 * Represents the UI state of the main screen
 */
data class HomeViewState(
    val isLoading: Boolean = false,
    val todayWishUiState: WishUiState? = null,
    val totalCount: Int = 0,
    val targetCount: Int = Constants.DEFAULT_TARGET_COUNT,
    val recentRecords: List<DailyRecord> = emptyList(),
    val streakInfo: StreakInfo? = null,
    val bleConnectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val deviceBatteryLevel: Int? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showCompletionAnimation: Boolean = false,
    val lastSyncTime: Long? = null,

    // ===== 공유 기능 =====
    val showShareDialog: Boolean = false,
    val isSharing: Boolean = false,

    // ===== 권한 관련 =====
    val showPermissionExplanation: Boolean = false,
    val permissionExplanations: Map<String, String> = emptyMap(),
    val showPermissionDenied: Boolean = false,
    val permissionDeniedMessage: String = "",
    val bluetoothProgressMessage: String = "",

    // ===== BLE 기기 선택 =====
    val showBleDevicePicker: Boolean = false,
    val availableBleDevices: List<DeviceInfo> = emptyList(),
    val lastBleScanTime: Long = 0L, // 마지막 BLE 스캔 시간 (3초 제한용)

    // ===== BLE 연결 시도 =====
    val isScanning: Boolean = false,
    val isAttemptingConnection: Boolean = false,
    val connectionStartTime: Long? = null,

    // ===== 자동 연결 =====
    val autoConnectAttempted: Boolean = false,

    // ===== 연결 성공 애니메이션 =====
    val showConnectionSuccessAnimation: Boolean = false,

    // ===== 디버깅 모드 =====
    val showDebugPanel: Boolean = false,
    val debugEventHistory: List<String> = emptyList()
) {
    /**
     * Current count display
     */
    val currentCount: Int
        get() = totalCount

    /**
     * Progress float value (0.0-1.0)
     */
    val progress: Float
        get() = if (targetCount > 0) {
            (totalCount.toFloat() / targetCount).coerceIn(0f, 1f)
        } else {
            0f
        }

    /**
     * Is goal completed
     */
    val isCompleted: Boolean
        get() = totalCount >= targetCount

    /**
     * Is BLE connected
     */
    val isBleConnected: Boolean
        get() = bleConnectionState == BleConnectionState.CONNECTED

    /**
     * Is BLE connecting
     */
    val isBleConnecting: Boolean
        get() = bleConnectionState == BleConnectionState.CONNECTING

    /**
     * Show low battery warning
     */
    val showLowBatteryWarning: Boolean
        get() = deviceBatteryLevel != null && deviceBatteryLevel < 20

    /**
     * Can increment count (disabled - only BLE can increment)
     */
    val canIncrement: Boolean
        get() = false // Manual increment disabled

    /**
     * Connection attempt duration in seconds
     */
    val connectionAttemptDurationSeconds: Int
        get() = connectionStartTime?.let { startTime ->
            ((System.currentTimeMillis() - startTime) / 1000).toInt()
        } ?: 0

    /**
     * Is connection attempt timed out (30 seconds)
     */
    val isConnectionAttemptTimedOut: Boolean
        get() = isAttemptingConnection && connectionAttemptDurationSeconds >= 30

    /**
     * Should show connection loading
     * Shows loading when attempting connection or when BLE is connecting
     */
    fun shouldShowConnectionLoading(isAutoConnecting: Boolean): Boolean =
        isAutoConnecting || isScanning || isAttemptingConnection || isBleConnecting

    /**
     * Connection button text based on state
     * Manual operations (scanning, attempting connection) take priority over auto-connect
     */
    fun getConnectionButtonText(isAutoConnecting: Boolean): String = when {
        isScanning -> "기기 검색 중..."
        isAttemptingConnection -> "연결 중..."
        isBleConnecting -> "연결 중..."
        isBleConnected -> "연결됨"
        isAutoConnecting -> "자동 연결 중..."
        else -> "WISH RING 연결하기"
    }

    /**
     * Is connection button enabled
     */
    fun isConnectionButtonEnabled(isAutoConnecting: Boolean): Boolean =
        !isAutoConnecting && !isScanning && !isAttemptingConnection && !isBleConnecting && !isBleConnected

}