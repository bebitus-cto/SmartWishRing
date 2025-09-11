package com.wishring.app.presentation.detail

import java.time.LocalDate

/**
 * User events for Detail screen
 * Represents user interactions and actions
 */
sealed class DetailEvent {
    
    /**
     * Load initial data
     * @param date Initial date to load
     */
    data class LoadData(val date: LocalDate = LocalDate.now()) : DetailEvent()
    
    /**
     * Select date
     * @param date Date to select
     */
    data class SelectDate(val date: LocalDate) : DetailEvent()
    
    /**
     * Navigate to previous month
     */
    object NavigatePreviousMonth : DetailEvent()
    
    /**
     * Navigate to next month
     */
    object NavigateNextMonth : DetailEvent()
    
    /**
     * Navigate to today
     */
    object NavigateToToday : DetailEvent()
    
    /**
     * Change view mode
     * @param mode New view mode
     */
    data class ChangeViewMode(val mode: ViewMode) : DetailEvent()
    
    /**
     * Show reset details
     * @param resetLog Reset log to show details for
     */
    data class ShowResetDetails(val resetLog: ResetLog) : DetailEvent()
    
    /**
     * Hide reset details
     */
    object HideResetDetails : DetailEvent()
    
    /**
     * Delete reset log
     * @param resetLogId Reset log ID to delete
     */
    data class DeleteResetLog(val resetLogId: Long) : DetailEvent()
    
    /**
     * Edit record
     * @param date Date of record to edit
     */
    data class EditRecord(val date: LocalDate) : DetailEvent()
    
    /**
     * Delete record
     * @param date Date of record to delete
     */
    data class DeleteRecord(val date: LocalDate) : DetailEvent()
    
    /**
     * Share record
     * @param date Date of record to share
     */
    data class ShareRecord(val date: LocalDate = LocalDate.now()) : DetailEvent()
    
    /**
     * Export data
     * @param startDate Start date for export
     * @param endDate End date for export
     * @param format Export format
     */
    data class ExportData(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val format: ExportFormat
    ) : DetailEvent()
    
    /**
     * Refresh data
     */
    object RefreshData : DetailEvent()
    
    /**
     * Navigate back
     */
    object NavigateBack : DetailEvent()
    
    /**
     * Show statistics details
     */
    object ShowStatisticsDetails : DetailEvent()
    
    /**
     * Show chart options
     */
    object ShowChartOptions : DetailEvent()
    
    /**
     * Update chart type
     * @param chartType New chart type
     */
    data class UpdateChartType(val chartType: ChartType) : DetailEvent()
    
    /**
     * Show date picker
     */
    object ShowDatePicker : DetailEvent()
    
    /**
     * Filter by completion status
     * @param showCompleted Show completed records
     * @param showIncomplete Show incomplete records
     */
    data class FilterByCompletion(
        val showCompleted: Boolean,
        val showIncomplete: Boolean
    ) : DetailEvent()
    
    /**
     * Dismiss error
     */
    object DismissError : DetailEvent()
    
    /**
     * Select message
     * @param index Index of message to select
     */
    data class SelectMessage(val index: Int) : DetailEvent()
}

/**
 * Export format enum
 */
enum class ExportFormat {
    CSV,
    JSON,
    PDF,
    IMAGE
}

/**
 * Reset log for detail view
 */
data class ResetLog(
    val id: Long,
    val time: String,
    val countLost: Int,
    val type: String,
    val reason: String?
)