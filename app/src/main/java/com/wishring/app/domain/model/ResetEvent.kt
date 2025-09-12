package com.wishring.app.domain.model

/**
 * Reset event from the WISH RING device
 * Indicates that the device counter was reset
 */
data class ResetEvent(
    val timestamp: Long,
    val previousCount: Int,
    val deviceInfo: String
)