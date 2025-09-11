package com.wishring.app.presentation.settings

import com.wishring.app.domain.repository.ThemeMode

/**
 * Side effects for Settings screen
 * Represents one-time UI actions
 */
sealed class SettingsEffect {
    
    /**
     * Show toast message
     * @param message Message to display
     */
    data class ShowToast(val message: String) : SettingsEffect()
    
    /**
     * Show snackbar
     * @param message Message to display
     * @param actionLabel Action button label
     * @param action Action to perform
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : SettingsEffect()
    
    /**
     * Navigate back
     */
    object NavigateBack : SettingsEffect()
    
    /**
     * Navigate to about screen
     */
    object NavigateToAbout : SettingsEffect()
    
    /**
     * Navigate to support screen
     */
    object NavigateToSupport : SettingsEffect()
    
    /**
     * Open URL in browser
     * @param url URL to open
     */
    data class OpenUrl(val url: String) : SettingsEffect()
    
    /**
     * Show theme picker
     * @param currentMode Current theme mode
     * @param onModeSelected Callback when mode selected
     */
    data class ShowThemePicker(
        val currentMode: ThemeMode,
        val onModeSelected: (ThemeMode) -> Unit
    ) : SettingsEffect()
    
    /**
     * Show language picker
     * @param currentLanguage Current language
     * @param onLanguageSelected Callback when language selected
     */
    data class ShowLanguagePicker(
        val currentLanguage: String,
        val onLanguageSelected: (String) -> Unit
    ) : SettingsEffect()
    
    /**
     * Show time picker
     * @param currentTime Current time
     * @param onTimeSelected Callback when time selected
     */
    data class ShowTimePicker(
        val currentTime: String?,
        val onTimeSelected: (String) -> Unit
    ) : SettingsEffect()
    
    /**
     * Show number picker
     * @param title Picker title
     * @param currentValue Current value
     * @param minValue Minimum value
     * @param maxValue Maximum value
     * @param onValueSelected Callback when value selected
     */
    data class ShowNumberPicker(
        val title: String,
        val currentValue: Int,
        val minValue: Int,
        val maxValue: Int,
        val onValueSelected: (Int) -> Unit
    ) : SettingsEffect()
    
    /**
     * Show BLE device picker
     * @param devices Available devices
     * @param onDeviceSelected Callback when device selected
     */
    data class ShowBleDevicePicker(
        val devices: List<BleDeviceInfo>,
        val onDeviceSelected: (String) -> Unit
    ) : SettingsEffect()
    
    /**
     * Request permission
     * @param permission Permission to request
     * @param onGranted Callback when granted
     */
    data class RequestPermission(
        val permission: Permission,
        val onGranted: () -> Unit
    ) : SettingsEffect()
    
    /**
     * Open app settings
     */
    object OpenAppSettings : SettingsEffect()
    
    /**
     * Show backup options
     * @param onOptionSelected Callback when option selected
     */
    data class ShowBackupOptions(
        val onOptionSelected: (BackupOption) -> Unit
    ) : SettingsEffect()
    
    /**
     * Show restore confirmation
     * @param backupInfo Backup information
     * @param onConfirm Callback when confirmed
     */
    data class ShowRestoreConfirmation(
        val backupInfo: BackupInfo,
        val onConfirm: () -> Unit
    ) : SettingsEffect()
    
    /**
     * Show delete confirmation
     * @param title Confirmation title
     * @param message Confirmation message
     * @param onConfirm Callback when confirmed
     */
    data class ShowDeleteConfirmation(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit
    ) : SettingsEffect()
    
    /**
     * Show file picker
     * @param mimeType File MIME type
     * @param onFileSelected Callback when file selected
     */
    data class ShowFilePicker(
        val mimeType: String,
        val onFileSelected: (String) -> Unit
    ) : SettingsEffect()
    
    /**
     * Save file
     * @param fileName File name
     * @param content File content
     * @param mimeType MIME type
     */
    data class SaveFile(
        val fileName: String,
        val content: ByteArray,
        val mimeType: String
    ) : SettingsEffect() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as SaveFile
            
            if (fileName != other.fileName) return false
            if (!content.contentEquals(other.content)) return false
            if (mimeType != other.mimeType) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = fileName.hashCode()
            result = 31 * result + content.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }
    
    /**
     * Apply theme
     * @param mode Theme mode to apply
     */
    data class ApplyTheme(val mode: ThemeMode) : SettingsEffect()
    
    /**
     * Apply language
     * @param language Language code to apply
     */
    data class ApplyLanguage(val language: String) : SettingsEffect()
    
    /**
     * Schedule notification
     * @param time Time to schedule
     * @param message Notification message
     */
    data class ScheduleNotification(
        val time: String,
        val message: String
    ) : SettingsEffect()
    
    /**
     * Cancel scheduled notification
     */
    object CancelScheduledNotification : SettingsEffect()
    
    /**
     * Play test sound
     */
    object PlayTestSound : SettingsEffect()
    
    /**
     * Vibrate test
     */
    object VibrateTest : SettingsEffect()
    
    /**
     * Show firmware update dialog
     * @param currentVersion Current version
     * @param newVersion New version
     * @param onConfirm Callback when confirmed
     */
    data class ShowFirmwareUpdateDialog(
        val currentVersion: String,
        val newVersion: String,
        val onConfirm: () -> Unit
    ) : SettingsEffect()
    
    /**
     * Show rate app dialog
     */
    object ShowRateAppDialog : SettingsEffect()
    
    /**
     * Share app
     * @param shareText Share text
     */
    data class ShareApp(val shareText: String) : SettingsEffect()
    
    /**
     * Send feedback email
     * @param email Email address
     * @param subject Email subject
     * @param body Email body
     */
    data class SendFeedbackEmail(
        val email: String,
        val subject: String,
        val body: String
    ) : SettingsEffect()
    
    /**
     * Track analytics event
     * @param eventName Event name
     * @param parameters Event parameters
     */
    data class TrackAnalyticsEvent(
        val eventName: String,
        val parameters: Map<String, Any>
    ) : SettingsEffect()
    
    /**
     * Restart app
     */
    object RestartApp : SettingsEffect()
    
    /**
     * Restart required
     * @param reason Reason for restart
     */
    data class RestartRequired(val reason: String) : SettingsEffect()
}

/**
 * BLE device info
 */
data class BleDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int
)

/**
 * Permission enum
 */
enum class Permission {
    NOTIFICATION,
    BLUETOOTH,
    LOCATION,
    STORAGE
}

/**
 * Backup option enum
 */
enum class BackupOption {
    LOCAL,
    GOOGLE_DRIVE,
    EXPORT_FILE
}

/**
 * Backup info
 */
data class BackupInfo(
    val date: String,
    val recordCount: Int,
    val fileSize: Long,
    val version: String
)