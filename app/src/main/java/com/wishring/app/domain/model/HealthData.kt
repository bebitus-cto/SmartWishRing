package com.wishring.app.domain.model

/**
 * Health data models for MRD SDK integration
 */

/**
 * Heart rate data
 */
data class HeartRateData(
    val bpm: Int,
    val timestamp: Long,
    val quality: DataQuality = DataQuality.GOOD
)

/**
 * Blood pressure data
 */
data class BloodPressureData(
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Long,
    val quality: DataQuality = DataQuality.GOOD
)

/**
 * Sleep data
 */
data class SleepData(
    val date: String,
    val totalSleepMinutes: Int,
    val deepSleepMinutes: Int,
    val lightSleepMinutes: Int,
    val remSleepMinutes: Int,
    val awakeMinutes: Int,
    val sleepQuality: SleepQuality,
    val bedTime: String,
    val wakeTime: String
)

/**
 * Step data
 */
data class StepData(
    val date: String,
    val steps: Int,
    val distance: Float, // in km
    val calories: Int,
    val activeMinutes: Int
)

/**
 * Temperature data
 */
data class TemperatureData(
    val temperature: Float, // in celsius
    val timestamp: Long,
    val quality: DataQuality = DataQuality.GOOD
)

/**
 * ECG data
 */
data class EcgData(
    val data: ByteArray,
    val heartRate: Int,
    val timestamp: Long,
    val duration: Int, // in seconds
    val quality: DataQuality = DataQuality.GOOD
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EcgData
        return data.contentEquals(other.data) && heartRate == other.heartRate && timestamp == other.timestamp
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + heartRate
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Blood oxygen data
 */
data class BloodOxygenData(
    val oxygenLevel: Int, // percentage
    val timestamp: Long,
    val quality: DataQuality = DataQuality.GOOD
)

/**
 * Health data update wrapper
 */
data class HealthDataUpdate(
    val type: HealthDataType,
    val data: Any,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data quality enum
 */
enum class DataQuality {
    POOR, FAIR, GOOD, EXCELLENT
}

/**
 * Sleep quality enum
 */
enum class SleepQuality {
    POOR, FAIR, GOOD, EXCELLENT
}

/**
 * Health data type enum
 */
enum class HealthDataType {
    HEART_RATE, BLOOD_PRESSURE, STEPS, SLEEP, TEMPERATURE, ECG, BLOOD_OXYGEN
}