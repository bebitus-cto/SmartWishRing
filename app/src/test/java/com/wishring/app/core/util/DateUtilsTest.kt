package com.wishring.app.core.util

import com.google.common.truth.Truth.assertThat
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for DateUtils
 * 
 * Implements "Time Traveler's Gauntlet" testing strategy:
 * - Tests across different timezones
 * - Tests around daylight saving time transitions
 * - Tests at date boundaries (midnight, year end, month end)
 * - Tests with extreme past and future dates
 * - Property-based testing for date conversions
 */
@DisplayName("DateUtils Test Suite - Time Traveler's Gauntlet")
class DateUtilsTest {

    @Nested
    @DisplayName("Date Format Conversion Tests")
    inner class DateFormatConversionTests {

        @Test
        @DisplayName("getTodayString이 올바른 형식으로 오늘 날짜를 반환해야 함")
        fun `getTodayString should return today in correct format`() {
            val result = DateUtils.getTodayString()
            val expected = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            assertThat(result).isEqualTo(expected)
            assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}")
        }

        @Test
        @DisplayName("getTodayDisplayString이 표시 형식으로 오늘 날짜를 반환해야 함")
        fun `getTodayDisplayString should return today in display format`() {
            val result = DateUtils.getTodayDisplayString()
            val expected = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
            
            assertThat(result).isEqualTo(expected)
            assertThat(result).matches("\\d{4}\\.\\d{2}\\.\\d{2}")
        }

        @ParameterizedTest
        @CsvSource(
            "2024-01-01, 2024.01.01",
            "2024-12-31, 2024.12.31",
            "2024-02-29, 2024.02.29", // Leap year
            "2023-02-28, 2023.02.28",
            "2024-07-15, 2024.07.15"
        )
        @DisplayName("toDisplayFormat이 DB 형식을 표시 형식으로 변환해야 함")
        fun `toDisplayFormat should convert DB format to display format`(
            dbFormat: String,
            expectedDisplay: String
        ) {
            val result = DateUtils.toDisplayFormat(dbFormat)
            assertThat(result).isEqualTo(expectedDisplay)
        }

        @ParameterizedTest
        @CsvSource(
            "2024.01.01, 2024-01-01",
            "2024.12.31, 2024-12-31",
            "2024.02.29, 2024-02-29",
            "2023.02.28, 2023-02-28"
        )
        @DisplayName("toDbFormat이 표시 형식을 DB 형식으로 변환해야 함")
        fun `toDbFormat should convert display format to DB format`(
            displayFormat: String,
            expectedDb: String
        ) {
            val result = DateUtils.toDbFormat(displayFormat)
            assertThat(result).isEqualTo(expectedDb)
        }

        @Test
        @DisplayName("잘못된 형식의 날짜는 원본 문자열을 반환해야 함")
        fun `invalid date format should return original string`() {
            val invalidDate = "not-a-date"
            
            assertThat(DateUtils.toDisplayFormat(invalidDate)).isEqualTo(invalidDate)
            assertThat(DateUtils.toDbFormat(invalidDate)).isEqualTo(invalidDate)
        }

        @Test
        @DisplayName("빈 문자열 처리")
        fun `empty string handling`() {
            assertThat(DateUtils.toDisplayFormat("")).isEqualTo("")
            assertThat(DateUtils.toDbFormat("")).isEqualTo("")
        }
    }

    @Nested
    @DisplayName("Timestamp Formatting Tests")
    inner class TimestampFormattingTests {

        @Test
        @DisplayName("getCurrentTimestamp가 현재 시간을 밀리초로 반환해야 함")
        fun `getCurrentTimestamp should return current time in milliseconds`() {
            val before = System.currentTimeMillis()
            val result = DateUtils.getCurrentTimestamp()
            val after = System.currentTimeMillis()
            
            assertThat(result).isAtLeast(before)
            assertThat(result).isAtMost(after)
        }

        @Test
        @DisplayName("formatTimestamp가 올바른 datetime 형식으로 변환해야 함")
        fun `formatTimestamp should convert to datetime format`() {
            // 2024-01-15 15:30:45 UTC+9
            val timestamp = 1705300245000L
            val result = DateUtils.formatTimestamp(timestamp)
            
            // 결과는 시스템 타임존에 따라 달라질 수 있음
            assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
        }

        @Test
        @DisplayName("formatTime이 시간만 HH:mm 형식으로 반환해야 함")
        fun `formatTime should return time in HH_mm format`() {
            val timestamp = System.currentTimeMillis()
            val result = DateUtils.formatTime(timestamp)
            
            assertThat(result).matches("\\d{2}:\\d{2}")
        }

        @ParameterizedTest
        @ValueSource(longs = [0L, 1L, 946684800000L, 253402300799999L]) // Various timestamps
        @DisplayName("다양한 timestamp 값에 대한 포맷팅 테스트")
        fun `various timestamps should format correctly`(timestamp: Long) {
            val datetimeResult = DateUtils.formatTimestamp(timestamp)
            val timeResult = DateUtils.formatTime(timestamp)
            
            assertThat(datetimeResult).isNotEmpty()
            assertThat(timeResult).matches("\\d{2}:\\d{2}")
        }
    }

    @Nested
    @DisplayName("Date Comparison and Validation Tests")
    inner class DateComparisonTests {

        @Test
        @DisplayName("isToday가 오늘 날짜를 정확히 판별해야 함")
        fun `isToday should correctly identify today's date`() {
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            assertTrue(DateUtils.isToday(today))
            assertFalse(DateUtils.isToday(yesterday))
            assertFalse(DateUtils.isToday(tomorrow))
        }

        @ParameterizedTest
        @CsvSource(
            "2024-01-01, 2024-01-01, 0",
            "2024-01-01, 2024-01-02, 1",
            "2024-01-01, 2024-01-10, 9",
            "2024-01-10, 2024-01-01, -9",
            "2024-01-01, 2024-02-01, 31",
            "2024-01-01, 2025-01-01, 366" // Leap year
        )
        @DisplayName("getDaysDifference가 날짜 차이를 정확히 계산해야 함")
        fun `getDaysDifference should calculate correct day difference`(
            date1: String,
            date2: String,
            expectedDiff: Long
        ) {
            val result = DateUtils.getDaysDifference(date1, date2)
            assertThat(result).isEqualTo(expectedDiff)
        }

        @Test
        @DisplayName("잘못된 날짜로 getDaysDifference 호출 시 0을 반환해야 함")
        fun `getDaysDifference with invalid dates should return 0`() {
            assertThat(DateUtils.getDaysDifference("invalid", "2024-01-01")).isEqualTo(0)
            assertThat(DateUtils.getDaysDifference("2024-01-01", "invalid")).isEqualTo(0)
            assertThat(DateUtils.getDaysDifference("", "")).isEqualTo(0)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "2024-01-01",
            "2024-12-31",
            "2024-02-29",
            "2000-01-01",
            "2099-12-31"
        ])
        @DisplayName("유효한 날짜를 올바르게 검증해야 함")
        fun `isValidDate should return true for valid dates`(dateString: String) {
            assertTrue(DateUtils.isValidDate(dateString))
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "not-a-date",
            "2024/01/01",
            "2024.01.01",
            "01-01-2024",
            "2024-13-01", // Invalid month
            "2024-01-32", // Invalid day
            "2023-02-29", // Not a leap year
            "",
            "null"
        ])
        @DisplayName("유효하지 않은 날짜를 올바르게 검증해야 함")
        fun `isValidDate should return false for invalid dates`(dateString: String) {
            assertFalse(DateUtils.isValidDate(dateString))
        }
    }

    @Nested
    @DisplayName("Relative Date String Tests")
    inner class RelativeDateStringTests {

        @Test
        @DisplayName("getRelativeDateString이 오늘을 '오늘'로 표시해야 함")
        fun `getRelativeDateString should show today as 오늘`() {
            val today = DateUtils.getTodayString()
            assertThat(DateUtils.getRelativeDateString(today)).isEqualTo("오늘")
        }

        @Test
        @DisplayName("getRelativeDateString이 어제를 '어제'로 표시해야 함")
        fun `getRelativeDateString should show yesterday as 어제`() {
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            assertThat(DateUtils.getRelativeDateString(yesterday)).isEqualTo("어제")
        }

        @ParameterizedTest
        @CsvSource(
            "2, 2일 전",
            "3, 3일 전",
            "4, 4일 전",
            "5, 5일 전",
            "6, 6일 전"
        )
        @DisplayName("getRelativeDateString이 2-6일 전을 올바르게 표시해야 함")
        fun `getRelativeDateString should show N days ago for 2-6 days`(
            daysAgo: Long,
            expected: String
        ) {
            val date = LocalDate.now().minusDays(daysAgo).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            assertThat(DateUtils.getRelativeDateString(date)).isEqualTo(expected)
        }

        @Test
        @DisplayName("getRelativeDateString이 7일 이상 전은 날짜 형식으로 표시해야 함")
        fun `getRelativeDateString should show date format for 7+ days ago`() {
            val date = LocalDate.now().minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val result = DateUtils.getRelativeDateString(date)
            
            assertThat(result).matches("\\d{4}\\.\\d{2}\\.\\d{2}")
        }

        @Test
        @DisplayName("미래 날짜도 올바르게 처리해야 함")
        fun `getRelativeDateString should handle future dates`() {
            val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val result = DateUtils.getRelativeDateString(tomorrow)
            
            // 미래 날짜는 날짜 형식으로 표시
            assertThat(result).matches("\\d{4}\\.\\d{2}\\.\\d{2}")
        }
    }

    @Nested
    @DisplayName("Date Range and List Generation Tests")
    inner class DateRangeTests {

        @Test
        @DisplayName("getDateDaysAgo가 N일 전 날짜를 올바르게 반환해야 함")
        fun `getDateDaysAgo should return correct date N days ago`() {
            val daysAgo = 5
            val expected = LocalDate.now().minusDays(daysAgo.toLong()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            assertThat(DateUtils.getDateDaysAgo(daysAgo)).isEqualTo(expected)
        }

        @Test
        @DisplayName("getDateDaysAgo가 0일 전(오늘)을 올바르게 처리해야 함")
        fun `getDateDaysAgo with 0 should return today`() {
            assertThat(DateUtils.getDateDaysAgo(0)).isEqualTo(DateUtils.getTodayString())
        }

        @Test
        @DisplayName("getDateDaysAgo가 음수(미래)도 처리해야 함")
        fun `getDateDaysAgo should handle negative days (future)`() {
            val result = DateUtils.getDateDaysAgo(-1)
            val expected = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            assertThat(result).isEqualTo(expected)
        }

        @Test
        @DisplayName("getLastNDaysDates가 최근 N일의 날짜 리스트를 반환해야 함")
        fun `getLastNDaysDates should return list of last N days`() {
            val days = 7
            val result = DateUtils.getLastNDaysDates(days)
            
            assertThat(result).hasSize(days)
            
            // 오래된 날짜부터 최신 날짜 순으로 정렬되어야 함
            val expected = (6 downTo 0).map { 
                LocalDate.now().minusDays(it.toLong()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            
            assertThat(result).isEqualTo(expected)
        }

        @Test
        @DisplayName("getLastNDaysDates가 1일만 요청하면 오늘만 반환해야 함")
        fun `getLastNDaysDates with 1 day should return only today`() {
            val result = DateUtils.getLastNDaysDates(1)
            
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(DateUtils.getTodayString())
        }

        @Test
        @DisplayName("getLastNDaysDates가 0일 요청하면 빈 리스트를 반환해야 함")
        fun `getLastNDaysDates with 0 days should return empty list`() {
            val result = DateUtils.getLastNDaysDates(0)
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("Legacy Date API Conversion Tests")
    inner class LegacyDateConversionTests {

        @Test
        @DisplayName("dateToString이 Date를 올바른 문자열로 변환해야 함")
        fun `dateToString should convert Date to string correctly`() {
            val date = Date(1705248000000L) // 2024-01-15 00:00:00 UTC
            val pattern = "yyyy-MM-dd"
            
            val result = DateUtils.dateToString(date, pattern)
            
            assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}")
        }

        @Test
        @DisplayName("dateToString이 기본 패턴을 사용해야 함")
        fun `dateToString should use default pattern when not specified`() {
            val date = Date()
            val result = DateUtils.dateToString(date)
            
            assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}")
        }

        @Test
        @DisplayName("stringToDate가 문자열을 Date로 변환해야 함")
        fun `stringToDate should convert string to Date`() {
            val dateString = "2024-01-15"
            val result = DateUtils.stringToDate(dateString)
            
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("stringToDate가 잘못된 형식에 대해 null을 반환해야 함")
        fun `stringToDate should return null for invalid format`() {
            assertThat(DateUtils.stringToDate("not-a-date")).isNull()
            assertThat(DateUtils.stringToDate("")).isNull()
            assertThat(DateUtils.stringToDate("2024/01/15")).isNull()
        }

        @Test
        @DisplayName("Date와 String 간 왕복 변환이 일관성 있어야 함")
        fun `round-trip conversion between Date and String should be consistent`() {
            val originalDate = Date()
            val dateString = DateUtils.dateToString(originalDate)
            val convertedDate = DateUtils.stringToDate(dateString)
            val finalString = DateUtils.dateToString(convertedDate!!)
            
            assertThat(finalString).isEqualTo(dateString)
        }
    }

    @Nested
    @DisplayName("Time Traveler's Gauntlet - Boundary & Edge Cases")
    inner class TimeTravelerGauntletTests {

        @Test
        @DisplayName("자정(00:00:00) 경계에서의 날짜 처리")
        fun `date handling at midnight boundary`() {
            // 자정 직전과 직후
            val beforeMidnight = LocalDateTime.of(2024, 1, 15, 23, 59, 59)
            val atMidnight = LocalDateTime.of(2024, 1, 16, 0, 0, 0)
            
            val beforeTimestamp = beforeMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val atTimestamp = atMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val beforeFormatted = DateUtils.formatTimestamp(beforeTimestamp)
            val atFormatted = DateUtils.formatTimestamp(atTimestamp)
            
            // 날짜가 바뀌어야 함
            assertThat(beforeFormatted).contains("2024-01-15")
            assertThat(atFormatted).contains("2024-01-16")
        }

        @Test
        @DisplayName("연말/연초 경계에서의 날짜 처리")
        fun `date handling at year boundary`() {
            val lastDayOfYear = "2023-12-31"
            val firstDayOfYear = "2024-01-01"
            
            val daysDiff = DateUtils.getDaysDifference(lastDayOfYear, firstDayOfYear)
            assertThat(daysDiff).isEqualTo(1)
            
            // 상대 날짜 계산
            val lastDayList = DateUtils.getLastNDaysDates(2)
            // 리스트가 연도 경계를 넘을 수 있음
            assertThat(lastDayList).hasSize(2)
        }

        @Test
        @DisplayName("윤년(2월 29일) 처리")
        fun `leap year February 29th handling`() {
            val leapDay2024 = "2024-02-29"
            val notLeapDay2023 = "2023-02-29"
            
            assertTrue(DateUtils.isValidDate(leapDay2024))
            assertFalse(DateUtils.isValidDate(notLeapDay2023))
            
            // 윤년의 2월 28일과 3월 1일 사이
            val feb28 = "2024-02-28"
            val mar1 = "2024-03-01"
            assertThat(DateUtils.getDaysDifference(feb28, mar1)).isEqualTo(2) // 29일이 있음
            
            // 평년의 2월 28일과 3월 1일 사이
            val feb28_2023 = "2023-02-28"
            val mar1_2023 = "2023-03-01"
            assertThat(DateUtils.getDaysDifference(feb28_2023, mar1_2023)).isEqualTo(1) // 29일이 없음
        }

        @Test
        @DisplayName("먼 과거와 미래 날짜 처리")
        fun `extreme past and future date handling`() {
            val farPast = "1900-01-01"
            val farFuture = "2100-12-31"
            val today = DateUtils.getTodayString()
            
            assertTrue(DateUtils.isValidDate(farPast))
            assertTrue(DateUtils.isValidDate(farFuture))
            
            // 매우 큰 일수 차이 계산
            val daysToPast = DateUtils.getDaysDifference(farPast, today)
            val daysToFuture = DateUtils.getDaysDifference(today, farFuture)
            
            assertThat(daysToPast).isGreaterThan(40000) // 100년 이상
            assertThat(daysToFuture).isGreaterThan(20000) // 수십 년
        }

        @Test
        @DisplayName("월말 날짜 처리 (30일 vs 31일)")
        fun `month end date handling`() {
            // 31일이 있는 달
            val jan31 = "2024-01-31"
            val feb1 = "2024-02-01"
            assertThat(DateUtils.getDaysDifference(jan31, feb1)).isEqualTo(1)
            
            // 30일까지만 있는 달
            val apr30 = "2024-04-30"
            val may1 = "2024-05-01"
            assertThat(DateUtils.getDaysDifference(apr30, may1)).isEqualTo(1)
            
            // 잘못된 날짜 (4월 31일)
            assertFalse(DateUtils.isValidDate("2024-04-31"))
        }
    }

    @Nested
    @DisplayName("Property-Based Testing")
    inner class PropertyBasedTests {

        @Test
        @DisplayName("날짜 변환의 가역성 - DB 형식 ↔ 표시 형식")
        fun `date format conversion should be reversible`() = runTest {
            checkAll(
                Arb.int(2000..2100), // year
                Arb.int(1..12),       // month
                Arb.int(1..28)        // day (28로 제한하여 모든 달에서 유효)
            ) { year, month, day ->
                val dbFormat = String.format("%04d-%02d-%02d", year, month, day)
                
                if (DateUtils.isValidDate(dbFormat)) {
                    val displayFormat = DateUtils.toDisplayFormat(dbFormat)
                    val backToDb = DateUtils.toDbFormat(displayFormat)
                    
                    assertThat(backToDb).isEqualTo(dbFormat)
                }
            }
        }

        @Test
        @DisplayName("getDaysDifference의 대칭성")
        fun `getDaysDifference should be symmetric`() = runTest {
            checkAll(
                Arb.int(2020..2025),
                Arb.int(1..12),
                Arb.int(1..28),
                Arb.int(2020..2025),
                Arb.int(1..12),
                Arb.int(1..28)
            ) { year1, month1, day1, year2, month2, day2 ->
                val date1 = String.format("%04d-%02d-%02d", year1, month1, day1)
                val date2 = String.format("%04d-%02d-%02d", year2, month2, day2)
                
                if (DateUtils.isValidDate(date1) && DateUtils.isValidDate(date2)) {
                    val diff1to2 = DateUtils.getDaysDifference(date1, date2)
                    val diff2to1 = DateUtils.getDaysDifference(date2, date1)
                    
                    assertThat(diff1to2).isEqualTo(-diff2to1)
                }
            }
        }

        @Test
        @DisplayName("getLastNDaysDates가 항상 정렬된 순서를 유지해야 함")
        fun `getLastNDaysDates should always return sorted dates`() = runTest {
            checkAll(Arb.int(1..365)) { days ->
                val dates = DateUtils.getLastNDaysDates(days)
                
                // 날짜들이 오름차순으로 정렬되어 있는지 확인
                for (i in 1 until dates.size) {
                    val prevDate = LocalDate.parse(dates[i - 1])
                    val currDate = LocalDate.parse(dates[i])
                    assertThat(currDate).isGreaterThan(prevDate)
                }
            }
        }

        @Test
        @DisplayName("timestamp 포맷팅이 항상 유효한 형식을 반환해야 함")
        fun `timestamp formatting should always return valid format`() = runTest {
            checkAll(Arb.long(0L..System.currentTimeMillis() * 2)) { timestamp ->
                val datetimeResult = DateUtils.formatTimestamp(timestamp)
                val timeResult = DateUtils.formatTime(timestamp)
                
                // 날짜시간 형식 검증
                assertThat(datetimeResult).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
                
                // 시간 형식 검증
                assertThat(timeResult).matches("\\d{2}:\\d{2}")
            }
        }
    }

    @Nested
    @DisplayName("Timezone and DST Handling Tests")
    inner class TimezoneTests {

        @Test
        @DisplayName("다른 타임존에서도 일관된 날짜 처리")
        fun `consistent date handling across timezones`() {
            // 현재 시스템 타임존 저장
            val originalZone = TimeZone.getDefault()
            
            try {
                // 다양한 타임존에서 테스트
                val timezones = listOf(
                    TimeZone.getTimeZone("UTC"),
                    TimeZone.getTimeZone("America/New_York"),
                    TimeZone.getTimeZone("Asia/Seoul"),
                    TimeZone.getTimeZone("Europe/London")
                )
                
                val testDate = "2024-01-15"
                
                timezones.forEach { tz ->
                    TimeZone.setDefault(tz)
                    
                    // 날짜 유효성은 타임존과 무관해야 함
                    assertTrue(DateUtils.isValidDate(testDate))
                    
                    // 날짜 변환도 타임존과 무관해야 함
                    val displayFormat = DateUtils.toDisplayFormat(testDate)
                    assertThat(displayFormat).isEqualTo("2024.01.15")
                }
                
            } finally {
                // 원래 타임존으로 복원
                TimeZone.setDefault(originalZone)
            }
        }

        @Test
        @DisplayName("DST 전환 시점에서의 날짜 계산")
        fun `date calculations around DST transitions`() {
            // 2024년 미국 DST 시작: 3월 10일
            // 2024년 미국 DST 종료: 11월 3일
            
            val beforeDST = "2024-03-09"
            val afterDST = "2024-03-11"
            
            // DST 전환을 넘어도 날짜 차이는 정확해야 함
            val daysDiff = DateUtils.getDaysDifference(beforeDST, afterDST)
            assertThat(daysDiff).isEqualTo(2)
        }
    }
}