package com.wishring.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.domain.repository.BleRepository
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
import io.kotest.matchers.types.shouldBeInstanceOf
import app.cash.turbine.test
import java.time.LocalDate
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
@DisplayName("HomeViewModel 테스트")
class HomeViewModelTest {

    @MockK
    private lateinit var wishCountRepository: WishCountRepository
    
    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    
    @MockK
    private lateinit var bleRepository: BleRepository
    
    @MockK
    private lateinit var savedStateHandle: SavedStateHandle
    
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks
        every { savedStateHandle.get<Any>(any()) } returns null
        every { savedStateHandle.set(any(), any<Any>()) } just Runs
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Nested
    @DisplayName("초기화 및 상태 관리 테스트")
    inner class InitializationTests {
        
        @Test
        @DisplayName("ViewModel 초기 상태 확인")
        fun `should have correct initial state`() = runTest {
            // Given
            setupDefaultMocks()
            
            // When
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // Then
            viewModel.viewState.test {
                val initialState = awaitItem()
                initialState.isLoading shouldBe true
                initialState.todayCount shouldBe 0
                initialState.dailyGoal shouldBe 100
                initialState.progress shouldBe 0f
                initialState.currentStreak shouldBe 0
                initialState.isConnected shouldBe false
            }
        }
        
        @Test
        @DisplayName("데이터 로드 성공")
        fun `should load data successfully`() = runTest {
            // Given
            val wishCount = WishCount(
                date = LocalDate.now(),
                count = 75,
                dailyGoal = 100
            )
            
            every { wishCountRepository.getTodayWishCount() } returns flowOf(wishCount)
            every { preferencesRepository.getDailyGoalFlow() } returns flowOf(100)
            every { wishCountRepository.getCurrentStreak() } coReturns 5
            every { bleRepository.connectionState } returns flowOf(BleConnectionState.Connected("device"))
            
            // When
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isLoading shouldBe false
                state.todayCount shouldBe 75
                state.dailyGoal shouldBe 100
                state.progress shouldBe 0.75f
                state.currentStreak shouldBe 5
                state.isConnected shouldBe true
            }
        }
        
        @Test
        @DisplayName("로딩 에러 처리")
        fun `should handle loading error`() = runTest {
            // Given
            every { wishCountRepository.getTodayWishCount() } returns flow {
                throw Exception("Database error")
            }
            every { preferencesRepository.getDailyGoalFlow() } returns flowOf(100)
            every { bleRepository.connectionState } returns flowOf(BleConnectionState.Disconnected)
            
            // When
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            advanceUntilIdle()
            
            // Then
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<HomeEffect.ShowError>()
                (effect as HomeEffect.ShowError).message shouldBe "Database error"
            }
        }
    }
    
    @Nested
    @DisplayName("사용자 이벤트 처리 테스트")
    inner class EventHandlingTests {
        
        @Test
        @DisplayName("위시 카운트 증가 이벤트")
        fun `should handle increment wish count event`() = runTest {
            // Given
            setupDefaultMocks()
            coEvery { wishCountRepository.incrementCount() } just Runs
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(HomeEvent.IncrementWishCount)
            advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.incrementCount() }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<HomeEffect.ShowCountAnimation>()
            }
        }
        
        @Test
        @DisplayName("수동 리셋 이벤트")
        fun `should handle manual reset event`() = runTest {
            // Given
            setupDefaultMocks()
            coEvery { wishCountRepository.resetCount(any()) } just Runs
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(HomeEvent.ResetCount)
            advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.resetCount(ResetReason.MANUAL) }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<HomeEffect.ShowResetConfirmation>()
            }
        }
        
        @Test
        @DisplayName("BLE 연결 토글 이벤트")
        fun `should handle BLE connection toggle`() = runTest {
            // Given
            setupDefaultMocks()
            every { bleRepository.connectionState } returns MutableStateFlow(BleConnectionState.Disconnected)
            coEvery { bleRepository.connect(any()) } just Runs
            coEvery { bleRepository.disconnect() } just Runs
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When - Connect
            viewModel.handleEvent(HomeEvent.ToggleBleConnection)
            advanceUntilIdle()
            
            // Then
            coVerify { bleRepository.connect(any()) }
            
            // When - Disconnect
            viewModel.updateState { copy(isConnected = true) }
            viewModel.handleEvent(HomeEvent.ToggleBleConnection)
            advanceUntilIdle()
            
            // Then
            coVerify { bleRepository.disconnect() }
        }
        
        @Test
        @DisplayName("새로고침 이벤트")
        fun `should handle refresh event`() = runTest {
            // Given
            setupDefaultMocks()
            coEvery { wishCountRepository.getCurrentStreak() } coReturnsMany listOf(3, 5)
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(HomeEvent.Refresh)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isRefreshing shouldBe false
                state.currentStreak shouldBe 5
            }
        }
    }
    
    @Nested
    @DisplayName("목표 달성 로직 테스트")
    inner class GoalAchievementTests {
        
        @ParameterizedTest
        @CsvSource(
            "100,100,true",
            "99,100,false",
            "150,100,true",
            "0,100,false"
        )
        @DisplayName("목표 달성 판단")
        fun `should determine goal achievement correctly`(
            count: Int,
            goal: Int,
            expected: Boolean
        ) = runTest {
            // Given
            val wishCount = WishCount(
                date = LocalDate.now(),
                count = count,
                dailyGoal = goal
            )
            
            every { wishCountRepository.getTodayWishCount() } returns flowOf(wishCount)
            every { preferencesRepository.getDailyGoalFlow() } returns flowOf(goal)
            setupBleAndStreakMocks()
            
            // When
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isGoalAchieved shouldBe expected
            }
            
            if (expected) {
                viewModel.effect.test {
                    val effect = awaitItem()
                    effect.shouldBeInstanceOf<HomeEffect.ShowGoalAchievement>()
                }
            }
        }
        
        @Test
        @DisplayName("목표 달성 시 자동 리셋 옵션")
        fun `should auto reset on goal achievement if enabled`() = runTest {
            // Given
            val wishCount = WishCount(
                date = LocalDate.now(),
                count = 100,
                dailyGoal = 100
            )
            
            every { wishCountRepository.getTodayWishCount() } returns flowOf(wishCount)
            every { preferencesRepository.getDailyGoalFlow() } returns flowOf(100)
            every { preferencesRepository.isAutoResetOnGoal() } coReturns true
            coEvery { wishCountRepository.resetCount(ResetReason.GOAL_ACHIEVED) } just Runs
            setupBleAndStreakMocks()
            
            // When
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.resetCount(ResetReason.GOAL_ACHIEVED) }
        }
    }
    
    @Nested
    @DisplayName("통계 업데이트 테스트")
    inner class StatisticsUpdateTests {
        
        @Test
        @DisplayName("주간 통계 조회")
        fun `should load weekly statistics`() = runTest {
            // Given
            setupDefaultMocks()
            val weeklyStats = WeeklyStatistics(
                totalCount = 500,
                averageCount = 71.4,
                achievementDays = 5
            )
            
            coEvery { wishCountRepository.getWeeklyStatistics() } returns weeklyStats
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(HomeEvent.LoadStatistics)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.weeklyStats shouldBe weeklyStats
            }
        }
        
        @Test
        @DisplayName("최근 7일 기록 조회")
        fun `should load recent records`() = runTest {
            // Given
            setupDefaultMocks()
            val recentRecords = (0..6).map { day ->
                DailyRecord(
                    date = LocalDate.now().minusDays(day.toLong()),
                    count = 50 + day * 10,
                    goalAchieved = day % 2 == 0
                )
            }
            
            coEvery { wishCountRepository.getRecentRecords(7) } returns recentRecords
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(HomeEvent.LoadRecentRecords)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.recentRecords shouldBe recentRecords
            }
        }
    }
    
    @Nested
    @DisplayName("BLE 상태 동기화 테스트")
    inner class BleStateSyncTests {
        
        @Test
        @DisplayName("BLE 연결 상태 변경 감지")
        fun `should detect BLE connection state changes`() = runTest {
            // Given
            val connectionStateFlow = MutableStateFlow<BleConnectionState>(
                BleConnectionState.Disconnected
            )
            
            setupDefaultMocksWithCustomBle(connectionStateFlow)
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // Then - Initially disconnected
            viewModel.viewState.test {
                expectMostRecentItem().isConnected shouldBe false
            }
            
            // When - Connect
            connectionStateFlow.value = BleConnectionState.Connecting
            advanceUntilIdle()
            
            viewModel.viewState.test {
                expectMostRecentItem().isConnecting shouldBe true
            }
            
            // When - Connected
            connectionStateFlow.value = BleConnectionState.Connected("device_address")
            advanceUntilIdle()
            
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isConnected shouldBe true
                state.isConnecting shouldBe false
                state.connectedDeviceAddress shouldBe "device_address"
            }
        }
        
        @Test
        @DisplayName("BLE 버튼 프레스 이벤트 수신")
        fun `should receive BLE button press events`() = runTest {
            // Given
            val buttonEventFlow = MutableSharedFlow<BleButtonEvent>()
            
            setupDefaultMocks()
            every { bleRepository.buttonEvents } returns buttonEventFlow
            coEvery { wishCountRepository.incrementCount() } just Runs
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            buttonEventFlow.emit(BleButtonEvent.SinglePress)
            advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.incrementCount() }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<HomeEffect.ShowCountAnimation>()
            }
        }
        
        @Test
        @DisplayName("BLE 배터리 레벨 업데이트")
        fun `should update battery level from BLE`() = runTest {
            // Given
            val batteryFlow = MutableStateFlow(85)
            
            setupDefaultMocks()
            every { bleRepository.batteryLevel } returns batteryFlow
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                expectMostRecentItem().batteryLevel shouldBe 85
            }
            
            // When - Battery level changes
            batteryFlow.value = 50
            advanceUntilIdle()
            
            viewModel.viewState.test {
                expectMostRecentItem().batteryLevel shouldBe 50
            }
            
            // When - Low battery
            batteryFlow.value = 15
            advanceUntilIdle()
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<HomeEffect.ShowLowBatteryWarning>()
            }
        }
    }
    
    @Nested
    @DisplayName("상태 복원 테스트")
    inner class StateRestorationTests {
        
        @Test
        @DisplayName("SavedStateHandle에서 상태 복원")
        fun `should restore state from SavedStateHandle`() = runTest {
            // Given
            every { savedStateHandle.get<Int>("todayCount") } returns 75
            every { savedStateHandle.get<Int>("dailyGoal") } returns 150
            every { savedStateHandle.get<Int>("currentStreak") } returns 10
            every { savedStateHandle.get<Boolean>("isConnected") } returns true
            
            setupDefaultMocks()
            
            // When
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // Then
            viewModel.viewState.test {
                val state = awaitItem()
                state.todayCount shouldBe 75
                state.dailyGoal shouldBe 150
                state.currentStreak shouldBe 10
                state.isConnected shouldBe true
            }
        }
        
        @Test
        @DisplayName("상태 변경 시 SavedStateHandle 업데이트")
        fun `should update SavedStateHandle on state change`() = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.updateState {
                copy(todayCount = 100, currentStreak = 15)
            }
            
            // Then
            verify {
                savedStateHandle.set("todayCount", 100)
                savedStateHandle.set("currentStreak", 15)
            }
        }
    }
    
    @Nested
    @DisplayName("동시성 및 레이스 컨디션 테스트")
    inner class ConcurrencyTests {
        
        @Test
        @DisplayName("빠른 연속 증가 요청 처리")
        fun `should handle rapid increment requests`() = runTest {
            // Given
            setupDefaultMocks()
            var incrementCount = 0
            coEvery { wishCountRepository.incrementCount() } coAnswers {
                incrementCount++
            }
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When - 10 rapid increments
            repeat(10) {
                viewModel.handleEvent(HomeEvent.IncrementWishCount)
            }
            advanceUntilIdle()
            
            // Then - All increments should be processed
            incrementCount shouldBe 10
        }
        
        @Test
        @DisplayName("동시 데이터 소스 업데이트 처리")
        fun `should handle concurrent data source updates`() = runTest {
            // Given
            val wishCountFlow = MutableStateFlow(
                WishCount(date = LocalDate.now(), count = 0, dailyGoal = 100)
            )
            val goalFlow = MutableStateFlow(100)
            
            every { wishCountRepository.getTodayWishCount() } returns wishCountFlow
            every { preferencesRepository.getDailyGoalFlow() } returns goalFlow
            setupBleAndStreakMocks()
            
            viewModel = HomeViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When - Update both flows simultaneously
            wishCountFlow.value = WishCount(
                date = LocalDate.now(), 
                count = 50, 
                dailyGoal = 100
            )
            goalFlow.value = 200
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.todayCount shouldBe 50
                state.dailyGoal shouldBe 200
                state.progress shouldBe 0.25f // 50/200
            }
        }
    }
    
    // Helper functions
    private fun setupDefaultMocks() {
        every { wishCountRepository.getTodayWishCount() } returns flowOf(
            WishCount(date = LocalDate.now(), count = 0, dailyGoal = 100)
        )
        every { preferencesRepository.getDailyGoalFlow() } returns flowOf(100)
        every { bleRepository.connectionState } returns flowOf(BleConnectionState.Disconnected)
        every { bleRepository.buttonEvents } returns emptyFlow()
        every { bleRepository.batteryLevel } returns flowOf(100)
        coEvery { wishCountRepository.getCurrentStreak() } returns 0
        coEvery { wishCountRepository.getWeeklyStatistics() } returns WeeklyStatistics(0, 0.0, 0)
        coEvery { wishCountRepository.getRecentRecords(any()) } returns emptyList()
        coEvery { preferencesRepository.isAutoResetOnGoal() } returns false
    }
    
    private fun setupBleAndStreakMocks() {
        every { bleRepository.connectionState } returns flowOf(BleConnectionState.Disconnected)
        every { bleRepository.buttonEvents } returns emptyFlow()
        every { bleRepository.batteryLevel } returns flowOf(100)
        coEvery { wishCountRepository.getCurrentStreak() } returns 0
    }
    
    private fun setupDefaultMocksWithCustomBle(connectionFlow: StateFlow<BleConnectionState>) {
        every { wishCountRepository.getTodayWishCount() } returns flowOf(
            WishCount(date = LocalDate.now(), count = 0, dailyGoal = 100)
        )
        every { preferencesRepository.getDailyGoalFlow() } returns flowOf(100)
        every { bleRepository.connectionState } returns connectionFlow
        every { bleRepository.buttonEvents } returns emptyFlow()
        every { bleRepository.batteryLevel } returns flowOf(100)
        coEvery { wishCountRepository.getCurrentStreak() } returns 0
    }
}

// Mock ViewModel implementation
class HomeViewModel(
    private val wishCountRepository: WishCountRepository,
    private val preferencesRepository: PreferencesRepository,
    private val bleRepository: BleRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<HomeViewState, HomeEvent, HomeEffect>(
    initialState = HomeViewState()
) {
    
    init {
        loadInitialData()
        observeDataSources()
        observeBleEvents()
        restoreState()
    }
    
    override fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.IncrementWishCount -> incrementCount()
            is HomeEvent.ResetCount -> resetCount()
            is HomeEvent.ToggleBleConnection -> toggleBleConnection()
            is HomeEvent.Refresh -> refresh()
            is HomeEvent.LoadStatistics -> loadStatistics()
            is HomeEvent.LoadRecentRecords -> loadRecentRecords()
        }
    }
    
    private fun loadInitialData() {
        launch {
            try {
                val streak = wishCountRepository.getCurrentStreak()
                updateState { copy(currentStreak = streak, isLoading = false) }
            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }
    
    private fun observeDataSources() {
        // Observe wish count
        wishCountRepository.getTodayWishCount()
            .onEach { wishCount ->
                val count = wishCount?.count ?: 0
                val goal = wishCount?.dailyGoal ?: 100
                updateState { 
                    copy(
                        todayCount = count,
                        dailyGoal = goal,
                        progress = count.toFloat() / goal,
                        isGoalAchieved = count >= goal
                    )
                }
                
                if (count >= goal) {
                    handleGoalAchievement()
                }
            }
            .launchIn(viewModelScope)
        
        // Observe daily goal preference
        preferencesRepository.getDailyGoalFlow()
            .onEach { goal ->
                updateState { 
                    copy(
                        dailyGoal = goal,
                        progress = todayCount.toFloat() / goal
                    )
                }
            }
            .launchIn(viewModelScope)
        
        // Observe BLE connection state
        bleRepository.connectionState
            .onEach { state ->
                updateState {
                    when (state) {
                        is BleConnectionState.Connected -> copy(
                            isConnected = true,
                            isConnecting = false,
                            connectedDeviceAddress = state.deviceAddress
                        )
                        is BleConnectionState.Connecting -> copy(
                            isConnecting = true
                        )
                        is BleConnectionState.Disconnected -> copy(
                            isConnected = false,
                            isConnecting = false,
                            connectedDeviceAddress = null
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
        
        // Observe battery level
        bleRepository.batteryLevel
            .onEach { level ->
                updateState { copy(batteryLevel = level) }
                if (level <= 20) {
                    sendEffect(HomeEffect.ShowLowBatteryWarning)
                }
            }
            .launchIn(viewModelScope)
    }
    
    private fun observeBleEvents() {
        bleRepository.buttonEvents
            .onEach { event ->
                when (event) {
                    is BleButtonEvent.SinglePress -> {
                        incrementCount()
                    }
                    is BleButtonEvent.DoublePress -> {
                        // Handle double press if needed
                    }
                    is BleButtonEvent.LongPress -> {
                        // Handle long press if needed
                    }
                }
            }
            .launchIn(viewModelScope)
    }
    
    private fun incrementCount() {
        launch {
            wishCountRepository.incrementCount()
            sendEffect(HomeEffect.ShowCountAnimation)
        }
    }
    
    private fun resetCount() {
        launch {
            sendEffect(HomeEffect.ShowResetConfirmation)
            wishCountRepository.resetCount(ResetReason.MANUAL)
        }
    }
    
    private fun toggleBleConnection() {
        launch {
            if (viewState.value.isConnected) {
                bleRepository.disconnect()
            } else {
                bleRepository.connect("device_address")
            }
        }
    }
    
    private fun refresh() {
        updateState { copy(isRefreshing = true) }
        launch {
            val streak = wishCountRepository.getCurrentStreak()
            updateState { 
                copy(currentStreak = streak, isRefreshing = false)
            }
        }
    }
    
    private fun loadStatistics() {
        launch {
            val stats = wishCountRepository.getWeeklyStatistics()
            updateState { copy(weeklyStats = stats) }
        }
    }
    
    private fun loadRecentRecords() {
        launch {
            val records = wishCountRepository.getRecentRecords(7)
            updateState { copy(recentRecords = records) }
        }
    }
    
    private fun handleGoalAchievement() {
        launch {
            sendEffect(HomeEffect.ShowGoalAchievement)
            if (preferencesRepository.isAutoResetOnGoal()) {
                wishCountRepository.resetCount(ResetReason.GOAL_ACHIEVED)
            }
        }
    }
    
    private fun restoreState() {
        savedStateHandle.get<Int>("todayCount")?.let { count ->
            updateState { copy(todayCount = count) }
        }
        savedStateHandle.get<Int>("dailyGoal")?.let { goal ->
            updateState { copy(dailyGoal = goal) }
        }
        savedStateHandle.get<Int>("currentStreak")?.let { streak ->
            updateState { copy(currentStreak = streak) }
        }
        savedStateHandle.get<Boolean>("isConnected")?.let { connected ->
            updateState { copy(isConnected = connected) }
        }
    }
    
    fun updateState(update: HomeViewState.() -> HomeViewState) {
        val newState = viewState.value.update()
        _viewState.value = newState
        
        // Save to SavedStateHandle
        savedStateHandle.set("todayCount", newState.todayCount)
        savedStateHandle.set("dailyGoal", newState.dailyGoal)
        savedStateHandle.set("currentStreak", newState.currentStreak)
        savedStateHandle.set("isConnected", newState.isConnected)
    }
}

// Supporting classes
data class HomeViewState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val todayCount: Int = 0,
    val dailyGoal: Int = 100,
    val progress: Float = 0f,
    val currentStreak: Int = 0,
    val isGoalAchieved: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val connectedDeviceAddress: String? = null,
    val batteryLevel: Int = 100,
    val weeklyStats: WeeklyStatistics? = null,
    val recentRecords: List<DailyRecord> = emptyList()
)

sealed class HomeEvent {
    object IncrementWishCount : HomeEvent()
    object ResetCount : HomeEvent()
    object ToggleBleConnection : HomeEvent()
    object Refresh : HomeEvent()
    object LoadStatistics : HomeEvent()
    object LoadRecentRecords : HomeEvent()
}

sealed class HomeEffect {
    object ShowCountAnimation : HomeEffect()
    object ShowResetConfirmation : HomeEffect()
    object ShowGoalAchievement : HomeEffect()
    object ShowLowBatteryWarning : HomeEffect()
    data class ShowError(val message: String) : HomeEffect()
}

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Connecting : BleConnectionState()
    data class Connected(val deviceAddress: String) : BleConnectionState()
}

sealed class BleButtonEvent {
    object SinglePress : BleButtonEvent()
    object DoublePress : BleButtonEvent()
    object LongPress : BleButtonEvent()
}

data class WeeklyStatistics(
    val totalCount: Int,
    val averageCount: Double,
    val achievementDays: Int
)

enum class ResetReason {
    MANUAL, MIDNIGHT, GOAL_ACHIEVED
}