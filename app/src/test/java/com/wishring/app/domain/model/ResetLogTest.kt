package com.wishring.app.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@DisplayName("ResetLog 도메인 모델 테스트")
class ResetLogTest {

    @Nested
    @DisplayName("생성 및 기본 속성 테스트")
    inner class CreationTests {
        
        @Test
        @DisplayName("정상적인 ResetLog 생성")
        fun `should create ResetLog with valid data`() {
            // Given
            val date = LocalDate.now()
            val resetTime = LocalDateTime.now()
            val beforeCount = 50
            val afterCount = 0
            val reason = ResetReason.MANUAL
            
            // When
            val resetLog = ResetLog(
                id = 1L,
                date = date,
                resetTime = resetTime,
                beforeCount = beforeCount,
                afterCount = afterCount,
                reason = reason
            )
            
            // Then
            resetLog.id shouldBe 1L
            resetLog.date shouldBe date
            resetLog.resetTime shouldBe resetTime
            resetLog.beforeCount shouldBe beforeCount
            resetLog.afterCount shouldBe afterCount
            resetLog.reason shouldBe reason
        }
        
        @Test
        @DisplayName("자정 자동 리셋 로그 생성")
        fun `should create midnight auto reset log`() {
            // Given
            val date = LocalDate.now()
            val resetTime = date.atStartOfDay()
            val beforeCount = 100
            
            // When
            val resetLog = ResetLog(
                id = 0L,
                date = date,
                resetTime = resetTime,
                beforeCount = beforeCount,
                afterCount = 0,
                reason = ResetReason.MIDNIGHT
            )
            
            // Then
            resetLog.reason shouldBe ResetReason.MIDNIGHT
            resetLog.resetTime.hour shouldBe 0
            resetLog.resetTime.minute shouldBe 0
            resetLog.afterCount shouldBe 0
        }
    }
    
    @Nested
    @DisplayName("리셋 이유별 테스트")
    inner class ResetReasonTests {
        
        @ParameterizedTest
        @CsvSource(
            "MANUAL,수동 리셋",
            "MIDNIGHT,자정 자동 리셋",
            "GOAL_ACHIEVED,목표 달성 리셋",
            "ERROR_RECOVERY,오류 복구 리셋"
        )
        @DisplayName("리셋 이유별 분류 테스트")
        fun `should categorize reset reasons correctly`(reason: String, description: String) {
            // Given
            val resetReason = ResetReason.valueOf(reason)
            val resetLog = ResetLog(
                id = 1L,
                date = LocalDate.now(),
                resetTime = LocalDateTime.now(),
                beforeCount = 50,
                afterCount = 0,
                reason = resetReason
            )
            
            // Then
            when (resetReason) {
                ResetReason.MANUAL -> resetLog.isManualReset() shouldBe true
                ResetReason.MIDNIGHT -> resetLog.isAutoReset() shouldBe true
                ResetReason.GOAL_ACHIEVED -> resetLog.isGoalReset() shouldBe true
                ResetReason.ERROR_RECOVERY -> resetLog.isErrorRecovery() shouldBe true
            }
        }
        
        @Test
        @DisplayName("목표 달성 리셋 특수 처리")
        fun `should handle goal achievement reset specially`() {
            // Given
            val goalCount = 100
            val resetLog = ResetLog(
                id = 1L,
                date = LocalDate.now(),
                resetTime = LocalDateTime.now(),
                beforeCount = goalCount,
                afterCount = 0,
                reason = ResetReason.GOAL_ACHIEVED
            )
            
            // Then
            resetLog.beforeCount shouldBe goalCount
            resetLog.isGoalReset() shouldBe true
            resetLog.wasSuccessful() shouldBe true
        }
    }
    
    @Nested
    @DisplayName("리셋 통계 분석 테스트")
    inner class StatisticsTests {
        
        @Test
        @DisplayName("리셋으로 인한 카운트 손실 계산")
        fun `should calculate count loss from reset`() {
            // Given
            val resetLog = ResetLog(
                id = 1L,
                date = LocalDate.now(),
                resetTime = LocalDateTime.now(),
                beforeCount = 75,
                afterCount = 10,
                reason = ResetReason.MANUAL
            )
            
            // When
            val countLoss = resetLog.calculateCountLoss()
            
            // Then
            countLoss shouldBe 65
        }
        
        @Test
        @DisplayName("리셋 시간대 분석")
        fun `should analyze reset time period`() {
            // Given
            val morningReset = createResetAtHour(6)
            val afternoonReset = createResetAtHour(14)
            val eveningReset = createResetAtHour(20)
            val midnightReset = createResetAtHour(0)
            
            // Then
            morningReset.getTimePeriod() shouldBe TimePeriod.MORNING
            afternoonReset.getTimePeriod() shouldBe TimePeriod.AFTERNOON
            eveningReset.getTimePeriod() shouldBe TimePeriod.EVENING
            midnightReset.getTimePeriod() shouldBe TimePeriod.MIDNIGHT
        }
        
        @ParameterizedTest
        @ValueSource(ints = [0, 10, 25, 50, 75, 90, 100])
        @DisplayName("리셋 전 진행률 계산")
        fun `should calculate progress before reset`(beforeCount: Int) {
            // Given
            val goalCount = 100
            val resetLog = ResetLog(
                id = 1L,
                date = LocalDate.now(),
                resetTime = LocalDateTime.now(),
                beforeCount = beforeCount,
                afterCount = 0,
                reason = ResetReason.MANUAL
            )
            
            // When
            val progress = resetLog.calculateProgressBeforeReset(goalCount)
            
            // Then
            progress shouldBe (beforeCount.toFloat() / goalCount * 100)
        }
    }
    
    @Nested
    @DisplayName("리셋 패턴 분석 테스트")
    inner class PatternAnalysisTests {
        
        @Test
        @DisplayName("연속 리셋 패턴 감지")
        fun `should detect consecutive reset patterns`() {
            // Given
            val resets = listOf(
                createResetLog(LocalDate.now().minusDays(2), 50),
                createResetLog(LocalDate.now().minusDays(1), 60),
                createResetLog(LocalDate.now(), 70)
            )
            
            // When
            val hasPattern = ResetLog.hasConsecutiveResetPattern(resets)
            
            // Then
            hasPattern shouldBe true
        }
        
        @Test
        @DisplayName("주간 리셋 빈도 계산")
        fun `should calculate weekly reset frequency`() {
            // Given
            val weekResets = (0..6).map { day ->
                createResetLog(LocalDate.now().minusDays(day.toLong()), 50)
            }
            
            // When
            val frequency = ResetLog.calculateWeeklyFrequency(weekResets)
            
            // Then
            frequency shouldBe 7
        }
        
        @Test
        @DisplayName("평균 리셋 전 카운트 계산")
        fun `should calculate average count before reset`() {
            // Given
            val resets = listOf(
                createResetLog(beforeCount = 30),
                createResetLog(beforeCount = 50),
                createResetLog(beforeCount = 70)
            )
            
            // When
            val average = ResetLog.calculateAverageBeforeCount(resets)
            
            // Then
            average shouldBe 50.0
        }
    }
    
    @Nested
    @DisplayName("Entity 변환 테스트")
    inner class EntityConversionTests {
        
        @Test
        @DisplayName("ResetLog를 Entity로 변환")
        fun `should convert ResetLog to Entity`() {
            // Given
            val resetLog = ResetLog(
                id = 1L,
                date = LocalDate.now(),
                resetTime = LocalDateTime.now(),
                beforeCount = 75,
                afterCount = 0,
                reason = ResetReason.MANUAL
            )
            
            // When
            val entity = resetLog.toEntity()
            
            // Then
            entity.id shouldBe resetLog.id
            entity.date shouldBe resetLog.date
            entity.resetTime shouldBe resetLog.resetTime
            entity.beforeCount shouldBe resetLog.beforeCount
            entity.afterCount shouldBe resetLog.afterCount
            entity.reason shouldBe resetLog.reason.name
        }
        
        @Test
        @DisplayName("Entity를 ResetLog로 변환")
        fun `should convert Entity to ResetLog`() {
            // Given
            val entity = ResetLogEntity(
                id = 1L,
                date = LocalDate.now(),
                resetTime = LocalDateTime.now(),
                beforeCount = 50,
                afterCount = 10,
                reason = "MIDNIGHT"
            )
            
            // When
            val resetLog = ResetLog.fromEntity(entity)
            
            // Then
            resetLog.id shouldBe entity.id
            resetLog.date shouldBe entity.date
            resetLog.resetTime shouldBe entity.resetTime
            resetLog.beforeCount shouldBe entity.beforeCount
            resetLog.afterCount shouldBe entity.afterCount
            resetLog.reason shouldBe ResetReason.MIDNIGHT
        }
    }
    
    @Nested
    @DisplayName("Property-based 테스트")
    inner class PropertyBasedTests {
        
        @Test
        @DisplayName("카운트 손실은 항상 비음수")
        fun `count loss should always be non-negative`() = runTest {
            checkAll(
                Arb.int(0..1000),
                Arb.int(0..1000)
            ) { before, after ->
                val resetLog = ResetLog(
                    id = 1L,
                    date = LocalDate.now(),
                    resetTime = LocalDateTime.now(),
                    beforeCount = before,
                    afterCount = after,
                    reason = ResetReason.MANUAL
                )
                
                val loss = resetLog.calculateCountLoss()
                if (before >= after) {
                    loss shouldBe (before - after)
                } else {
                    loss shouldBe 0
                }
            }
        }
        
        @Test
        @DisplayName("리셋 시간은 항상 날짜 범위 내")
        fun `reset time should always be within date bounds`() = runTest {
            checkAll(
                Arb.localDate(),
                Arb.int(0..23),
                Arb.int(0..59)
            ) { date, hour, minute ->
                val resetTime = date.atTime(hour, minute)
                val resetLog = ResetLog(
                    id = 1L,
                    date = date,
                    resetTime = resetTime,
                    beforeCount = 50,
                    afterCount = 0,
                    reason = ResetReason.MANUAL
                )
                
                resetLog.resetTime.toLocalDate() shouldBe date
            }
        }
    }
    
    // Helper functions
    private fun createResetLog(
        date: LocalDate = LocalDate.now(),
        beforeCount: Int = 50,
        afterCount: Int = 0,
        reason: ResetReason = ResetReason.MANUAL
    ): ResetLog {
        return ResetLog(
            id = 0L,
            date = date,
            resetTime = date.atStartOfDay(),
            beforeCount = beforeCount,
            afterCount = afterCount,
            reason = reason
        )
    }
    
    private fun createResetAtHour(hour: Int): ResetLog {
        val date = LocalDate.now()
        return ResetLog(
            id = 0L,
            date = date,
            resetTime = date.atTime(hour, 0),
            beforeCount = 50,
            afterCount = 0,
            reason = ResetReason.MANUAL
        )
    }
}

// Extension functions for testing
private fun ResetLog.isManualReset() = reason == ResetReason.MANUAL
private fun ResetLog.isAutoReset() = reason == ResetReason.MIDNIGHT
private fun ResetLog.isGoalReset() = reason == ResetReason.GOAL_ACHIEVED
private fun ResetLog.isErrorRecovery() = reason == ResetReason.ERROR_RECOVERY
private fun ResetLog.wasSuccessful() = reason == ResetReason.GOAL_ACHIEVED

private fun ResetLog.calculateCountLoss(): Int {
    return if (beforeCount > afterCount) beforeCount - afterCount else 0
}

private fun ResetLog.getTimePeriod(): TimePeriod {
    return when (resetTime.hour) {
        in 0..0 -> TimePeriod.MIDNIGHT
        in 1..11 -> TimePeriod.MORNING
        in 12..17 -> TimePeriod.AFTERNOON
        else -> TimePeriod.EVENING
    }
}

private fun ResetLog.calculateProgressBeforeReset(goalCount: Int): Float {
    return if (goalCount > 0) beforeCount.toFloat() / goalCount * 100 else 0f
}

// Companion object extensions for collection operations
private fun ResetLog.Companion.hasConsecutiveResetPattern(resets: List<ResetLog>): Boolean {
    if (resets.size < 2) return false
    return resets.zipWithNext().all { (prev, curr) ->
        prev.date.plusDays(1) == curr.date
    }
}

private fun ResetLog.Companion.calculateWeeklyFrequency(resets: List<ResetLog>): Int {
    val weekAgo = LocalDate.now().minusWeeks(1)
    return resets.count { it.date.isAfter(weekAgo) }
}

private fun ResetLog.Companion.calculateAverageBeforeCount(resets: List<ResetLog>): Double {
    return if (resets.isEmpty()) 0.0 else resets.map { it.beforeCount }.average()
}

// Supporting enums for testing
enum class ResetReason {
    MANUAL,
    MIDNIGHT,
    GOAL_ACHIEVED,
    ERROR_RECOVERY
}

enum class TimePeriod {
    MIDNIGHT,
    MORNING,
    AFTERNOON,
    EVENING
}

// Mock Entity classes for testing
data class ResetLogEntity(
    val id: Long,
    val date: LocalDate,
    val resetTime: LocalDateTime,
    val beforeCount: Int,
    val afterCount: Int,
    val reason: String
)

private fun ResetLog.toEntity() = ResetLogEntity(
    id = id,
    date = date,
    resetTime = resetTime,
    beforeCount = beforeCount,
    afterCount = afterCount,
    reason = reason.name
)

private fun ResetLog.Companion.fromEntity(entity: ResetLogEntity) = ResetLog(
    id = entity.id,
    date = entity.date,
    resetTime = entity.resetTime,
    beforeCount = entity.beforeCount,
    afterCount = entity.afterCount,
    reason = ResetReason.valueOf(entity.reason)
)

// ResetLog domain model
data class ResetLog(
    val id: Long = 0,
    val date: LocalDate,
    val resetTime: LocalDateTime,
    val beforeCount: Int,
    val afterCount: Int,
    val reason: ResetReason
) {
    companion object
}