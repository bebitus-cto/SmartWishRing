package com.wishring.app.domain.repository

import com.wishring.app.data.local.database.entity.ResetType
import com.wishring.app.domain.model.ResetLog
import com.wishring.app.domain.model.ResetStatistics
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ResetLog domain operations
 * Defines contract between domain and data layers
 */
interface ResetLogRepository {
    
    /**
     * Get all reset logs as Flow
     * @return Flow of all reset logs ordered by time desc
     */
    fun getAllResetLogs(): Flow<List<ResetLog>>
    
    /**
     * Get reset logs for specific date
     * @param date Date in yyyy-MM-dd format
     * @return List of reset logs for the date
     */
    suspend fun getResetLogsByDate(date: String): List<ResetLog>
    
    /**
     * Get reset logs between dates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of reset logs
     */
    suspend fun getResetLogsBetween(startDate: String, endDate: String): List<ResetLog>
    
    /**
     * Get recent reset logs
     * @param limit Number of logs to fetch
     * @return List of recent reset logs
     */
    suspend fun getRecentResetLogs(limit: Int = 10): List<ResetLog>
    
    /**
     * Get reset logs by type
     * @param resetType Type of reset
     * @return Flow of reset logs of specified type
     */
    fun getResetLogsByType(resetType: ResetType): Flow<List<ResetLog>>
    
    /**
     * Get today's reset logs
     * @return List of today's reset logs
     */
    suspend fun getTodayResetLogs(): List<ResetLog>
    
    /**
     * Get significant reset logs (where count > 0)
     * @return Flow of significant reset logs
     */
    fun getSignificantResetLogs(): Flow<List<ResetLog>>
    
    /**
     * Get last reset log
     * @return Most recent reset log or null
     */
    suspend fun getLastReset(): ResetLog?
    
    /**
     * Get total lost count from all resets
     * @return Sum of all lost counts
     */
    suspend fun getTotalLostCount(): Int
    
    /**
     * Get reset count for today
     * @return Number of resets today
     */
    suspend fun getTodayResetCount(): Int
    
    /**
     * Get reset count by type
     * @param resetType Type of reset
     * @return Number of resets of specified type
     */
    suspend fun getResetCountByType(resetType: ResetType): Int
    
    /**
     * Check if reset occurred today
     * @return True if any reset occurred today
     */
    suspend fun hasResetToday(): Boolean
    
    /**
     * Get reset statistics
     * @return ResetStatistics with summary data
     */
    suspend fun getResetStatistics(): ResetStatistics
    
    /**
     * Get reset statistics for date range
     * @param startDate Start date
     * @param endDate End date
     * @return ResetStatistics for the period
     */
    suspend fun getResetStatisticsForPeriod(
        startDate: String,
        endDate: String
    ): ResetStatistics
    
    /**
     * Save reset log
     * @param resetLog ResetLog to save
     * @return Saved ResetLog with ID
     */
    suspend fun saveResetLog(resetLog: ResetLog): ResetLog
    
    /**
     * Log manual reset
     * @param countBeforeReset Count before reset
     * @param targetCount Target count
     * @param wishText Wish text
     * @param reason Reset reason
     * @return Created ResetLog
     */
    suspend fun logManualReset(
        countBeforeReset: Int,
        targetCount: Int,
        wishText: String,
        reason: String? = null
    ): ResetLog
    
    /**
     * Log auto reset (daily)
     * @param countBeforeReset Count before reset
     * @param targetCount Target count
     * @param wishText Wish text
     * @return Created ResetLog
     */
    suspend fun logAutoReset(
        countBeforeReset: Int,
        targetCount: Int,
        wishText: String
    ): ResetLog
    
    /**
     * Log emergency reset
     * @param countBeforeReset Count before reset
     * @param targetCount Target count
     * @param wishText Wish text
     * @param reason Emergency reason
     * @return Created ResetLog
     */
    suspend fun logEmergencyReset(
        countBeforeReset: Int,
        targetCount: Int,
        wishText: String,
        reason: String
    ): ResetLog
    
    /**
     * Delete reset log
     * @param id Reset log ID
     * @return True if deleted
     */
    suspend fun deleteResetLog(id: Long): Boolean
    
    /**
     * Delete old reset logs
     * @param beforeDate Delete logs before this date
     * @return Number of deleted logs
     */
    suspend fun deleteOldResetLogs(beforeDate: String): Int
    
    /**
     * Get reset frequency analysis
     * @return ResetFrequencyAnalysis
     */
    suspend fun getResetFrequencyAnalysis(): ResetFrequencyAnalysis
    
    /**
     * Get reset impact analysis
     * @return ResetImpactAnalysis
     */
    suspend fun getResetImpactAnalysis(): ResetImpactAnalysis
}

/**
 * Reset frequency analysis data class
 */
data class ResetFrequencyAnalysis(
    val dailyAverage: Float,
    val weeklyAverage: Float,
    val monthlyAverage: Float,
    val mostFrequentDay: String?,      // Day of week
    val mostFrequentTime: String?,     // Time of day
    val resetsByDayOfWeek: Map<String, Int>,
    val resetsByHour: Map<Int, Int>
)

/**
 * Reset impact analysis data class
 */
data class ResetImpactAnalysis(
    val totalLostCount: Int,
    val averageLostCount: Float,
    val maxLostCount: Int,
    val minLostCount: Int,
    val lostCountByType: Map<ResetType, Int>,
    val mostImpactfulReset: ResetLog?,
    val recoveryRate: Float            // How quickly user recovers after reset
)