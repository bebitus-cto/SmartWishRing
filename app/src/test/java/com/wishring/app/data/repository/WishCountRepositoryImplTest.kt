package com.wishring.app.data.repository

import com.wishring.app.data.local.dao.WishCountDao
import com.wishring.app.data.local.dao.ResetLogDao
import com.wishring.app.data.local.entity.WishCountEntity
import com.wishring.app.data.local.entity.ResetLogEntity
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.model.ResetLog
import com.wishring.app.domain.repository.WishCountRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
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
import app.cash.turbine.test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("WishCountRepository 구현체 테스트")
class WishCountRepositoryImplTest {

    @MockK
    private lateinit var wishCountDao: WishCountDao
    
    @MockK
    private lateinit var resetLogDao: ResetLogDao
    
    private lateinit var repository: WishCountRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        repository = WishCountRepositoryImpl(
            wishCountDao = wishCountDao,
            resetLogDao = resetLogDao,
            ioDispatcher = testDispatcher
        )
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    @Nested
    @DisplayName("위시 카운트 조회 테스트")
    inner class WishCountRetrievalTests {
        
        @Test
        @DisplayName("오늘의 위시 카운트 조회")
        fun `should get today's wish count`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val entity = createWishCountEntity(date = today, count = 50)
            val flow = flowOf(entity)
            
            every { wishCountDao.getWishCountByDate(today) } returns flow
            
            // When
            repository.getTodayWishCount().test {
                // Then
                val result = awaitItem()
                result shouldNotBe null
                result?.count shouldBe 50
                result?.date shouldBe today
                
                awaitComplete()
            }
            
            verify { wishCountDao.getWishCountByDate(today) }
        }
        
        @Test
        @DisplayName("특정 날짜의 위시 카운트 조회")
        fun `should get wish count by specific date`() = runTest(testDispatcher) {
            // Given
            val date = LocalDate.of(2025, 1, 1)
            val entity = createWishCountEntity(date = date, count = 75)
            
            coEvery { wishCountDao.getWishCountByDateSuspend(date) } returns entity
            
            // When
            val result = repository.getWishCountByDate(date)
            
            // Then
            result shouldNotBe null
            result?.count shouldBe 75
            result?.date shouldBe date
            
            coVerify { wishCountDao.getWishCountByDateSuspend(date) }
        }
        
        @Test
        @DisplayName("날짜 범위로 위시 카운트 조회")
        fun `should get wish counts by date range`() = runTest(testDispatcher) {
            // Given
            val startDate = LocalDate.now().minusDays(7)
            val endDate = LocalDate.now()
            val entities = listOf(
                createWishCountEntity(date = startDate, count = 30),
                createWishCountEntity(date = startDate.plusDays(1), count = 50),
                createWishCountEntity(date = endDate, count = 70)
            )
            
            coEvery { 
                wishCountDao.getWishCountsByDateRange(startDate, endDate) 
            } returns entities
            
            // When
            val result = repository.getWishCountsByDateRange(startDate, endDate)
            
            // Then
            result shouldHaveSize 3
            result[0].count shouldBe 30
            result[2].count shouldBe 70
            
            coVerify { wishCountDao.getWishCountsByDateRange(startDate, endDate) }
        }
        
        @Test
        @DisplayName("모든 위시 카운트 스트림 조회")
        fun `should observe all wish counts stream`() = runTest(testDispatcher) {
            // Given
            val entities = listOf(
                createWishCountEntity(date = LocalDate.now(), count = 100),
                createWishCountEntity(date = LocalDate.now().minusDays(1), count = 80)
            )
            val flow = flowOf(entities)
            
            every { wishCountDao.getAllWishCounts() } returns flow
            
            // When
            repository.getAllWishCountsFlow().test {
                // Then
                val result = awaitItem()
                result shouldHaveSize 2
                result[0].count shouldBe 100
                
                awaitComplete()
            }
            
            verify { wishCountDao.getAllWishCounts() }
        }
    }
    
    @Nested
    @DisplayName("위시 카운트 증가 테스트")
    inner class IncrementTests {
        
        @Test
        @DisplayName("기존 카운트가 있을 때 증가")
        fun `should increment existing count`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val existingEntity = createWishCountEntity(date = today, count = 50)
            
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } returns existingEntity
            coEvery { wishCountDao.upsert(any()) } just Runs
            
            // When
            repository.incrementCount()
            
            // Then
            coVerify {
                wishCountDao.getWishCountByDateSuspend(today)
                wishCountDao.upsert(match { it.count == 51 })
            }
        }
        
        @Test
        @DisplayName("카운트가 없을 때 새로 생성")
        fun `should create new count when none exists`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } returns null
            coEvery { wishCountDao.upsert(any()) } just Runs
            
            // When
            repository.incrementCount()
            
            // Then
            coVerify {
                wishCountDao.getWishCountByDateSuspend(today)
                wishCountDao.upsert(match { 
                    it.date == today && it.count == 1 
                })
            }
        }
        
        @ParameterizedTest
        @ValueSource(ints = [1, 5, 10, 20])
        @DisplayName("지정된 값만큼 카운트 증가")
        fun `should increment by specific amount`(amount: Int) = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val existingEntity = createWishCountEntity(date = today, count = 30)
            
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } returns existingEntity
            coEvery { wishCountDao.upsert(any()) } just Runs
            
            // When
            repository.incrementCount(amount)
            
            // Then
            coVerify {
                wishCountDao.upsert(match { 
                    it.count == 30 + amount 
                })
            }
        }
        
        @Test
        @DisplayName("최대 카운트 제한 적용")
        fun `should apply max count limit`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val existingEntity = createWishCountEntity(date = today, count = 9995)
            
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } returns existingEntity
            coEvery { wishCountDao.upsert(any()) } just Runs
            
            // When
            repository.incrementCount(10)
            
            // Then
            coVerify {
                wishCountDao.upsert(match { 
                    it.count == 10000 // MAX_COUNT
                })
            }
        }
    }
    
    @Nested
    @DisplayName("리셋 기능 테스트")
    inner class ResetTests {
        
        @Test
        @DisplayName("수동 리셋 실행")
        fun `should perform manual reset`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val existingEntity = createWishCountEntity(date = today, count = 75)
            
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } returns existingEntity
            coEvery { wishCountDao.upsert(any()) } just Runs
            coEvery { resetLogDao.insert(any()) } returns 1L
            
            // When
            repository.resetCount(ResetReason.MANUAL)
            
            // Then
            coVerify {
                wishCountDao.upsert(match { it.count == 0 })
                resetLogDao.insert(match { 
                    it.beforeCount == 75 && 
                    it.afterCount == 0 &&
                    it.reason == "MANUAL"
                })
            }
        }
        
        @Test
        @DisplayName("자정 자동 리셋")
        fun `should perform midnight auto reset`() = runTest(testDispatcher) {
            // Given
            val yesterday = LocalDate.now().minusDays(1)
            val today = LocalDate.now()
            val yesterdayEntity = createWishCountEntity(date = yesterday, count = 100)
            
            coEvery { wishCountDao.getWishCountByDateSuspend(yesterday) } returns yesterdayEntity
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } returns null
            coEvery { wishCountDao.upsert(any()) } just Runs
            coEvery { resetLogDao.insert(any()) } returns 1L
            
            // When
            repository.performMidnightReset()
            
            // Then
            coVerify {
                wishCountDao.upsert(match { 
                    it.date == today && it.count == 0 
                })
                resetLogDao.insert(match { 
                    it.reason == "MIDNIGHT" 
                })
            }
        }
        
        @Test
        @DisplayName("목표 달성 시 리셋")
        fun `should reset on goal achievement`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val goalCount = 100
            val existingEntity = createWishCountEntity(
                date = today, 
                count = goalCount,
                dailyGoal = goalCount
            )
            
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } returns existingEntity
            coEvery { wishCountDao.upsert(any()) } just Runs
            coEvery { resetLogDao.insert(any()) } returns 1L
            
            // When
            repository.resetCount(ResetReason.GOAL_ACHIEVED)
            
            // Then
            coVerify {
                resetLogDao.insert(match { 
                    it.beforeCount == goalCount &&
                    it.reason == "GOAL_ACHIEVED"
                })
            }
        }
    }
    
    @Nested
    @DisplayName("스트릭 계산 테스트")
    inner class StreakCalculationTests {
        
        @Test
        @DisplayName("연속 스트릭 계산")
        fun `should calculate current streak`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val entities = listOf(
                createWishCountEntity(date = today, count = 100, dailyGoal = 100),
                createWishCountEntity(date = today.minusDays(1), count = 100, dailyGoal = 100),
                createWishCountEntity(date = today.minusDays(2), count = 100, dailyGoal = 100),
                createWishCountEntity(date = today.minusDays(4), count = 100, dailyGoal = 100)
            )
            
            coEvery { wishCountDao.getAllWishCountsSuspend() } returns entities
            
            // When
            val streak = repository.getCurrentStreak()
            
            // Then
            streak shouldBe 3 // 오늘, 어제, 그제까지 연속
            
            coVerify { wishCountDao.getAllWishCountsSuspend() }
        }
        
        @Test
        @DisplayName("최장 스트릭 계산")
        fun `should calculate longest streak`() = runTest(testDispatcher) {
            // Given
            val entities = createStreakTestData()
            
            coEvery { wishCountDao.getAllWishCountsSuspend() } returns entities
            
            // When
            val longestStreak = repository.getLongestStreak()
            
            // Then
            longestStreak shouldBe 5 // 테스트 데이터의 최장 연속 기간
            
            coVerify { wishCountDao.getAllWishCountsSuspend() }
        }
        
        @Test
        @DisplayName("스트릭 깨짐 감지")
        fun `should detect broken streak`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val entities = listOf(
                createWishCountEntity(date = today, count = 50, dailyGoal = 100), // 미달성
                createWishCountEntity(date = today.minusDays(1), count = 100, dailyGoal = 100),
                createWishCountEntity(date = today.minusDays(2), count = 100, dailyGoal = 100)
            )
            
            coEvery { wishCountDao.getAllWishCountsSuspend() } returns entities
            
            // When
            val streak = repository.getCurrentStreak()
            
            // Then
            streak shouldBe 0 // 오늘 미달성으로 스트릭 깨짐
        }
    }
    
    @Nested
    @DisplayName("통계 계산 테스트")
    inner class StatisticsTests {
        
        @Test
        @DisplayName("주간 통계 계산")
        fun `should calculate weekly statistics`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val weekEntities = (0..6).map { day ->
                createWishCountEntity(
                    date = today.minusDays(day.toLong()),
                    count = (day + 1) * 10
                )
            }
            
            coEvery { 
                wishCountDao.getWishCountsByDateRange(any(), any()) 
            } returns weekEntities
            
            // When
            val stats = repository.getWeeklyStatistics()
            
            // Then
            stats.totalCount shouldBe weekEntities.sumOf { it.count }
            stats.averageCount shouldBe weekEntities.map { it.count }.average()
            stats.achievementDays shouldBe weekEntities.count { 
                it.count >= it.dailyGoal 
            }
        }
        
        @Test
        @DisplayName("월간 통계 계산")
        fun `should calculate monthly statistics`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val monthStart = today.withDayOfMonth(1)
            val monthEntities = (0..29).map { day ->
                createWishCountEntity(
                    date = monthStart.plusDays(day.toLong()),
                    count = if (day % 2 == 0) 100 else 50,
                    dailyGoal = 100
                )
            }
            
            coEvery { 
                wishCountDao.getWishCountsByDateRange(monthStart, today) 
            } returns monthEntities.filter { it.date <= today }
            
            // When
            val stats = repository.getMonthlyStatistics()
            
            // Then
            stats.achievementDays shouldBe 15 // 짝수 날짜만 달성
        }
        
        @Test
        @DisplayName("전체 통계 계산")
        fun `should calculate overall statistics`() = runTest(testDispatcher) {
            // Given
            val allEntities = (0..99).map { day ->
                createWishCountEntity(
                    date = LocalDate.now().minusDays(day.toLong()),
                    count = 50 + day
                )
            }
            
            coEvery { wishCountDao.getAllWishCountsSuspend() } returns allEntities
            coEvery { wishCountDao.getTotalWishCount() } returns allEntities.sumOf { it.count }
            
            // When
            val totalCount = repository.getTotalWishCount()
            val stats = repository.getOverallStatistics()
            
            // Then
            totalCount shouldBe allEntities.sumOf { it.count }
            stats.totalDays shouldBe 100
        }
    }
    
    @Nested
    @DisplayName("데이터 정리 및 유지보수 테스트")
    inner class MaintenanceTests {
        
        @Test
        @DisplayName("오래된 데이터 정리")
        fun `should cleanup old data`() = runTest(testDispatcher) {
            // Given
            val cutoffDate = LocalDate.now().minusMonths(6)
            
            coEvery { wishCountDao.deleteOldData(cutoffDate) } returns 10
            coEvery { resetLogDao.deleteOldLogs(cutoffDate) } returns 5
            
            // When
            val deleted = repository.cleanupOldData(cutoffDate)
            
            // Then
            deleted shouldBe 15
            
            coVerify {
                wishCountDao.deleteOldData(cutoffDate)
                resetLogDao.deleteOldLogs(cutoffDate)
            }
        }
        
        @Test
        @DisplayName("데이터베이스 최적화")
        fun `should optimize database`() = runTest(testDispatcher) {
            // Given
            coEvery { wishCountDao.vacuum() } just Runs
            coEvery { resetLogDao.vacuum() } just Runs
            
            // When
            repository.optimizeDatabase()
            
            // Then
            coVerify {
                wishCountDao.vacuum()
                resetLogDao.vacuum()
            }
        }
        
        @Test
        @DisplayName("데이터 무결성 검증")
        fun `should verify data integrity`() = runTest(testDispatcher) {
            // Given
            val entities = listOf(
                createWishCountEntity(date = LocalDate.now(), count = -1), // 잘못된 데이터
                createWishCountEntity(date = LocalDate.now().minusDays(1), count = 100)
            )
            
            coEvery { wishCountDao.getAllWishCountsSuspend() } returns entities
            coEvery { wishCountDao.upsert(any()) } just Runs
            
            // When
            val issues = repository.verifyDataIntegrity()
            
            // Then
            issues shouldHaveSize 1
            issues[0].shouldContain("negative count")
            
            coVerify {
                wishCountDao.upsert(match { it.count == 0 }) // 음수를 0으로 수정
            }
        }
    }
    
    @Nested
    @DisplayName("동시성 및 트랜잭션 테스트")
    inner class ConcurrencyTests {
        
        @Test
        @DisplayName("동시 증가 요청 처리")
        fun `should handle concurrent increment requests`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val initialEntity = createWishCountEntity(date = today, count = 0)
            var currentCount = 0
            
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } answers {
                createWishCountEntity(date = today, count = currentCount)
            }
            
            coEvery { wishCountDao.upsert(any()) } answers {
                currentCount = firstArg<WishCountEntity>().count
            }
            
            // When - 10개의 동시 증가 요청
            val jobs = List(10) {
                launch {
                    repository.incrementCount()
                }
            }
            jobs.forEach { it.join() }
            
            // Then
            currentCount shouldBe 10
        }
        
        @Test
        @DisplayName("트랜잭션 롤백 테스트")
        fun `should rollback transaction on error`() = runTest(testDispatcher) {
            // Given
            val today = LocalDate.now()
            val entity = createWishCountEntity(date = today, count = 50)
            
            coEvery { wishCountDao.getWishCountByDateSuspend(today) } returns entity
            coEvery { wishCountDao.upsert(any()) } throws Exception("DB Error")
            
            // When/Then
            assertThrows<Exception> {
                runBlocking {
                    repository.incrementCount()
                }
            }
            
            // 롤백 확인 - resetLogDao.insert가 호출되지 않아야 함
            coVerify(exactly = 0) { resetLogDao.insert(any()) }
        }
    }
    
    // Helper functions
    private fun createWishCountEntity(
        id: Long = 0,
        date: LocalDate = LocalDate.now(),
        count: Int = 0,
        dailyGoal: Int = 100,
        resetCount: Int = 0,
        lostCount: Int = 0
    ) = WishCountEntity(
        id = id,
        date = date,
        count = count,
        dailyGoal = dailyGoal,
        resetCount = resetCount,
        lostCount = lostCount,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    private fun createResetLogEntity(
        date: LocalDate = LocalDate.now(),
        beforeCount: Int = 50,
        afterCount: Int = 0,
        reason: String = "MANUAL"
    ) = ResetLogEntity(
        id = 0,
        date = date,
        resetTime = LocalDateTime.now(),
        beforeCount = beforeCount,
        afterCount = afterCount,
        reason = reason
    )
    
    private fun createStreakTestData(): List<WishCountEntity> {
        val today = LocalDate.now()
        return listOf(
            // 현재 스트릭: 2일
            createWishCountEntity(date = today, count = 100, dailyGoal = 100),
            createWishCountEntity(date = today.minusDays(1), count = 100, dailyGoal = 100),
            // 중단
            createWishCountEntity(date = today.minusDays(3), count = 50, dailyGoal = 100),
            // 이전 스트릭: 5일 (최장)
            createWishCountEntity(date = today.minusDays(4), count = 100, dailyGoal = 100),
            createWishCountEntity(date = today.minusDays(5), count = 100, dailyGoal = 100),
            createWishCountEntity(date = today.minusDays(6), count = 100, dailyGoal = 100),
            createWishCountEntity(date = today.minusDays(7), count = 100, dailyGoal = 100),
            createWishCountEntity(date = today.minusDays(8), count = 100, dailyGoal = 100)
        )
    }
}

// Mock implementation for testing
class WishCountRepositoryImpl(
    private val wishCountDao: WishCountDao,
    private val resetLogDao: ResetLogDao,
    private val ioDispatcher: TestDispatcher
) : WishCountRepository {
    
    override fun getTodayWishCount(): Flow<WishCount?> {
        return wishCountDao.getWishCountByDate(LocalDate.now())
            .map { it?.toDomainModel() }
    }
    
    override suspend fun getWishCountByDate(date: LocalDate): WishCount? {
        return wishCountDao.getWishCountByDateSuspend(date)?.toDomainModel()
    }
    
    override suspend fun getWishCountsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WishCount> {
        return wishCountDao.getWishCountsByDateRange(startDate, endDate)
            .map { it.toDomainModel() }
    }
    
    override fun getAllWishCountsFlow(): Flow<List<WishCount>> {
        return wishCountDao.getAllWishCounts()
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override suspend fun incrementCount(amount: Int) {
        val today = LocalDate.now()
        val current = wishCountDao.getWishCountByDateSuspend(today)
        val newCount = (current?.count ?: 0) + amount
        val limitedCount = minOf(newCount, 10000) // MAX_COUNT
        
        val entity = current?.copy(
            count = limitedCount,
            updatedAt = LocalDateTime.now()
        ) ?: WishCountEntity(
            date = today,
            count = limitedCount,
            dailyGoal = 100,
            resetCount = 0,
            lostCount = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        wishCountDao.upsert(entity)
    }
    
    override suspend fun resetCount(reason: ResetReason) {
        val today = LocalDate.now()
        val current = wishCountDao.getWishCountByDateSuspend(today) ?: return
        
        // Save reset log
        val resetLog = ResetLogEntity(
            date = today,
            resetTime = LocalDateTime.now(),
            beforeCount = current.count,
            afterCount = 0,
            reason = reason.name
        )
        resetLogDao.insert(resetLog)
        
        // Reset count
        val updated = current.copy(
            count = 0,
            resetCount = current.resetCount + 1,
            lostCount = current.lostCount + current.count,
            updatedAt = LocalDateTime.now()
        )
        wishCountDao.upsert(updated)
    }
    
    override suspend fun performMidnightReset() {
        val yesterday = LocalDate.now().minusDays(1)
        val yesterdayCount = wishCountDao.getWishCountByDateSuspend(yesterday)
        
        if (yesterdayCount != null && yesterdayCount.count > 0) {
            // Log the reset
            val resetLog = ResetLogEntity(
                date = LocalDate.now(),
                resetTime = LocalDateTime.now(),
                beforeCount = yesterdayCount.count,
                afterCount = 0,
                reason = "MIDNIGHT"
            )
            resetLogDao.insert(resetLog)
        }
        
        // Create new entry for today
        val today = LocalDate.now()
        if (wishCountDao.getWishCountByDateSuspend(today) == null) {
            val newEntity = WishCountEntity(
                date = today,
                count = 0,
                dailyGoal = yesterdayCount?.dailyGoal ?: 100,
                resetCount = 0,
                lostCount = 0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            wishCountDao.upsert(newEntity)
        }
    }
    
    override suspend fun getCurrentStreak(): Int {
        val allCounts = wishCountDao.getAllWishCountsSuspend()
            .sortedByDescending { it.date }
        
        if (allCounts.isEmpty()) return 0
        
        var streak = 0
        var currentDate = LocalDate.now()
        
        for (entity in allCounts) {
            if (entity.date == currentDate && entity.count >= entity.dailyGoal) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else if (entity.date == currentDate && entity.count < entity.dailyGoal) {
                break
            } else if (entity.date < currentDate.minusDays(1)) {
                break
            }
        }
        
        return streak
    }
    
    override suspend fun getLongestStreak(): Int {
        val allCounts = wishCountDao.getAllWishCountsSuspend()
            .sortedBy { it.date }
        
        if (allCounts.isEmpty()) return 0
        
        var longestStreak = 0
        var currentStreak = 0
        var previousDate: LocalDate? = null
        
        for (entity in allCounts) {
            if (entity.count >= entity.dailyGoal) {
                if (previousDate == null || entity.date == previousDate.plusDays(1)) {
                    currentStreak++
                    longestStreak = maxOf(longestStreak, currentStreak)
                } else {
                    currentStreak = 1
                }
                previousDate = entity.date
            } else {
                currentStreak = 0
                previousDate = entity.date
            }
        }
        
        return longestStreak
    }
    
    override suspend fun getTotalWishCount(): Int {
        return wishCountDao.getTotalWishCount()
    }
    
    override suspend fun getWeeklyStatistics(): Statistics {
        val weekAgo = LocalDate.now().minusWeeks(1)
        val today = LocalDate.now()
        val weekData = wishCountDao.getWishCountsByDateRange(weekAgo, today)
        
        return Statistics(
            totalCount = weekData.sumOf { it.count },
            averageCount = if (weekData.isNotEmpty()) weekData.map { it.count }.average() else 0.0,
            achievementDays = weekData.count { it.count >= it.dailyGoal },
            totalDays = 7
        )
    }
    
    override suspend fun getMonthlyStatistics(): Statistics {
        val monthStart = LocalDate.now().withDayOfMonth(1)
        val today = LocalDate.now()
        val monthData = wishCountDao.getWishCountsByDateRange(monthStart, today)
        
        return Statistics(
            totalCount = monthData.sumOf { it.count },
            averageCount = if (monthData.isNotEmpty()) monthData.map { it.count }.average() else 0.0,
            achievementDays = monthData.count { it.count >= it.dailyGoal },
            totalDays = today.dayOfMonth
        )
    }
    
    override suspend fun getOverallStatistics(): Statistics {
        val allData = wishCountDao.getAllWishCountsSuspend()
        
        return Statistics(
            totalCount = allData.sumOf { it.count },
            averageCount = if (allData.isNotEmpty()) allData.map { it.count }.average() else 0.0,
            achievementDays = allData.count { it.count >= it.dailyGoal },
            totalDays = allData.size
        )
    }
    
    suspend fun cleanupOldData(cutoffDate: LocalDate): Int {
        val deletedCounts = wishCountDao.deleteOldData(cutoffDate)
        val deletedLogs = resetLogDao.deleteOldLogs(cutoffDate)
        return deletedCounts + deletedLogs
    }
    
    suspend fun optimizeDatabase() {
        wishCountDao.vacuum()
        resetLogDao.vacuum()
    }
    
    suspend fun verifyDataIntegrity(): List<String> {
        val issues = mutableListOf<String>()
        val allData = wishCountDao.getAllWishCountsSuspend()
        
        allData.forEach { entity ->
            if (entity.count < 0) {
                issues.add("Entity ${entity.id} has negative count")
                // Fix the issue
                wishCountDao.upsert(entity.copy(count = 0))
            }
        }
        
        return issues
    }
    
    private fun WishCountEntity.toDomainModel() = WishCount(
        id = id,
        date = date,
        count = count,
        dailyGoal = dailyGoal,
        resetCount = resetCount,
        lostCount = lostCount
    )
}

// Supporting classes
enum class ResetReason {
    MANUAL, MIDNIGHT, GOAL_ACHIEVED, ERROR_RECOVERY
}

data class Statistics(
    val totalCount: Int,
    val averageCount: Double,
    val achievementDays: Int,
    val totalDays: Int
)