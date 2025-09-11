package com.wishring.app.domain.model

import com.google.common.truth.Truth.assertThat
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for DailyRecord domain model
 * 
 * Test coverage includes:
 * - Progress and completion rate calculations
 * - Date formatting and relative date strings
 * - Reset count and lost count handling
 * - Status text generation based on progress
 * - Factory methods and edge cases
 * - Property-based testing for invariants
 */
@DisplayName("DailyRecord Domain Model Tests")
class DailyRecordTest {

    companion object {
        private val TEST_DATE = LocalDate.of(2024, 1, 15)
        private const val TEST_WISH_TEXT = "매일 운동하기"
        private const val TEST_TARGET_COUNT = 100
        
        private fun createTestDailyRecord(
            date: LocalDate = TEST_DATE,
            totalCount: Int = 50,
            wishText: String = TEST_WISH_TEXT,
            targetCount: Int = TEST_TARGET_COUNT,
            isCompleted: Boolean = false,
            resetCount: Int = 0,
            lostCount: Int = 0
        ): DailyRecord = DailyRecord(
            date = date,
            totalCount = totalCount,
            wishText = wishText,
            targetCount = targetCount,
            isCompleted = isCompleted,
            resetCount = resetCount,
            lostCount = lostCount
        )
    }

    @Nested
    @DisplayName("Date Formatting Tests")
    inner class DateFormattingTests {

        @Test
        @DisplayName("displayDate가 올바른 형식으로 날짜를 반환해야 함")
        fun `displayDate should return date in correct display format`() {
            val record = createTestDailyRecord(date = LocalDate.of(2024, 1, 15))
            
            assertThat(record.displayDate).isEqualTo("2024.01.15")
        }

        @Test
        @DisplayName("dateString이 ISO 형식으로 날짜를 반환해야 함")
        fun `dateString should return date in ISO format`() {
            val record = createTestDailyRecord(date = LocalDate.of(2024, 1, 15))
            
            assertThat(record.dateString).isEqualTo("2024-01-15")
        }

        @Test
        @DisplayName("isToday가 오늘 날짜를 올바르게 판별해야 함")
        fun `isToday should correctly identify today's record`() {
            val todayRecord = createTestDailyRecord(date = LocalDate.now())
            val yesterdayRecord = createTestDailyRecord(date = LocalDate.now().minusDays(1))
            val tomorrowRecord = createTestDailyRecord(date = LocalDate.now().plusDays(1))
            
            assertTrue(todayRecord.isToday)
            assertFalse(yesterdayRecord.isToday)
            assertFalse(tomorrowRecord.isToday)
        }

        @Test
        @DisplayName("isYesterday가 어제 날짜를 올바르게 판별해야 함")
        fun `isYesterday should correctly identify yesterday's record`() {
            val todayRecord = createTestDailyRecord(date = LocalDate.now())
            val yesterdayRecord = createTestDailyRecord(date = LocalDate.now().minusDays(1))
            val twoDaysAgoRecord = createTestDailyRecord(date = LocalDate.now().minusDays(2))
            
            assertFalse(todayRecord.isYesterday)
            assertTrue(yesterdayRecord.isYesterday)
            assertFalse(twoDaysAgoRecord.isYesterday)
        }

        @Test
        @DisplayName("relativeDateString이 상대적 날짜를 올바르게 표시해야 함")
        fun `relativeDateString should return correct relative date`() {
            val todayRecord = createTestDailyRecord(date = LocalDate.now())
            val yesterdayRecord = createTestDailyRecord(date = LocalDate.now().minusDays(1))
            val threeDaysAgoRecord = createTestDailyRecord(date = LocalDate.now().minusDays(3))
            
            assertThat(todayRecord.relativeDateString).isEqualTo("오늘")
            assertThat(yesterdayRecord.relativeDateString).isEqualTo("어제")
            assertThat(threeDaysAgoRecord.relativeDateString).isEqualTo("3일 전")
        }

        @ParameterizedTest
        @ValueSource(longs = [2, 5, 10, 30, 100, 365])
        @DisplayName("relativeDateString이 N일 전을 올바르게 표시해야 함")
        fun `relativeDateString should show N days ago correctly`(daysAgo: Long) {
            val record = createTestDailyRecord(date = LocalDate.now().minusDays(daysAgo))
            
            assertThat(record.relativeDateString).isEqualTo("${daysAgo}일 전")
        }
    }

    @Nested
    @DisplayName("Progress Calculation Tests")
    inner class ProgressCalculationTests {

        @ParameterizedTest
        @CsvSource(
            "0, 100, 0",
            "25, 100, 25",
            "50, 100, 50",
            "75, 100, 75",
            "100, 100, 100",
            "150, 100, 100", // 최대 100%
            "50, 0, 0" // 목표가 0인 경우
        )
        @DisplayName("completionRate가 올바르게 계산되어야 함")
        fun `completionRate should calculate correctly`(
            totalCount: Int,
            targetCount: Int,
            expectedRate: Int
        ) {
            val record = createTestDailyRecord(
                totalCount = totalCount,
                targetCount = targetCount
            )
            
            assertThat(record.completionRate).isEqualTo(expectedRate)
        }

        @ParameterizedTest
        @CsvSource(
            "0, 100, 0.0",
            "25, 100, 0.25",
            "50, 100, 0.5",
            "75, 100, 0.75",
            "100, 100, 1.0",
            "150, 100, 1.0", // 최대 1.0
            "50, 0, 0.0" // 목표가 0인 경우
        )
        @DisplayName("progress가 Float로 올바르게 계산되어야 함")
        fun `progress should calculate correctly as float`(
            totalCount: Int,
            targetCount: Int,
            expectedProgress: Float
        ) {
            val record = createTestDailyRecord(
                totalCount = totalCount,
                targetCount = targetCount
            )
            
            assertThat(record.progress).isWithin(0.001f).of(expectedProgress)
        }

        @Test
        @DisplayName("remainingCount가 올바르게 계산되어야 함")
        fun `remainingCount should calculate correctly`() {
            val record = createTestDailyRecord(totalCount = 30, targetCount = 100)
            
            assertThat(record.remainingCount).isEqualTo(70)
        }

        @Test
        @DisplayName("목표를 초과한 경우 remainingCount는 0이어야 함")
        fun `remainingCount should be 0 when goal exceeded`() {
            val record = createTestDailyRecord(totalCount = 120, targetCount = 100)
            
            assertThat(record.remainingCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Reset and Lost Count Tests")
    inner class ResetAndLostCountTests {

        @Test
        @DisplayName("actualCount가 totalCount와 lostCount의 합이어야 함")
        fun `actualCount should be sum of totalCount and lostCount`() {
            val record = createTestDailyRecord(
                totalCount = 50,
                lostCount = 20
            )
            
            assertThat(record.actualCount).isEqualTo(70)
        }

        @Test
        @DisplayName("hasResets가 리셋 여부를 올바르게 표시해야 함")
        fun `hasResets should correctly identify reset presence`() {
            val withReset = createTestDailyRecord(resetCount = 2)
            val withoutReset = createTestDailyRecord(resetCount = 0)
            
            assertTrue(withReset.hasResets)
            assertFalse(withoutReset.hasResets)
        }

        @Test
        @DisplayName("여러 번 리셋된 경우도 올바르게 처리해야 함")
        fun `multiple resets should be handled correctly`() {
            val record = createTestDailyRecord(
                totalCount = 30,
                resetCount = 3,
                lostCount = 150 // 3번 리셋으로 잃은 총 카운트
            )
            
            assertThat(record.hasResets).isTrue()
            assertThat(record.resetCount).isEqualTo(3)
            assertThat(record.actualCount).isEqualTo(180) // 30 + 150
        }

        @Test
        @DisplayName("리셋 없이도 lostCount가 있을 수 있음 (데이터 오류 케이스)")
        fun `lostCount without resets should be handled`() {
            val record = createTestDailyRecord(
                totalCount = 50,
                resetCount = 0,
                lostCount = 10 // 데이터 불일치 케이스
            )
            
            assertThat(record.hasResets).isFalse()
            assertThat(record.actualCount).isEqualTo(60)
        }
    }

    @Nested
    @DisplayName("Status Text Generation Tests")
    inner class StatusTextTests {

        @Test
        @DisplayName("목표 달성 시 올바른 상태 텍스트를 표시해야 함")
        fun `statusText should show achievement message when completed`() {
            val record = createTestDailyRecord(
                totalCount = 100,
                targetCount = 100,
                isCompleted = true
            )
            
            assertThat(record.statusText).isEqualTo("목표 달성 ✨")
        }

        @Test
        @DisplayName("80% 이상 달성 시 거의 다 왔다는 메시지를 표시해야 함")
        fun `statusText should show almost there message above 80 percent`() {
            val record = createTestDailyRecord(
                totalCount = 85,
                targetCount = 100
            )
            
            assertThat(record.statusText).isEqualTo("거의 다 왔어요!")
        }

        @Test
        @DisplayName("50% 이상 달성 시 절반 이상 메시지를 표시해야 함")
        fun `statusText should show halfway message above 50 percent`() {
            val record = createTestDailyRecord(
                totalCount = 60,
                targetCount = 100
            )
            
            assertThat(record.statusText).isEqualTo("절반 이상 달성!")
        }

        @Test
        @DisplayName("진행 중일 때 시작이 반이라는 메시지를 표시해야 함")
        fun `statusText should show starting message when in progress`() {
            val record = createTestDailyRecord(
                totalCount = 10,
                targetCount = 100
            )
            
            assertThat(record.statusText).isEqualTo("시작이 반이에요")
        }

        @Test
        @DisplayName("카운트가 0일 때 화이팅 메시지를 표시해야 함")
        fun `statusText should show encouragement when count is 0`() {
            val record = createTestDailyRecord(
                totalCount = 0,
                targetCount = 100
            )
            
            assertThat(record.statusText).isEqualTo("오늘도 화이팅!")
        }

        @ParameterizedTest
        @CsvSource(
            "100, 100, 목표 달성 ✨",
            "90, 100, 거의 다 왔어요!",
            "81, 100, 거의 다 왔어요!",
            "80, 100, 절반 이상 달성!",
            "51, 100, 절반 이상 달성!",
            "50, 100, 시작이 반이에요",
            "1, 100, 시작이 반이에요",
            "0, 100, 오늘도 화이팅!"
        )
        @DisplayName("진행률에 따른 상태 텍스트 경계값 테스트")
        fun `statusText boundary values test`(
            totalCount: Int,
            targetCount: Int,
            expectedText: String
        ) {
            val record = createTestDailyRecord(
                totalCount = totalCount,
                targetCount = targetCount,
                isCompleted = totalCount >= targetCount
            )
            
            assertThat(record.statusText).isEqualTo(expectedText)
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    inner class FactoryMethodTests {

        @Test
        @DisplayName("fromWishCount가 WishCount를 DailyRecord로 변환해야 함")
        fun `fromWishCount should convert WishCount to DailyRecord`() {
            val wishCount = WishCount(
                date = "2024-01-15",
                totalCount = 75,
                wishText = TEST_WISH_TEXT,
                targetCount = TEST_TARGET_COUNT,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            val record = DailyRecord.fromWishCount(
                wishCount = wishCount,
                resetCount = 2,
                lostCount = 30
            )
            
            assertThat(record.date).isEqualTo(LocalDate.of(2024, 1, 15))
            assertThat(record.totalCount).isEqualTo(75)
            assertThat(record.wishText).isEqualTo(TEST_WISH_TEXT)
            assertThat(record.targetCount).isEqualTo(TEST_TARGET_COUNT)
            assertThat(record.isCompleted).isFalse()
            assertThat(record.resetCount).isEqualTo(2)
            assertThat(record.lostCount).isEqualTo(30)
        }

        @Test
        @DisplayName("fromWishCount가 기본값으로 resetCount와 lostCount를 0으로 설정해야 함")
        fun `fromWishCount should default resetCount and lostCount to 0`() {
            val wishCount = WishCount(
                date = "2024-01-15",
                totalCount = 50,
                wishText = TEST_WISH_TEXT,
                targetCount = TEST_TARGET_COUNT,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            val record = DailyRecord.fromWishCount(wishCount)
            
            assertThat(record.resetCount).isEqualTo(0)
            assertThat(record.lostCount).isEqualTo(0)
        }

        @Test
        @DisplayName("empty가 빈 DailyRecord를 생성해야 함")
        fun `empty should create empty DailyRecord`() {
            val date = LocalDate.of(2024, 1, 15)
            val record = DailyRecord.empty(date)
            
            assertThat(record.date).isEqualTo(date)
            assertThat(record.totalCount).isEqualTo(0)
            assertThat(record.wishText).isEmpty()
            assertThat(record.targetCount).isEqualTo(0)
            assertThat(record.isCompleted).isFalse()
            assertThat(record.resetCount).isEqualTo(0)
            assertThat(record.lostCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    inner class EdgeCasesTests {

        @Test
        @DisplayName("음수 카운트 처리")
        fun `negative counts should be handled`() {
            val record = createTestDailyRecord(
                totalCount = -10,
                targetCount = 100
            )
            
            // progress는 0으로 제한됨
            assertThat(record.progress).isEqualTo(0f)
            assertThat(record.completionRate).isEqualTo(0)
            assertThat(record.remainingCount).isEqualTo(100)
        }

        @Test
        @DisplayName("매우 큰 카운트 값 처리")
        fun `very large count values should be handled`() {
            val record = createTestDailyRecord(
                totalCount = Int.MAX_VALUE,
                targetCount = 100
            )
            
            assertThat(record.progress).isEqualTo(1.0f) // 최대값으로 제한
            assertThat(record.completionRate).isEqualTo(100)
            assertThat(record.remainingCount).isEqualTo(0)
        }

        @Test
        @DisplayName("빈 소원 텍스트 처리")
        fun `empty wish text should be handled`() {
            val record = createTestDailyRecord(wishText = "")
            
            assertThat(record.wishText).isEmpty()
            // 다른 기능은 정상 동작해야 함
            assertThat(record.progress).isEqualTo(0.5f)
        }

        @Test
        @DisplayName("먼 미래 날짜 처리")
        fun `far future dates should be handled`() {
            val futureDate = LocalDate.of(2100, 12, 31)
            val record = createTestDailyRecord(date = futureDate)
            
            assertThat(record.isToday).isFalse()
            assertThat(record.isYesterday).isFalse()
            assertThat(record.relativeDateString).contains("일 전") // 음수일 수도 있음
        }

        @Test
        @DisplayName("먼 과거 날짜 처리")
        fun `far past dates should be handled`() {
            val pastDate = LocalDate.of(1900, 1, 1)
            val record = createTestDailyRecord(date = pastDate)
            
            assertThat(record.isToday).isFalse()
            assertThat(record.isYesterday).isFalse()
            
            val daysAgo = LocalDate.now().toEpochDay() - pastDate.toEpochDay()
            assertThat(record.relativeDateString).isEqualTo("${daysAgo}일 전")
        }
    }

    @Nested
    @DisplayName("Property-Based Testing")
    inner class PropertyBasedTests {

        @Test
        @DisplayName("completionRate는 항상 0-100 사이여야 함")
        fun `completionRate should always be between 0 and 100`() = runTest {
            checkAll(
                Arb.int(-1000..10000), // totalCount (음수 포함)
                Arb.int(1..10000)       // targetCount
            ) { totalCount, targetCount ->
                val record = createTestDailyRecord(
                    totalCount = totalCount,
                    targetCount = targetCount
                )
                
                assertThat(record.completionRate).isAtLeast(0)
                assertThat(record.completionRate).isAtMost(100)
            }
        }

        @Test
        @DisplayName("progress는 항상 0.0-1.0 사이여야 함")
        fun `progress should always be between 0_0 and 1_0`() = runTest {
            checkAll(
                Arb.int(-1000..10000), // totalCount
                Arb.int(0..10000)      // targetCount
            ) { totalCount, targetCount ->
                val record = createTestDailyRecord(
                    totalCount = totalCount,
                    targetCount = targetCount
                )
                
                assertThat(record.progress).isAtLeast(0.0f)
                assertThat(record.progress).isAtMost(1.0f)
            }
        }

        @Test
        @DisplayName("remainingCount는 항상 0 이상이어야 함")
        fun `remainingCount should always be non-negative`() = runTest {
            checkAll(
                Arb.int(-1000..10000), // totalCount
                Arb.int(-1000..10000)  // targetCount
            ) { totalCount, targetCount ->
                val record = createTestDailyRecord(
                    totalCount = totalCount,
                    targetCount = targetCount
                )
                
                assertThat(record.remainingCount).isAtLeast(0)
            }
        }

        @Test
        @DisplayName("actualCount는 항상 totalCount + lostCount여야 함")
        fun `actualCount should always equal totalCount plus lostCount`() = runTest {
            checkAll(
                Arb.int(0..1000),  // totalCount
                Arb.int(0..1000)   // lostCount
            ) { totalCount, lostCount ->
                val record = createTestDailyRecord(
                    totalCount = totalCount,
                    lostCount = lostCount
                )
                
                assertThat(record.actualCount).isEqualTo(totalCount + lostCount)
            }
        }

        @Test
        @DisplayName("날짜 포맷 변환의 일관성")
        fun `date format conversions should be consistent`() = runTest {
            checkAll(
                Arb.int(2000..2100),
                Arb.int(1..12),
                Arb.int(1..28) // 모든 달에 유효한 날
            ) { year, month, day ->
                val date = LocalDate.of(year, month, day)
                val record = createTestDailyRecord(date = date)
                
                // displayDate와 dateString이 같은 날짜를 나타내야 함
                val displayParts = record.displayDate.split(".")
                val dateParts = record.dateString.split("-")
                
                assertThat(displayParts[0]).isEqualTo(dateParts[0]) // year
                assertThat(displayParts[1]).isEqualTo(dateParts[1]) // month
                assertThat(displayParts[2]).isEqualTo(dateParts[2]) // day
            }
        }

        @Test
        @DisplayName("상태 텍스트는 항상 비어있지 않아야 함")
        fun `statusText should never be empty`() = runTest {
            checkAll(
                Arb.int(0..200),    // totalCount
                Arb.int(1..200),    // targetCount
                Arb.boolean()       // isCompleted
            ) { totalCount, targetCount, isCompleted ->
                val record = createTestDailyRecord(
                    totalCount = totalCount,
                    targetCount = targetCount,
                    isCompleted = isCompleted
                )
                
                assertThat(record.statusText).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("Consistency Tests")
    inner class ConsistencyTests {

        @Test
        @DisplayName("isCompleted와 progress의 일관성")
        fun `isCompleted should be consistent with progress`() {
            // 완료된 경우 progress는 1.0이어야 함
            val completed = createTestDailyRecord(
                totalCount = 100,
                targetCount = 100,
                isCompleted = true
            )
            assertThat(completed.progress).isEqualTo(1.0f)
            
            // 미완료인 경우 progress는 1.0 미만이어야 함
            val notCompleted = createTestDailyRecord(
                totalCount = 50,
                targetCount = 100,
                isCompleted = false
            )
            assertThat(notCompleted.progress).isLessThan(1.0f)
        }

        @Test
        @DisplayName("오늘과 어제는 동시에 true일 수 없음")
        fun `isToday and isYesterday should be mutually exclusive`() {
            val todayRecord = createTestDailyRecord(date = LocalDate.now())
            val yesterdayRecord = createTestDailyRecord(date = LocalDate.now().minusDays(1))
            
            // 오늘인 레코드는 어제가 아님
            if (todayRecord.isToday) {
                assertThat(todayRecord.isYesterday).isFalse()
            }
            
            // 어제인 레코드는 오늘이 아님
            if (yesterdayRecord.isYesterday) {
                assertThat(yesterdayRecord.isToday).isFalse()
            }
        }

        @Test
        @DisplayName("hasResets와 resetCount의 일관성")
        fun `hasResets should be consistent with resetCount`() {
            val withResets = createTestDailyRecord(resetCount = 5)
            val withoutResets = createTestDailyRecord(resetCount = 0)
            
            // resetCount > 0이면 hasResets는 true
            assertThat(withResets.hasResets).isTrue()
            assertThat(withResets.resetCount).isGreaterThan(0)
            
            // resetCount = 0이면 hasResets는 false
            assertThat(withoutResets.hasResets).isFalse()
            assertThat(withoutResets.resetCount).isEqualTo(0)
        }
    }
}