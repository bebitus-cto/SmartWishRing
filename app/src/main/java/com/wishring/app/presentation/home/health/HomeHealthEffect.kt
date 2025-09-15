package com.wishring.app.presentation.home.health

import com.wishring.app.domain.model.*

/**
 * Side effects for Home Health functionality
 * Represents one-time events that should be handled by the UI
 */
sealed interface HomeHealthEffect {
    
    // Toast messages
    data class ShowToast(val message: String) : HomeHealthEffect
    
    // Health data dialogs
    data class ShowHealthDetailDialog(val info: HealthDetailInfo) : HomeHealthEffect
    data object DismissHealthDetailDialog : HomeHealthEffect
    
    // ECG data display
    data class ShowEcgData(val data: EcgDisplayData) : HomeHealthEffect
    
    // Real-time monitoring UI updates
    data class UpdateRealTimeHeartRate(val heartRate: Int) : HomeHealthEffect
    data class UpdateRealTimeBloodPressure(
        val systolic: Int, 
        val diastolic: Int
    ) : HomeHealthEffect
    
    // Device feedback
    data object VibrateDevice : HomeHealthEffect
    data object PlayNotificationSound : HomeHealthEffect
    
    // Navigation
    data object NavigateToHealthSettings : HomeHealthEffect
    data object NavigateToUserProfile : HomeHealthEffect
    data object NavigateToDeviceSettings : HomeHealthEffect
    
    // Error handling
    data class ShowError(val message: String, val isRetryable: Boolean = false) : HomeHealthEffect
    
    // Success confirmations
    data object ShowSuccessAnimation : HomeHealthEffect
    data class ShowSettingsUpdated(val settingName: String) : HomeHealthEffect
}

/**
 * Health detail information for dialog display
 */
data class HealthDetailInfo(
    val type: HealthDataType,
    val title: String,
    val currentValue: String,
    val additionalInfo: String? = null,
    val timestamp: Long,
    val quality: String? = null,
    val trend: String? = null,
    val recommendation: String? = null
)

/**
 * ECG display data for real-time monitoring
 */
data class EcgDisplayData(
    val data: List<Float>,
    val heartRate: Int?,
    val timestamp: Long,
    val quality: String,
    val duration: Int? = null
)