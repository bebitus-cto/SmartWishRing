package com.wishring.app.domain.usecase

import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.ResetLogRepository
import com.wishring.app.domain.repository.PreferencesRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * WishCount 관련 UseCase 테스트
 * 
 * 비즈니스 로직 유스케이스를 검증합니다.
 */
@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("WishCount UseCase 테스트")
class WishCountUseCaseTest {

    private lateinit var testScope: TestScope
    private lateinit var wishCountRepository: WishCountRepository
    private lateinit var resetLogRepository: ResetLogRepository
    private lateinit var preferencesRepository: PreferencesRepository
    
    @BeforeEach
    fun setup() {
        testScope = TestScope()
        wishCountRepository = mockk(relaxed = true)
        resetLogRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
    }
    
    @Nested
    @DisplayName("IncrementWishCountUseCase - 위시 카운트 증가")
    inner class IncrementWishCountUseCaseTest {
        
        private lateinit var useCase: IncrementWishCountUseCase
        
        @BeforeEach
        fun setup() {
            useCase = IncrementWishCountUseCase(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository
            )
        }
        
        @Test
        @DisplayName("정상적인 카운트 증가")
        fun testNormalIncrement() = testScope.runTest {
            // Given
            val currentWish = WishCount(
                date = LocalDate.now(),
                wishText = "테스트 위시",
                targetCount = 100,
                currentCount = 50
            )
            
            coEvery { wishCountRepository.getTodayWishCount() } returns flowOf(currentWish)
            coEvery { wishCountRepository.incrementCount() } returns true
            
            // When
            val result = useCase.execute()
            
            // Then
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 51
            
            coVerify { 
                wishCountRepository.getTodayWishCount()
                wishCountRepository.incrementCount() 
            }
        }
        
        @Test
        @DisplayName("목표 달성 시 완료 처리")
        fun testCompletionOnTargetReached() = testScope.runTest {
            // Given
            val currentWish = WishCount(
                date = LocalDate.now(),
                wishText = "테스트 위시",
                targetCount = 100,
                currentCount = 99
            )
            
            coEvery { wishCountRepository.getTodayWishCount() } returns flowOf(currentWish)
            coEvery { wishCountRepository.incrementCount() } returns true
            coEvery { wishCountRepository.markAsCompleted() } just Runs
            coEvery { preferencesRepository.incrementTotalCompleted() } just Runs
            
            // When
            val result = useCase.execute()
            
            // Then
            result.isSuccess shouldBe true
            result.getOrNull() shouldBe 100
            
            coVerify { 
                wishCountRepository.markAsCompleted()
                preferencesRepository.incrementTotalCompleted()
            }
        }
        
        @Test
        @DisplayName("오늘의 위시가 없을 때 실패")
        fun testIncrementWithoutTodayWish() = testScope.runTest {
            // Given
            coEvery { wishCountRepository.getTodayWishCount() } returns flowOf(null)
            
            // When
            val result = useCase.execute()
            
            // Then
            result.isFailure shouldBe true
            result.exceptionOrNull()?.message shouldBe "No wish for today"
        }
    }
    
    @Nested
    @DisplayName("ResetWishCountUseCase - 위시 카운트 리셋")
    inner class ResetWishCountUseCaseTest {
        
        private lateinit var useCase: ResetWishCountUseCase
        
        @BeforeEach
        fun setup() {
            useCase = ResetWishCountUseCase(
                wishCountRepository = wishCountRepository,
                resetLogRepository = resetLogRepository
            )
        }
        
        @Test
        @DisplayName("수동 리셋 실행")
        fun testManualReset() = testScope.runTest {
            // Given
            val currentWish = WishCount(
                id = 1,
                date = LocalDate.now(),
                wishText = "테스트 위시",
                targetCount = 100,
                currentCount = 75
            )
            val reason = "목표 변경"
            
            coEvery { wishCountRepository.getTodayWishCount() } returns flowOf(currentWish)
            coEvery { wishCountRepository.resetCount() } returns true
            coEvery { resetLogRepository.createResetLog(any()) } just Runs
            
            // When
            val result = useCase.execute(
                type = ResetWishCountUseCase.ResetType.MANUAL,
                reason = reason
            )
            
            // Then
            result.isSuccess shouldBe true
            
            coVerify {
                wishCountRepository.resetCount()
                resetLogRepository.createResetLog(
                    withArg { log ->
                        log.wishCountId shouldBe 1
                        log.previousCount shouldBe 75
                        log.lostCount shouldBe 25
                        log.resetType shouldBe "MANUAL"
                        log.reason shouldBe reason
                    }
                )
            }
        }
        
        @Test
        @DisplayName("자정 자동 리셋")
        fun testMidnightAutoReset() = testScope.runTest {
            // Given
            val currentWish = WishCount(
                id = 2,
                date = LocalDate.now().minusDays(1), // 어제 날짜
                wishText = "어제 위시",
                targetCount = 50,
                currentCount = 30
            )
            
            coEvery { wishCountRepository.getYesterdayWishCount() } returns flowOf(currentWish)
            coEvery { wishCountRepository.resetCount() } returns true
            coEvery { resetLogRepository.createResetLog(any()) } just Runs
            
            // When
            val result = useCase.execute(
                type = ResetWishCountUseCase.ResetType.MIDNIGHT_AUTO
            )
            
            // Then
            result.isSuccess shouldBe true
            
            coVerify {
                resetLogRepository.createResetLog(
                    withArg { log ->
                        log.resetType shouldBe "MIDNIGHT_AUTO"
                        log.previousCount shouldBe 30
                        log.lostCount shouldBe 20
                    }
                )
            }
        }
    }
    
    @Nested
    @DisplayName("CalculateStreakUseCase - 스트릭 계산")
    inner class CalculateStreakUseCaseTest {
        
        private lateinit var useCase: CalculateStreakUseCase
        
        @BeforeEach
        fun setup() {
            useCase = CalculateStreakUseCase(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository
            )
        }
        
        @Test
        @DisplayName("연속 달성 일수 계산")
        fun testCalculateConsecutiveStreak() = testScope.runTest {
            // Given
            val today = LocalDate.now()
            val wishes = listOf(
                WishCount(date = today, wishText = "오늘", targetCount = 10, 
                    currentCount = 10, isCompleted = true),
                WishCount(date = today.minusDays(1), wishText = "어제", targetCount = 10,
                    currentCount = 10, isCompleted = true),
                WishCount(date = today.minusDays(2), wishText = "그제", targetCount = 10,
                    currentCount = 10, isCompleted = true),
                WishCount(date = today.minusDays(3), wishText = "3일전", targetCount = 10,
                    currentCount = 8, isCompleted = false), // 스트릭 끊김
                WishCount(date = today.minusDays(4), wishText = "4일전", targetCount = 10,
                    currentCount = 10, isCompleted = true)
            )
            
            coEvery { wishCountRepository.getRecentWishCounts(30) } returns flowOf(wishes)
            
            // When
            val streak = useCase.execute()
            
            // Then
            streak shouldBe 3 // 오늘부터 3일 연속
            
            coVerify { preferencesRepository.updateCurrentStreak(3) }
        }
        
        @Test
        @DisplayName("최대 스트릭 갱신")
        fun testUpdateMaxStreak() = testScope.runTest {
            // Given
            val wishes = List(10) { index ->
                WishCount(
                    date = LocalDate.now().minusDays(index.toLong()),
                    wishText = "Day $index",
                    targetCount = 10,
                    currentCount = 10,
                    isCompleted = true
                )
            }
            
            coEvery { wishCountRepository.getRecentWishCounts(30) } returns flowOf(wishes)
            coEvery { preferencesRepository.getCurrentMaxStreak() } returns 7
            
            // When
            val streak = useCase.execute()
            
            // Then
            streak shouldBe 10
            
            coVerify { 
                preferencesRepository.updateCurrentStreak(10)
                preferencesRepository.updateMaxStreak(10)
            }
        }
    }
    
    @Nested
    @DisplayName("GenerateStatisticsUseCase - 통계 생성")
    inner class GenerateStatisticsUseCaseTest {
        
        private lateinit var useCase: GenerateStatisticsUseCase
        
        @BeforeEach
        fun setup() {
            useCase = GenerateStatisticsUseCase(
                wishCountRepository = wishCountRepository,
                resetLogRepository = resetLogRepository
            )
        }
        
        @ParameterizedTest
        @CsvSource(
            "7,WEEKLY",
            "30,MONTHLY",
            "365,YEARLY"
        )
        @DisplayName("기간별 통계 생성")
        fun testGeneratePeriodStatistics(days: Int, period: String) = testScope.runTest {
            // Given
            val wishes = List(days) { index ->
                WishCount(
                    date = LocalDate.now().minusDays(index.toLong()),
                    wishText = "Day $index",
                    targetCount = 100,
                    currentCount = if (index % 3 == 0) 100 else 70,
                    isCompleted = index % 3 == 0
                )
            }
            
            val resetLogs = List(days / 7) { index ->
                ResetLog(
                    id = index.toLong(),
                    wishCountId = 1,
                    resetTime = LocalDateTime.now().minusDays(index * 7L),
                    previousCount = 50,
                    lostCount = 50,
                    resetType = "MIDNIGHT_AUTO"
                )
            }
            
            coEvery { 
                wishCountRepository.getWishCountsForDateRange(any(), any()) 
            } returns flowOf(wishes)
            
            coEvery {
                resetLogRepository.getResetLogsForDateRange(any(), any())
            } returns flowOf(resetLogs)
            
            // When
            val stats = useCase.execute(GenerateStatisticsUseCase.Period.valueOf(period))
            
            // Then
            stats.totalDays shouldBe days
            stats.completedDays shouldBe (days / 3 + if (days % 3 > 0) 1 else 0)
            stats.totalCount shouldBe wishes.sumOf { it.currentCount }
            stats.averageCount shouldBe wishes.map { it.currentCount }.average().toInt()
            stats.completionRate shouldBe (stats.completedDays * 100 / stats.totalDays)
            stats.totalResets shouldBe resetLogs.size
            stats.totalLostCount shouldBe resetLogs.sumOf { it.lostCount }
        }
        
        @Test
        @DisplayName("베스트 기록 계산")
        fun testCalculateBestRecords() = testScope.runTest {
            // Given
            val wishes = listOf(
                WishCount(date = LocalDate.now(), wishText = "오늘", 
                    targetCount = 100, currentCount = 150), // 최고 카운트
                WishCount(date = LocalDate.now().minusDays(1), wishText = "어제",
                    targetCount = 50, currentCount = 50, isCompleted = true,
                    completedAt = LocalDateTime.now().minusDays(1).withHour(10)), // 가장 빠른 완료
                WishCount(date = LocalDate.now().minusDays(2), wishText = "그제",
                    targetCount = 200, currentCount = 180) // 최고 목표
            )
            
            coEvery { 
                wishCountRepository.getWishCountsForDateRange(any(), any()) 
            } returns flowOf(wishes)
            
            coEvery {
                resetLogRepository.getResetLogsForDateRange(any(), any())
            } returns flowOf(emptyList())
            
            // When
            val stats = useCase.execute(GenerateStatisticsUseCase.Period.WEEKLY)
            
            // Then
            stats.bestDayCount shouldBe 150
            stats.bestDayDate shouldBe LocalDate.now()
            stats.earliestCompletionTime shouldBe LocalDateTime.now().minusDays(1).withHour(10)
            stats.highestTarget shouldBe 200
        }
    }
}

// UseCase 구현체들
class IncrementWishCountUseCase(
    private val wishCountRepository: WishCountRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(): Result<Int> {
        val currentWish = wishCountRepository.getTodayWishCount().first()
            ?: return Result.failure(Exception("No wish for today"))
        
        val success = wishCountRepository.incrementCount()
        if (!success) {
            return Result.failure(Exception("Failed to increment count"))
        }
        
        val newCount = currentWish.currentCount + 1
        
        if (newCount >= currentWish.targetCount && !currentWish.isCompleted) {
            wishCountRepository.markAsCompleted()
            preferencesRepository.incrementTotalCompleted()
        }
        
        return Result.success(newCount)
    }
}

class ResetWishCountUseCase(
    private val wishCountRepository: WishCountRepository,
    private val resetLogRepository: ResetLogRepository
) {
    enum class ResetType { MANUAL, MIDNIGHT_AUTO, COMPLETION }
    
    suspend fun execute(type: ResetType, reason: String? = null): Result<Unit> {
        val currentWish = when (type) {
            ResetType.MIDNIGHT_AUTO -> wishCountRepository.getYesterdayWishCount().first()
            else -> wishCountRepository.getTodayWishCount().first()
        } ?: return Result.failure(Exception("No wish to reset"))
        
        val resetLog = ResetLog(
            wishCountId = currentWish.id ?: 0,
            resetTime = LocalDateTime.now(),
            previousCount = currentWish.currentCount,
            lostCount = maxOf(0, currentWish.targetCount - currentWish.currentCount),
            resetType = type.name,
            reason = reason
        )
        
        resetLogRepository.createResetLog(resetLog)
        wishCountRepository.resetCount()
        
        return Result.success(Unit)
    }
}

class CalculateStreakUseCase(
    private val wishCountRepository: WishCountRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(): Int {
        val recentWishes = wishCountRepository.getRecentWishCounts(30).first()
            .sortedByDescending { it.date }
        
        var streak = 0
        for (wish in recentWishes) {
            if (wish.isCompleted) {
                streak++
            } else {
                break
            }
        }
        
        preferencesRepository.updateCurrentStreak(streak)
        
        val currentMax = preferencesRepository.getCurrentMaxStreak()
        if (streak > currentMax) {
            preferencesRepository.updateMaxStreak(streak)
        }
        
        return streak
    }
}

class GenerateStatisticsUseCase(
    private val wishCountRepository: WishCountRepository,
    private val resetLogRepository: ResetLogRepository
) {
    enum class Period { WEEKLY, MONTHLY, YEARLY }
    
    suspend fun execute(period: Period): Statistics {
        val days = when (period) {
            Period.WEEKLY -> 7
            Period.MONTHLY -> 30
            Period.YEARLY -> 365
        }
        
        val startDate = LocalDate.now().minusDays(days.toLong())
        val endDate = LocalDate.now()
        
        val wishes = wishCountRepository.getWishCountsForDateRange(startDate, endDate).first()
        val resetLogs = resetLogRepository.getResetLogsForDateRange(startDate, endDate).first()
        
        return Statistics(
            totalDays = days,
            completedDays = wishes.count { it.isCompleted },
            totalCount = wishes.sumOf { it.currentCount },
            averageCount = if (wishes.isEmpty()) 0 else wishes.map { it.currentCount }.average().toInt(),
            completionRate = if (days == 0) 0 else (wishes.count { it.isCompleted } * 100 / days),
            totalResets = resetLogs.size,
            totalLostCount = resetLogs.sumOf { it.lostCount },
            bestDayCount = wishes.maxOfOrNull { it.currentCount } ?: 0,
            bestDayDate = wishes.maxByOrNull { it.currentCount }?.date,
            earliestCompletionTime = wishes.filter { it.isCompleted }
                .mapNotNull { it.completedAt }
                .minOrNull(),
            highestTarget = wishes.maxOfOrNull { it.targetCount } ?: 0
        )
    }
}

data class Statistics(
    val totalDays: Int,
    val completedDays: Int,
    val totalCount: Int,
    val averageCount: Int,
    val completionRate: Int,
    val totalResets: Int,
    val totalLostCount: Int,
    val bestDayCount: Int,
    val bestDayDate: LocalDate?,
    val earliestCompletionTime: LocalDateTime?,
    val highestTarget: Int
)

data class ResetLog(
    val id: Long? = null,
    val wishCountId: Long,
    val resetTime: LocalDateTime,
    val previousCount: Int,
    val lostCount: Int,
    val resetType: String,
    val reason: String? = null
)

// Repository extension functions for test
private suspend fun WishCountRepository.getYesterdayWishCount() = 
    getWishCountByDate(LocalDate.now().minusDays(1))

private suspend fun WishCountRepository.markAsCompleted() {
    // Implementation would mark current wish as completed
}

private suspend fun WishCountRepository.resetCount(): Boolean {
    // Implementation would reset the count
    return true
}

private suspend fun PreferencesRepository.incrementTotalCompleted() {
    // Implementation would increment total completed count
}

private suspend fun PreferencesRepository.updateCurrentStreak(streak: Int) {
    // Implementation would update current streak
}

private suspend fun PreferencesRepository.getCurrentMaxStreak(): Int {
    // Implementation would get current max streak
    return 0
}

private suspend fun PreferencesRepository.updateMaxStreak(streak: Int) {
    // Implementation would update max streak
}

private suspend fun ResetLogRepository.createResetLog(log: ResetLog) {
    // Implementation would save reset log
}