package com.wishring.app.data.model

/**
 * Battery status levels
 */
enum class BatteryStatus {
    GOOD,    // Battery > 60% (alias for HIGH for better UX)
    HIGH,    // Battery > 60% (deprecated, use GOOD)
    MEDIUM,  // Battery 30-60%
    LOW      // Battery < 30%
}