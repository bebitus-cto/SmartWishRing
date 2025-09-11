package com.wishring.app.presentation.settings

import com.wishring.app.domain.repository.ThemeMode

/**
 * User events for Settings screen
 * Represents user interactions and actions
 */
sealed class SettingsEvent {
    
    /**
     * Load initial data
     */
    object LoadData : SettingsEvent()
    
    // General Settings Events
    
    /**
     * Update theme mode
     * @param mode New theme mode
     */
    data class UpdateThemeMode(val mode: ThemeMode) : SettingsEvent()
    
    /**
     * Update language
     * @param language New language code
     */
    data class UpdateLanguage(val language: String) : SettingsEvent()
    
    /**
     * Update default wish text
     * @param text New default wish text
     */
    data class UpdateDefaultWishText(val text: String) : SettingsEvent()
    
    /**
     * Update default target count
     * @param count New default target count
     */
    data class UpdateDefaultTargetCount(val count: Int) : SettingsEvent()
    
    // Notification Settings Events
    
    /**
     * Toggle notification enabled
     */
    object ToggleNotification : SettingsEvent()
    
    /**
     * Toggle daily reminder
     */
    object ToggleDailyReminder : SettingsEvent()
    
    /**
     * Update daily reminder time
     * @param time Time in HH:mm format
     */
    data class UpdateDailyReminderTime(val time: String) : SettingsEvent()
    
    /**
     * Toggle achievement notification
     */
    object ToggleAchievementNotification : SettingsEvent()
    
    // Sound & Vibration Events
    
    /**
     * Toggle sound enabled
     */
    object ToggleSound : SettingsEvent()
    
    /**
     * Toggle vibration enabled
     */
    object ToggleVibration : SettingsEvent()
    
    // BLE Settings Events
    
    /**
     * Start BLE scanning
     */
    object StartBleScanning : SettingsEvent()
    
    /**
     * Connect to BLE device
     * @param deviceAddress Device MAC address
     */
    data class ConnectBleDevice(val deviceAddress: String) : SettingsEvent()
    
    /**
     * Disconnect BLE device
     */
    object DisconnectBleDevice : SettingsEvent()
    
    /**
     * Toggle BLE auto connect
     */
    object ToggleBleAutoConnect : SettingsEvent()
    
    /**
     * Update BLE sync interval
     * @param minutes Sync interval in minutes
     */
    data class UpdateBleSyncInterval(val minutes: Int) : SettingsEvent()
    
    /**
     * Test BLE connection
     */
    object TestBleConnection : SettingsEvent()
    
    /**
     * Update device firmware
     */
    object UpdateDeviceFirmware : SettingsEvent()
    
    // Data & Backup Events
    
    /**
     * Toggle auto backup
     */
    object ToggleAutoBackup : SettingsEvent()
    
    /**
     * Backup now
     */
    object BackupNow : SettingsEvent()
    
    /**
     * Restore from backup
     */
    object RestoreFromBackup : SettingsEvent()
    
    /**
     * Export data
     * @param format Export format
     */
    data class ExportData(val format: ExportFormat) : SettingsEvent()
    
    /**
     * Import data
     */
    object ImportData : SettingsEvent()
    
    /**
     * Clear all data
     */
    object ClearAllData : SettingsEvent()
    
    /**
     * Clear old data
     * @param beforeDays Days to keep
     */
    data class ClearOldData(val beforeDays: Int) : SettingsEvent()
    
    // UI Events
    
    /**
     * Toggle section expansion
     * @param section Section to toggle
     */
    data class ToggleSectionExpansion(val section: SettingsSection) : SettingsEvent()
    
    /**
     * Show reset confirmation
     */
    object ShowResetConfirmation : SettingsEvent()
    
    /**
     * Hide reset confirmation
     */
    object HideResetConfirmation : SettingsEvent()
    
    /**
     * Confirm reset
     */
    object ConfirmReset : SettingsEvent()
    
    /**
     * Show delete data confirmation
     */
    object ShowDeleteDataConfirmation : SettingsEvent()
    
    /**
     * Hide delete data confirmation
     */
    object HideDeleteDataConfirmation : SettingsEvent()
    
    /**
     * Confirm delete data
     */
    object ConfirmDeleteData : SettingsEvent()
    
    // Navigation Events
    
    /**
     * Navigate back
     */
    object NavigateBack : SettingsEvent()
    
    /**
     * Navigate to about screen
     */
    object NavigateToAbout : SettingsEvent()
    
    /**
     * Navigate to privacy policy
     */
    object NavigateToPrivacyPolicy : SettingsEvent()
    
    /**
     * Navigate to terms of service
     */
    object NavigateToTermsOfService : SettingsEvent()
    
    /**
     * Navigate to licenses
     */
    object NavigateToLicenses : SettingsEvent()
    
    // Added Events for SettingsScreen
    
    /**
     * Connect device
     */
    object ConnectDevice : SettingsEvent()
    
    /**
     * Disconnect device
     */
    object DisconnectDevice : SettingsEvent()
    
    /**
     * Update vibration
     * @param enabled Vibration enabled state
     */
    data class UpdateVibration(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Update LED
     * @param enabled LED enabled state
     */
    data class UpdateLed(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Update notifications
     * @param enabled Notifications enabled state
     */
    data class UpdateNotifications(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Update dark mode
     * @param enabled Dark mode enabled state
     */
    data class UpdateDarkMode(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Update sound
     * @param enabled Sound enabled state
     */
    data class UpdateSound(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Navigate to support
     */
    object NavigateToSupport : SettingsEvent()
    
    /**
     * Backup data
     */
    object BackupData : SettingsEvent()
    
    /**
     * Restore data
     */
    object RestoreData : SettingsEvent()
    
    /**
     * Clear data
     */
    object ClearData : SettingsEvent()
    
    /**
     * Open privacy policy
     */
    object OpenPrivacyPolicy : SettingsEvent()
    
    // Other Events
    
    /**
     * Reset to defaults
     */
    object ResetToDefaults : SettingsEvent()
    
    /**
     * Send feedback
     */
    object SendFeedback : SettingsEvent()
    
    /**
     * Rate app
     */
    object RateApp : SettingsEvent()
    
    /**
     * Share app
     */
    object ShareApp : SettingsEvent()
    
    /**
     * Check for updates
     */
    object CheckForUpdates : SettingsEvent()
    
    /**
     * Dismiss error
     */
    object DismissError : SettingsEvent()
}

/**
 * Export format enum
 */
enum class ExportFormat {
    CSV,
    JSON,
    EXCEL
}