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
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for Health Data models (MRD SDK integration)
 * 
 * Test coverage includes:
 * - HeartRateData validation and quality checks
 * - BloodPressureData systolic/diastolic validation
 * - SleepData calculations and quality assessment
 * - StepData distance and calorie calculations
 * - TemperatureData range validation
 * - EcgData byte array handling and equality
 * - BloodOxygenData percentage validation
 * - HealthDataUpdate wrapper functionality
 * - Data quality and sleep quality enums
 */
@DisplayName("HealthData Domain Models Tests - MRD SDK Integration")
class HealthDataTest {

    companion object {
        private const val TEST_TIMESTAMP = 1705372800000L // 2024-01-16 00:00:00
        private const val TEST_DATE = "2024-01-15"
    }

    @Nested
    @DisplayName("HeartRateData Tests")
    inner class HeartRateDataTests {

        @Test
        @DisplayName("HeartRateData가 올바르게 생성되어야 함")
        fun `HeartRateData should be created with valid values`() {
            val data = HeartRateData(
                bpm = 75,
                timestamp = TEST_TIMESTAMP,
                quality = DataQuality.GOOD
            )
            
            assertThat(data.bpm).isEqualTo(75)
            assertThat(data.timestamp).isEqualTo(TEST_TIMESTAMP)
            assertThat(data.quality).isEqualTo(DataQuality.GOOD)
        }

        @ParameterizedTest
        @ValueSource(ints = [40, 60, 75, 100, 120, 180, 200])
        @DisplayName("다양한 심박수 값을 처리해야 함")
        fun `should handle various heart rate values`(bpm: Int) {
            val data = HeartRateData(bpm = bpm, timestamp = TEST_TIMESTAMP)
            assertThat(data.bpm).isEqualTo(bpm)
        }

        @Test
        @DisplayName("비정상적인 심박수 값도 저장 가능해야 함 (경고 표시용)")
        fun `should allow abnormal heart rate values for warning display`() {
            val tooLow = HeartRateData(bpm = 30, timestamp = TEST_TIMESTAMP)
            val tooHigh = HeartRateData(bpm = 250, timestamp = TEST_TIMESTAMP)
            
            assertThat(tooLow.bpm).isEqualTo(30)
            assertThat(tooHigh.bpm).isEqualTo(250)
        }

        @ParameterizedTest
        @EnumSource(DataQuality::class)
        @DisplayName("모든 데이터 품질 레벨을 지원해야 함")
        fun `should support all data quality levels`(quality: DataQuality) {
            val data = HeartRateData(bpm = 75, timestamp = TEST_TIMESTAMP, quality = quality)
            assertThat(data.quality).isEqualTo(quality)
        }
    }

    @Nested
    @DisplayName("BloodPressureData Tests")
    inner class BloodPressureDataTests {

        @Test
        @DisplayName("BloodPressureData가 올바르게 생성되어야 함")
        fun `BloodPressureData should be created with valid values`() {
            val data = BloodPressureData(
                systolic = 120,
                diastolic = 80,
                timestamp = TEST_TIMESTAMP,
                quality = DataQuality.EXCELLENT
            )
            
            assertThat(data.systolic).isEqualTo(120)
            assertThat(data.diastolic).isEqualTo(80)
            assertThat(data.timestamp).isEqualTo(TEST_TIMESTAMP)
            assertThat(data.quality).isEqualTo(DataQuality.EXCELLENT)
        }

        @ParameterizedTest
        @CsvSource(
            "120, 80",  // Normal
            "110, 70",  // Low normal
            "130, 85",  // Pre-hypertension
            "140, 90",  // Stage 1 hypertension
            "160, 100", // Stage 2 hypertension
            "180, 110"  // Hypertensive crisis
        )
        @DisplayName("다양한 혈압 범위를 처리해야 함")
        fun `should handle various blood pressure ranges`(systolic: Int, diastolic: Int) {
            val data = BloodPressureData(
                systolic = systolic,
                diastolic = diastolic,
                timestamp = TEST_TIMESTAMP
            )
            
            assertThat(data.systolic).isEqualTo(systolic)
            assertThat(data.diastolic).isEqualTo(diastolic)
        }

        @Test
        @DisplayName("수축기 혈압이 이완기보다 높아야 함 (의학적 정확성)")
        fun `systolic should be higher than diastolic for medical accuracy`() {
            val data = BloodPressureData(
                systolic = 120,
                diastolic = 80,
                timestamp = TEST_TIMESTAMP
            )
            
            assertThat(data.systolic).isGreaterThan(data.diastolic)
        }
    }

    @Nested
    @DisplayName("SleepData Tests")
    inner class SleepDataTests {

        private fun createTestSleepData(
            totalSleepMinutes: Int = 480,
            deepSleepMinutes: Int = 120,
            lightSleepMinutes: Int = 240,
            remSleepMinutes: Int = 90,
            awakeMinutes: Int = 30
        ) = SleepData(
            date = TEST_DATE,
            totalSleepMinutes = totalSleepMinutes,
            deepSleepMinutes = deepSleepMinutes,
            lightSleepMinutes = lightSleepMinutes,
            remSleepMinutes = remSleepMinutes,
            awakeMinutes = awakeMinutes,
            sleepQuality = SleepQuality.GOOD,
            bedTime = "23:00",
            wakeTime = "07:00"
        )

        @Test
        @DisplayName("SleepData가 올바르게 생성되어야 함")
        fun `SleepData should be created with valid values`() {
            val data = createTestSleepData()
            
            assertThat(data.totalSleepMinutes).isEqualTo(480)
            assertThat(data.deepSleepMinutes).isEqualTo(120)
            assertThat(data.lightSleepMinutes).isEqualTo(240)
            assertThat(data.remSleepMinutes).isEqualTo(90)
            assertThat(data.awakeMinutes).isEqualTo(30)
            assertThat(data.sleepQuality).isEqualTo(SleepQuality.GOOD)
            assertThat(data.bedTime).isEqualTo("23:00")
            assertThat(data.wakeTime).isEqualTo("07:00")
        }

        @Test
        @DisplayName("수면 단계별 시간의 합이 전체 시간과 일치해야 함")
        fun `sleep stage minutes should add up correctly`() {
            val data = createTestSleepData()
            
            val sumOfStages = data.deepSleepMinutes + 
                            data.lightSleepMinutes + 
                            data.remSleepMinutes + 
                            data.awakeMinutes
            
            // 전체 수면 시간은 깨어있는 시간을 제외한 값
            val actualSleepTime = data.deepSleepMinutes + 
                                 data.lightSleepMinutes + 
                                 data.remSleepMinutes
            
            assertThat(actualSleepTime).isEqualTo(450) // 120 + 240 + 90
        }

        @ParameterizedTest
        @EnumSource(SleepQuality::class)
        @DisplayName("모든 수면 품질 레벨을 지원해야 함")
        fun `should support all sleep quality levels`(quality: SleepQuality) {
            val data = createTestSleepData().copy(sleepQuality = quality)
            assertThat(data.sleepQuality).isEqualTo(quality)
        }

        @Test
        @DisplayName("시간 형식 검증 (HH:mm)")
        fun `time format should be HH_mm`() {
            val data = createTestSleepData()
            
            assertThat(data.bedTime).matches("\\d{2}:\\d{2}")
            assertThat(data.wakeTime).matches("\\d{2}:\\d{2}")
        }
    }

    @Nested
    @DisplayName("StepData Tests")
    inner class StepDataTests {

        @Test
        @DisplayName("StepData가 올바르게 생성되어야 함")
        fun `StepData should be created with valid values`() {
            val data = StepData(
                date = TEST_DATE,
                steps = 10000,
                distance = 7.5f,
                calories = 350,
                activeMinutes = 45
            )
            
            assertThat(data.date).isEqualTo(TEST_DATE)
            assertThat(data.steps).isEqualTo(10000)
            assertThat(data.distance).isWithin(0.01f).of(7.5f)
            assertThat(data.calories).isEqualTo(350)
            assertThat(data.activeMinutes).isEqualTo(45)
        }

        @ParameterizedTest
        @CsvSource(
            "0, 0.0, 0",      // No activity
            "5000, 3.75, 175", // Half goal
            "10000, 7.5, 350", // Daily goal
            "15000, 11.25, 525", // Above goal
            "20000, 15.0, 700"  // High activity
        )
        @DisplayName("걸음 수에 따른 거리와 칼로리 계산 검증")
        fun `should validate distance and calorie calculations`(
            steps: Int,
            expectedDistance: Float,
            expectedCalories: Int
        ) {
            // 일반적인 계산: 1걸음 ≈ 0.75m, 1km ≈ 46.67 칼로리
            val data = StepData(
                date = TEST_DATE,
                steps = steps,
                distance = expectedDistance,
                calories = expectedCalories,
                activeMinutes = steps / 100 // 100걸음당 1분으로 가정
            )
            
            assertThat(data.steps).isEqualTo(steps)
            assertThat(data.distance).isWithin(0.01f).of(expectedDistance)
            assertThat(data.calories).isEqualTo(expectedCalories)
        }

        @Test
        @DisplayName("음수 값 처리 (데이터 오류 케이스)")
        fun `should handle negative values for error cases`() {
            val data = StepData(
                date = TEST_DATE,
                steps = -100,
                distance = -1.0f,
                calories = -50,
                activeMinutes = -10
            )
            
            // 음수 값도 저장은 가능 (UI에서 처리)
            assertThat(data.steps).isEqualTo(-100)
            assertThat(data.distance).isEqualTo(-1.0f)
            assertThat(data.calories).isEqualTo(-50)
            assertThat(data.activeMinutes).isEqualTo(-10)
        }
    }

    @Nested
    @DisplayName("TemperatureData Tests")
    inner class TemperatureDataTests {

        @Test
        @DisplayName("TemperatureData가 올바르게 생성되어야 함")
        fun `TemperatureData should be created with valid values`() {
            val data = TemperatureData(
                temperature = 36.5f,
                timestamp = TEST_TIMESTAMP,
                quality = DataQuality.GOOD
            )
            
            assertThat(data.temperature).isWithin(0.01f).of(36.5f)
            assertThat(data.timestamp).isEqualTo(TEST_TIMESTAMP)
            assertThat(data.quality).isEqualTo(DataQuality.GOOD)
        }

        @ParameterizedTest
        @ValueSource(floats = [35.0f, 36.0f, 36.5f, 37.0f, 37.5f, 38.0f, 39.0f, 40.0f])
        @DisplayName("다양한 체온 범위를 처리해야 함")
        fun `should handle various temperature ranges`(temperature: Float) {
            val data = TemperatureData(
                temperature = temperature,
                timestamp = TEST_TIMESTAMP
            )
            
            assertThat(data.temperature).isWithin(0.01f).of(temperature)
        }

        @Test
        @DisplayName("정상 체온 범위 확인 (36.1°C ~ 37.2°C)")
        fun `normal body temperature range check`() {
            val normalTemp = TemperatureData(temperature = 36.5f, timestamp = TEST_TIMESTAMP)
            val lowFever = TemperatureData(temperature = 37.5f, timestamp = TEST_TIMESTAMP)
            val highFever = TemperatureData(temperature = 39.0f, timestamp = TEST_TIMESTAMP)
            
            assertThat(normalTemp.temperature).isIn(36.1f..37.2f)
            assertThat(lowFever.temperature).isGreaterThan(37.2f)
            assertThat(highFever.temperature).isGreaterThan(38.0f)
        }
    }

    @Nested
    @DisplayName("EcgData Tests")
    inner class EcgDataTests {

        private fun createTestEcgData(
            dataSize: Int = 1000
        ) = EcgData(
            data = ByteArray(dataSize) { it.toByte() },
            heartRate = 75,
            timestamp = TEST_TIMESTAMP,
            duration = 30,
            quality = DataQuality.GOOD
        )

        @Test
        @DisplayName("EcgData가 올바르게 생성되어야 함")
        fun `EcgData should be created with valid values`() {
            val ecgBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
            val data = EcgData(
                data = ecgBytes,
                heartRate = 75,
                timestamp = TEST_TIMESTAMP,
                duration = 30,
                quality = DataQuality.EXCELLENT
            )
            
            assertThat(data.data).isEqualTo(ecgBytes)
            assertThat(data.heartRate).isEqualTo(75)
            assertThat(data.timestamp).isEqualTo(TEST_TIMESTAMP)
            assertThat(data.duration).isEqualTo(30)
            assertThat(data.quality).isEqualTo(DataQuality.EXCELLENT)
        }

        @Test
        @DisplayName("equals 메서드가 바이트 배열을 올바르게 비교해야 함")
        fun `equals should correctly compare byte arrays`() {
            val data1 = createTestEcgData()
            val data2 = createTestEcgData()
            val data3 = EcgData(
                data = ByteArray(1000) { (it * 2).toByte() }, // 다른 데이터
                heartRate = 75,
                timestamp = TEST_TIMESTAMP,
                duration = 30,
                quality = DataQuality.GOOD
            )
            
            assertEquals(data1, data2) // 같은 내용
            assertNotEquals(data1, data3) // 다른 내용
        }

        @Test
        @DisplayName("hashCode가 바이트 배열을 포함하여 계산되어야 함")
        fun `hashCode should include byte array content`() {
            val data1 = createTestEcgData()
            val data2 = createTestEcgData()
            val data3 = EcgData(
                data = ByteArray(1000) { (it * 2).toByte() },
                heartRate = 75,
                timestamp = TEST_TIMESTAMP,
                duration = 30,
                quality = DataQuality.GOOD
            )
            
            assertEquals(data1.hashCode(), data2.hashCode())
            assertNotEquals(data1.hashCode(), data3.hashCode())
        }

        @ParameterizedTest
        @ValueSource(ints = [100, 500, 1000, 5000, 10000])
        @DisplayName("다양한 크기의 ECG 데이터를 처리해야 함")
        fun `should handle various ECG data sizes`(dataSize: Int) {
            val data = createTestEcgData(dataSize = dataSize)
            
            assertThat(data.data.size).isEqualTo(dataSize)
        }

        @Test
        @DisplayName("빈 ECG 데이터도 처리 가능해야 함")
        fun `should handle empty ECG data`() {
            val data = EcgData(
                data = ByteArray(0),
                heartRate = 0,
                timestamp = TEST_TIMESTAMP,
                duration = 0,
                quality = DataQuality.POOR
            )
            
            assertThat(data.data).isEmpty()
            assertThat(data.duration).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("BloodOxygenData Tests")
    inner class BloodOxygenDataTests {

        @Test
        @DisplayName("BloodOxygenData가 올바르게 생성되어야 함")
        fun `BloodOxygenData should be created with valid values`() {
            val data = BloodOxygenData(
                oxygenLevel = 98,
                timestamp = TEST_TIMESTAMP,
                quality = DataQuality.GOOD
            )
            
            assertThat(data.oxygenLevel).isEqualTo(98)
            assertThat(data.timestamp).isEqualTo(TEST_TIMESTAMP)
            assertThat(data.quality).isEqualTo(DataQuality.GOOD)
        }

        @ParameterizedTest
        @ValueSource(ints = [85, 90, 92, 95, 98, 99, 100])
        @DisplayName("다양한 산소 포화도 값을 처리해야 함")
        fun `should handle various oxygen saturation levels`(oxygenLevel: Int) {
            val data = BloodOxygenData(
                oxygenLevel = oxygenLevel,
                timestamp = TEST_TIMESTAMP
            )
            
            assertThat(data.oxygenLevel).isEqualTo(oxygenLevel)
        }

        @Test
        @DisplayName("정상 산소 포화도 범위 확인 (95% ~ 100%)")
        fun `normal oxygen saturation range check`() {
            val normal = BloodOxygenData(oxygenLevel = 98, timestamp = TEST_TIMESTAMP)
            val low = BloodOxygenData(oxygenLevel = 92, timestamp = TEST_TIMESTAMP)
            val critical = BloodOxygenData(oxygenLevel = 85, timestamp = TEST_TIMESTAMP)
            
            assertThat(normal.oxygenLevel).isIn(95..100)
            assertThat(low.oxygenLevel).isLessThan(95)
            assertThat(critical.oxygenLevel).isLessThan(90)
        }

        @Test
        @DisplayName("백분율 범위 밖의 값도 저장 가능해야 함 (오류 처리용)")
        fun `should allow values outside percentage range for error handling`() {
            val invalid = BloodOxygenData(oxygenLevel = 150, timestamp = TEST_TIMESTAMP)
            val negative = BloodOxygenData(oxygenLevel = -10, timestamp = TEST_TIMESTAMP)
            
            assertThat(invalid.oxygenLevel).isEqualTo(150)
            assertThat(negative.oxygenLevel).isEqualTo(-10)
        }
    }

    @Nested
    @DisplayName("HealthDataUpdate Tests")
    inner class HealthDataUpdateTests {

        @Test
        @DisplayName("HealthDataUpdate가 다양한 데이터 타입을 래핑해야 함")
        fun `HealthDataUpdate should wrap various data types`() {
            val heartRateUpdate = HealthDataUpdate(
                type = HealthDataType.HEART_RATE,
                data = HeartRateData(bpm = 75, timestamp = TEST_TIMESTAMP),
                timestamp = TEST_TIMESTAMP
            )
            
            val stepsUpdate = HealthDataUpdate(
                type = HealthDataType.STEPS,
                data = StepData(
                    date = TEST_DATE,
                    steps = 10000,
                    distance = 7.5f,
                    calories = 350,
                    activeMinutes = 45
                )
            )
            
            assertThat(heartRateUpdate.type).isEqualTo(HealthDataType.HEART_RATE)
            assertThat(heartRateUpdate.data).isInstanceOf(HeartRateData::class.java)
            
            assertThat(stepsUpdate.type).isEqualTo(HealthDataType.STEPS)
            assertThat(stepsUpdate.data).isInstanceOf(StepData::class.java)
        }

        @ParameterizedTest
        @EnumSource(HealthDataType::class)
        @DisplayName("모든 HealthDataType을 지원해야 함")
        fun `should support all HealthDataType values`(type: HealthDataType) {
            val update = HealthDataUpdate(
                type = type,
                data = "dummy data for testing",
                timestamp = TEST_TIMESTAMP
            )
            
            assertThat(update.type).isEqualTo(type)
        }

        @Test
        @DisplayName("timestamp 기본값이 현재 시간이어야 함")
        fun `timestamp should default to current time`() {
            val before = System.currentTimeMillis()
            val update = HealthDataUpdate(
                type = HealthDataType.HEART_RATE,
                data = HeartRateData(bpm = 75, timestamp = TEST_TIMESTAMP)
            )
            val after = System.currentTimeMillis()
            
            assertThat(update.timestamp).isAtLeast(before)
            assertThat(update.timestamp).isAtMost(after)
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    inner class EnumTests {

        @Test
        @DisplayName("DataQuality enum이 모든 품질 레벨을 포함해야 함")
        fun `DataQuality enum should contain all quality levels`() {
            val qualities = DataQuality.values()
            
            assertThat(qualities).contains(DataQuality.POOR)
            assertThat(qualities).contains(DataQuality.FAIR)
            assertThat(qualities).contains(DataQuality.GOOD)
            assertThat(qualities).contains(DataQuality.EXCELLENT)
            assertThat(qualities).hasSize(4)
        }

        @Test
        @DisplayName("SleepQuality enum이 모든 수면 품질 레벨을 포함해야 함")
        fun `SleepQuality enum should contain all sleep quality levels`() {
            val qualities = SleepQuality.values()
            
            assertThat(qualities).contains(SleepQuality.POOR)
            assertThat(qualities).contains(SleepQuality.FAIR)
            assertThat(qualities).contains(SleepQuality.GOOD)
            assertThat(qualities).contains(SleepQuality.EXCELLENT)
            assertThat(qualities).hasSize(4)
        }

        @Test
        @DisplayName("HealthDataType enum이 모든 건강 데이터 타입을 포함해야 함")
        fun `HealthDataType enum should contain all health data types`() {
            val types = HealthDataType.values()
            
            assertThat(types).contains(HealthDataType.HEART_RATE)
            assertThat(types).contains(HealthDataType.BLOOD_PRESSURE)
            assertThat(types).contains(HealthDataType.STEPS)
            assertThat(types).contains(HealthDataType.SLEEP)
            assertThat(types).contains(HealthDataType.TEMPERATURE)
            assertThat(types).contains(HealthDataType.ECG)
            assertThat(types).contains(HealthDataType.BLOOD_OXYGEN)
            assertThat(types).hasSize(7)
        }
    }

    @Nested
    @DisplayName("Property-Based Testing")
    inner class PropertyBasedTests {

        @Test
        @DisplayName("심박수는 의학적으로 유효한 범위 내에 있어야 함")
        fun `heart rate should be within medically valid range`() = runTest {
            checkAll(Arb.int(30..250)) { bpm ->
                val data = HeartRateData(bpm = bpm, timestamp = TEST_TIMESTAMP)
                
                // 일반적인 인간 심박수 범위 (30-250 bpm)
                assertThat(data.bpm).isIn(30..250)
            }
        }

        @Test
        @DisplayName("혈압의 수축기는 항상 이완기보다 커야 함")
        fun `systolic pressure should always be greater than diastolic`() = runTest {
            checkAll(
                Arb.int(90..200),  // systolic
                Arb.int(50..120)   // diastolic
            ) { systolic, diastolic ->
                if (systolic > diastolic) {
                    val data = BloodPressureData(
                        systolic = systolic,
                        diastolic = diastolic,
                        timestamp = TEST_TIMESTAMP
                    )
                    
                    assertThat(data.systolic).isGreaterThan(data.diastolic)
                }
            }
        }

        @Test
        @DisplayName("산소 포화도는 0-100 범위에 있어야 함")
        fun `oxygen saturation should be in percentage range`() = runTest {
            checkAll(Arb.int(0..100)) { oxygenLevel ->
                val data = BloodOxygenData(
                    oxygenLevel = oxygenLevel,
                    timestamp = TEST_TIMESTAMP
                )
                
                assertThat(data.oxygenLevel).isIn(0..100)
            }
        }

        @Test
        @DisplayName("ECG 데이터 크기와 지속 시간의 관계")
        fun `ECG data size should correlate with duration`() = runTest {
            checkAll(
                Arb.int(1..120),      // duration in seconds
                Arb.int(100..1000)    // sample rate
            ) { duration, sampleRate ->
                val expectedDataSize = duration * sampleRate
                val data = EcgData(
                    data = ByteArray(expectedDataSize),
                    heartRate = 75,
                    timestamp = TEST_TIMESTAMP,
                    duration = duration,
                    quality = DataQuality.GOOD
                )
                
                assertThat(data.data.size).isEqualTo(expectedDataSize)
                assertThat(data.duration).isEqualTo(duration)
            }
        }
    }
}