package com.wishring.app.presentation.detail

import com.wishring.app.domain.model.DailyRecord

import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.WishCountStatistics
import java.time.LocalDate
import com.wishring.app.presentation.detail.ResetLog

/**
 * ViewState for Detail screen
 * Represents the UI state of daily records and statistics
 */
data class DetailViewState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedRecord: DailyRecord? = null,
    val currentRecord: DailyRecord? = null,  // Added for DetailScreen
    val monthlyRecords: List<DailyRecord> = emptyList(),
    val weeklyStats: List<DailyRecord> = emptyList(),  // Added for DetailScreen
    val resetLogs: List<ResetLog> = emptyList(),
    val statistics: WishCountStatistics? = null,
    val viewMode: ViewMode = ViewMode.CALENDAR,
    val selectedMonth: LocalDate = LocalDate.now(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val showResetDetails: Boolean = false,
    val showStatistics: Boolean = false,  // Added for DetailScreen
    val chartData: ChartData? = null,
    val motivationalMessages: List<String> = emptyList(),  // Added for DetailScreen
    val selectedMessageIndex: Int = -1,  // Added for DetailScreen
    val achievements: List<String> = emptyList()  // Added for DetailScreen
) {
    /**
     * Has data for selected date
     */
    val hasSelectedDateData: Boolean
        get() = selectedRecord != null
    
    /**
     * Selected date display
     */
    val selectedDateDisplay: String
        get() = "${selectedDate.year}.${selectedDate.monthValue.toString().padStart(2, '0')}.${selectedDate.dayOfMonth.toString().padStart(2, '0')}"
    
    /**
     * Selected month display
     */
    val selectedMonthDisplay: String
        get() = "${selectedMonth.year}년 ${selectedMonth.monthValue}월"
    
    /**
     * Total count for selected date
     */
    val selectedDateCount: Int
        get() = selectedRecord?.totalCount ?: 0
    
    /**
     * Target count for selected date
     */
    val selectedDateTarget: Int
        get() = selectedRecord?.targetCount ?: 0
    
    /**
     * Completion rate for selected date
     */
    val selectedDateCompletionRate: Int
        get() = selectedRecord?.completionRate ?: 0
    
    /**
     * Is selected date completed
     */
    val isSelectedDateCompleted: Boolean
        get() = selectedRecord?.isCompleted ?: false
    
    /**
     * Reset count for selected date
     */
    val selectedDateResetCount: Int
        get() = selectedRecord?.resetCount ?: 0
    
    /**
     * Lost count for selected date
     */
    val selectedDateLostCount: Int
        get() = selectedRecord?.lostCount ?: 0
    
    /**
     * Has reset logs for selected date
     */
    val hasResetLogs: Boolean
        get() = resetLogs.isNotEmpty()
    
    /**
     * Monthly completion rate
     */
    val monthlyCompletionRate: Float
        get() = if (monthlyRecords.isNotEmpty()) {
            monthlyRecords.count { it.isCompleted }.toFloat() / monthlyRecords.size
        } else {
            0f
        }
    
    /**
     * Monthly total count
     */
    val monthlyTotalCount: Int
        get() = monthlyRecords.sumOf { it.totalCount }
    
    /**
     * Monthly average count
     */
    val monthlyAverageCount: Float
        get() = if (monthlyRecords.isNotEmpty()) {
            monthlyTotalCount.toFloat() / monthlyRecords.size
        } else {
            0f
        }
    
    /**
     * Best day in month
     */
    val bestDayInMonth: DailyRecord?
        get() = monthlyRecords.maxByOrNull { it.totalCount }
    
    /**
     * Calendar days data
     */
    val calendarDays: List<CalendarDay>
        get() {
            val firstDay = selectedMonth.withDayOfMonth(1)
            val lastDay = selectedMonth.withDayOfMonth(selectedMonth.lengthOfMonth())
            val days = mutableListOf<CalendarDay>()
            
            // Add empty days for alignment
            val firstDayOfWeek = firstDay.dayOfWeek.value % 7
            repeat(firstDayOfWeek) {
                days.add(CalendarDay.Empty)
            }
            
            // Add month days
            var currentDay = firstDay
            while (currentDay <= lastDay) {
                val record = monthlyRecords.find { 
                    it.date == currentDay 
                }
                days.add(
                    CalendarDay.Day(
                        date = currentDay,
                        record = record,
                        isSelected = currentDay == selectedDate,
                        isToday = currentDay == LocalDate.now()
                    )
                )
                currentDay = currentDay.plusDays(1)
            }
            
            return days
        }
    
    /**
     * Can navigate to previous month
     */
    val canNavigatePrevious: Boolean
        get() = true // Could add logic to limit how far back
    
    /**
     * Can navigate to next month
     */
    val canNavigateNext: Boolean
        get() = selectedMonth < LocalDate.now().withDayOfMonth(1)
    
    /**
     * Show empty state
     */
    val showEmptyState: Boolean
        get() = !isLoading && monthlyRecords.isEmpty()
    
    /**
     * Chart type for display
     */
    val chartType: ChartType
        get() = when (viewMode) {
            ViewMode.CALENDAR -> ChartType.NONE
            ViewMode.CHART -> ChartType.BAR
            ViewMode.STATISTICS -> ChartType.PIE
        }
}

/**
 * View mode enum
 */
enum class ViewMode {
    CALENDAR,       // 달력 보기
    CHART,          // 차트 보기
    STATISTICS      // 통계 보기
}

/**
 * Calendar day sealed class
 */
sealed class CalendarDay {
    object Empty : CalendarDay()
    
    data class Day(
        val date: LocalDate,
        val record: DailyRecord?,
        val isSelected: Boolean,
        val isToday: Boolean
    ) : CalendarDay()
}

/**
 * Chart data class
 */
data class ChartData(
    val type: ChartType,
    val dataPoints: List<ChartDataPoint>,
    val labels: List<String>,
    val colors: List<String>
)

/**
 * Chart data point
 */
data class ChartDataPoint(
    val value: Float,
    val label: String,
    val color: String
)

/**
 * Chart type enum
 */
enum class ChartType {
    NONE,
    BAR,
    LINE,
    PIE,
    SCATTER
}