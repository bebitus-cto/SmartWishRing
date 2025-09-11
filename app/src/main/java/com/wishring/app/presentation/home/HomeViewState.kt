package com.wishring.app.presentation.home

import com.wishring.app.domain.model.*
import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.domain.repository.StreakInfo

/**
 * ViewState for Home screen
 * Represents the UI state of the main screen
 */
data class HomeViewState(
    val isLoading: Boolean = false,
    val todayWishCount: WishCount? = null,
    val recentRecords: List<DailyRecord> = emptyList(),
    val streakInfo: StreakInfo? = null,
    val bleConnectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val deviceBatteryLevel: Int? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showCompletionAnimation: Boolean = false,
    val lastSyncTime: Long? = null,
    
    // ===== MRD SDK 건강 데이터 =====
    val heartRateData: HeartRateData? = null,
    val stepData: StepData? = null,
    val sleepData: SleepData? = null,
    val temperatureData: TemperatureData? = null,
    val bloodPressureData: BloodPressureData? = null,
    val isRealTimeHeartRateActive: Boolean = false,
    val healthDataLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val deviceSettings: DeviceStatus? = null
) {
    /**
     * Current count display
     */
    val currentCount: Int
        get() = todayWishCount?.totalCount ?: 0
    
    /**
     * Target count display
     */
    val targetCount: Int
        get() = todayWishCount?.targetCount ?: 0
    
    /**
     * Wish text display
     */
    val wishText: String
        get() = todayWishCount?.wishText ?: ""
    
    /**
     * Progress percentage (0-100)
     */
    val progressPercentage: Int
        get() = todayWishCount?.progressPercentage ?: 0
    
    /**
     * Progress float value (0.0-1.0)
     */
    val progress: Float
        get() = todayWishCount?.progress ?: 0f
    
    /**
     * Is goal completed
     */
    val isCompleted: Boolean
        get() = todayWishCount?.isCompleted ?: false
    
    /**
     * Remaining count to target
     */
    val remainingCount: Int
        get() = todayWishCount?.remainingCount ?: targetCount
    
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
     * Current streak display
     */
    val currentStreak: Int
        get() = streakInfo?.currentStreak ?: 0
    
    /**
     * Best streak display
     */
    val bestStreak: Int
        get() = streakInfo?.bestStreak ?: 0
    
    /**
     * Has recent records
     */
    val hasRecentRecords: Boolean
        get() = recentRecords.isNotEmpty()
    
    /**
     * Show empty state
     */
    val showEmptyState: Boolean
        get() = !isLoading && todayWishCount == null
    
    /**
     * Can increment count
     */
    val canIncrement: Boolean
        get() = !isLoading && !isCompleted && currentCount < 99999
    
    /**
     * Show sync status
     */
    val showSyncStatus: Boolean
        get() = isBleConnected && lastSyncTime != null
    
    /**
     * Get sync status text
     */
    val syncStatusText: String
        get() = when {
            !isBleConnected -> "동기화 연결 끊김"
            lastSyncTime == null -> "동기화 대기 중"
            else -> {
                val diff = System.currentTimeMillis() - lastSyncTime
                when {
                    diff < 60_000 -> "방금 동기화됨"
                    diff < 3_600_000 -> "${diff / 60_000}분 전 동기화"
                    else -> "${diff / 3_600_000}시간 전 동기화"
                }
            }
        }
    
    /**
     * Get connection status text
     */
    val connectionStatusText: String
        get() = when (bleConnectionState) {
            BleConnectionState.DISCONNECTED -> "연결 끊김"
            BleConnectionState.CONNECTING -> "연결 중..."
            BleConnectionState.CONNECTED -> "연결됨"
            BleConnectionState.DISCONNECTING -> "연결 해제 중..."
            BleConnectionState.ERROR -> "연결 오류"
            else -> "알 수 없음"
        }
    
    /**
     * Get battery level text
     */
    val batteryLevelText: String
        get() = deviceBatteryLevel?.let { "${it}%" } ?: "--"
    
    /**
     * Should show battery level
     * Only show when device is connected and battery level is available
     */
    val shouldShowBatteryLevel: Boolean
        get() = isBleConnected && deviceBatteryLevel != null
    
    /**
     * Get battery level icon
     */
    val batteryLevelIcon: String
        get() = when {
            deviceBatteryLevel == null -> "battery_unknown"
            deviceBatteryLevel >= 80 -> "battery_full"
            deviceBatteryLevel >= 60 -> "battery_4_bar"
            deviceBatteryLevel >= 40 -> "battery_3_bar"
            deviceBatteryLevel >= 20 -> "battery_2_bar"
            deviceBatteryLevel >= 10 -> "battery_1_bar"
            else -> "battery_alert"
        }
}