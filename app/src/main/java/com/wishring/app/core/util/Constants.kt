package com.wishring.app.core.util

/**
 * App-wide constants
 */
object Constants {
    
    // App Configuration
    const val APP_NAME = "WISH RING"
    const val DATABASE_NAME = "wishring_database"
    const val PREFERENCES_NAME = "wishring_preferences"
    
    // Default Values
    const val DEFAULT_TARGET_COUNT = 1000
    const val DEFAULT_WISH_TEXT = "나는 매일 성장하고 있다."
    const val MAX_DAILY_COUNT = 99999
    const val MIN_TARGET_COUNT = 1
    
    // UI Configuration
    const val SPLASH_SCREEN_DURATION = 2000L // 2 seconds
    const val ANIMATION_DURATION_SHORT = 300L
    const val ANIMATION_DURATION_MEDIUM = 500L
    const val ANIMATION_DURATION_LONG = 1000L
    
    // BLE Configuration
    const val BLE_SCAN_TIMEOUT = 10000L // 10 seconds
    const val BLE_CONNECTION_TIMEOUT = 5000L // 5 seconds
    const val BLE_RECONNECT_DELAY = 3000L // 3 seconds
    const val BLE_MAX_RECONNECT_ATTEMPTS = 3
    
    // BLE Service UUIDs - MRD SDK 실제 UUID
    const val BLE_SERVICE_UUID = "f000efe0-0451-4000-0000-00000000b000"
    const val BLE_COUNTER_CHAR_UUID = "f000efe3-0451-4000-0000-00000000b000"
    const val BLE_BATTERY_CHAR_UUID = "0000fff2-0000-1000-8000-00805f9b34fb"
    const val BLE_RESET_CHAR_UUID = "0000fff3-0000-1000-8000-00805f9b34fb"
    
    // BLE Device Name - removed fake device name filtering
    // const val BLE_DEVICE_NAME_PREFIX = "WISHRING" // 가짜 기기명 제거됨
    
    // BLE Sync Configuration
    const val DEFAULT_BLE_SYNC_INTERVAL = 5 // minutes
    
    // Notification Configuration
    const val NOTIFICATION_CHANNEL_ID = "wish_ring_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Wish Ring Notifications"
    const val BLE_SERVICE_NOTIFICATION_ID = 1001
    const val DAILY_REMINDER_NOTIFICATION_ID = 1002
    
    // Preference Keys
    object PreferenceKeys {
        const val LAST_CONNECTED_DEVICE = "last_connected_device"
        const val DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
        const val DAILY_REMINDER_TIME = "daily_reminder_time"

        const val SOUND_ENABLED = "sound_enabled"
        const val AUTO_RECONNECT = "auto_reconnect"
        const val THEME_MODE = "theme_mode"
    }
    
    // Intent Extras
    object IntentExtras {
        const val DATE = "date"
        const val WISH_TEXT = "wish_text"
        const val TARGET_COUNT = "target_count"
        const val CURRENT_COUNT = "current_count"
    }
    
    // Request Codes
    object RequestCodes {
        const val BLE_PERMISSIONS = 1001
        const val NOTIFICATION_PERMISSION = 1002
        const val SHARE_IMAGE = 1003
    }
    
    // Result Keys
    object ResultKeys {
        const val WISH_SAVED = "wish_saved"
        const val CONNECTION_STATUS = "connection_status"
    }
    
    // Share Configuration
    const val SHARE_HASHTAG_1 = "#WishRing"
    const val SHARE_HASHTAG_2 = "#잠재의식"
    const val SHARE_HASHTAG_3 = "#긍정확언"
    const val SHARE_HASHTAG_4 = "#성공습관"
    const val SHARE_MESSAGE_TEMPLATE = "오늘 나는 %d번 더 성장했습니다 ✨"
    
    // Validation
    const val MAX_WISH_TEXT_LENGTH = 100
    const val MIN_WISH_TEXT_LENGTH = 1
    
    // Database
    const val DATABASE_VERSION = 3
    const val TABLE_WISHES = "wishes"
    const val TABLE_RESET_LOGS = "reset_logs"
    
    // Error Messages
    object ErrorMessages {
        const val BLE_NOT_SUPPORTED = "이 기기는 BLE를 지원하지 않습니다."
        const val BLE_NOT_ENABLED = "블루투스를 켜주세요."
        const val DEVICE_NOT_FOUND = "WISH RING 디바이스를 찾을 수 없습니다."
        const val CONNECTION_FAILED = "연결에 실패했습니다. 다시 시도해주세요."
        const val PERMISSION_DENIED = "권한이 필요합니다."
        const val INVALID_WISH_TEXT = "위시 문장을 입력해주세요."
        const val INVALID_TARGET_COUNT = "올바른 목표 횟수를 입력해주세요."
        const val DATABASE_ERROR = "데이터 저장 중 오류가 발생했습니다."
    }
    
    // Success Messages
    object SuccessMessages {
        const val DEVICE_CONNECTED = "WISH RING이 연결되었습니다."
        const val WISH_SAVED = "위시가 저장되었습니다."
        const val GOAL_ACHIEVED = "🎉 목표를 달성했습니다!"
        const val SHARE_SUCCESS = "공유가 완료되었습니다."
    }
}