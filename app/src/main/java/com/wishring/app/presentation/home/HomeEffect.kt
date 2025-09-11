package com.wishring.app.presentation.home

import com.wishring.app.domain.model.HealthDataType

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
     * Navigate to settings screen
     */
    object NavigateToSettings : HomeEffect()
    
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
     * Play completion animation
     */
    object PlayCompletionAnimation : HomeEffect()
    
    /**
     * Play sound effect
     * @param soundType Type of sound to play
     */
    data class PlaySound(val soundType: SoundType) : HomeEffect()
    
    /**
     * Vibrate device
     * @param pattern Vibration pattern
     */
    data class Vibrate(val pattern: VibrationPattern) : HomeEffect()
    
    /**
     * Show reset confirmation dialog
     * @param onConfirm Action when confirmed
     */
    data class ShowResetConfirmation(
        val onConfirm: (reason: String?) -> Unit
    ) : HomeEffect()
    
    /**
     * Show streak details dialog
     * @param streakInfo Streak information
     */
    data class ShowStreakDetailsDialog(
        val streakInfo: StreakDetailsInfo
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
     * Show ECG data
     * @param ecgData ECG display data
     */
    data class ShowEcgData(
        val ecgData: EcgDisplayData
    ) : HomeEffect()
    
    /**
     * Show health detail dialog
     * @param healthInfo Health detail information
     */
    data class ShowHealthDetailDialog(
        val healthInfo: HealthDetailInfo
    ) : HomeEffect()
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
enum class VibrationPattern {
    SHORT,
    DOUBLE,
    LONG,
    SUCCESS,
    ERROR
}

/**
 * Permission type enum
 */
enum class PermissionType {
    BLUETOOTH,
    NOTIFICATION,
    LOCATION
}

/**
 * Streak details info
 */
data class StreakDetailsInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val streakHistory: List<StreakPeriod>,
    val achievements: List<StreakAchievement>
)

/**
 * Streak period data
 */
data class StreakPeriod(
    val startDate: String,
    val endDate: String,
    val days: Int
)

/**
 * Streak achievement data
 */
data class StreakAchievement(
    val title: String,
    val description: String,
    val iconRes: Int,
    val isUnlocked: Boolean
)

/**
 * ECG display data
 */
data class EcgDisplayData(
    val data: ByteArray,
    val heartRate: Int,
    val timestamp: Long,
    val quality: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EcgDisplayData
        return data.contentEquals(other.data) && heartRate == other.heartRate && timestamp == other.timestamp && quality == other.quality
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + heartRate
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + quality.hashCode()
        return result
    }
}

/**
 * Health detail information
 */
data class HealthDetailInfo(
    val type: HealthDataType,
    val title: String,
    val currentValue: String,
    val additionalInfo: String? = null,
    val timestamp: Long,
    val quality: String
)