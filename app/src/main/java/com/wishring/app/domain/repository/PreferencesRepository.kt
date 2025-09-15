package com.wishring.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app preferences
 * Manages user settings and app configuration
 */
interface PreferencesRepository {
    
    /**
     * Get default wish text
     * @return Current default wish text
     */
    suspend fun getDefaultWishText(): String
    
    /**
     * Set default wish text
     * @param wishText New default wish text
     */
    suspend fun setDefaultWishText(wishText: String)
    
    /**
     * Observe default wish text changes
     * @return Flow of wish text
     */
    fun observeDefaultWishText(): Flow<String>
    
    /**
     * Get default target count
     * @return Current default target count
     */
    suspend fun getDefaultTargetCount(): Int
    
    /**
     * Set default target count
     * @param targetCount New default target count
     */
    suspend fun setDefaultTargetCount(targetCount: Int)
    
    /**
     * Observe default target count changes
     * @return Flow of target count
     */
    fun observeDefaultTargetCount(): Flow<Int>
    
    /**
     * Check if onboarding is completed
     * @return True if onboarding completed
     */
    suspend fun isOnboardingCompleted(): Boolean
    
    /**
     * Set onboarding completed
     * @param completed True to mark as completed
     */
    suspend fun setOnboardingCompleted(completed: Boolean)
    
    /**
     * Get notification enabled status
     * @return True if notifications enabled
     */
    suspend fun isNotificationEnabled(): Boolean
    
    /**
     * Set notification enabled status
     * @param enabled True to enable notifications
     */
    suspend fun setNotificationEnabled(enabled: Boolean)
    
    /**
     * Observe notification enabled status
     * @return Flow of notification status
     */
    fun observeNotificationEnabled(): Flow<Boolean>
    
    /**
     * Get daily reminder time
     * @return Reminder time in "HH:mm" format or null
     */
    suspend fun getDailyReminderTime(): String?
    
    /**
     * Set daily reminder time
     * @param time Time in "HH:mm" format or null to disable
     */
    suspend fun setDailyReminderTime(time: String?)
    
    /**
     * Get achievement notification enabled
     * @return True if achievement notifications enabled
     */
    suspend fun isAchievementNotificationEnabled(): Boolean
    
    /**
     * Set achievement notification enabled
     * @param enabled True to enable
     */
    suspend fun setAchievementNotificationEnabled(enabled: Boolean)
    

    
    /**
     * Get sound enabled status
     * @return True if sound enabled
     */
    suspend fun isSoundEnabled(): Boolean
    
    /**
     * Set sound enabled status
     * @param enabled True to enable sound
     */
    suspend fun setSoundEnabled(enabled: Boolean)
    
    /**
     * Get theme mode
     * @return Theme mode (light, dark, system)
     */
    suspend fun getThemeMode(): ThemeMode
    
    /**
     * Set theme mode
     * @param mode New theme mode
     */
    suspend fun setThemeMode(mode: ThemeMode)
    
    /**
     * Observe theme mode changes
     * @return Flow of theme mode
     */
    fun observeThemeMode(): Flow<ThemeMode>
    
    /**
     * Get language preference
     * @return Language code (ko, en, etc.)
     */
    suspend fun getLanguage(): String
    
    /**
     * Set language preference
     * @param languageCode Language code
     */
    suspend fun setLanguage(languageCode: String)
    
    /**
     * Get auto backup enabled
     * @return True if auto backup enabled
     */
    suspend fun isAutoBackupEnabled(): Boolean
    
    /**
     * Set auto backup enabled
     * @param enabled True to enable auto backup
     */
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    
    /**
     * Get last backup time
     * @return Last backup timestamp or null
     */
    suspend fun getLastBackupTime(): Long?
    
    /**
     * Set last backup time
     * @param timestamp Backup timestamp
     */
    suspend fun setLastBackupTime(timestamp: Long)
    
    /**
     * Get BLE auto connect enabled
     * @return True if auto connect enabled
     */
    suspend fun isBleAutoConnectEnabled(): Boolean
    
    /**
     * Set BLE auto connect enabled
     * @param enabled True to enable auto connect
     */
    suspend fun setBleAutoConnectEnabled(enabled: Boolean)
    
    /**
     * Get last connected BLE device address
     * @return Device address or null
     */
    suspend fun getLastBleDeviceAddress(): String?
    
    /**
     * Set last connected BLE device address
     * @param address Device address
     */
    suspend fun setLastBleDeviceAddress(address: String?)
    
    /**
     * Get BLE sync interval
     * @return Sync interval in minutes
     */
    suspend fun getBleSyncInterval(): Int
    
    /**
     * Set BLE sync interval
     * @param minutes Sync interval in minutes
     */
    suspend fun setBleSyncInterval(minutes: Int)
    
    /**
     * Get all preferences as bundle
     * @return Map of all preferences
     */
    suspend fun getAllPreferences(): Map<String, Any?>
    
    /**
     * Reset all preferences to defaults
     */
    suspend fun resetToDefaults()
    
    /**
     * Clear all preferences
     */
    suspend fun clearAll()
}

/**
 * Theme mode enum
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}