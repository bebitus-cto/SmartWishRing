package com.wishring.app.presentation.detail

import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import com.wishring.app.core.base.BaseViewModel
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.repository.ResetLogRepository
import com.wishring.app.domain.repository.WishCountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// Import types from DetailEvent
import com.wishring.app.presentation.detail.ResetLog as PresentationResetLog
import com.wishring.app.presentation.detail.ExportFormat

// Import types from DetailEffect  
import com.wishring.app.presentation.detail.ResetDetailsInfo
import com.wishring.app.presentation.detail.ShareContent
import com.wishring.app.presentation.detail.StatisticsDetails
import com.wishring.app.presentation.detail.TransitionDirection
import com.wishring.app.presentation.detail.SoundType

// Import types from DetailViewState
import com.wishring.app.presentation.detail.ViewMode
import com.wishring.app.presentation.detail.ChartData
import com.wishring.app.presentation.detail.ChartDataPoint
import com.wishring.app.presentation.detail.ChartType

/**
 * ViewModel for Detail screen
 * Manages daily records, statistics, and calendar view
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val wishCountRepository: WishCountRepository,
    private val resetLogRepository: ResetLogRepository
) :
    BaseViewModel<DetailViewState, DetailEvent, DetailEffect>() {

    override val _uiState = MutableStateFlow(DetailViewState())

    override fun onEvent(event: DetailEvent) {
        when (event) {
            is DetailEvent.LoadData -> loadData(event.date)
            is DetailEvent.SelectDate -> selectDate(event.date)
            is DetailEvent.NavigatePreviousMonth -> navigatePreviousMonth()
            is DetailEvent.NavigateNextMonth -> navigateNextMonth()
            is DetailEvent.NavigateToToday -> navigateToToday()
            is DetailEvent.ChangeViewMode -> changeViewMode(event.mode)
            is DetailEvent.ShowResetDetails -> showResetDetails(event.resetLog)
            is DetailEvent.HideResetDetails -> hideResetDetails()
            is DetailEvent.DeleteResetLog -> deleteResetLog(event.resetLogId)
            is DetailEvent.EditRecord -> editRecord(event.date)
            is DetailEvent.DeleteRecord -> deleteRecord(event.date)
            is DetailEvent.ShareRecord -> shareRecord(event.date)
            is DetailEvent.ExportData -> exportData(event.startDate, event.endDate, event.format)
            is DetailEvent.RefreshData -> refreshData()
            is DetailEvent.NavigateBack -> navigateBack()
            is DetailEvent.ShowStatisticsDetails -> showStatisticsDetails()
            is DetailEvent.ShowChartOptions -> showChartOptions()
            is DetailEvent.UpdateChartType -> updateChartType(event.chartType)
            is DetailEvent.ShowDatePicker -> showDatePicker()
            is DetailEvent.FilterByCompletion -> filterByCompletion(
                event.showCompleted,
                event.showIncomplete
            )

            is DetailEvent.DismissError -> dismissError()
            else -> {
                // Handle other events
            }
        }
    }

    init {
        loadData(LocalDate.now())
    }

    private fun loadData(date: LocalDate) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, selectedDate = date, error = null) }

            try {
                // Load selected date record
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val selectedRecord = wishCountRepository.getDailyRecord(dateString)

                // Load month records
                val monthStart = date.withDayOfMonth(1)
                val monthEnd = date.withDayOfMonth(date.lengthOfMonth())
                val monthRecords = wishCountRepository.getDailyRecords(limit = 31)
                    .filter { record ->
                        val recordDate =
                            LocalDate.parse(record.date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        recordDate >= monthStart && recordDate <= monthEnd
                    }

                // Load reset logs for selected date
                val resetLogs = if (selectedRecord != null) {
                    resetLogRepository.getResetLogsByDate(dateString)
                        .map { log ->
                            PresentationResetLog(
                                id = log.id,
                                time = log.displayTime,
                                countLost = log.countBeforeReset,
                                type = log.resetTypeText,
                                reason = log.resetReason
                            )
                        }
                } else {
                    emptyList()
                }

                // Load statistics for the month
                val statistics = wishCountRepository.getStatistics(
                    monthStart.toString(),
                    monthEnd.toString()
                )

                // Generate chart data
                val chartData = generateChartData(monthRecords)

                updateState {
                    copy(
                        isLoading = false,
                        selectedDate = date,
                        selectedRecord = selectedRecord,
                        monthlyRecords = monthRecords,
                        resetLogs = resetLogs,
                        statistics = statistics,
                        selectedMonth = date.withDayOfMonth(1),
                        chartData = chartData
                    )
                }

            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "데이터를 불러오는 중 오류가 발생했습니다"
                    )
                }
            }
        }
    }

    private fun selectDate(date: LocalDate) {
        if (date == currentState.selectedDate) return

        updateState { copy(selectedDate = date) }

        // Load data for selected date
        viewModelScope.launch {
            try {
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val selectedRecord = wishCountRepository.getDailyRecord(dateString)

                val resetLogs = if (selectedRecord != null) {
                    resetLogRepository.getResetLogsByDate(dateString)
                        .map { log ->
                            PresentationResetLog(
                                id = log.id,
                                time = log.displayTime,
                                countLost = log.countBeforeReset,
                                type = log.resetTypeText,
                                reason = log.resetReason
                            )
                        }
                } else {
                    emptyList()
                }

                updateState {
                    copy(
                        selectedRecord = selectedRecord,
                        resetLogs = resetLogs
                    )
                }

                sendEffect(DetailEffect.HighlightDate(date))
                sendEffect(DetailEffect.PlaySoundEffect(SoundType.TAP))

            } catch (e: Exception) {
                sendEffect(DetailEffect.ShowToast("날짜 데이터를 불러올 수 없습니다"))
            }
        }
    }

    private fun navigatePreviousMonth() {
        val newMonth = currentState.selectedMonth.minusMonths(1)
        updateState { copy(selectedMonth = newMonth) }
        loadMonthData(newMonth)

        sendEffect(DetailEffect.AnimateCalendarTransition(TransitionDirection.RIGHT))
    }

    private fun navigateNextMonth() {
        if (!currentState.canNavigateNext) return

        val newMonth = currentState.selectedMonth.plusMonths(1)
        updateState { copy(selectedMonth = newMonth) }
        loadMonthData(newMonth)

        sendEffect(DetailEffect.AnimateCalendarTransition(TransitionDirection.LEFT))
    }

    private fun navigateToToday() {
        val today = LocalDate.now()

        if (currentState.selectedMonth.month != today.month ||
            currentState.selectedMonth.year != today.year
        ) {
            updateState { copy(selectedMonth = today.withDayOfMonth(1)) }
            loadMonthData(today.withDayOfMonth(1))
        }

        selectDate(today)
        sendEffect(DetailEffect.ScrollToDate(today))
    }

    private fun loadMonthData(month: LocalDate) {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }

            try {
                val monthStart = month.withDayOfMonth(1)
                val monthEnd = month.withDayOfMonth(month.lengthOfMonth())

                val monthRecords = wishCountRepository.getDailyRecords(limit = 31)
                    .filter { record ->
                        val recordDate =
                            LocalDate.parse(record.date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        recordDate >= monthStart && recordDate <= monthEnd
                    }

                val statistics = wishCountRepository.getStatistics(
                    monthStart.toString(),
                    monthEnd.toString()
                )

                val chartData = generateChartData(monthRecords)

                updateState {
                    copy(
                        isRefreshing = false,
                        monthlyRecords = monthRecords,
                        statistics = statistics,
                        chartData = chartData
                    )
                }

            } catch (e: Exception) {
                updateState { copy(isRefreshing = false) }
                sendEffect(DetailEffect.ShowToast("월 데이터를 불러올 수 없습니다"))
            }
        }
    }

    private fun changeViewMode(mode: ViewMode) {
        updateState { copy(viewMode = mode) }

        // Generate appropriate chart data for the mode
        when (mode) {
            ViewMode.CHART -> {
                val chartData = generateChartData(currentState.monthlyRecords)
                updateState { copy(chartData = chartData) }
            }

            ViewMode.STATISTICS -> {
                loadStatisticsDetails()
            }

            else -> { /* Calendar mode - no additional data needed */
            }
        }

        sendEffect(DetailEffect.PlaySoundEffect(SoundType.SWIPE))
    }

    private fun showResetDetails(resetLog: PresentationResetLog) {
        updateState { copy(showResetDetails = true) }

        val selectedRecord = currentState.selectedRecord
        val resetDetails = ResetDetailsInfo(
            date = currentState.selectedDateDisplay,
            time = resetLog.time,
            countBefore = resetLog.countLost,
            targetCount = selectedRecord?.targetCount ?: 0,
            type = resetLog.type,
            reason = resetLog.reason,
            impact = when {
                resetLog.countLost == 0 -> "영향 없음"
                resetLog.countLost < 100 -> "낮음"
                resetLog.countLost < 500 -> "중간"
                else -> "높음"
            },
            progressLost = if (selectedRecord != null && selectedRecord.targetCount > 0) {
                resetLog.countLost.toFloat() / selectedRecord.targetCount
            } else {
                0f
            }
        )

        sendEffect(DetailEffect.ShowResetDetailsDialog(resetDetails))
    }

    private fun hideResetDetails() {
        updateState { copy(showResetDetails = false) }
    }

    private fun deleteResetLog(resetLogId: Long) {
        viewModelScope.launch {
            try {
                val success = resetLogRepository.deleteResetLog(resetLogId)

                if (success) {
                    // Reload reset logs
                    val dateString =
                        currentState.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val resetLogs = resetLogRepository.getResetLogsByDate(dateString)
                        .map { log ->
                            PresentationResetLog(
                                id = log.id,
                                time = log.displayTime,
                                countLost = log.countBeforeReset,
                                type = log.resetTypeText,
                                reason = log.resetReason
                            )
                        }

                    updateState { copy(resetLogs = resetLogs) }
                    sendEffect(DetailEffect.ShowToast("리셋 로그가 삭제되었습니다"))
                } else {
                    sendEffect(DetailEffect.ShowToast("삭제 실패"))
                }

            } catch (e: Exception) {
                sendEffect(DetailEffect.ShowToast("삭제 중 오류: ${e.message}"))
            }
        }
    }

    private fun editRecord(date: LocalDate) {
        sendEffect(DetailEffect.NavigateToEdit(date))
    }

    private fun deleteRecord(date: LocalDate) {
        sendEffect(
            DetailEffect.ShowDeleteConfirmation(date) {
                performDeleteRecord(date)
            }
        )
    }

    private fun performDeleteRecord(date: LocalDate) {
        viewModelScope.launch {
            try {
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val success = wishCountRepository.deleteWishCount(dateString)

                if (success) {
                    // Reload data
                    loadMonthData(currentState.selectedMonth)
                    sendEffect(DetailEffect.ShowToast("기록이 삭제되었습니다"))

                    // Clear selection if deleted date was selected
                    if (date == currentState.selectedDate) {
                        updateState { copy(selectedRecord = null, resetLogs = emptyList()) }
                    }
                } else {
                    sendEffect(DetailEffect.ShowToast("삭제 실패"))
                }

            } catch (e: Exception) {
                sendEffect(DetailEffect.ShowToast("삭제 중 오류: ${e.message}"))
            }
        }
    }


    private fun shareRecord(date: LocalDate) {
        val record = currentState.monthlyRecords.find { it.date == date } ?: return

        val shareText = """
            📅 ${record.displayDate}
            🎯 목표: ${record.targetCount}회
            ✅ 달성: ${record.totalCount}회 (${record.completionRate}%)
            💭 ${record.wishText}
            
            #WishRing #잠재의식 #긍정확언
        """.trimIndent()

        sendEffect(DetailEffect.ShowShareSheet(ShareContent(text = shareText)))
    }

    private fun exportData(startDate: LocalDate, endDate: LocalDate, format: ExportFormat) {
        viewModelScope.launch {
            try {
                val records = wishCountRepository.getWishCountsBetween(
                    startDate.toString(),
                    endDate.toString()
                )

                val exportData = when (format) {
                    ExportFormat.CSV -> generateCsvData(records)
                    ExportFormat.JSON -> generateJsonData(records)
                    ExportFormat.PDF -> generatePdfData(records)
                    ExportFormat.IMAGE -> generateImageData(records)
                }

                val fileName = "wish_records_${startDate}_${endDate}.${format.name.lowercase()}"

                sendEffect(
                    DetailEffect.ExportData(
                        data = exportData,
                        format = format,
                        fileName = fileName
                    )
                )

                sendEffect(DetailEffect.ShowToast("데이터 내보내기 완료"))

            } catch (e: Exception) {
                sendEffect(DetailEffect.ShowToast("내보내기 실패: ${e.message}"))
            }
        }
    }

    private fun refreshData() {
        loadData(currentState.selectedDate)
    }

    private fun navigateBack() {
        sendEffect(DetailEffect.NavigateBack)
    }

    private fun showStatisticsDetails() {
        val stats = currentState.statistics ?: return

        val statisticsDetails = StatisticsDetails(
            period = currentState.selectedMonthDisplay,
            totalDays = stats.totalDays,
            completedDays = stats.completedDays,
            totalCount = stats.totalCount,
            averageCount = stats.averageCount,
            bestDay = stats.bestDay?.displayDate,
            bestDayCount = stats.bestDay?.totalCount ?: 0,
            completionRate = stats.completionRate,
            streakCurrent = 0, // Would get from streak info
            streakBest = 0 // Would get from streak info
        )

        sendEffect(DetailEffect.ShowStatisticsDetailsDialog(statisticsDetails))
    }

    private fun showChartOptions() {
        sendEffect(
            DetailEffect.ShowChartOptionsDialog(
                currentType = currentState.chartType,
                onTypeSelected = { type ->
                    updateChartType(type)
                }
            )
        )
    }


    private fun updateChartType(chartType: ChartType) {
        val chartData = generateChartData(currentState.monthlyRecords, chartType)
        updateState { copy(chartData = chartData) }
    }

    private fun showDatePicker() {
        sendEffect(
            DetailEffect.ShowDatePicker(
                currentDate = currentState.selectedDate,
                onDateSelected = { date ->
                    selectDate(date)
                }
            )
        )
    }

    private fun filterByCompletion(showCompleted: Boolean, showIncomplete: Boolean) {
        // Apply filter logic
        val filtered = currentState.monthlyRecords.filter { record ->
            (showCompleted && record.isCompleted) ||
                    (showIncomplete && !record.isCompleted)
        }

        // Update chart with filtered data
        val chartData = generateChartData(filtered)
        updateState { copy(chartData = chartData) }
    }

    private fun loadStatisticsDetails() {
        // Load additional statistics data
        viewModelScope.launch {
            try {
                val yearStart = LocalDate.now().withDayOfYear(1)
                val yearEnd = LocalDate.now()

                val yearStats = wishCountRepository.getStatistics(
                    yearStart.toString(),
                    yearEnd.toString()
                )

                // Update state with year statistics
                // This would be used for the statistics view

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun generateChartData(
        records: List<DailyRecord>,
        type: ChartType = ChartType.BAR
    ): ChartData {
        val dataPoints = records.map { record ->
            ChartDataPoint(
                value = record.totalCount.toFloat(),
                label = record.date.dayOfMonth.toString(),
                color = if (record.isCompleted) "#4CAF50" else "#FF9800"
            )
        }

        return ChartData(
            type = type,
            dataPoints = dataPoints,
            labels = records.map { it.date.dayOfMonth.toString() },
            colors = records.map { if (it.isCompleted) "#4CAF50" else "#FF9800" }
        )
    }

    private fun generateCsvData(records: List<com.wishring.app.domain.model.WishCount>): String {
        val header = "Date,Wish Text,Target Count,Total Count,Completed\n"
        val rows = records.joinToString("\n") { record ->
            "${record.date},\"${record.wishText}\",${record.targetCount},${record.totalCount},${record.isCompleted}"
        }
        return header + rows
    }

    private fun generateJsonData(records: List<com.wishring.app.domain.model.WishCount>): String {
        // Convert to JSON format
        return "[]" // Simplified - would use actual JSON serialization
    }

    private fun generatePdfData(records: List<com.wishring.app.domain.model.WishCount>): String {
        // Generate PDF data
        return "" // Would use PDF library
    }

    private fun generateImageData(records: List<com.wishring.app.domain.model.WishCount>): String {
        // Generate image data
        return "" // Would use image generation library
    }

    private fun dismissError() {
        updateState { copy(error = null) }
    }
}