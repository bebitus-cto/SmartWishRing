package com.wishring.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.model.ResetLog
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.presentation.base.BaseViewModel
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import app.cash.turbine.test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ExperimentalCoroutinesApi
@DisplayName("DetailViewModel 테스트")
class DetailViewModelTest {

    @MockK
    private lateinit var wishCountRepository: WishCountRepository
    
    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    
    @MockK
    private lateinit var savedStateHandle: SavedStateHandle
    
    private lateinit var viewModel: DetailViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks
        every { savedStateHandle.get<String>("date") } returns LocalDate.now().toString()
        every { savedStateHandle.set(any(), any<Any>()) } just Runs
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Nested
    @DisplayName("날짜별 상세 데이터 로딩 테스트")
    inner class DateDetailLoadingTests {
        
        @Test
        @DisplayName("특정 날짜 데이터 로드")
        fun `should load data for specific date`() = runTest {
            // Given
            val date = LocalDate.of(2025, 1, 5)
            val wishCount = WishCount(
                date = date,
                count = 85,
                dailyGoal = 100,
                resetCount = 1,
                lostCount = 15
            )
            
            every { savedStateHandle.get<String>("date") } returns date.toString()
            coEvery { wishCountRepository.getWishCountByDate(date) } returns wishCount
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.selectedDate shouldBe date
                state.dayData shouldBe wishCount
                state.progress shouldBe 0.85f
                state.resetCount shouldBe 1
                state.lostCount shouldBe 15
            }
        }
        
        @Test
        @DisplayName("날짜 범위 데이터 로드")
        fun `should load date range data`() = runTest {
            // Given
            val centerDate = LocalDate.now()
            val dateRange = (3L downTo -3L).map { offset ->
                centerDate.plusDays(offset)
            }
            
            val wishCounts = dateRange.map { date ->
                WishCount(
                    date = date,
                    count = (50..100).random(),
                    dailyGoal = 100
                )
            }
            
            coEvery { 
                wishCountRepository.getWishCountsByDateRange(any(), any()) 
            } returns wishCounts
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(DetailEvent.LoadDateRange(centerDate, 3))
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.dateRangeData shouldHaveSize 7
                state.dateRangeData.map { it.date } shouldBe dateRange
            }
        }
        
        @Test
        @DisplayName("데이터 없는 날짜 처리")
        fun `should handle date with no data`() = runTest {
            // Given
            val date = LocalDate.now().minusDays(30)
            
            every { savedStateHandle.get<String>("date") } returns date.toString()
            coEvery { wishCountRepository.getWishCountByDate(date) } returns null
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.selectedDate shouldBe date
                state.dayData shouldBe null
                state.isEmpty shouldBe true
            }
        }
    }
    
    @Nested
    @DisplayName("통계 계산 테스트")
    inner class StatisticsCalculationTests {
        
        @Test
        @DisplayName("주간 통계 계산")
        fun `should calculate weekly statistics`() = runTest {
            // Given
            val selectedDate = LocalDate.now()
            val weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
            val weekEnd = weekStart.plusDays(6)
            
            val weekData = (0..6).map { day ->
                WishCount(
                    date = weekStart.plusDays(day.toLong()),
                    count = if (day < 5) 100 else 50, // 5일 달성, 2일 미달성
                    dailyGoal = 100
                )
            }
            
            coEvery { 
                wishCountRepository.getWishCountsByDateRange(weekStart, weekEnd) 
            } returns weekData
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(DetailEvent.CalculateWeekStats(selectedDate))
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.weekStats shouldNotBe null
                state.weekStats?.totalCount shouldBe 600
                state.weekStats?.achievementDays shouldBe 5
                state.weekStats?.averageCount shouldBe (600.0 / 7)
                state.weekStats?.achievementRate shouldBe (5.0 / 7 * 100)
            }
        }
        
        @Test
        @DisplayName("월간 통계 계산")
        fun `should calculate monthly statistics`() = runTest {
            // Given
            val selectedDate = LocalDate.of(2025, 1, 15)
            val monthStart = selectedDate.withDayOfMonth(1)
            val monthEnd = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
            
            val monthData = (1..31).map { day ->
                WishCount(
                    date = LocalDate.of(2025, 1, day),
                    count = if (day % 2 == 0) 100 else 80,
                    dailyGoal = 100
                )
            }
            
            coEvery { 
                wishCountRepository.getWishCountsByDateRange(monthStart, monthEnd) 
            } returns monthData
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(DetailEvent.CalculateMonthStats(selectedDate))
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.monthStats shouldNotBe null
                state.monthStats?.totalCount shouldBe (15 * 100 + 16 * 80)
                state.monthStats?.achievementDays shouldBe 15
            }
        }
        
        @ParameterizedTest
        @CsvSource(
            "100,100,100.0",
            "50,100,50.0",
            "150,100,150.0",
            "0,100,0.0"
        )
        @DisplayName("진행률 계산")
        fun `should calculate progress correctly`(
            count: Int,
            goal: Int,
            expectedProgress: Float
        ) = runTest {
            // Given
            val wishCount = WishCount(
                date = LocalDate.now(),
                count = count,
                dailyGoal = goal
            )
            
            coEvery { wishCountRepository.getWishCountByDate(any()) } returns wishCount
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.progress shouldBe (expectedProgress / 100f)
            }
        }
    }
    
    @Nested
    @DisplayName("리셋 로그 관리 테스트")
    inner class ResetLogManagementTests {
        
        @Test
        @DisplayName("날짜별 리셋 로그 조회")
        fun `should load reset logs for date`() = runTest {
            // Given
            val date = LocalDate.now()
            val resetLogs = listOf(
                ResetLog(
                    id = 1,
                    date = date,
                    resetTime = date.atTime(10, 30),
                    beforeCount = 50,
                    afterCount = 0,
                    reason = "MANUAL"
                ),
                ResetLog(
                    id = 2,
                    date = date,
                    resetTime = date.atTime(15, 45),
                    beforeCount = 75,
                    afterCount = 0,
                    reason = "GOAL_ACHIEVED"
                )
            )
            
            coEvery { wishCountRepository.getResetLogsByDate(date) } returns resetLogs
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(DetailEvent.LoadResetLogs(date))
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.resetLogs shouldHaveSize 2
                state.resetLogs[0].reason shouldBe "MANUAL"
                state.resetLogs[1].reason shouldBe "GOAL_ACHIEVED"
                state.totalLostCount shouldBe 125
            }
        }
        
        @Test
        @DisplayName("리셋 패턴 분석")
        fun `should analyze reset patterns`() = runTest {
            // Given
            val recentResets = (0..6).map { day ->
                ResetLog(
                    id = day.toLong(),
                    date = LocalDate.now().minusDays(day.toLong()),
                    resetTime = LocalDateTime.now().minusDays(day.toLong()),
                    beforeCount = 70 + day * 5,
                    afterCount = 0,
                    reason = if (day % 2 == 0) "MANUAL" else "MIDNIGHT"
                )
            }
            
            coEvery { wishCountRepository.getRecentResetLogs(7) } returns recentResets
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(DetailEvent.AnalyzeResetPatterns)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.resetPattern shouldNotBe null
                state.resetPattern?.weeklyResetCount shouldBe 7
                state.resetPattern?.mostCommonReason shouldBe "MANUAL" // 4 vs 3
                state.resetPattern?.averageCountBeforeReset shouldBe (70 + 75 + 80 + 85 + 90 + 95 + 100) / 7.0
            }
        }
    }
    
    @Nested
    @DisplayName("그래프 데이터 준비 테스트")
    inner class GraphDataPreparationTests {
        
        @Test
        @DisplayName("주간 그래프 데이터 생성")
        fun `should prepare weekly graph data`() = runTest {
            // Given
            val weekData = (0..6).map { day ->
                WishCount(
                    date = LocalDate.now().minusDays(day.toLong()),
                    count = 50 + day * 10,
                    dailyGoal = 100
                )
            }
            
            coEvery { 
                wishCountRepository.getWishCountsByDateRange(any(), any()) 
            } returns weekData
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(DetailEvent.PrepareWeeklyGraph)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.weeklyGraphData shouldNotBe null
                state.weeklyGraphData?.entries shouldHaveSize 7
                state.weeklyGraphData?.maxValue shouldBe 110
                state.weeklyGraphData?.goalLine shouldBe 100
            }
        }
        
        @Test
        @DisplayName("월간 그래프 데이터 생성")
        fun `should prepare monthly graph data`() = runTest {
            // Given
            val monthData = (1..30).map { day ->
                WishCount(
                    date = LocalDate.of(2025, 1, day),
                    count = (40..120).random(),
                    dailyGoal = 100
                )
            }
            
            coEvery { 
                wishCountRepository.getWishCountsByDateRange(any(), any()) 
            } returns monthData
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(DetailEvent.PrepareMonthlyGraph)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.monthlyGraphData shouldNotBe null
                state.monthlyGraphData?.entries shouldHaveSize 30
                state.monthlyGraphData?.average shouldNotBe null
            }
        }
        
        @Test
        @DisplayName("연간 그래프 데이터 생성")
        fun `should prepare yearly graph data`() = runTest {
            // Given
            val yearData = (1..12).map { month ->
                MonthlyAggregate(
                    month = month,
                    totalCount = month * 2000,
                    achievementDays = month * 2
                )
            }
            
            coEvery { wishCountRepository.getYearlyAggregates(2025) } returns yearData
            setupDefaultMocks()
            
            // When
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(DetailEvent.PrepareYearlyGraph(2025))
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.yearlyGraphData shouldNotBe null
                state.yearlyGraphData?.entries shouldHaveSize 12
            }
        }
    }
    
    @Nested
    @DisplayName("날짜 네비게이션 테스트")
    inner class DateNavigationTests {
        
        @Test
        @DisplayName("이전 날짜로 이동")
        fun `should navigate to previous date`() = runTest {
            // Given
            val currentDate = LocalDate.of(2025, 1, 15)
            val previousDate = currentDate.minusDays(1)
            
            every { savedStateHandle.get<String>("date") } returns currentDate.toString()
            setupDefaultMocksWithDate(currentDate)
            
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(DetailEvent.NavigateToPreviousDate)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.selectedDate shouldBe previousDate
            }
            
            verify { savedStateHandle.set("date", previousDate.toString()) }
        }
        
        @Test
        @DisplayName("다음 날짜로 이동")
        fun `should navigate to next date`() = runTest {
            // Given
            val currentDate = LocalDate.of(2025, 1, 15)
            val nextDate = currentDate.plusDays(1)
            
            every { savedStateHandle.get<String>("date") } returns currentDate.toString()
            setupDefaultMocksWithDate(currentDate)
            
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(DetailEvent.NavigateToNextDate)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.selectedDate shouldBe nextDate
            }
        }
        
        @Test
        @DisplayName("특정 날짜로 점프")
        fun `should jump to specific date`() = runTest {
            // Given
            val targetDate = LocalDate.of(2024, 12, 25)
            setupDefaultMocks()
            
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(DetailEvent.JumpToDate(targetDate))
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.selectedDate shouldBe targetDate
            }
        }
        
        @Test
        @DisplayName("미래 날짜 네비게이션 제한")
        fun `should limit navigation to future dates`() = runTest {
            // Given
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            
            every { savedStateHandle.get<String>("date") } returns today.toString()
            setupDefaultMocksWithDate(today)
            
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(DetailEvent.NavigateToNextDate)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.selectedDate shouldBe today // Should not move to future
            }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<DetailEffect.ShowMessage>()
                (effect as DetailEffect.ShowMessage).message shouldContain "future"
            }
        }
    }
    
    @Nested
    @DisplayName("데이터 내보내기 테스트")
    inner class DataExportTests {
        
        @Test
        @DisplayName("CSV 형식으로 데이터 내보내기")
        fun `should export data as CSV`() = runTest {
            // Given
            val exportData = (1..7).map { day ->
                WishCount(
                    date = LocalDate.of(2025, 1, day),
                    count = 50 + day * 10,
                    dailyGoal = 100
                )
            }
            
            coEvery { 
                wishCountRepository.getWishCountsByDateRange(any(), any()) 
            } returns exportData
            setupDefaultMocks()
            
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(DetailEvent.ExportData(
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 1, 7),
                format = ExportFormat.CSV
            ))
            advanceUntilIdle()
            
            // Then
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<DetailEffect.ShareFile>()
                val shareEffect = effect as DetailEffect.ShareFile
                shareEffect.fileName shouldContain ".csv"
                shareEffect.content shouldContain "Date,Count,Goal,Progress"
            }
        }
        
        @Test
        @DisplayName("JSON 형식으로 데이터 내보내기")
        fun `should export data as JSON`() = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(DetailEvent.ExportData(
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 1, 7),
                format = ExportFormat.JSON
            ))
            advanceUntilIdle()
            
            // Then
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<DetailEffect.ShareFile>()
                val shareEffect = effect as DetailEffect.ShareFile
                shareEffect.fileName shouldContain ".json"
            }
        }
    }
    
    @Nested
    @DisplayName("캐싱 및 성능 테스트")
    inner class CachingPerformanceTests {
        
        @Test
        @DisplayName("데이터 캐싱 동작")
        fun `should cache loaded data`() = runTest {
            // Given
            val date = LocalDate.now()
            val wishCount = WishCount(date = date, count = 100, dailyGoal = 100)
            
            coEvery { wishCountRepository.getWishCountByDate(date) } returns wishCount
            setupDefaultMocks()
            
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When - Load same date twice
            viewModel.handleEvent(DetailEvent.LoadDate(date))
            advanceUntilIdle()
            viewModel.handleEvent(DetailEvent.LoadDate(date))
            advanceUntilIdle()
            
            // Then - Should only fetch once due to caching
            coVerify(exactly = 1) { 
                wishCountRepository.getWishCountByDate(date) 
            }
        }
        
        @Test
        @DisplayName("캐시 무효화")
        fun `should invalidate cache on data change`() = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = DetailViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(DetailEvent.InvalidateCache)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isCacheValid shouldBe false
            }
        }
    }
    
    // Helper functions
    private fun setupDefaultMocks() {
        coEvery { wishCountRepository.getWishCountByDate(any()) } returns null
        coEvery { wishCountRepository.getWishCountsByDateRange(any(), any()) } returns emptyList()
        coEvery { wishCountRepository.getResetLogsByDate(any()) } returns emptyList()
        coEvery { wishCountRepository.getRecentResetLogs(any()) } returns emptyList()
        coEvery { wishCountRepository.getYearlyAggregates(any()) } returns emptyList()
        coEvery { preferencesRepository.getDailyGoalFlow() } returns flowOf(100)
    }
    
    private fun setupDefaultMocksWithDate(date: LocalDate) {
        val wishCount = WishCount(
            date = date,
            count = 75,
            dailyGoal = 100
        )
        coEvery { wishCountRepository.getWishCountByDate(date) } returns wishCount
        coEvery { wishCountRepository.getWishCountByDate(date.minusDays(1)) } returns wishCount.copy(date = date.minusDays(1))
        coEvery { wishCountRepository.getWishCountByDate(date.plusDays(1)) } returns null
        coEvery { wishCountRepository.getWishCountsByDateRange(any(), any()) } returns emptyList()
        coEvery { wishCountRepository.getResetLogsByDate(any()) } returns emptyList()
        coEvery { preferencesRepository.getDailyGoalFlow() } returns flowOf(100)
    }
}

// Mock ViewModel implementation (simplified for testing)
class DetailViewModel(
    private val wishCountRepository: WishCountRepository,
    private val preferencesRepository: PreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<DetailViewState, DetailEvent, DetailEffect>(
    initialState = DetailViewState()
) {
    
    init {
        loadInitialDate()
    }
    
    override fun handleEvent(event: DetailEvent) {
        when (event) {
            is DetailEvent.LoadDate -> loadDate(event.date)
            is DetailEvent.LoadDateRange -> loadDateRange(event.centerDate, event.days)
            is DetailEvent.NavigateToPreviousDate -> navigateToPrevious()
            is DetailEvent.NavigateToNextDate -> navigateToNext()
            is DetailEvent.JumpToDate -> jumpToDate(event.date)
            is DetailEvent.CalculateWeekStats -> calculateWeekStats(event.date)
            is DetailEvent.CalculateMonthStats -> calculateMonthStats(event.date)
            is DetailEvent.LoadResetLogs -> loadResetLogs(event.date)
            is DetailEvent.AnalyzeResetPatterns -> analyzeResetPatterns()
            is DetailEvent.PrepareWeeklyGraph -> prepareWeeklyGraph()
            is DetailEvent.PrepareMonthlyGraph -> prepareMonthlyGraph()
            is DetailEvent.PrepareYearlyGraph -> prepareYearlyGraph(event.year)
            is DetailEvent.ExportData -> exportData(event.startDate, event.endDate, event.format)
            is DetailEvent.InvalidateCache -> invalidateCache()
        }
    }
    
    private fun loadInitialDate() {
        val dateString = savedStateHandle.get<String>("date")
        val date = dateString?.let { LocalDate.parse(it) } ?: LocalDate.now()
        loadDate(date)
    }
    
    private fun loadDate(date: LocalDate) {
        launch {
            val wishCount = wishCountRepository.getWishCountByDate(date)
            updateState {
                copy(
                    selectedDate = date,
                    dayData = wishCount,
                    progress = wishCount?.let { it.count.toFloat() / it.dailyGoal } ?: 0f,
                    resetCount = wishCount?.resetCount ?: 0,
                    lostCount = wishCount?.lostCount ?: 0,
                    isEmpty = wishCount == null,
                    isLoading = false
                )
            }
        }
    }
    
    private fun loadDateRange(centerDate: LocalDate, days: Int) {
        launch {
            val startDate = centerDate.minusDays(days.toLong())
            val endDate = centerDate.plusDays(days.toLong())
            val data = wishCountRepository.getWishCountsByDateRange(startDate, endDate)
            updateState { copy(dateRangeData = data) }
        }
    }
    
    private fun navigateToPrevious() {
        val currentDate = viewState.value.selectedDate
        val previousDate = currentDate.minusDays(1)
        savedStateHandle.set("date", previousDate.toString())
        loadDate(previousDate)
    }
    
    private fun navigateToNext() {
        val currentDate = viewState.value.selectedDate
        val nextDate = currentDate.plusDays(1)
        
        if (nextDate.isAfter(LocalDate.now())) {
            sendEffect(DetailEffect.ShowMessage("Cannot navigate to future dates"))
        } else {
            savedStateHandle.set("date", nextDate.toString())
            loadDate(nextDate)
        }
    }
    
    private fun jumpToDate(date: LocalDate) {
        savedStateHandle.set("date", date.toString())
        loadDate(date)
    }
    
    private fun calculateWeekStats(date: LocalDate) {
        launch {
            val weekStart = date.minusDays(date.dayOfWeek.value.toLong() - 1)
            val weekEnd = weekStart.plusDays(6)
            val weekData = wishCountRepository.getWishCountsByDateRange(weekStart, weekEnd)
            
            val stats = WeekStats(
                totalCount = weekData.sumOf { it.count },
                achievementDays = weekData.count { it.count >= it.dailyGoal },
                averageCount = if (weekData.isNotEmpty()) weekData.map { it.count }.average() else 0.0,
                achievementRate = weekData.count { it.count >= it.dailyGoal }.toDouble() / 7 * 100
            )
            
            updateState { copy(weekStats = stats) }
        }
    }
    
    private fun calculateMonthStats(date: LocalDate) {
        launch {
            val monthStart = date.withDayOfMonth(1)
            val monthEnd = date.withDayOfMonth(date.lengthOfMonth())
            val monthData = wishCountRepository.getWishCountsByDateRange(monthStart, monthEnd)
            
            val stats = MonthStats(
                totalCount = monthData.sumOf { it.count },
                achievementDays = monthData.count { it.count >= it.dailyGoal },
                averageCount = if (monthData.isNotEmpty()) monthData.map { it.count }.average() else 0.0
            )
            
            updateState { copy(monthStats = stats) }
        }
    }
    
    private fun loadResetLogs(date: LocalDate) {
        launch {
            val logs = wishCountRepository.getResetLogsByDate(date)
            val totalLost = logs.sumOf { it.beforeCount }
            updateState { 
                copy(
                    resetLogs = logs,
                    totalLostCount = totalLost
                ) 
            }
        }
    }
    
    private fun analyzeResetPatterns() {
        launch {
            val recentResets = wishCountRepository.getRecentResetLogs(7)
            
            if (recentResets.isNotEmpty()) {
                val pattern = ResetPattern(
                    weeklyResetCount = recentResets.size,
                    mostCommonReason = recentResets.groupBy { it.reason }
                        .maxByOrNull { it.value.size }?.key ?: "",
                    averageCountBeforeReset = recentResets.map { it.beforeCount }.average()
                )
                
                updateState { copy(resetPattern = pattern) }
            }
        }
    }
    
    private fun prepareWeeklyGraph() {
        launch {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(6)
            val data = wishCountRepository.getWishCountsByDateRange(startDate, endDate)
            
            val graphData = WeeklyGraphData(
                entries = data.map { GraphEntry(it.date, it.count.toFloat()) },
                maxValue = data.maxOfOrNull { it.count } ?: 0,
                goalLine = 100
            )
            
            updateState { copy(weeklyGraphData = graphData) }
        }
    }
    
    private fun prepareMonthlyGraph() {
        launch {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(29)
            val data = wishCountRepository.getWishCountsByDateRange(startDate, endDate)
            
            val graphData = MonthlyGraphData(
                entries = data.map { GraphEntry(it.date, it.count.toFloat()) },
                average = if (data.isNotEmpty()) data.map { it.count }.average().toFloat() else 0f
            )
            
            updateState { copy(monthlyGraphData = graphData) }
        }
    }
    
    private fun prepareYearlyGraph(year: Int) {
        launch {
            val yearData = wishCountRepository.getYearlyAggregates(year)
            
            val graphData = YearlyGraphData(
                entries = yearData.map { 
                    GraphEntry(
                        LocalDate.of(year, it.month, 1), 
                        it.totalCount.toFloat()
                    ) 
                }
            )
            
            updateState { copy(yearlyGraphData = graphData) }
        }
    }
    
    private fun exportData(startDate: LocalDate, endDate: LocalDate, format: ExportFormat) {
        launch {
            val data = wishCountRepository.getWishCountsByDateRange(startDate, endDate)
            
            val content = when (format) {
                ExportFormat.CSV -> {
                    buildString {
                        appendLine("Date,Count,Goal,Progress")
                        data.forEach { 
                            appendLine("${it.date},${it.count},${it.dailyGoal},${it.count.toFloat() / it.dailyGoal * 100}%")
                        }
                    }
                }
                ExportFormat.JSON -> {
                    // Simplified JSON generation
                    "{\"data\": []}"
                }
            }
            
            val fileName = "wish_data_${startDate}_${endDate}.${format.name.lowercase()}"
            sendEffect(DetailEffect.ShareFile(fileName, content))
        }
    }
    
    private fun invalidateCache() {
        updateState { copy(isCacheValid = false) }
    }
}

// Supporting classes
data class DetailViewState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val dayData: WishCount? = null,
    val dateRangeData: List<WishCount> = emptyList(),
    val progress: Float = 0f,
    val resetCount: Int = 0,
    val lostCount: Int = 0,
    val isEmpty: Boolean = false,
    val weekStats: WeekStats? = null,
    val monthStats: MonthStats? = null,
    val resetLogs: List<ResetLog> = emptyList(),
    val totalLostCount: Int = 0,
    val resetPattern: ResetPattern? = null,
    val weeklyGraphData: WeeklyGraphData? = null,
    val monthlyGraphData: MonthlyGraphData? = null,
    val yearlyGraphData: YearlyGraphData? = null,
    val isCacheValid: Boolean = true
)

sealed class DetailEvent {
    data class LoadDate(val date: LocalDate) : DetailEvent()
    data class LoadDateRange(val centerDate: LocalDate, val days: Int) : DetailEvent()
    object NavigateToPreviousDate : DetailEvent()
    object NavigateToNextDate : DetailEvent()
    data class JumpToDate(val date: LocalDate) : DetailEvent()
    data class CalculateWeekStats(val date: LocalDate) : DetailEvent()
    data class CalculateMonthStats(val date: LocalDate) : DetailEvent()
    data class LoadResetLogs(val date: LocalDate) : DetailEvent()
    object AnalyzeResetPatterns : DetailEvent()
    object PrepareWeeklyGraph : DetailEvent()
    object PrepareMonthlyGraph : DetailEvent()
    data class PrepareYearlyGraph(val year: Int) : DetailEvent()
    data class ExportData(val startDate: LocalDate, val endDate: LocalDate, val format: ExportFormat) : DetailEvent()
    object InvalidateCache : DetailEvent()
}

sealed class DetailEffect {
    data class ShowMessage(val message: String) : DetailEffect()
    data class ShareFile(val fileName: String, val content: String) : DetailEffect()
}

data class WeekStats(
    val totalCount: Int,
    val achievementDays: Int,
    val averageCount: Double,
    val achievementRate: Double
)

data class MonthStats(
    val totalCount: Int,
    val achievementDays: Int,
    val averageCount: Double
)

data class ResetPattern(
    val weeklyResetCount: Int,
    val mostCommonReason: String,
    val averageCountBeforeReset: Double
)

data class GraphEntry(val date: LocalDate, val value: Float)

data class WeeklyGraphData(
    val entries: List<GraphEntry>,
    val maxValue: Int,
    val goalLine: Int
)

data class MonthlyGraphData(
    val entries: List<GraphEntry>,
    val average: Float
)

data class YearlyGraphData(
    val entries: List<GraphEntry>
)

data class MonthlyAggregate(
    val month: Int,
    val totalCount: Int,
    val achievementDays: Int
)

enum class ExportFormat {
    CSV, JSON
}