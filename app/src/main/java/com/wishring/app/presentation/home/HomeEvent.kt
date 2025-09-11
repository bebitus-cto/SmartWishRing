package com.wishring.app.presentation.home

import com.wishring.app.domain.model.AppNotification
import com.wishring.app.domain.model.HealthDataType
import com.wishring.app.domain.model.UnitPreferences
import com.wishring.app.domain.model.UserProfile

/**
 * User events for Home screen
 * Represents user interactions and actions
 */
sealed class HomeEvent {
    
    /**
     * Load initial data
     */
    object LoadData : HomeEvent()
    
    /**
     * Refresh data
     */
    object RefreshData : HomeEvent()
    
    /**
     * Increment wish count
     * @param amount Amount to increment (default 1)
     */
    data class IncrementCount(val amount: Int = 1) : HomeEvent()
    
    /**
     * Reset today's count
     * @param reason Reset reason
     */
    data class ResetCount(val reason: String? = null) : HomeEvent()
    
    /**
     * Navigate to wish input screen
     */
    object NavigateToWishInput : HomeEvent()
    
    /**
     * Navigate to detail screen
     * @param date Date to show details for
     */
    data class NavigateToDetail(val date: String) : HomeEvent()
    
    /**
     * Navigate to settings screen
     */
    object NavigateToSettings : HomeEvent()
    
    /**
     * Start BLE scanning
     */
    object StartBleScanning : HomeEvent()
    
    /**
     * Connect to BLE device
     * @param deviceAddress Device MAC address
     */
    data class ConnectBleDevice(val deviceAddress: String) : HomeEvent()
    
    /**
     * Disconnect BLE device
     */
    object DisconnectBleDevice : HomeEvent()
    
    /**
     * Sync data with device
     */
    object SyncWithDevice : HomeEvent()
    
    /**
     * Share achievement
     */
    object ShareAchievement : HomeEvent()
    
    /**
     * Show streak details
     */
    object ShowStreakDetails : HomeEvent()
    
    /**
     * Dismiss error
     */
    object DismissError : HomeEvent()
    
    /**
     * Handle button press from device
     * @param pressCount Number of presses
     */
    data class HandleDeviceButtonPress(val pressCount: Int) : HomeEvent()
    
    /**
     * Update from background sync
     */
    object BackgroundSyncCompleted : HomeEvent()
    
    /**
     * Handle deep link
     * @param action Deep link action
     */
    data class HandleDeepLink(val action: String) : HomeEvent()
    
    /**
     * Toggle completion animation
     */
    object ToggleCompletionAnimation : HomeEvent()
    
    /**
     * Request notification permission
     */
    object RequestNotificationPermission : HomeEvent()
    
    /**
     * Request BLE permission
     */
    object RequestBlePermission : HomeEvent()
    
    // ===== MRD SDK 건강 데이터 이벤트들 =====
    
    /**
     * Load health data from device
     */
    object LoadHealthData : HomeEvent()
    
    /**
     * Start real-time heart rate monitoring
     */
    object StartRealTimeHeartRate : HomeEvent()
    
    /**
     * Stop real-time heart rate monitoring
     */
    object StopRealTimeHeartRate : HomeEvent()
    
    /**
     * Start real-time ECG monitoring
     */
    object StartRealTimeEcg : HomeEvent()
    
    /**
     * Stop real-time ECG monitoring
     */
    object StopRealTimeEcg : HomeEvent()
    
    /**
     * Update user profile
     * @param userProfile User profile data
     */
    data class UpdateUserProfile(val userProfile: UserProfile) : HomeEvent()
    
    /**
     * Set sport target
     * @param steps Daily step goal
     */
    data class SetSportTarget(val steps: Int) : HomeEvent()
    
    /**
     * Send app notification to device
     * @param notification Notification data
     */
    data class SendAppNotification(val notification: AppNotification) : HomeEvent()
    
    /**
     * Update device settings
     * @param units Unit preferences
     */
    data class UpdateDeviceSettings(val units: UnitPreferences) : HomeEvent()
    
    /**
     * Find device (make device vibrate/beep)
     */
    object FindDevice : HomeEvent()
    
    /**
     * Show health data details
     * @param type Type of health data to show
     */
    data class ShowHealthDetails(val type: HealthDataType) : HomeEvent()
}