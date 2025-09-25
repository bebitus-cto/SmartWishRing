package com.wishring.app.presentation.home

import com.wishring.app.data.model.WishUiState
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.presentation.main.DeviceInfo

/**
 * Pagination information for wish history
 */
data class PageInfo(
    val currentPage: Int = 0,
    val hasNextPage: Boolean = false,
    val totalItems: Int = 0
)

/**
 * ViewState for Home screen using sealed class architecture
 * Represents different UI states based on bluetooth connection and wish data
 */
sealed class HomeViewState {
    // Abstract common properties
    abstract val wishHistory: List<WishDayUiState>
    abstract val todayWish: WishUiState?
    abstract val isLoading: Boolean
    abstract val error: String?
    abstract val pageInfo: PageInfo?  // nullable for disconnected state
    abstract val deviceBatteryLevel: Int?
    
    /**
     * Bluetooth disconnected state - only show connection UI
     */
    data class BluetoothDisconnected(
        override val wishHistory: List<WishDayUiState> = emptyList(),
        override val todayWish: WishUiState? = null,
        override val isLoading: Boolean = false,
        override val error: String? = null,
        override val pageInfo: PageInfo? = null, // always null when disconnected
        override val deviceBatteryLevel: Int? = null,
        // Disconnected-specific properties
        val showBleDevicePicker: Boolean = false,
        val availableBleDevices: List<DeviceInfo> = emptyList(),
        val lastBleScanTime: Long = 0L,
        val isScanning: Boolean = false,
        val isAttemptingConnection: Boolean = false,
        val connectionStartTime: Long? = null,
        val autoConnectAttempted: Boolean = false,
        val showConnectionSuccessAnimation: Boolean = false,
        val showDebugPanel: Boolean = false,
        val debugEventHistory: List<String> = emptyList(),
        // Permission related
        val showPermissionExplanation: Boolean = false,
        val permissionExplanations: Map<String, String> = emptyMap(),
        val showPermissionDenied: Boolean = false,
        val permissionDeniedMessage: String = "",
        val bluetoothProgressMessage: String = ""
    ) : HomeViewState() {
        
    }
    
    /**
     * Connected with no wishes (0 wishes) - show registration prompt
     */
    data class ConnectedNoWishes(
        override val wishHistory: List<WishDayUiState> = emptyList(),
        override val todayWish: WishUiState? = null,
        override val isLoading: Boolean = false,
        override val error: String? = null,
        override val pageInfo: PageInfo? = null,
        override val deviceBatteryLevel: Int? = null,
        // Share functionality
        val isSharing: Boolean = false,
        val lastSyncTime: Long? = null
    ) : HomeViewState()
    
    /**
     * Connected with partial wishes (1-2 wishes) - show wish button
     */
    data class ConnectedPartialWishes(
        override val wishHistory: List<WishDayUiState> = emptyList(),
        override val todayWish: WishUiState? = null,
        override val isLoading: Boolean = false,
        override val error: String? = null,
        override val pageInfo: PageInfo? = null,
        override val deviceBatteryLevel: Int? = null,
        // Share functionality
        val isSharing: Boolean = false,
        val lastSyncTime: Long? = null
    ) : HomeViewState()
    
    /**
     * Connected with full wishes (3+ wishes) - no additional buttons
     */
    data class ConnectedFullWishes(
        override val wishHistory: List<WishDayUiState> = emptyList(),
        override val todayWish: WishUiState? = null,
        override val isLoading: Boolean = false,
        override val error: String? = null,
        override val pageInfo: PageInfo? = null,
        override val deviceBatteryLevel: Int? = null,
        val showCompletionAnimation: Boolean = false,
        // Share functionality
        val isSharing: Boolean = false,
        val lastSyncTime: Long? = null
    ) : HomeViewState()
}

// Extension functions for common functionality
/**
 * Show low battery warning
 */
val HomeViewState.showLowBatteryWarning: Boolean
    get() {
        val battery = deviceBatteryLevel
        return battery != null && battery < 20
    }

/**
 * Current count from today's wish
 */
val HomeViewState.currentCount: Int
    get() = todayWish?.currentCount ?: 0

/**
 * Target count from today's wish
 */
val HomeViewState.targetCount: Int
    get() = todayWish?.targetCount ?: 0

/**
 * Progress float value (0.0-1.0)
 */
val HomeViewState.progress: Float
    get() {
        val wish = todayWish
        return if (wish != null && wish.targetCount > 0) {
            (wish.currentCount.toFloat() / wish.targetCount).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

/**
 * Is goal completed
 */
val HomeViewState.isCompleted: Boolean
    get() = todayWish?.isCompleted ?: false