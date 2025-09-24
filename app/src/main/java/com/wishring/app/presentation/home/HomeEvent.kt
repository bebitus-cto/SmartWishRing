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
     * Navigate to wish input screen
     */
    object NavigateToWishInput : HomeEvent()

    /**
     * Navigate to detail screen
     * @param date Date to show details for
     */
    data class NavigateToDetail(val date: String) : HomeEvent()


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
     * Dismiss error
     */
    object DismissError : HomeEvent()

    /**
     * Handle button press from device
     * @param pressCount Number of presses
     */
    data class HandleDeviceButtonPress(val pressCount: Int) : HomeEvent()

    /**
     * Update battery level from device
     * @param level Battery level percentage (0-100) or null
     */
    data class UpdateBatteryLevel(val level: Int?) : HomeEvent()
}