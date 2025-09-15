package com.wishring.app.presentation.home



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
     * Increment count
     * @param amount Amount to increment
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
     * Select BLE device from picker
     * @param deviceAddress Device MAC address
     */
    data class SelectBleDevice(val deviceAddress: String) : HomeEvent()
    
    /**
     * Dismiss BLE device picker dialog
     */
    object DismissBleDevicePicker : HomeEvent()
    
    /**
     * Share achievement (show dialog)
     */
    object ShareAchievement : HomeEvent()
    
    /**
     * Confirm share with message and hashtags
     * @param message Share message
     * @param hashtags Hashtags to include
     */
    data class ConfirmShare(val message: String, val hashtags: String) : HomeEvent()
    
    /**
     * Dismiss share dialog
     */
    object DismissShareDialog : HomeEvent()
    
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
    
    /**
     * Enable Bluetooth (prompt user to enable)
     */
    object EnableBluetooth : HomeEvent()
    
    /**
     * Dismiss permission explanation dialog
     */
    object DismissPermissionExplanation : HomeEvent()
    
    /**
     * Request permissions from explanation dialog
     */
    object RequestPermissionsFromExplanation : HomeEvent()
    
    /**
     * Dismiss permission denied dialog
     */
    object DismissPermissionDenied : HomeEvent()
    
    
    /**
     * Toggle debug panel visibility
     */
    object ToggleDebugPanel : HomeEvent()
    
    /**
     * Clear debug event history
     */
    object ClearDebugHistory : HomeEvent()
    /**
     * Open app settings from permission denied dialog
     */
    object OpenAppSettingsFromDialog : HomeEvent()

}