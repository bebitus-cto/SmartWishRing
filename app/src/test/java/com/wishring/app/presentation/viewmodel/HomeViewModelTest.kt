package com.wishring.app.presentation.viewmodel

import android.content.Context
import app.cash.turbine.test
import com.wishring.app.data.model.WishUiState
import com.wishring.app.data.repository.WishCountRepository
import com.wishring.app.data.repository.PreferencesRepository
import com.wishring.app.data.repository.BleRepository
import com.wishring.app.data.repository.BleConnectionState
import com.wishring.app.data.repository.StreakInfo
import com.wishring.app.data.local.database.entity.WishData
import com.wishring.app.presentation.home.*
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

@ExperimentalCoroutinesApi
@DisplayName("HomeViewModel 테스트 - 새 아키텍처")
class HomeViewModelTest {

    @MockK
    private lateinit var context: Context
    
    @MockK
    private lateinit var wishCountRepository: WishCountRepository
    
    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    
    @MockK
    private lateinit var bleRepository: BleRepository
    
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks for dependencies
        every { context.packageName } returns "com.wishring.app"
        setupDefaultMocks()
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Nested
    @DisplayName("초기화 및 데이터 로드 테스트")
    inner class InitializationTests {
        
        @Test
        @DisplayName("ViewModel 초기 상태 확인")
        fun `should have correct initial state`() = runTest {
            // Given
            setupDefaultMocks()
            
            // When
            viewModel = createViewModel()
            
            // Then
            val initialState = viewModel.uiState.value
            assertTrue(initialState.isLoading)
            assertEquals(0, initialState.totalCount)
            assertEquals(emptyList<WishData>(), initialState.todayWishes)
            assertEquals(0, initialState.activeWishIndex)
        }
        
        @Test
        @DisplayName("오늘의 위시 데이터 로드 성공")
        fun `should load today wishes successfully`() = runTest {
            // Given
            val wishData = listOf(
                WishData("위시 1", 1000),
                WishData("위시 2", 2000)
            )
            val totalCount = 500
            val activeIndex = 1
            
            coEvery { wishCountRepository.getTodayWishes() } returns wishData
            coEvery { wishCountRepository.getActiveWishIndex() } returns activeIndex
            val mockWishUiState = WishUiState.createDefault().copy(targetCount = totalCount)
            coEvery { wishCountRepository.getTodayWishCount() } returns mockWishUiState
            
            // When
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(wishData, state.todayWishes)
                assertEquals(activeIndex, state.activeWishIndex)
                assertEquals(totalCount, state.totalCount)
                assertEquals(2000, state.targetCount) // 활성 위시의 targetCount
            }
        }
        
        @Test
        @DisplayName("데이터 로드 에러 처리")
        fun `should handle loading error`() = runTest {
            // Given
            coEvery { wishCountRepository.getTodayWishes() } throws RuntimeException("Database error")
            
            // When
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNotNull(state.error)
                assertTrue(state.error!!.contains("Database error"))
            }
        }
    }
    
    @Nested
    @DisplayName("BLE 통합 테스트")
    inner class BleIntegrationTests {
        
        @Test
        @DisplayName("BLE 연결 상태 변경 처리")
        fun `should handle BLE connection state changes`() = runTest {
            // Given
            val connectionStateFlow = MutableStateFlow(BleConnectionState.DISCONNECTED)
            every { bleRepository.getConnectionState() } returns connectionStateFlow
            
            // When
            viewModel = createViewModel()
            
            // Then - Initially disconnected
            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(BleConnectionState.DISCONNECTED, state.bleConnectionState)
                assertFalse(state.isBleConnected)
            }
            
            // When - Connect
            connectionStateFlow.value = BleConnectionState.CONNECTED
            testDispatcher.scheduler.advanceUntilIdle()
            
            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(BleConnectionState.CONNECTED, state.bleConnectionState)
                assertTrue(state.isBleConnected)
            }
        }
        
        @Test
        @DisplayName("BLE 카운터 증가 이벤트 처리")
        fun `should handle BLE counter increment events`() = runTest {
            // Given
            val counterFlow = MutableSharedFlow<Int>()
            every { bleRepository.counterIncrements } returns counterFlow
            coEvery { wishCountRepository.incrementTodayCount(any()) } returns WishUiState.createDefault()
            
            // When
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // When - BLE sends increment
            counterFlow.emit(1)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then - Repository should be called
            coVerify { wishCountRepository.incrementTodayCount(1) }
        }
        
        @Test
        @DisplayName("배터리 레벨 업데이트")
        fun `should update battery level`() = runTest {
            // Given
            val batteryFlow = MutableStateFlow<Int?>(85)
            every { bleRepository.subscribeToBatteryLevel() } returns batteryFlow.filterNotNull()
            
            // When
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(85, state.deviceBatteryLevel)
            }
            
            // When - Battery level changes
            batteryFlow.value = 20
            testDispatcher.scheduler.advanceUntilIdle()
            
            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(20, state.deviceBatteryLevel)
                assertTrue(state.showLowBatteryWarning)
            }
        }
    }
    
    @Nested
    @DisplayName("위시 네비게이션 테스트")
    inner class WishNavigationTests {
        
        @Test
        @DisplayName("다음 위시로 네비게이션")
        fun `should navigate to next wish`() = runTest {
            // Given
            val wishData = listOf(
                WishData("위시 1", 1000),
                WishData("위시 2", 2000),
                WishData("위시 3", 3000)
            )
            
            coEvery { wishCountRepository.getTodayWishes() } returns wishData
            coEvery { wishCountRepository.getActiveWishIndex() } returns 0
            coEvery { wishCountRepository.setActiveWishIndex(any()) } returns WishUiState.createDefault()
            
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // When
            viewModel.onEvent(HomeEvent.NavigateToNextWish)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.setActiveWishIndex(1) }
        }
        
        @Test
        @DisplayName("이전 위시로 네비게이션")
        fun `should navigate to previous wish`() = runTest {
            // Given
            val wishData = listOf(
                WishData("위시 1", 1000),
                WishData("위시 2", 2000),
                WishData("위시 3", 3000)
            )
            
            coEvery { wishCountRepository.getTodayWishes() } returns wishData
            coEvery { wishCountRepository.getActiveWishIndex() } returns 2 // 마지막 위시
            coEvery { wishCountRepository.setActiveWishIndex(any()) } returns WishUiState.createDefault()
            
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // When
            viewModel.onEvent(HomeEvent.NavigateToPreviousWish)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.setActiveWishIndex(1) }
        }
        
        @Test
        @DisplayName("특정 위시 선택")
        fun `should select specific wish`() = runTest {
            // Given
            val wishData = listOf(
                WishData("위시 1", 1000),
                WishData("위시 2", 2000),
                WishData("위시 3", 3000)
            )
            
            coEvery { wishCountRepository.getTodayWishes() } returns wishData
            coEvery { wishCountRepository.getActiveWishIndex() } returns 0
            coEvery { wishCountRepository.setActiveWishIndex(any()) } returns WishUiState.createDefault()
            
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // When
            viewModel.onEvent(HomeEvent.SelectWish(2))
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.setActiveWishIndex(2) }
        }
        
        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2])
        @DisplayName("경계값 위시 인덱스 처리")
        fun `should handle boundary wish indices`(index: Int) = runTest {
            // Given
            val wishData = (1..3).map { WishData("위시 $it", it * 1000) }
            
            coEvery { wishCountRepository.getTodayWishes() } returns wishData
            coEvery { wishCountRepository.getActiveWishIndex() } returns 0
            coEvery { wishCountRepository.setActiveWishIndex(any()) } returns WishUiState.createDefault()
            
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // When
            viewModel.onEvent(HomeEvent.SelectWish(index))
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.setActiveWishIndex(index) }
        }
    }
    
    @Nested
    @DisplayName("데이터 새로고침 테스트")
    inner class RefreshTests {
        
        @Test
        @DisplayName("데이터 새로고침 성공")
        fun `should refresh data successfully`() = runTest {
            // Given
            val initialWishes = listOf(WishData("위시 1", 1000))
            val refreshedWishes = listOf(
                WishData("위시 1", 1000),
                WishData("위시 2", 2000)
            )
            
            coEvery { wishCountRepository.getTodayWishes() } returnsMany listOf(initialWishes, refreshedWishes)
            coEvery { wishCountRepository.getActiveWishIndex() } returns 0
            coEvery { wishCountRepository.getTodayWishCount() } returns WishUiState.createDefault()
            
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // When
            viewModel.onEvent(HomeEvent.RefreshData)
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isRefreshing)
                assertEquals(refreshedWishes, state.todayWishes)
            }
        }
        
        @Test
        @DisplayName("새로고침 중 상태 표시")
        fun `should show refreshing state`() = runTest {
            // Given
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // When
            viewModel.onEvent(HomeEvent.RefreshData)
            
            // Then - 새로고침 상태가 즉시 반영되어야 함
            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isRefreshing)
            }
        }
    }
    
    @Nested
    @DisplayName("목표 달성 및 진행률 테스트")
    inner class ProgressTests {
        
        @ParameterizedTest
        @CsvSource(
            "0,1000,0.0,false",
            "500,1000,0.5,false", 
            "1000,1000,1.0,true",
            "1500,1000,1.0,true"
        )
        @DisplayName("진행률 계산 및 목표 달성 판단")
        fun `should calculate progress and goal achievement correctly`(
            totalCount: Int,
            targetCount: Int,
            expectedProgress: Float,
            expectedCompleted: Boolean
        ) = runTest {
            // Given
            val wishData = listOf(WishData("위시", targetCount))
            val wishUiState = WishUiState.createDefault().copy(targetCount = totalCount)
            
            coEvery { wishCountRepository.getTodayWishes() } returns wishData
            coEvery { wishCountRepository.getActiveWishIndex() } returns 0
            coEvery { wishCountRepository.getTodayWishCount() } returns wishUiState
            
            // When
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(expectedProgress, state.progress, 0.01f)
                assertEquals(expectedCompleted, state.isCompleted)
                assertEquals(totalCount, state.totalCount)
                assertEquals(targetCount, state.targetCount)
            }
        }
    }
    
    @Nested
    @DisplayName("에러 처리 테스트")
    inner class ErrorHandlingTests {
        
        @Test
        @DisplayName("에러 상태 해제")
        fun `should dismiss error`() = runTest {
            // Given
            viewModel = createViewModel()
            
            // 에러 상태 설정
            viewModel.updateState { copy(error = "Test error") }
            
            // When
            viewModel.onEvent(HomeEvent.DismissError)
            
            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.error)
            }
        }
        
        @Test
        @DisplayName("BLE 연결 에러 처리")
        fun `should handle BLE connection error`() = runTest {
            // Given
            every { bleRepository.getConnectionState() } returns flowOf(BleConnectionState.ERROR)
            
            // When
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(BleConnectionState.ERROR, state.bleConnectionState)
            }
        }
    }
    
    // Helper methods
    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            context = context,
            wishCountRepository = wishCountRepository,
            bleRepository = bleRepository,
            preferencesRepository = preferencesRepository
        )
    }
    
    private fun setupDefaultMocks() {
        // WishCountRepository mocks
        coEvery { wishCountRepository.getTodayWishes() } returns emptyList()
        coEvery { wishCountRepository.getActiveWishIndex() } returns 0
        coEvery { wishCountRepository.getTodayWishCount() } returns WishUiState.createDefault()
        coEvery { wishCountRepository.getDailyRecords(any()) } returns emptyList()
        coEvery { wishCountRepository.getStreakInfo() } returns StreakInfo(0, 0, null, null, false)
        coEvery { wishCountRepository.setActiveWishIndex(any()) } returns WishUiState.createDefault()
        coEvery { wishCountRepository.incrementTodayCount(any()) } returns WishUiState.createDefault()
        
        // BleRepository mocks
        every { bleRepository.getConnectionState() } returns flowOf(BleConnectionState.DISCONNECTED)
        every { bleRepository.counterIncrements } returns emptyFlow()
        every { bleRepository.subscribeToBatteryLevel() } returns flowOf(100)
        
        // PreferencesRepository mocks
        coEvery { preferencesRepository.getDefaultWishText() } returns "기본 위시"
        coEvery { preferencesRepository.getDefaultTargetCount() } returns 1000
        coEvery { preferencesRepository.isBleAutoConnectEnabled() } returns false
        coEvery { preferencesRepository.isAchievementNotificationEnabled() } returns true
    }
}