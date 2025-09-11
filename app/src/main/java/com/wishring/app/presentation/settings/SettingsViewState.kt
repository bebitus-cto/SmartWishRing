package com.wishring.app.presentation.settings

import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.domain.repository.ThemeMode

/**
 * ViewState for Settings screen
 * Represents the UI state of app settings
 */
data class SettingsViewState(
    val isLoading: Boolean = false,
    
    // General Settings
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "ko",
    val defaultWishText: String = "",
    val defaultTargetCount: Int = 1000,
    
    // Notification Settings
    val notificationEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String? = null,
    val achievementNotificationEnabled: Boolean = true,
    
    // Sound & Vibration
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    
    // BLE Settings
    val bleConnectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val lastConnectedDevice: String? = null,
    val bleAutoConnect: Boolean = true,
    val bleSyncInterval: Int = 5,
    val deviceBatteryLevel: Int? = null,
    val deviceFirmwareVersion: String? = null,
    val isDeviceConnected: Boolean = false,  // Added for SettingsScreen
    val connectedDeviceName: String? = null,  // Added for SettingsScreen
    val ledEnabled: Boolean = true,  // Added for SettingsScreen
    val notificationsEnabled: Boolean = true,  // Added for SettingsScreen
    val darkModeEnabled: Boolean = false,  // Added for SettingsScreen
    
    // Data & Backup
    val autoBackupEnabled: Boolean = false,
    val lastBackupTime: Long? = null,
    val totalRecordsCount: Int = 0,
    val databaseSize: Long = 0,
    
    // App Info
    val appVersion: String = "",
    val buildNumber: String = "",
    
    // UI State
    val expandedSection: SettingsSection? = null,
    val showResetConfirmation: Boolean = false,
    val showDeleteDataConfirmation: Boolean = false,
    val error: String? = null
) {
    /**
     * Is BLE connected
     */
    val isBleConnected: Boolean
        get() = bleConnectionState == BleConnectionState.CONNECTED
    
    /**
     * Last backup display
     */
    val lastBackupDisplay: String
        get() = lastBackupTime?.let {
            val diff = System.currentTimeMillis() - it
            when {
                diff < 86_400_000 -> "오늘"
                diff < 172_800_000 -> "어제"
                diff < 604_800_000 -> "${diff / 86_400_000}일 전"
                else -> "${diff / 604_800_000}주 전"
            }
        } ?: "백업 없음"
    
    /**
     * Database size display
     */
    val databaseSizeDisplay: String
        get() = when {
            databaseSize < 1024 -> "${databaseSize}B"
            databaseSize < 1024 * 1024 -> "${databaseSize / 1024}KB"
            else -> "${databaseSize / (1024 * 1024)}MB"
        }
    
    /**
     * Daily reminder time display
     */
    val dailyReminderTimeDisplay: String
        get() = dailyReminderTime ?: "설정 안 됨"
    
    /**
     * BLE sync interval display
     */
    val bleSyncIntervalDisplay: String
        get() = "${bleSyncInterval}분"
    
    /**
     * Device info display
     */
    val deviceInfoDisplay: String
        get() = when {
            !isBleConnected -> "연결 안 됨"
            deviceFirmwareVersion != null -> "v$deviceFirmwareVersion"
            else -> "연결됨"
        }
    
    /**
     * Theme mode display
     */
    val themeModeDisplay: String
        get() = when (themeMode) {
            ThemeMode.LIGHT -> "라이트 모드"
            ThemeMode.DARK -> "다크 모드"
            ThemeMode.SYSTEM -> "시스템 설정 따름"
        }
    
    /**
     * Language display
     */
    val languageDisplay: String
        get() = when (language) {
            "ko" -> "한국어"
            "en" -> "English"
            "ja" -> "日本語"
            "zh" -> "中文"
            else -> language
        }
    
    /**
     * Has unsaved changes
     */
    val hasUnsavedChanges: Boolean = false
    
    /**
     * Can backup now
     */
    val canBackupNow: Boolean
        get() = !isLoading && totalRecordsCount > 0
    
    /**
     * Needs backup
     */
    val needsBackup: Boolean
        get() = lastBackupTime?.let {
            System.currentTimeMillis() - it > 7 * 24 * 60 * 60 * 1000 // 7 days
        } ?: true
    
    /**
     * Show backup warning
     */
    val showBackupWarning: Boolean
        get() = needsBackup && totalRecordsCount > 100
}

/**
 * Settings section enum
 */
enum class SettingsSection {
    GENERAL,
    NOTIFICATIONS,
    SOUND_VIBRATION,
    BLUETOOTH,
    DATA_BACKUP,
    ABOUT,
    ADVANCED
}