package com.wishring.app.data.model

/**
 * Device settings and configuration models
 */

/**
 * System information types
 */
enum class SystemInfoType {
    BATTERY,
    FIRMWARE_VERSION,
    HARDWARE_VERSION,
    SERIAL_NUMBER,
    MANUFACTURER
}

/**
 * App notification types
 */
enum class NotificationType {
    GOAL_ACHIEVED,
    REMINDER,
    BATTERY_LOW,
    CONNECTION_STATUS,
    FIRMWARE_UPDATE,
    HEALTH_ALERT,
    GENERAL
}

/**
 * App notification data class
 */
data class AppNotification(
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Unit preferences data class
 */
data class UnitPreferences(
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val timeFormat: TimeFormat = TimeFormat.HOUR_24
)

/**
 * Device status data class
 */
data class DeviceStatus(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val connectionStrength: Int, // RSSI
    val lastSyncTime: Long,
    val firmwareVersion: String
)

/**
 * Distance unit enum
 */
enum class DistanceUnit {
    KM, MILES
}

/**
 * Temperature unit enum
 */
enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT
}

/**
 * Time format enum
 */
enum class TimeFormat {
    HOUR_12, HOUR_24
}