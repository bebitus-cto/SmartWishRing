package com.wishring.app.presentation.home.health

import com.wishring.app.data.model.*

/**
 * ViewState for Home Health functionality
 * Represents the UI state for health-related features
 */
data class HomeHealthViewState(
    // Health data loading state
    val healthDataLoading: Boolean = false,
    
    // Health data
    val heartRateData: HeartRateData? = null,
    val stepData: StepData? = null,
    val sleepData: SleepData? = null,
    val temperatureData: TemperatureData? = null,
    val bloodPressureData: BloodPressureData? = null,
    
    // Real-time monitoring states
    val isRealTimeHeartRateActive: Boolean = false,
    val isRealTimeEcgActive: Boolean = false,
    val isRealTimeBloodPressureActive: Boolean = false,
    
    // User profile and device settings
    val userProfile: UserProfile? = null,
    val deviceStatus: DeviceStatus? = null,
    
    // Device operations
    val isDeviceFinding: Boolean = false,
    
    // Error state
    val error: String? = null,
    
    // Connection state (from parent)
    val isBleConnected: Boolean = false
) {
    
    /**
     * Has any health data
     */
    val hasHealthData: Boolean
        get() = heartRateData != null || stepData != null || sleepData != null || 
                temperatureData != null || bloodPressureData != null
    
    /**
     * Is any real-time monitoring active
     */
    val isAnyRealTimeActive: Boolean
        get() = isRealTimeHeartRateActive || isRealTimeEcgActive || isRealTimeBloodPressureActive
    
    /**
     * Device battery level from device status
     */
    val deviceBatteryLevel: Int?
        get() = deviceStatus?.batteryLevel
    
    /**
     * Show low battery warning
     */
    val showLowBatteryWarning: Boolean
        get() {
            val level = deviceBatteryLevel
            return level != null && level < 20
        }
    
    /**
     * Get battery level text
     */
    val batteryLevelText: String
        get() = deviceBatteryLevel?.let { "${it}%" } ?: "--"
    
    /**
     * Heart rate display text
     */
    val heartRateText: String
        get() = heartRateData?.let { "${it.bpm} BPM" } ?: "--"
    
    /**
     * Step count display text
     */
    val stepCountText: String
        get() = stepData?.let { "${it.steps} 걸음" } ?: "--"
    
    /**
     * Sleep duration display text
     */
    val sleepDurationText: String
        get() = sleepData?.let { 
            val hours = it.totalSleepMinutes / 60
            val minutes = it.totalSleepMinutes % 60
            "${hours}시간 ${minutes}분"
        } ?: "--"
    
    /**
     * Temperature display text
     */
    val temperatureText: String
        get() = temperatureData?.let { "${it.temperature}°C" } ?: "--"
    
    /**
     * Blood pressure display text
     */
    val bloodPressureText: String
        get() = bloodPressureData?.let { 
            "${it.systolic}/${it.diastolic} mmHg"
        } ?: "--"
    
    /**
     * User profile display name
     */
    val userDisplayName: String
        get() = "사용자" // UserProfile doesn't have name field
    
    /**
     * Can load health data
     */
    val canLoadHealthData: Boolean
        get() = isBleConnected && !healthDataLoading
    
    /**
     * Can start real-time monitoring
     */
    val canStartRealTimeMonitoring: Boolean
        get() = isBleConnected && !isAnyRealTimeActive
    
    /**
     * Can update device settings
     */
    val canUpdateDeviceSettings: Boolean
        get() = isBleConnected
}