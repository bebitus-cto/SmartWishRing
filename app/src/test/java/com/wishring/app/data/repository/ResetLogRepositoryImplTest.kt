package com.wishring.app.data.repository

import com.wishring.app.data.local.database.dao.ResetLogDao
import com.wishring.app.data.local.database.entity.ResetLogEntity
import com.wishring.app.data.local.repository.ResetLogRepositoryImpl
import com.wishring.app.domain.model.ResetLog
import com.wishring.app.domain.repository.ResetLogRepository
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * ResetLogRepositoryImpl 테스트
 * 
 * 리셋 로그 저장소의 동작을 검증합니다.
 */
@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ResetLogRepository 테스트")
class ResetLogRepositoryImplTest {

    private lateinit var repository: ResetLogRepositoryImpl
    private lateinit var resetLogDao: ResetLogDao
    private lateinit var testScope: TestScope
    
    @BeforeEach
    fun setup() {
        resetLogDao = mockk(relaxed = true)
        testScope = TestScope()
        
        repository = ResetLogRepositoryImpl(
            resetLogDao = resetLogDao,
            ioDispatcher = testScope.testScheduler
        )
    }
    
    @Nested
    @DisplayName("리셋 로그 생성 및 저장")
    inner class ResetLogCreationTest {
        
        @Test
        @DisplayName("자정 자동 리셋 로그 생성")
        fun testCreateMidnightResetLog() = testScope.runTest {
            // Given
            val previousCount = 85
            val targetCount = 100
            val resetTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
            
            coEvery { resetLogDao.insert(any()) } just Runs
            
            // When
            val resetLog = repository.createResetLog(
                wishCountId = 1,
                previousCount = previousCount,
                targetCount = targetCount,
                resetType = ResetLogRepository.ResetType.MIDNIGHT_AUTO,
                resetTime = resetTime
            )
            
            // Then
            resetLog.resetType shouldBe "MIDNIGHT_AUTO"
            resetLog.previousCount shouldBe previousCount
            resetLog.lostCount shouldBe (targetCount - previousCount)
            resetLog.resetTime shouldBe resetTime
            
            coVerify {
                resetLogDao.insert(
                    withArg { entity ->
                        entity.resetType shouldBe "MIDNIGHT_AUTO"
                        entity.previousCount shouldBe previousCount
                        entity.lostCount shouldBe 15
                    }
                )
            }
        }
        
        @Test
        @DisplayName("수동 리셋 로그 생성")
        fun testCreateManualResetLog() = testScope.runTest {
            // Given
            val previousCount = 50
            val reason = "목표 변경으로 인한 리셋"
            
            coEvery { resetLogDao.insert(any()) } just Runs
            
            // When
            val resetLog = repository.createResetLog(
                wishCountId = 2,
                previousCount = previousCount,
                targetCount = 100,
                resetType = ResetLogRepository.ResetType.MANUAL,
                reason = reason
            )
            
            // Then
            resetLog.resetType shouldBe "MANUAL"
            resetLog.reason shouldBe reason
            resetLog.lostCount shouldBe 50
            
            coVerify { resetLogDao.insert(any()) }
        }
        
        @Test
        @DisplayName("목표 달성 후 리셋")
        fun testCreateCompletionResetLog() = testScope.runTest {
            // Given
            val targetCount = 108
            
            coEvery { resetLogDao.insert(any()) } just Runs
            
            // When
            val resetLog = repository.createResetLog(
                wishCountId = 3,
                previousCount = targetCount,
                targetCount = targetCount,
                resetType = ResetLogRepository.ResetType.COMPLETION
            )
            
            // Then
            resetLog.resetType shouldBe "COMPLETION"
            resetLog.lostCount shouldBe 0 // 완료 후 리셋은 손실 없음
            resetLog.previousCount shouldBe targetCount
        }
    }
    
    @Nested
    @DisplayName("리셋 로그 조회")
    inner class ResetLogQueryTest {
        
        @Test
        @DisplayName("특정 날짜의 리셋 로그 조회")
        fun testGetResetLogsForDate() = testScope.runTest {
            // Given
            val date = LocalDate.now()
            val entities = listOf(
                ResetLogEntity(
                    id = 1,
                    wishCountId = 1,
                    resetTime = LocalDateTime.of(date, LocalTime.of(0, 0)),
                    previousCount = 50,
                    lostCount = 50,
                    resetType = "MIDNIGHT_AUTO"
                ),
                ResetLogEntity(
                    id = 2,
                    wishCountId = 1,
                    resetTime = LocalDateTime.of(date, LocalTime.of(14, 30)),
                    previousCount = 30,
                    lostCount = 70,
                    resetType = "MANUAL"
                )
            )
            
            coEvery { resetLogDao.getResetLogsByDate(date) } returns flowOf(entities)
            
            // When
            val logs = repository.getResetLogsForDate(date).first()
            
            // Then
            logs shouldHaveSize 2
            logs[0].resetType shouldBe "MIDNIGHT_AUTO"
            logs[1].resetType shouldBe "MANUAL"
        }
        
        @Test
        @DisplayName("날짜 범위로 리셋 로그 조회")
        fun testGetResetLogsForDateRange() = testScope.runTest {
            // Given
            val startDate = LocalDate.now().minusDays(7)
            val endDate = LocalDate.now()
            
            val entities = List(5) { index ->
                ResetLogEntity(
                    id = index.toLong(),
                    wishCountId = 1,
                    resetTime = LocalDateTime.of(
                        startDate.plusDays(index.toLong()),
                        LocalTime.MIDNIGHT
                    ),
                    previousCount = 10 * index,
                    lostCount = 100 - (10 * index),
                    resetType = "MIDNIGHT_AUTO"
                )
            }
            
            coEvery { 
                resetLogDao.getResetLogsByDateRange(startDate, endDate) 
            } returns flowOf(entities)
            
            // When
            val logs = repository.getResetLogsForDateRange(startDate, endDate).first()
            
            // Then
            logs shouldHaveSize 5
            logs.first().resetTime.toLocalDate() shouldBe startDate
            logs.last().resetTime.toLocalDate() shouldBe startDate.plusDays(4)
        }
        
        @Test
        @DisplayName("모든 리셋 로그 조회")
        fun testGetAllResetLogs() = testScope.runTest {
            // Given
            val entities = List(100) { index ->
                ResetLogEntity(
                    id = index.toLong(),
                    wishCountId = index.toLong() % 10,
                    resetTime = LocalDateTime.now().minusDays(index.toLong()),
                    previousCount = index,
                    lostCount = 100 - index,
                    resetType = if (index % 2 == 0) "MIDNIGHT_AUTO" else "MANUAL"
                )
            }
            
            coEvery { resetLogDao.getAllResetLogs() } returns flowOf(entities)
            
            // When
            val logs = repository.getAllResetLogs().first()
            
            // Then
            logs shouldHaveSize 100
            logs.count { it.resetType == "MIDNIGHT_AUTO" } shouldBe 50
            logs.count { it.resetType == "MANUAL" } shouldBe 50
        }
    }
    
    @Nested
    @DisplayName("리셋 통계 분석")
    inner class ResetStatisticsTest {
        
        @Test
        @DisplayName("총 손실 카운트 계산")
        fun testCalculateTotalLostCount() = testScope.runTest {
            // Given
            val entities = listOf(
                ResetLogEntity(id = 1, wishCountId = 1, resetTime = LocalDateTime.now(), 
                    previousCount = 80, lostCount = 20, resetType = "MIDNIGHT_AUTO"),
                ResetLogEntity(id = 2, wishCountId = 1, resetTime = LocalDateTime.now(), 
                    previousCount = 50, lostCount = 50, resetType = "MANUAL"),
                ResetLogEntity(id = 3, wishCountId = 1, resetTime = LocalDateTime.now(), 
                    previousCount = 95, lostCount = 5, resetType = "MIDNIGHT_AUTO")
            )
            
            coEvery { resetLogDao.getAllResetLogs() } returns flowOf(entities)
            
            // When
            val totalLost = repository.getTotalLostCount().first()
            
            // Then
            totalLost shouldBe 75 // 20 + 50 + 5
        }
        
        @Test
        @DisplayName("리셋 타입별 통계")
        fun testGetResetTypeStatistics() = testScope.runTest {
            // Given
            val entities = List(30) { index ->
                ResetLogEntity(
                    id = index.toLong(),
                    wishCountId = 1,
                    resetTime = LocalDateTime.now().minusDays(index.toLong()),
                    previousCount = 50,
                    lostCount = 50,
                    resetType = when (index % 3) {
                        0 -> "MIDNIGHT_AUTO"
                        1 -> "MANUAL"
                        else -> "COMPLETION"
                    }
                )
            }
            
            coEvery { resetLogDao.getAllResetLogs() } returns flowOf(entities)
            
            // When
            val stats = repository.getResetTypeStatistics().first()
            
            // Then
            stats["MIDNIGHT_AUTO"] shouldBe 10
            stats["MANUAL"] shouldBe 10
            stats["COMPLETION"] shouldBe 10
        }
        
        @Test
        @DisplayName("평균 손실 카운트 계산")
        fun testCalculateAverageLostCount() = testScope.runTest {
            // Given
            val entities = listOf(
                ResetLogEntity(id = 1, wishCountId = 1, resetTime = LocalDateTime.now(), 
                    previousCount = 80, lostCount = 20, resetType = "MIDNIGHT_AUTO"),
                ResetLogEntity(id = 2, wishCountId = 1, resetTime = LocalDateTime.now(), 
                    previousCount = 60, lostCount = 40, resetType = "MANUAL"),
                ResetLogEntity(id = 3, wishCountId = 1, resetTime = LocalDateTime.now(), 
                    previousCount = 70, lostCount = 30, resetType = "MIDNIGHT_AUTO")
            )
            
            coEvery { resetLogDao.getAllResetLogs() } returns flowOf(entities)
            
            // When
            val avgLost = repository.getAverageLostCount().first()
            
            // Then
            avgLost shouldBe 30.0 // (20 + 40 + 30) / 3
        }
    }
    
    @Nested
    @DisplayName("리셋 패턴 분석")
    inner class ResetPatternAnalysisTest {
        
        @Test
        @DisplayName("가장 빈번한 리셋 시간대 분석")
        fun testFindMostFrequentResetHour() = testScope.runTest {
            // Given
            val entities = listOf(
                // 자정 리셋 (5개)
                ResetLogEntity(id = 1, wishCountId = 1, 
                    resetTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0)),
                    previousCount = 50, lostCount = 50, resetType = "MIDNIGHT_AUTO"),
                ResetLogEntity(id = 2, wishCountId = 1,
                    resetTime = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(0, 0)),
                    previousCount = 60, lostCount = 40, resetType = "MIDNIGHT_AUTO"),
                ResetLogEntity(id = 3, wishCountId = 1,
                    resetTime = LocalDateTime.of(LocalDate.now().minusDays(2), LocalTime.of(0, 0)),
                    previousCount = 70, lostCount = 30, resetType = "MIDNIGHT_AUTO"),
                ResetLogEntity(id = 4, wishCountId = 1,
                    resetTime = LocalDateTime.of(LocalDate.now().minusDays(3), LocalTime.of(0, 0)),
                    previousCount = 80, lostCount = 20, resetType = "MIDNIGHT_AUTO"),
                ResetLogEntity(id = 5, wishCountId = 1,
                    resetTime = LocalDateTime.of(LocalDate.now().minusDays(4), LocalTime.of(0, 0)),
                    previousCount = 90, lostCount = 10, resetType = "MIDNIGHT_AUTO"),
                // 오후 리셋 (2개)
                ResetLogEntity(id = 6, wishCountId = 1,
                    resetTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 30)),
                    previousCount = 40, lostCount = 60, resetType = "MANUAL"),
                ResetLogEntity(id = 7, wishCountId = 1,
                    resetTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 0)),
                    previousCount = 35, lostCount = 65, resetType = "MANUAL")
            )
            
            coEvery { resetLogDao.getAllResetLogs() } returns flowOf(entities)
            
            // When
            val mostFrequentHour = repository.getMostFrequentResetHour().first()
            
            // Then
            mostFrequentHour shouldBe 0 // 자정이 가장 빈번
        }
        
        @ParameterizedTest
        @ValueSource(ints = [7, 14, 30, 60])
        @DisplayName("특정 기간 내 리셋 빈도 계산")
        fun testCalculateResetFrequency(days: Int) = testScope.runTest {
            // Given
            val entities = List(days) { index ->
                ResetLogEntity(
                    id = index.toLong(),
                    wishCountId = 1,
                    resetTime = LocalDateTime.now().minusDays(index.toLong()),
                    previousCount = 50,
                    lostCount = 50,
                    resetType = "MIDNIGHT_AUTO"
                )
            }
            
            val startDate = LocalDate.now().minusDays(days.toLong())
            val endDate = LocalDate.now()
            
            coEvery { 
                resetLogDao.getResetLogsByDateRange(startDate, endDate) 
            } returns flowOf(entities)
            
            // When
            val frequency = repository.getResetFrequency(days).first()
            
            // Then
            frequency shouldBe 1.0 // 매일 1회 리셋
        }
        
        @Test
        @DisplayName("연속 리셋 없는 최대 일수")
        fun testMaxDaysWithoutReset() = testScope.runTest {
            // Given - 불규칙한 리셋 패턴
            val resetDates = listOf(0L, 1L, 5L, 6L, 10L, 15L)
            val entities = resetDates.map { daysAgo ->
                ResetLogEntity(
                    id = daysAgo,
                    wishCountId = 1,
                    resetTime = LocalDateTime.now().minusDays(daysAgo),
                    previousCount = 50,
                    lostCount = 50,
                    resetType = "MIDNIGHT_AUTO"
                )
            }
            
            coEvery { resetLogDao.getAllResetLogs() } returns flowOf(entities)
            
            // When
            val maxStreak = repository.getMaxDaysWithoutReset().first()
            
            // Then
            maxStreak shouldBe 4 // 15일 전 ~ 10일 전 사이 (5일)
        }
    }
    
    @Nested
    @DisplayName("리셋 로그 삭제")
    inner class ResetLogDeletionTest {
        
        @Test
        @DisplayName("특정 리셋 로그 삭제")
        fun testDeleteResetLog() = testScope.runTest {
            // Given
            val logId = 42L
            coEvery { resetLogDao.deleteById(logId) } just Runs
            
            // When
            repository.deleteResetLog(logId)
            
            // Then
            coVerify { resetLogDao.deleteById(logId) }
        }
        
        @Test
        @DisplayName("날짜별 리셋 로그 삭제")
        fun testDeleteResetLogsByDate() = testScope.runTest {
            // Given
            val date = LocalDate.now()
            coEvery { resetLogDao.deleteByDate(date) } just Runs
            
            // When
            repository.deleteResetLogsByDate(date)
            
            // Then
            coVerify { resetLogDao.deleteByDate(date) }
        }
        
        @Test
        @DisplayName("오래된 리셋 로그 정리")
        fun testCleanupOldResetLogs() = testScope.runTest {
            // Given
            val retentionDays = 90
            val cutoffDate = LocalDate.now().minusDays(retentionDays.toLong())
            
            coEvery { resetLogDao.deleteOlderThan(cutoffDate) } returns 15
            
            // When
            val deletedCount = repository.cleanupOldResetLogs(retentionDays)
            
            // Then
            deletedCount shouldBe 15
            coVerify { resetLogDao.deleteOlderThan(cutoffDate) }
        }
    }
}

// Extension functions for Repository
private suspend fun ResetLogRepository.getTotalLostCount(): kotlinx.coroutines.flow.Flow<Int> {
    return getAllResetLogs().map { logs ->
        logs.sumOf { it.lostCount }
    }
}

private suspend fun ResetLogRepository.getResetTypeStatistics(): kotlinx.coroutines.flow.Flow<Map<String, Int>> {
    return getAllResetLogs().map { logs ->
        logs.groupingBy { it.resetType }.eachCount()
    }
}

private suspend fun ResetLogRepository.getAverageLostCount(): kotlinx.coroutines.flow.Flow<Double> {
    return getAllResetLogs().map { logs ->
        if (logs.isEmpty()) 0.0 else logs.map { it.lostCount }.average()
    }
}

private suspend fun ResetLogRepository.getMostFrequentResetHour(): kotlinx.coroutines.flow.Flow<Int> {
    return getAllResetLogs().map { logs ->
        logs.groupingBy { it.resetTime.hour }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: 0
    }
}

private suspend fun ResetLogRepository.getResetFrequency(days: Int): kotlinx.coroutines.flow.Flow<Double> {
    return getResetLogsForDateRange(
        LocalDate.now().minusDays(days.toLong()),
        LocalDate.now()
    ).map { logs ->
        logs.size.toDouble() / days
    }
}

private suspend fun ResetLogRepository.getMaxDaysWithoutReset(): kotlinx.coroutines.flow.Flow<Int> {
    return getAllResetLogs().map { logs ->
        if (logs.size < 2) return@map 0
        
        val sortedDates = logs.map { it.resetTime.toLocalDate() }.sorted()
        var maxGap = 0
        
        for (i in 1 until sortedDates.size) {
            val gap = sortedDates[i].toEpochDay() - sortedDates[i-1].toEpochDay()
            maxGap = maxOf(maxGap, gap.toInt())
        }
        
        maxGap
    }
}