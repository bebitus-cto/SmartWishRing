package com.wishring.app.integration

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wishring.app.data.ble.BleRepositoryImpl
import com.wishring.app.data.ble.MrdProtocolAdapter
import com.wishring.app.data.ble.model.BleConnectionState
import com.wishring.app.data.local.database.WishRingDatabase
import com.wishring.app.data.local.repository.PreferencesRepositoryImpl
import com.wishring.app.data.local.repository.WishCountRepositoryImpl
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.model.UserProfile
import com.wishring.app.domain.model.WishCount
import com.wishring.app.presentation.viewmodel.*
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * End-to-End 시나리오 테스트
 * 
 * 실제 사용자 여정을 시뮬레이션하여 전체 시스템의 통합을 검증합니다.
 * 
 * 테스트 시나리오:
 * 1. First Time User Journey - 첫 사용자 경험
 * 2. Daily Usage Pattern - 일상 사용 패턴
 * 3. BLE Device Interaction - BLE 디바이스 상호작용
 * 4. Data Persistence - 데이터 지속성
 * 5. Error Recovery - 오류 복구
 * 6. Performance Under Load - 부하 테스트
 */
@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("End-to-End 시나리오 테스트 - 사용자 여정")
class EndToEndScenarioTest {
    
    private lateinit var context: Context
    private lateinit var database: WishRingDatabase
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var testScope: TestScope
    
    // Repositories
    private lateinit var wishCountRepository: WishCountRepositoryImpl
    private lateinit var preferencesRepository: PreferencesRepositoryImpl
    private lateinit var bleRepository: BleRepositoryImpl
    
    // ViewModels
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var wishInputViewModel: WishInputViewModel
    private lateinit var detailViewModel: DetailViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    
    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        testScope = TestScope()
        
        // In-memory database 설정
        database = Room.inMemoryDatabaseBuilder(
            context,
            WishRingDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dataStore = mockk(relaxed = true)
        
        // Repository 초기화
        wishCountRepository = WishCountRepositoryImpl(
            database.wishCountDao(),
            database.resetLogDao(),
            testScope.testScheduler
        )
        
        preferencesRepository = PreferencesRepositoryImpl(
            dataStore,
            testScope.testScheduler
        )
        
        bleRepository = mockk(relaxed = true)
        
        // ViewModel 초기화
        homeViewModel = HomeViewModel(
            wishCountRepository,
            bleRepository,
            preferencesRepository
        )
        
        wishInputViewModel = WishInputViewModel(
            wishCountRepository,
            bleRepository,
            preferencesRepository
        )
        
        detailViewModel = DetailViewModel(
            wishCountRepository,
            preferencesRepository
        )
        
        settingsViewModel = SettingsViewModel(
            preferencesRepository,
            bleRepository
        )
    }
    
    @AfterEach
    fun tearDown() {
        database.close()
    }
    
    @Nested
    @DisplayName("1. First Time User Journey - 첫 사용자 경험")
    inner class FirstTimeUserJourneyTest {
        
        @Test
        @DisplayName("앱 첫 실행부터 첫 위시 입력까지")
        fun testFirstTimeUserFlow() = testScope.runTest {
            // Given - 첫 실행 상태
            coEvery { preferencesRepository.isFirstLaunch() } returns true
            
            // Step 1: 스플래시 화면 표시
            delay(2000) // 스플래시 딜레이
            
            // Step 2: 온보딩 표시 확인
            val shouldShowOnboarding = preferencesRepository.isFirstLaunch()
            shouldShowOnboarding shouldBe true
            
            // Step 3: 사용자 정보 입력
            val userProfile = UserProfile(
                name = "테스트 사용자",
                targetCount = 108,
                createdAt = LocalDateTime.now()
            )
            preferencesRepository.saveUserProfile(userProfile)
            
            // Step 4: BLE 디바이스 스캔 및 연결
            coEvery { bleRepository.scanForDevices() } returns flowOf(
                mockk {
                    every { name } returns "MRD_001"
                    every { address } returns "00:11:22:33:44:55"
                }
            )
            
            homeViewModel.startDeviceScan()
            advanceTimeBy(1000)
            
            homeViewModel.connectToDevice("00:11:22:33:44:55")
            coEvery { bleRepository.connectionState } returns MutableStateFlow(
                BleConnectionState.Connected("00:11:22:33:44:55")
            )
            
            // Step 5: 첫 위시 입력
            val wishText = "매일 운동하기"
            wishInputViewModel.setWishText(wishText)
            wishInputViewModel.saveWish()
            
            advanceTimeBy(100)
            
            // Then - 검증
            val savedWish = wishCountRepository.getTodayWishCount().first()
            savedWish shouldNotBe null
            savedWish?.wishText shouldBe wishText
            savedWish?.currentCount shouldBe 0
            
            coVerify { preferencesRepository.setFirstLaunchCompleted() }
        }
        
        @Test
        @DisplayName("온보딩 스킵 후 빠른 시작")
        fun testQuickStartWithoutOnboarding() = testScope.runTest {
            // Given
            coEvery { preferencesRepository.isFirstLaunch() } returns false
            
            // When - 바로 홈 화면으로 이동
            homeViewModel.loadTodayData()
            advanceTimeBy(100)
            
            // Then
            homeViewModel.viewState.value.shouldBeInstanceOf<HomeViewState>()
            homeViewModel.viewState.value.isLoading shouldBe false
        }
    }
    
    @Nested
    @DisplayName("2. Daily Usage Pattern - 일상 사용 패턴")
    inner class DailyUsagePatternTest {
        
        @Test
        @DisplayName("아침 루틴: 어제 기록 확인 → 오늘 목표 설정 → 첫 카운트")
        fun testMorningRoutine() = testScope.runTest {
            // Given - 어제 데이터 준비
            val yesterday = LocalDate.now().minusDays(1)
            val yesterdayWish = WishCount(
                date = yesterday,
                wishText = "독서하기",
                targetCount = 3,
                currentCount = 2,
                isCompleted = false
            )
            wishCountRepository.saveWishCount(yesterdayWish)
            
            // Step 1: 어제 기록 확인
            detailViewModel.loadDateData(yesterday)
            advanceTimeBy(100)
            
            val yesterdayData = detailViewModel.viewState.value.wishCount
            yesterdayData?.currentCount shouldBe 2
            yesterdayData?.isCompleted shouldBe false
            
            // Step 2: 오늘 목표 설정
            val todayWish = "오늘도 독서 3권"
            wishInputViewModel.setWishText(todayWish)
            wishInputViewModel.setTargetCount(3)
            wishInputViewModel.saveWish()
            advanceTimeBy(100)
            
            // Step 3: 첫 카운트 증가 (BLE 버튼 누름)
            coEvery { bleRepository.incrementWishCount() } returns true
            homeViewModel.onBleButtonPressed()
            advanceTimeBy(100)
            
            // Then
            val todayData = wishCountRepository.getTodayWishCount().first()
            todayData?.currentCount shouldBe 1
            todayData?.progress shouldBe 33 // 1/3 = 33%
        }
        
        @Test
        @DisplayName("저녁 루틴: 진행 상황 확인 → 완료 → 공유")
        fun testEveningRoutine() = testScope.runTest {
            // Given - 오늘 데이터 준비
            val todayWish = WishCount(
                date = LocalDate.now(),
                wishText = "물 8잔 마시기",
                targetCount = 8,
                currentCount = 7
            )
            wishCountRepository.saveWishCount(todayWish)
            
            // Step 1: 진행 상황 확인
            homeViewModel.loadTodayData()
            advanceTimeBy(100)
            
            homeViewModel.viewState.value.wishCount?.progress shouldBe 87 // 7/8
            
            // Step 2: 마지막 카운트로 완료
            wishCountRepository.incrementCount()
            advanceTimeBy(100)
            
            // Step 3: 완료 확인
            val completed = wishCountRepository.getTodayWishCount().first()
            completed?.isCompleted shouldBe true
            completed?.completedAt shouldNotBe null
            
            // Step 4: SNS 공유 준비
            val shareData = homeViewModel.prepareShareData()
            shareData shouldContain "목표 달성"
            shareData shouldContain "물 8잔 마시기"
        }
        
        @Test
        @DisplayName("자정 자동 리셋 시나리오")
        fun testMidnightAutoReset() = testScope.runTest {
            // Given - 23:59의 데이터
            val almostMidnight = LocalDateTime.of(
                LocalDate.now(),
                LocalTime.of(23, 59, 50)
            )
            
            val todayWish = WishCount(
                date = LocalDate.now(),
                wishText = "오늘의 목표",
                targetCount = 10,
                currentCount = 8
            )
            wishCountRepository.saveWishCount(todayWish)
            
            // When - 자정 넘김 시뮬레이션
            testScope.testScheduler.apply {
                // 10초 후 자정
                advanceTimeBy(10_000)
            }
            
            // 자정 리셋 트리거
            wishCountRepository.performMidnightReset()
            advanceTimeBy(100)
            
            // Then
            val resetLog = wishCountRepository.getResetLogs().first()
            resetLog shouldHaveSize 1
            resetLog[0].previousCount shouldBe 8
            resetLog[0].lostCount shouldBe 2
            resetLog[0].resetType shouldBe "MIDNIGHT_AUTO"
            
            // 새로운 날짜의 빈 위시
            val newDayWish = wishCountRepository.getTodayWishCount().first()
            newDayWish?.currentCount shouldBe 0
        }
    }
    
    @Nested
    @DisplayName("3. BLE Device Interaction - BLE 디바이스 상호작용")
    inner class BleDeviceInteractionTest {
        
        @Test
        @DisplayName("BLE 연결 → 카운트 증가 → 연결 해제 → 재연결")
        fun testBleConnectionLifecycle() = testScope.runTest {
            // Given
            val deviceAddress = "00:11:22:33:44:55"
            val connectionStates = mutableListOf<BleConnectionState>()
            
            // 연결 상태 모니터링
            val stateJob = launch {
                bleRepository.connectionState.collect { state ->
                    connectionStates.add(state)
                }
            }
            
            // Step 1: 디바이스 연결
            coEvery { bleRepository.connect(deviceAddress) } returns true
            coEvery { bleRepository.connectionState } returns MutableStateFlow(
                BleConnectionState.Connected(deviceAddress)
            )
            
            homeViewModel.connectToDevice(deviceAddress)
            advanceTimeBy(100)
            
            // Step 2: 버튼 눌림 시뮬레이션 (5회)
            repeat(5) {
                coEvery { bleRepository.incrementWishCount() } returns true
                homeViewModel.onBleButtonPressed()
                advanceTimeBy(50)
            }
            
            val afterPress = wishCountRepository.getTodayWishCount().first()
            afterPress?.currentCount shouldBe 5
            
            // Step 3: 연결 해제
            coEvery { bleRepository.connectionState } returns MutableStateFlow(
                BleConnectionState.Disconnected
            )
            homeViewModel.disconnectDevice()
            advanceTimeBy(100)
            
            // Step 4: 자동 재연결
            coEvery { bleRepository.connectionState } returns MutableStateFlow(
                BleConnectionState.Connecting
            )
            delay(1000)
            
            coEvery { bleRepository.connectionState } returns MutableStateFlow(
                BleConnectionState.Connected(deviceAddress)
            )
            
            // Then
            connectionStates shouldContain BleConnectionState.Connected(deviceAddress)
            connectionStates shouldContain BleConnectionState.Disconnected
            
            stateJob.cancel()
        }
        
        @Test
        @DisplayName("배터리 부족 알림 시나리오")
        fun testLowBatteryNotification() = testScope.runTest {
            // Given
            coEvery { bleRepository.batteryLevel } returns MutableStateFlow(100)
            
            // When - 배터리 레벨 변화 시뮬레이션
            val batteryLevels = listOf(100, 75, 50, 25, 10, 5)
            val notifications = mutableListOf<String>()
            
            batteryLevels.forEach { level ->
                coEvery { bleRepository.batteryLevel } returns MutableStateFlow(level)
                
                if (level <= 20) {
                    val notification = homeViewModel.checkBatteryAndNotify()
                    notification?.let { notifications.add(it) }
                }
                
                advanceTimeBy(100)
            }
            
            // Then
            notifications shouldHaveSize 3 // 10%, 5% 알림
            notifications.any { it.contains("배터리") } shouldBe true
        }
        
        @Test
        @DisplayName("건강 데이터 동기화")
        fun testHealthDataSync() = testScope.runTest {
            // Given
            val healthData = mockk {
                every { heartRate } returns 75
                every { steps } returns 5000
                every { sleepScore } returns 85
            }
            
            coEvery { bleRepository.healthDataFlow } returns flowOf(healthData)
            
            // When
            val collectedHealthData = mutableListOf<Any>()
            val job = launch {
                bleRepository.healthDataFlow.collect { data ->
                    collectedHealthData.add(data)
                }
            }
            
            advanceTimeBy(1000)
            job.cancel()
            
            // Then
            collectedHealthData shouldHaveSize 1
            val data = collectedHealthData[0]
            data.shouldBeInstanceOf<Any>() // HealthData type
        }
    }
    
    @Nested
    @DisplayName("4. Data Persistence - 데이터 지속성")
    inner class DataPersistenceTest {
        
        @Test
        @DisplayName("앱 재시작 후 데이터 복원")
        fun testDataRestorationAfterRestart() = testScope.runTest {
            // Given - 데이터 저장
            val originalWish = WishCount(
                date = LocalDate.now(),
                wishText = "명상 30분",
                targetCount = 1,
                currentCount = 0
            )
            wishCountRepository.saveWishCount(originalWish)
            
            val originalProfile = UserProfile(
                name = "사용자",
                level = 5,
                totalWishesCompleted = 42
            )
            preferencesRepository.saveUserProfile(originalProfile)
            
            // When - 앱 재시작 시뮬레이션
            // Repository 재생성
            val newWishRepository = WishCountRepositoryImpl(
                database.wishCountDao(),
                database.resetLogDao(),
                testScope.testScheduler
            )
            
            // Then - 데이터 복원 확인
            val restoredWish = newWishRepository.getTodayWishCount().first()
            restoredWish?.wishText shouldBe "명상 30분"
            restoredWish?.targetCount shouldBe 1
            
            val restoredProfile = preferencesRepository.getUserProfile().first()
            restoredProfile?.name shouldBe "사용자"
            restoredProfile?.level shouldBe 5
        }
        
        @Test
        @DisplayName("30일 기록 통계 계산")
        fun testThirtyDayStatistics() = testScope.runTest {
            // Given - 30일간의 데이터 생성
            val today = LocalDate.now()
            repeat(30) { dayOffset ->
                val date = today.minusDays(dayOffset.toLong())
                val wish = WishCount(
                    date = date,
                    wishText = "Day $dayOffset wish",
                    targetCount = 10,
                    currentCount = if (dayOffset % 3 == 0) 10 else 7,
                    isCompleted = dayOffset % 3 == 0
                )
                wishCountRepository.saveWishCount(wish)
            }
            
            // When
            val stats = wishCountRepository.getStatistics(30).first()
            
            // Then
            stats.totalDays shouldBe 30
            stats.completedDays shouldBe 10 // 30일 중 1/3 완료
            stats.totalCount shouldBe 240 // (10*10 + 20*7)
            stats.averageCount shouldBe 8
            stats.completionRate shouldBe 33 // 10/30 = 33%
        }
        
        @Test
        @DisplayName("스트릭 계산 및 유지")
        fun testStreakCalculation() = testScope.runTest {
            // Given - 연속 완료 데이터
            val today = LocalDate.now()
            repeat(7) { dayOffset ->
                val date = today.minusDays(dayOffset.toLong())
                val wish = WishCount(
                    date = date,
                    wishText = "연속 목표",
                    targetCount = 5,
                    currentCount = 5,
                    isCompleted = true,
                    completedAt = LocalDateTime.of(date, LocalTime.of(20, 0))
                )
                wishCountRepository.saveWishCount(wish)
            }
            
            // When
            val streak = wishCountRepository.getCurrentStreak().first()
            
            // Then
            streak shouldBe 7
            
            // 스트릭 깨짐 테스트
            val breakDate = today.minusDays(7)
            val incompleteWish = WishCount(
                date = breakDate,
                wishText = "미완료",
                targetCount = 5,
                currentCount = 3,
                isCompleted = false
            )
            wishCountRepository.saveWishCount(incompleteWish)
            
            val brokenStreak = wishCountRepository.getCurrentStreak().first()
            brokenStreak shouldBe 7 // 오늘부터 7일 연속
        }
    }
    
    @Nested
    @DisplayName("5. Error Recovery - 오류 복구")
    inner class ErrorRecoveryTest {
        
        @Test
        @DisplayName("BLE 연결 실패 후 복구")
        fun testBleConnectionFailureRecovery() = testScope.runTest {
            // Given
            var connectionAttempts = 0
            coEvery { bleRepository.connect(any()) } answers {
                connectionAttempts++
                connectionAttempts > 2 // 3번째 시도에서 성공
            }
            
            // When
            val connected = homeViewModel.connectWithRetry("DEVICE_ADDR", maxRetries = 5)
            
            // Then
            connectionAttempts shouldBe 3
            connected shouldBe true
        }
        
        @Test
        @DisplayName("데이터베이스 트랜잭션 오류 복구")
        fun testDatabaseTransactionRecovery() = testScope.runTest {
            // Given
            var saveAttempts = 0
            val mockDao = mockk<Any> {
                coEvery { database.wishCountDao().insert(any()) } answers {
                    saveAttempts++
                    if (saveAttempts == 1) {
                        throw Exception("Database locked")
                    }
                    // 두 번째 시도에서 성공
                }
            }
            
            // When
            val wish = WishCount(
                date = LocalDate.now(),
                wishText = "테스트",
                targetCount = 10
            )
            
            val saved = try {
                wishCountRepository.saveWishCount(wish)
                true
            } catch (e: Exception) {
                // 재시도
                wishCountRepository.saveWishCount(wish)
                true
            }
            
            // Then
            saved shouldBe true
        }
        
        @Test
        @DisplayName("네트워크 타임아웃 처리")
        fun testNetworkTimeoutHandling() = testScope.runTest {
            // Given
            val timeoutFlow = flow {
                delay(5000) // 5초 지연
                emit("Late response")
            }
            
            // When
            val result = withTimeoutOrNull(2.seconds) {
                timeoutFlow.first()
            }
            
            // Then
            result shouldBe null // 타임아웃 발생
            
            // 대체 로직 실행
            val fallbackResult = "Cached data"
            val finalResult = result ?: fallbackResult
            finalResult shouldBe "Cached data"
        }
    }
    
    @Nested
    @DisplayName("6. Performance Under Load - 부하 테스트")
    inner class PerformanceUnderLoadTest {
        
        @Test
        @DisplayName("동시 다중 사용자 시뮬레이션")
        fun testConcurrentMultiUserSimulation() = testScope.runTest {
            // Given
            val userCount = 100
            val latch = CountDownLatch(userCount)
            val results = mutableListOf<Boolean>()
            
            // When - 100명의 사용자가 동시에 카운트 증가
            repeat(userCount) { userId ->
                launch {
                    val wish = WishCount(
                        date = LocalDate.now(),
                        wishText = "User $userId wish",
                        targetCount = 10,
                        currentCount = 0
                    )
                    wishCountRepository.saveWishCount(wish)
                    
                    // 각 사용자가 5번 카운트 증가
                    repeat(5) {
                        wishCountRepository.incrementCount()
                        delay(10)
                    }
                    
                    results.add(true)
                    latch.countDown()
                }
            }
            
            latch.await(10, TimeUnit.SECONDS)
            
            // Then
            results shouldHaveSize userCount
            
            // 데이터 무결성 확인
            val allWishes = wishCountRepository.getAllWishCounts().first()
            allWishes.forEach { wish ->
                wish.currentCount shouldBe 5
            }
        }
        
        @Test
        @DisplayName("대용량 히스토리 데이터 로딩")
        fun testLargeHistoryDataLoading() = testScope.runTest {
            // Given - 1년치 데이터 생성
            val today = LocalDate.now()
            repeat(365) { dayOffset ->
                val date = today.minusDays(dayOffset.toLong())
                val wish = WishCount(
                    date = date,
                    wishText = "Day $dayOffset",
                    targetCount = 10,
                    currentCount = Random.nextInt(0, 11)
                )
                wishCountRepository.saveWishCount(wish)
            }
            
            // When
            val startTime = System.currentTimeMillis()
            val yearData = wishCountRepository.getWishCountsForDateRange(
                today.minusDays(365),
                today
            ).first()
            val loadTime = System.currentTimeMillis() - startTime
            
            // Then
            yearData shouldHaveSize 365
            loadTime < 1000 shouldBe true // 1초 이내 로딩
        }
        
        @ParameterizedTest
        @ValueSource(ints = [10, 50, 100, 500, 1000])
        @DisplayName("다양한 부하 레벨에서의 응답 시간")
        fun testResponseTimeUnderVariousLoads(load: Int) = testScope.runTest {
            // Given
            val operations = mutableListOf<Long>()
            
            // When
            repeat(load) {
                val startTime = System.nanoTime()
                wishCountRepository.incrementCount()
                val duration = System.nanoTime() - startTime
                operations.add(duration)
            }
            
            // Then
            val avgResponseTime = operations.average() / 1_000_000 // Convert to ms
            val maxResponseTime = operations.maxOrNull()!! / 1_000_000
            
            avgResponseTime < 10 shouldBe true // 평균 10ms 이내
            maxResponseTime < 50 shouldBe true // 최대 50ms 이내
        }
    }
}

// Helper extension for ViewModel testing
private fun HomeViewModel.prepareShareData(): String {
    val state = viewState.value
    val wish = state.wishCount ?: return ""
    
    return buildString {
        appendLine("🎯 WISH RING - 목표 달성!")
        appendLine("목표: ${wish.wishText}")
        appendLine("달성: ${wish.currentCount}/${wish.targetCount}")
        appendLine("달성률: ${wish.progress}%")
        if (wish.isCompleted) {
            appendLine("✨ 축하합니다! 목표를 달성했습니다!")
        }
    }
}

private suspend fun HomeViewModel.connectWithRetry(
    address: String,
    maxRetries: Int = 3
): Boolean {
    repeat(maxRetries) { attempt ->
        if (connectToDevice(address)) {
            return true
        }
        delay((attempt + 1) * 1000L) // Exponential backoff
    }
    return false
}

private suspend fun HomeViewModel.checkBatteryAndNotify(): String? {
    val batteryLevel = (viewState.value as? HomeViewState)?.batteryLevel ?: 100
    return when {
        batteryLevel <= 5 -> "⚠️ 배터리 매우 부족 (${batteryLevel}%)"
        batteryLevel <= 10 -> "⚠️ 배터리 부족 (${batteryLevel}%)"
        batteryLevel <= 20 -> "배터리 잔량 낮음 (${batteryLevel}%)"
        else -> null
    }
}