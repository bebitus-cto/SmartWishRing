package com.wishring.app.presentation.detail

import java.time.LocalDate

/**
 * Side effects for Detail screen
 * Represents one-time UI actions
 */
sealed class DetailEffect {
    
    /**
     * Show toast message
     * @param message Message to display
     */
    data class ShowToast(val message: String) : DetailEffect()
    
    /**
     * Show snackbar
     * @param message Message to display
     * @param actionLabel Action button label
     * @param action Action to perform
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : DetailEffect()
    
    /**
     * Navigate back
     */
    object NavigateBack : DetailEffect()
    
    /**
     * Navigate to edit screen
     * @param date Date to edit
     */
    data class NavigateToEdit(val date: LocalDate) : DetailEffect()
    
    /**
     * Show date picker dialog
     * @param currentDate Current selected date
     * @param onDateSelected Callback when date selected
     */
    data class ShowDatePicker(
        val currentDate: LocalDate,
        val onDateSelected: (LocalDate) -> Unit
    ) : DetailEffect()
    
    /**
     * Show delete confirmation
     * @param date Date to delete
     * @param onConfirm Callback when confirmed
     */
    data class ShowDeleteConfirmation(
        val date: LocalDate,
        val onConfirm: () -> Unit
    ) : DetailEffect()
    
    /**
     * Show reset details dialog
     * @param resetDetails Reset details to show
     */
    data class ShowResetDetailsDialog(
        val resetDetails: ResetDetailsInfo
    ) : DetailEffect()
    
    /**
     * Show share sheet
     * @param shareContent Content to share
     */
    data class ShowShareSheet(val shareContent: ShareContent) : DetailEffect()
    
    /**
     * Show export options
     * @param onFormatSelected Callback when format selected
     */
    data class ShowExportOptions(
        val onFormatSelected: (ExportFormat) -> Unit
    ) : DetailEffect()
    
    /**
     * Export data
     * @param data Data to export
     * @param format Export format
     * @param fileName File name
     */
    data class ExportData(
        val data: String,
        val format: ExportFormat,
        val fileName: String
    ) : DetailEffect()
    
    /**
     * Show statistics details
     * @param statistics Statistics to show
     */
    data class ShowStatisticsDetailsDialog(
        val statistics: StatisticsDetails
    ) : DetailEffect()
    
    /**
     * Show chart options
     * @param currentType Current chart type
     * @param onTypeSelected Callback when type selected
     */
    data class ShowChartOptionsDialog(
        val currentType: ChartType,
        val onTypeSelected: (ChartType) -> Unit
    ) : DetailEffect()
    
    /**
     * Animate calendar transition
     * @param direction Transition direction
     */
    data class AnimateCalendarTransition(
        val direction: TransitionDirection
    ) : DetailEffect()
    
    /**
     * Highlight date
     * @param date Date to highlight
     */
    data class HighlightDate(val date: LocalDate) : DetailEffect()
    
    /**
     * Scroll to date
     * @param date Date to scroll to
     */
    data class ScrollToDate(val date: LocalDate) : DetailEffect()
    
    /**
     * Play sound effect
     * @param soundType Sound type
     */
    data class PlaySoundEffect(val soundType: SoundType) : DetailEffect()
    
    /**
     * Vibrate for feedback
     */
    object VibrateForFeedback : DetailEffect()
    
    /**
     * Track analytics event
     * @param eventName Event name
     * @param parameters Event parameters
     */
    data class TrackAnalyticsEvent(
        val eventName: String,
        val parameters: Map<String, Any>
    ) : DetailEffect()
    
    /**
     * Show empty state guide
     */
    object ShowEmptyStateGuide : DetailEffect()
    
    /**
     * Request storage permission for export
     * @param onGranted Callback when granted
     */
    data class RequestStoragePermission(
        val onGranted: () -> Unit
    ) : DetailEffect()
}

/**
 * Reset details info
 */
data class ResetDetailsInfo(
    val date: String,
    val time: String,
    val countBefore: Int,
    val targetCount: Int,
    val type: String,
    val reason: String?,
    val impact: String,
    val progressLost: Float
)

/**
 * Share content
 */
data class ShareContent(
    val text: String,
    val imageUri: String? = null
)

/**
 * Statistics details
 */
data class StatisticsDetails(
    val period: String,
    val totalDays: Int,
    val completedDays: Int,
    val totalCount: Int,
    val averageCount: Float,
    val bestDay: String?,
    val bestDayCount: Int,
    val completionRate: Float,
    val streakCurrent: Int,
    val streakBest: Int
)

/**
 * Transition direction
 */
enum class TransitionDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    FADE
}

/**
 * Sound type
 */
enum class SoundType {
    TAP,
    SWIPE,
    SELECT,
    SUCCESS,
    ERROR
}