package com.wishring.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.wishring.app.core.util.Constants
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.domain.repository.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension property for DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFERENCES_NAME
)

/**
 * Implementation of PreferencesRepository using DataStore
 * Manages app preferences and settings
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {
    
    private val dataStore = context.dataStore
    
    // Preference Keys
    private object PreferenceKeys {
        val DEFAULT_WISH_TEXT = stringPreferencesKey("default_wish_text")
        val DEFAULT_TARGET_COUNT = intPreferencesKey("default_target_count")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
        val ACHIEVEMENT_NOTIFICATION = booleanPreferencesKey("achievement_notification")

        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val BLE_AUTO_CONNECT = booleanPreferencesKey("ble_auto_connect")
        val LAST_BLE_DEVICE = stringPreferencesKey("last_ble_device")
        val BLE_SYNC_INTERVAL = intPreferencesKey("ble_sync_interval")
    }
    
    override suspend fun getDefaultWishText(): String {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.DEFAULT_WISH_TEXT] ?: Constants.DEFAULT_WISH_TEXT
            }
            .first()
    }
    
    override suspend fun setDefaultWishText(wishText: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_WISH_TEXT] = wishText
        }
    }
    
    override fun observeDefaultWishText(): Flow<String> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.DEFAULT_WISH_TEXT] ?: Constants.DEFAULT_WISH_TEXT
            }
    }
    
    override suspend fun getDefaultTargetCount(): Int {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.DEFAULT_TARGET_COUNT] ?: Constants.DEFAULT_TARGET_COUNT
            }
            .first()
    }
    
    override suspend fun setDefaultTargetCount(targetCount: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_TARGET_COUNT] = targetCount
        }
    }
    
    override fun observeDefaultTargetCount(): Flow<Int> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.DEFAULT_TARGET_COUNT] ?: Constants.DEFAULT_TARGET_COUNT
            }
    }
    
    override suspend fun isOnboardingCompleted(): Boolean {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.ONBOARDING_COMPLETED] ?: false
            }
            .first()
    }
    
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ONBOARDING_COMPLETED] = completed
        }
    }
    
    override suspend fun isNotificationEnabled(): Boolean {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.NOTIFICATION_ENABLED] ?: true
            }
            .first()
    }
    
    override suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_ENABLED] = enabled
        }
    }
    
    override fun observeNotificationEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.NOTIFICATION_ENABLED] ?: true
            }
    }
    
    override suspend fun getDailyReminderTime(): String? {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.DAILY_REMINDER_TIME]
            }
            .first()
    }
    
    override suspend fun setDailyReminderTime(time: String?) {
        dataStore.edit { preferences ->
            if (time != null) {
                preferences[PreferenceKeys.DAILY_REMINDER_TIME] = time
            } else {
                preferences.remove(PreferenceKeys.DAILY_REMINDER_TIME)
            }
        }
    }
    
    override suspend fun isAchievementNotificationEnabled(): Boolean {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.ACHIEVEMENT_NOTIFICATION] ?: true
            }
            .first()
    }
    
    override suspend fun setAchievementNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ACHIEVEMENT_NOTIFICATION] = enabled
        }
    }
    

    
    override suspend fun isSoundEnabled(): Boolean {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.SOUND_ENABLED] ?: true
            }
            .first()
    }
    
    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SOUND_ENABLED] = enabled
        }
    }
    
    override suspend fun getThemeMode(): ThemeMode {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val modeString = preferences[PreferenceKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                ThemeMode.valueOf(modeString)
            }
            .first()
    }
    
    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = mode.name
        }
    }
    
    override fun observeThemeMode(): Flow<ThemeMode> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val modeString = preferences[PreferenceKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                ThemeMode.valueOf(modeString)
            }
    }
    
    override suspend fun getLanguage(): String {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.LANGUAGE] ?: "ko"
            }
            .first()
    }
    
    override suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LANGUAGE] = languageCode
        }
    }
    
    override suspend fun isAutoBackupEnabled(): Boolean {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.AUTO_BACKUP_ENABLED] ?: false
            }
            .first()
    }
    
    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }
    
    override suspend fun getLastBackupTime(): Long? {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.LAST_BACKUP_TIME]
            }
            .first()
    }
    
    override suspend fun setLastBackupTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_BACKUP_TIME] = timestamp
        }
    }
    
    override suspend fun isBleAutoConnectEnabled(): Boolean {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.BLE_AUTO_CONNECT] ?: true
            }
            .first()
    }
    
    override suspend fun setBleAutoConnectEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BLE_AUTO_CONNECT] = enabled
        }
    }
    
    override suspend fun getLastBleDeviceAddress(): String? {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.LAST_BLE_DEVICE]
            }
            .first()
    }
    
    override suspend fun setLastBleDeviceAddress(address: String?) {
        dataStore.edit { preferences ->
            if (address != null) {
                preferences[PreferenceKeys.LAST_BLE_DEVICE] = address
            } else {
                preferences.remove(PreferenceKeys.LAST_BLE_DEVICE)
            }
        }
    }
    
    override suspend fun getBleSyncInterval(): Int {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferenceKeys.BLE_SYNC_INTERVAL] ?: Constants.DEFAULT_BLE_SYNC_INTERVAL
            }
            .first()
    }
    
    override suspend fun setBleSyncInterval(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BLE_SYNC_INTERVAL] = minutes
        }
    }
    
    override suspend fun getAllPreferences(): Map<String, Any?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                mapOf(
                    "defaultWishText" to preferences[PreferenceKeys.DEFAULT_WISH_TEXT],
                    "defaultTargetCount" to preferences[PreferenceKeys.DEFAULT_TARGET_COUNT],
                    "onboardingCompleted" to preferences[PreferenceKeys.ONBOARDING_COMPLETED],
                    "notificationEnabled" to preferences[PreferenceKeys.NOTIFICATION_ENABLED],
                    "dailyReminderTime" to preferences[PreferenceKeys.DAILY_REMINDER_TIME],
                    "achievementNotification" to preferences[PreferenceKeys.ACHIEVEMENT_NOTIFICATION],

                    "soundEnabled" to preferences[PreferenceKeys.SOUND_ENABLED],
                    "themeMode" to preferences[PreferenceKeys.THEME_MODE],
                    "language" to preferences[PreferenceKeys.LANGUAGE],
                    "autoBackupEnabled" to preferences[PreferenceKeys.AUTO_BACKUP_ENABLED],
                    "lastBackupTime" to preferences[PreferenceKeys.LAST_BACKUP_TIME],
                    "bleAutoConnect" to preferences[PreferenceKeys.BLE_AUTO_CONNECT],
                    "lastBleDevice" to preferences[PreferenceKeys.LAST_BLE_DEVICE],
                    "bleSyncInterval" to preferences[PreferenceKeys.BLE_SYNC_INTERVAL]
                )
            }
            .first()
    }
    
    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
            preferences[PreferenceKeys.DEFAULT_WISH_TEXT] = Constants.DEFAULT_WISH_TEXT
            preferences[PreferenceKeys.DEFAULT_TARGET_COUNT] = Constants.DEFAULT_TARGET_COUNT
            preferences[PreferenceKeys.NOTIFICATION_ENABLED] = true
            preferences[PreferenceKeys.ACHIEVEMENT_NOTIFICATION] = true

            preferences[PreferenceKeys.SOUND_ENABLED] = true
            preferences[PreferenceKeys.THEME_MODE] = ThemeMode.SYSTEM.name
            preferences[PreferenceKeys.LANGUAGE] = "ko"
            preferences[PreferenceKeys.AUTO_BACKUP_ENABLED] = false
            preferences[PreferenceKeys.BLE_AUTO_CONNECT] = true
            preferences[PreferenceKeys.BLE_SYNC_INTERVAL] = Constants.DEFAULT_BLE_SYNC_INTERVAL
        }
    }
    
    override suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}