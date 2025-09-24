package com.wishring.app.data.model

import kotlinx.serialization.Serializable

/**
 * Connected Bluetooth device information
 * Used for auto-reconnection functionality
 */
@Serializable
data class ConnectedDevice(
    /**
     * Bluetooth device MAC address (unique identifier)
     */
    val address: String,
    
    /**
     * Bluetooth device name
     */
    val name: String,
    
    /**
     * Last successful connection timestamp
     */
    val lastConnectedTime: Long,
    
    /**
     * Connection count for statistics
     */
    val connectionCount: Int = 1
) {
    
    /**
     * Check if device connection is recent (within last 7 days)
     */
    fun isRecentConnection(): Boolean {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return lastConnectedTime > sevenDaysAgo
    }
    
    /**
     * Get formatted connection time
     */
    fun getFormattedLastConnectedTime(): String {
        val diff = System.currentTimeMillis() - lastConnectedTime
        return when {
            diff < 60_000 -> "방금 전"
            diff < 3_600_000 -> "${diff / 60_000}분 전"
            diff < 86_400_000 -> "${diff / 3_600_000}시간 전"
            else -> "${diff / 86_400_000}일 전"
        }
    }
    
    companion object {
        
        /**
         * Create ConnectedDevice from current connection
         */
        fun create(address: String, name: String): ConnectedDevice {
            return ConnectedDevice(
                address = address,
                name = name,
                lastConnectedTime = System.currentTimeMillis(),
                connectionCount = 1
            )
        }
        
        /**
         * Update existing device with new connection
         */
        fun ConnectedDevice.updateConnection(): ConnectedDevice {
            return copy(
                lastConnectedTime = System.currentTimeMillis(),
                connectionCount = connectionCount + 1
            )
        }
    }
}