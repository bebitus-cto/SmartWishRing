package com.wishring.app.presentation.home

/**
 * Side effects for Home screen
 * Represents one-time UI actions
 */
sealed class HomeEffect {
    
    /**
     * Show toast message
     * @param message Message to display
     */
    data class ShowToast(val message: String) : HomeEffect()
    
    /**
     * Show snackbar with action
     * @param message Message to display
     * @param actionLabel Action button label
     * @param action Action to perform
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : HomeEffect()
    
    /**
     * Navigate to wish input screen
     */
    object NavigateToWishInput : HomeEffect()
    
    /**
     * Navigate to detail screen
     * @param date Date to show details for
     */
    data class NavigateToDetail(val date: String) : HomeEffect()
    
/**
     * Show BLE device picker
     * @param devices List of available devices
     */
    data class ShowBleDevicePicker(
        val devices: List<DeviceInfo>
    ) : HomeEffect()
    
    /**
     * Show share sheet
     * @param shareContent Content to share
     */
    data class ShowShareSheet(val shareContent: ShareContent) : HomeEffect()
    
    /**
     * Share image with Android intent
     * @param imageFile Image file to share
     * @param message Share message
     * @param hashtags Hashtags to include
     */
    data class ShareImageWithIntent(
        val imageFile: java.io.File,
        val message: String,
        val hashtags: String
    ) : HomeEffect()
    
    /**
     * Play completion animation
     */
    object PlayCompletionAnimation : HomeEffect()
    
    /**
     * Play sound effect
     * @param soundType Type of sound to play
     */
    data class PlaySound(val soundType: SoundType) : HomeEffect()
    

    
    /**
     * Show reset confirmation dialog
     * @param onConfirm Action when confirmed
     */
    data class ShowResetConfirmation(
        val onConfirm: (reason: String?) -> Unit
    ) : HomeEffect()
    
    /**
     * Request permission
     * @param permissionType Type of permission to request
     */
    data class RequestPermission(
        val permissionType: PermissionType
    ) : HomeEffect()
    
    /**
     * Open app settings
     */
    object OpenAppSettings : HomeEffect()
    
    /**
     * Show error dialog
     * @param title Dialog title
     * @param message Error message
     * @param retryAction Retry action
     */
    data class ShowErrorDialog(
        val title: String,
        val message: String,
        val retryAction: (() -> Unit)? = null
    ) : HomeEffect()
    
    /**
     * Update widget
     * @param count Current count
     * @param target Target count
     */
    data class UpdateWidget(
        val count: Int,
        val target: Int
    ) : HomeEffect()
    
    /**
     * Send local notification
     * @param title Notification title
     * @param body Notification body
     */
    data class SendLocalNotification(
        val title: String,
        val body: String
    ) : HomeEffect()
    
    /**
     * Track analytics event
     * @param eventName Event name
     * @param parameters Event parameters
     */
    data class TrackAnalyticsEvent(
        val eventName: String,
        val parameters: Map<String, Any>
    ) : HomeEffect()
    
    /**
     * Enable Bluetooth (show system prompt)
     */
    object EnableBluetooth : HomeEffect()
    
    /**
     * Request Bluetooth permissions
     */
    object RequestBluetoothPermissions : HomeEffect()
    
    /**
     * Show permission explanation dialog
     * @param permissions List of denied permissions
     * @param explanations Explanation messages for each permission
     */
    data class ShowPermissionExplanation(
        val permissions: List<String>,
        val explanations: Map<String, String>
    ) : HomeEffect()
    
    /**
     * Show permission denied dialog with solutions
     * @param deniedPermissions List of denied permissions
     * @param solution Solution message
     */
    data class ShowPermissionDenied(
        val deniedPermissions: List<String>,
        val solution: String
    ) : HomeEffect()
    

    
    /**
     * Update bluetooth connection progress
     * @param message Progress message to display
     */
    data class UpdateBluetoothProgress(
        val message: String
    ) : HomeEffect()
    
    /**
     * Show connection success animation
     */
    object ShowConnectionSuccessAnimation : HomeEffect()

}

/**
 * Device info for BLE picker
 */
data class DeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int
)

/**
 * Share content data
 */
data class ShareContent(
    val text: String,
    val imageUri: String? = null,
    val hashtags: List<String> = emptyList()
)

/**
 * Sound type enum
 */
enum class SoundType {
    TAP,
    SUCCESS,
    ERROR,
    NOTIFICATION
}

/**
 * Vibration pattern enum
 */


/**
 * Permission type enum
 */
enum class PermissionType {
    BLUETOOTH,
    NOTIFICATION,
    LOCATION
}

