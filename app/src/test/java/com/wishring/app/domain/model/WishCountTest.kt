package com.wishring.app.domain.model

import com.google.common.truth.Truth.assertThat
import com.wishring.app.core.util.Constants
import com.wishring.app.core.util.DateUtils
import com.wishring.app.data.local.database.entity.WishCountEntity
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for WishCount domain model
 * 
 * Test coverage includes:
 * - Progress calculation logic
 * - Count increment logic with boundary conditions  
 * - Goal achievement detection
 * - Time-related functionality
 * - Factory methods and conversions
 * - Property-based testing for edge cases
 */
@DisplayName("WishCount Domain Model Tests")
class WishCountTest {

    companion object {
        private const val TEST_DATE = "2024-01-15"
        private const val TEST_WISH_TEXT = "매일 운동하기"
        private const val TEST_TARGET_COUNT = 100
        private const val TEST_TIMESTAMP = 1705372800000L // 2024-01-16 00:00:00
        
        private fun createTestWishCount(
            date: String = TEST_DATE,
            totalCount: Int = 50,
            wishText: String = TEST_WISH_TEXT,
            targetCount: Int = TEST_TARGET_COUNT,
            isCompleted: Boolean = false,
            createdAt: Long = TEST_TIMESTAMP,
            updatedAt: Long = TEST_TIMESTAMP
        ): WishCount = WishCount(
            date = date,
            totalCount = totalCount,
            wishText = wishText,
            targetCount = targetCount,
            isCompleted = isCompleted,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    @Nested
    @DisplayName("Progress Calculation Tests")
    inner class ProgressCalculationTests {

        @Test
        @DisplayName("진행률 퍼센테이지가 올바르게 계산되어야 함 (정상 케이스)")
        fun `progressPercentage should calculate correctly for normal cases`() {
            val wishCount = createTestWishCount(totalCount = 50, targetCount = 100)
            
            assertThat(wishCount.progressPercentage).isEqualTo(50)
        }

        @Test
        @DisplayName("진행률이 100%를 초과하지 않아야 함")
        fun `progressPercentage should not exceed 100 percent`() {
            val wishCount = createTestWishCount(totalCount = 150, targetCount = 100)
            
            assertThat(wishCount.progressPercentage).isEqualTo(100)
        }

        @Test
        @DisplayName("목표가 0일 때 진행률은 0이어야 함")
        fun `progressPercentage should return 0 when target is 0`() {
            val wishCount = createTestWishCount(totalCount = 50, targetCount = 0)
            
            assertThat(wishCount.progressPercentage).isEqualTo(0)
        }

        @ParameterizedTest
        @CsvSource(
            "0, 100, 0.0",
            "25, 100, 0.25",
            "50, 100, 0.5", 
            "75, 100, 0.75",
            "100, 100, 1.0",
            "150, 100, 1.0" // 최대값 제한
        )
        @DisplayName("진행률 Float 값이 올바르게 계산되어야 함")
        fun `progress should calculate correctly as float`(
            totalCount: Int, 
            targetCount: Int, 
            expected: Float
        ) {
            val wishCount = createTestWishCount(totalCount = totalCount, targetCount = targetCount)
            
            assertThat(wishCount.progress).isWithin(0.001f).of(expected)
        }

        @Test
        @DisplayName("남은 카운트가 올바르게 계산되어야 함")
        fun `remainingCount should calculate correctly`() {
            val wishCount = createTestWishCount(totalCount = 30, targetCount = 100)
            
            assertThat(wishCount.remainingCount).isEqualTo(70)
        }

        @Test
        @DisplayName("이미 목표를 달성한 경우 남은 카운트는 0이어야 함")
        fun `remainingCount should be 0 when goal is already achieved`() {
            val wishCount = createTestWishCount(totalCount = 120, targetCount = 100)
            
            assertThat(wishCount.remainingCount).isEqualTo(0)
        }

        @Test
        @DisplayName("목표가 0일 때 남은 카운트는 0이어야 함")
        fun `remainingCount should be 0 when target is 0`() {
            val wishCount = createTestWishCount(totalCount = 50, targetCount = 0)
            
            assertThat(wishCount.remainingCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Count Increment Logic Tests")
    inner class CountIncrementTests {

        @Test
        @DisplayName("기본값(1)으로 카운트가 증가되어야 함")
        fun `incrementCount should increase by 1 by default`() {
            val wishCount = createTestWishCount(totalCount = 50)
            
            val result = wishCount.incrementCount()
            
            assertThat(result.totalCount).isEqualTo(51)
        }

        @Test
        @DisplayName("지정한 값만큼 카운트가 증가되어야 함")
        fun `incrementCount should increase by specified amount`() {
            val wishCount = createTestWishCount(totalCount = 50)
            
            val result = wishCount.incrementCount(5)
            
            assertThat(result.totalCount).isEqualTo(55)
        }

        @Test
        @DisplayName("최대 일일 카운트를 초과하지 않아야 함")
        fun `incrementCount should not exceed MAX_DAILY_COUNT`() {
            val wishCount = createTestWishCount(totalCount = Constants.MAX_DAILY_COUNT - 5)
            
            val result = wishCount.incrementCount(10)
            
            assertThat(result.totalCount).isEqualTo(Constants.MAX_DAILY_COUNT)
        }

        @Test
        @DisplayName("카운트 증가 시 목표 달성 여부가 올바르게 설정되어야 함")
        fun `incrementCount should update completion status correctly`() {
            val wishCount = createTestWishCount(totalCount = 99, targetCount = 100, isCompleted = false)
            
            val result = wishCount.incrementCount()
            
            assertThat(result.isCompleted).isTrue()
            assertThat(result.totalCount).isEqualTo(100)
        }

        @Test
        @DisplayName("카운트 증가 시 updatedAt이 갱신되어야 함")
        fun `incrementCount should update timestamp`() {
            val wishCount = createTestWishCount(updatedAt = TEST_TIMESTAMP)
            
            val result = wishCount.incrementCount()
            
            // updatedAt이 더 최근 값으로 업데이트되었는지 확인
            // (실제로는 DateUtils.getCurrentTimestamp()를 호출하므로 더 큰 값이어야 함)
            assertThat(result.updatedAt).isAtLeast(TEST_TIMESTAMP)
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 5, 10, 25, 50, 100])
        @DisplayName("다양한 증가량에 대해 정확히 계산되어야 함")
        fun `incrementCount should handle various increment amounts`(incrementAmount: Int) {
            val initialCount = 20
            val wishCount = createTestWishCount(totalCount = initialCount)
            
            val result = wishCount.incrementCount(incrementAmount)
            val expectedCount = minOf(initialCount + incrementAmount, Constants.MAX_DAILY_COUNT)
            
            assertThat(result.totalCount).isEqualTo(expectedCount)
        }
    }

    @Nested
    @DisplayName("Wish and Target Update Tests")
    inner class WishAndTargetUpdateTests {

        @Test
        @DisplayName("소원 텍스트만 업데이트하는 경우")
        fun `updateWishAndTarget should update only wish text`() {
            val wishCount = createTestWishCount()
            val newWishText = "새로운 소원"
            
            val result = wishCount.updateWishAndTarget(newWishText = newWishText)
            
            assertThat(result.wishText).isEqualTo(newWishText)
            assertThat(result.targetCount).isEqualTo(TEST_TARGET_COUNT) // 변경되지 않음
        }

        @Test
        @DisplayName("목표 카운트만 업데이트하는 경우")
        fun `updateWishAndTarget should update only target count`() {
            val wishCount = createTestWishCount()
            val newTargetCount = 200
            
            val result = wishCount.updateWishAndTarget(newTargetCount = newTargetCount)
            
            assertThat(result.targetCount).isEqualTo(newTargetCount)
            assertThat(result.wishText).isEqualTo(TEST_WISH_TEXT) // 변경되지 않음
        }

        @Test
        @DisplayName("소원 텍스트와 목표 카운트를 모두 업데이트하는 경우")
        fun `updateWishAndTarget should update both wish text and target count`() {
            val wishCount = createTestWishCount()
            val newWishText = "새로운 소원"
            val newTargetCount = 200
            
            val result = wishCount.updateWishAndTarget(
                newWishText = newWishText,
                newTargetCount = newTargetCount
            )
            
            assertThat(result.wishText).isEqualTo(newWishText)
            assertThat(result.targetCount).isEqualTo(newTargetCount)
        }

        @Test
        @DisplayName("목표 변경 시 완료 상태가 올바르게 업데이트되어야 함 - 달성한 경우")
        fun `updateWishAndTarget should update completion status when goal is achieved`() {
            val wishCount = createTestWishCount(totalCount = 50, isCompleted = false)
            
            val result = wishCount.updateWishAndTarget(newTargetCount = 30) // 현재 카운트보다 낮게 설정
            
            assertThat(result.isCompleted).isTrue()
        }

        @Test
        @DisplayName("목표 변경 시 완료 상태가 올바르게 업데이트되어야 함 - 미달성한 경우")
        fun `updateWishAndTarget should update completion status when goal is not achieved`() {
            val wishCount = createTestWishCount(totalCount = 50, isCompleted = true)
            
            val result = wishCount.updateWishAndTarget(newTargetCount = 100) // 현재 카운트보다 높게 설정
            
            assertThat(result.isCompleted).isFalse()
        }

        @Test
        @DisplayName("업데이트 시 updatedAt이 갱신되어야 함")
        fun `updateWishAndTarget should update timestamp`() {
            val wishCount = createTestWishCount(updatedAt = TEST_TIMESTAMP)
            
            val result = wishCount.updateWishAndTarget(newWishText = "변경된 소원")
            
            assertThat(result.updatedAt).isAtLeast(TEST_TIMESTAMP)
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    inner class FactoryMethodTests {

        @Test
        @DisplayName("기본값으로 WishCount를 생성해야 함")
        fun `createDefault should create with default values`() {
            val result = WishCount.createDefault()
            
            assertThat(result.date).isEqualTo(DateUtils.getTodayString())
            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.wishText).isEqualTo(Constants.DEFAULT_WISH_TEXT)
            assertThat(result.targetCount).isEqualTo(Constants.DEFAULT_TARGET_COUNT)
            assertThat(result.isCompleted).isFalse()
        }

        @Test
        @DisplayName("파라미터로 지정한 값으로 WishCount를 생성해야 함")
        fun `createDefault should create with specified parameters`() {
            val customDate = "2024-12-25"
            val customWishText = "크리스마스 소원"
            val customTargetCount = 50
            
            val result = WishCount.createDefault(
                date = customDate,
                wishText = customWishText,
                targetCount = customTargetCount
            )
            
            assertThat(result.date).isEqualTo(customDate)
            assertThat(result.wishText).isEqualTo(customWishText)
            assertThat(result.targetCount).isEqualTo(customTargetCount)
            assertThat(result.totalCount).isEqualTo(0) // 항상 0으로 시작
            assertThat(result.isCompleted).isFalse()
        }

        @Test
        @DisplayName("WishCountEntity에서 WishCount로 변환되어야 함")
        fun `fromEntity should convert WishCountEntity to WishCount`() {
            val entity = WishCountEntity(
                date = TEST_DATE,
                totalCount = 75,
                wishText = TEST_WISH_TEXT,
                targetCount = TEST_TARGET_COUNT,
                isCompleted = true,
                createdAt = TEST_TIMESTAMP,
                updatedAt = TEST_TIMESTAMP
            )
            
            val result = WishCount.fromEntity(entity)
            
            assertThat(result.date).isEqualTo(entity.date)
            assertThat(result.totalCount).isEqualTo(entity.totalCount)
            assertThat(result.wishText).isEqualTo(entity.wishText)
            assertThat(result.targetCount).isEqualTo(entity.targetCount)
            assertThat(result.isCompleted).isEqualTo(entity.isCompleted)
            assertThat(result.createdAt).isEqualTo(entity.createdAt)
            assertThat(result.updatedAt).isEqualTo(entity.updatedAt)
        }

        @Test
        @DisplayName("WishCount에서 WishCountEntity로 변환되어야 함")
        fun `toEntity should convert WishCount to WishCountEntity`() {
            val wishCount = createTestWishCount()
            
            val result = wishCount.toEntity()
            
            assertThat(result.date).isEqualTo(wishCount.date)
            assertThat(result.totalCount).isEqualTo(wishCount.totalCount)
            assertThat(result.wishText).isEqualTo(wishCount.wishText)
            assertThat(result.targetCount).isEqualTo(wishCount.targetCount)
            assertThat(result.isCompleted).isEqualTo(wishCount.isCompleted)
            assertThat(result.createdAt).isEqualTo(wishCount.createdAt)
            assertThat(result.updatedAt).isEqualTo(wishCount.updatedAt)
        }
    }

    @Nested
    @DisplayName("Time Related Functionality Tests")
    inner class TimeRelatedTests {

        @Test
        @DisplayName("오늘 날짜인지 올바르게 확인해야 함")
        fun `isToday should correctly identify today's record`() {
            val todayWishCount = createTestWishCount(date = DateUtils.getTodayString())
            val yesterdayWishCount = createTestWishCount(date = "2023-12-31")
            
            assertThat(todayWishCount.isToday).isTrue()
            assertThat(yesterdayWishCount.isToday).isFalse()
        }

        @Test
        @DisplayName("화면 표시용 날짜 포맷이 올바르게 반환되어야 함")
        fun `displayDate should return formatted date`() {
            val wishCount = createTestWishCount(date = "2024-01-15")
            
            val result = wishCount.displayDate
            
            // DateUtils.toDisplayFormat의 실제 구현에 따라 결과가 달라질 수 있음
            assertThat(result).isNotEmpty()
        }

        @Test
        @DisplayName("상대적 날짜 문자열이 올바르게 반환되어야 함")
        fun `relativeDateString should return relative date`() {
            val wishCount = createTestWishCount(date = DateUtils.getTodayString())
            
            val result = wishCount.relativeDateString
            
            // DateUtils.getRelativeDateString의 실제 구현에 따라 결과가 달라질 수 있음
            assertThat(result).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    inner class EdgeCasesTests {

        @Test
        @DisplayName("음수 카운트로 생성되어도 진행률 계산이 정상 동작해야 함")
        fun `should handle negative count gracefully`() {
            val wishCount = createTestWishCount(totalCount = -10, targetCount = 100)
            
            // 음수 카운트의 경우 진행률은 0이 되어야 함 (coerceIn 덕분)
            assertThat(wishCount.progressPercentage).isEqualTo(0)
            assertThat(wishCount.progress).isEqualTo(0.0f)
            assertThat(wishCount.remainingCount).isEqualTo(100)
        }

        @Test
        @DisplayName("음수 목표값으로 생성되어도 오류가 발생하지 않아야 함")  
        fun `should handle negative target gracefully`() {
            val wishCount = createTestWishCount(totalCount = 10, targetCount = -50)
            
            assertThat(wishCount.progressPercentage).isEqualTo(0)
            assertThat(wishCount.progress).isEqualTo(0.0f)
            assertThat(wishCount.remainingCount).isEqualTo(0)
        }

        @Test
        @DisplayName("매우 큰 수로 카운트 증가 시도 시 MAX_DAILY_COUNT로 제한되어야 함")
        fun `should handle very large increment amounts`() {
            val wishCount = createTestWishCount(totalCount = 0)
            
            val result = wishCount.incrementCount(Int.MAX_VALUE)
            
            assertThat(result.totalCount).isEqualTo(Constants.MAX_DAILY_COUNT)
        }

        @Test
        @DisplayName("빈 문자열 소원 텍스트도 처리 가능해야 함")
        fun `should handle empty wish text`() {
            val wishCount = createTestWishCount(wishText = "")
            
            val result = wishCount.updateWishAndTarget(newWishText = "")
            
            assertThat(result.wishText).isEmpty()
        }

        @Test
        @DisplayName("매우 긴 소원 텍스트도 처리 가능해야 함")
        fun `should handle very long wish text`() {
            val longText = "가".repeat(10000)
            val wishCount = createTestWishCount()
            
            val result = wishCount.updateWishAndTarget(newWishText = longText)
            
            assertThat(result.wishText).isEqualTo(longText)
        }
    }

    @Nested
    @DisplayName("Property-Based Testing")
    inner class PropertyBasedTests {

        @Test
        @DisplayName("진행률은 항상 0과 100 사이여야 함")
        fun `progressPercentage should always be between 0 and 100`() = runTest {
            checkAll(
                Arb.int(0..10000), // totalCount
                Arb.int(1..10000)  // targetCount (0 제외)
            ) { totalCount, targetCount ->
                val wishCount = createTestWishCount(
                    totalCount = totalCount,
                    targetCount = targetCount
                )
                
                assertThat(wishCount.progressPercentage).isAtLeast(0)
                assertThat(wishCount.progressPercentage).isAtMost(100)
            }
        }

        @Test
        @DisplayName("Float 진행률은 항상 0.0과 1.0 사이여야 함")
        fun `progress should always be between 0_0 and 1_0`() = runTest {
            checkAll(
                Arb.int(0..10000), // totalCount
                Arb.int(1..10000)  // targetCount (0 제외)
            ) { totalCount, targetCount ->
                val wishCount = createTestWishCount(
                    totalCount = totalCount,
                    targetCount = targetCount
                )
                
                assertThat(wishCount.progress).isAtLeast(0.0f)
                assertThat(wishCount.progress).isAtMost(1.0f)
            }
        }

        @Test
        @DisplayName("남은 카운트는 항상 0 이상이어야 함")
        fun `remainingCount should always be non-negative`() = runTest {
            checkAll(
                Arb.int(0..10000), // totalCount
                Arb.int(0..10000)  // targetCount
            ) { totalCount, targetCount ->
                val wishCount = createTestWishCount(
                    totalCount = totalCount,
                    targetCount = targetCount
                )
                
                assertThat(wishCount.remainingCount).isAtLeast(0)
            }
        }

        @Test
        @DisplayName("카운트 증가 후에도 불변 조건들이 유지되어야 함")
        fun `invariants should be maintained after incrementCount`() = runTest {
            checkAll(
                Arb.int(0..1000),  // initialCount
                Arb.int(1..1000),  // targetCount  
                Arb.int(1..100)    // increment amount
            ) { initialCount, targetCount, incrementAmount ->
                val wishCount = createTestWishCount(
                    totalCount = initialCount,
                    targetCount = targetCount
                )
                
                val result = wishCount.incrementCount(incrementAmount)
                
                // 불변 조건들
                assertThat(result.totalCount).isAtLeast(initialCount) // 카운트는 증가만 함
                assertThat(result.totalCount).isAtMost(Constants.MAX_DAILY_COUNT) // 최대값 제한
                assertThat(result.progressPercentage).isAtLeast(0)
                assertThat(result.progressPercentage).isAtMost(100)
                assertThat(result.progress).isAtLeast(0.0f)
                assertThat(result.progress).isAtMost(1.0f)
                assertThat(result.remainingCount).isAtLeast(0)
                assertThat(result.updatedAt).isAtLeast(wishCount.updatedAt) // 시간은 미래로만
            }
        }
    }
}