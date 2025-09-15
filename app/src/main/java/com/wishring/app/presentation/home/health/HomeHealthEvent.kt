package com.wishring.app.presentation.home.health

import com.wishring.app.domain.model.*

/**
 * Events for Home Health functionality
 * Represents user actions related to health features
 */
sealed interface HomeHealthEvent {
    
    // Health data loading
    data object LoadHealthData : HomeHealthEvent
    data object RefreshHealthData : HomeHealthEvent
    
    // Real-time monitoring
    data object StartRealTimeHeartRate : HomeHealthEvent
    data object StopRealTimeHeartRate : HomeHealthEvent
    data object StartRealTimeEcg : HomeHealthEvent
    data object StopRealTimeEcg : HomeHealthEvent
    data object StartRealTimeBloodPressure : HomeHealthEvent
    data object StopRealTimeBloodPressure : HomeHealthEvent
    
    // User profile management
    data class UpdateUserProfile(val userProfile: UserProfile) : HomeHealthEvent
    data object LoadUserProfile : HomeHealthEvent
    
    // Sport and fitness
    data class SetSportTarget(val steps: Int) : HomeHealthEvent
    data object LoadSportTarget : HomeHealthEvent
    
    // Device settings
    data class UpdateDeviceSettings(val units: UnitPreferences) : HomeHealthEvent
    data class SetDeviceLanguage(val language: String) : HomeHealthEvent
    data class SetScreenBrightness(val brightness: Int) : HomeHealthEvent
    data class SetRaiseWristWake(val enabled: Boolean) : HomeHealthEvent
    data class SetAutoHeartRateMonitoring(
        val enabled: Boolean, 
        val interval: Int = 30
    ) : HomeHealthEvent
    
    // Notifications
    data class SendAppNotification(val notification: AppNotification) : HomeHealthEvent
    
    // Device operations
    data object FindDevice : HomeHealthEvent
    data object FactoryReset : HomeHealthEvent
    
    // Health details
    data class ShowHealthDetails(val type: HealthDataType) : HomeHealthEvent
    data object DismissHealthDetails : HomeHealthEvent
    
    // Error handling
    data object DismissError : HomeHealthEvent
    
    // Connection state updates (from parent)
    data class UpdateConnectionState(val isConnected: Boolean) : HomeHealthEvent
}