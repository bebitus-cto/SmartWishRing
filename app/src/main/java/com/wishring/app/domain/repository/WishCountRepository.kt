package com.wishring.app.domain.repository

import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.model.WishCount
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for WishCount domain operations
 * Defines contract between domain and data layers
 */
interface WishCountRepository {
    
    /**
     * Get today's wish count
     * Creates new record if doesn't exist
     */
    suspend fun getTodayWishCount(): WishCount
    
    /**
     * Get wish count for specific date
     * @param date Date in yyyy-MM-dd format
     * @return WishCount or null if not found
     */
    suspend fun getWishCountByDate(date: String): WishCount?
    
    /**
     * Get all wish counts as Flow
     * @return Flow of all wish counts ordered by date desc
     */
    fun getAllWishCounts(): Flow<List<WishCount>>
    
    /**
     * Get wish counts for date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of wish counts
     */
    suspend fun getWishCountsBetween(startDate: String, endDate: String): List<WishCount>
    
    /**
     * Get recent wish counts
     * @param limit Number of records to fetch
     * @return List of recent wish counts
     */
    suspend fun getRecentWishCounts(limit: Int = 7): List<WishCount>
    
    /**
     * Get daily records with reset information
     * @param limit Number of records to fetch
     * @return List of daily records
     */
    suspend fun getDailyRecords(limit: Int = 30): List<DailyRecord>
    
    /**
     * Get daily record for specific date
     * @param date Date in yyyy-MM-dd format
     * @return DailyRecord with reset info
     */
    suspend fun getDailyRecord(date: String): DailyRecord?
    
    /**
     * Get statistics for date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return WishCountStatistics
     */
    suspend fun getStatistics(startDate: String, endDate: String): WishCountStatistics
    
    /**
     * Insert or update wish count
     * @param wishCount WishCount to save
     * @return Saved WishCount with ID
     */
    suspend fun saveWishCount(wishCount: WishCount): WishCount
    
    /**
     * Increment today's count
     * @param amount Amount to increment (default 1)
     * @return Updated WishCount
     */
    suspend fun incrementTodayCount(amount: Int = 1): WishCount
    
    /**
     * Update today's wish and target
     * @param wishText New wish text (optional)
     * @param targetCount New target count (optional)
     * @return Updated WishCount
     */
    suspend fun updateTodayWishAndTarget(
        wishText: String? = null,
        targetCount: Int? = null
    ): WishCount
    
    /**
     * Reset today's count
     * @param reason Reset reason
     * @return New WishCount after reset
     */
    suspend fun resetTodayCount(reason: String? = null): WishCount
    
    /**
     * Check if today's target is completed
     * @return True if completed
     */
    suspend fun isTodayCompleted(): Boolean
    
    /**
     * Get achievement rate for date range
     * @param startDate Start date
     * @param endDate End date
     * @return Achievement rate (0.0 - 1.0)
     */
    suspend fun getAchievementRate(startDate: String, endDate: String): Float
    
    /**
     * Get streak information
     * @return StreakInfo with current and best streaks
     */
    suspend fun getStreakInfo(): StreakInfo
    
    /**
     * Delete wish count
     * @param date Date of record to delete
     * @return True if deleted
     */
    suspend fun deleteWishCount(date: String): Boolean
    
    /**
     * Delete old records
     * @param beforeDate Delete records before this date
     * @return Number of deleted records
     */
    suspend fun deleteOldRecords(beforeDate: String): Int
    
    /**
     * Observe today's wish count
     * @return Flow of today's WishCount
     */
    fun observeTodayWishCount(): Flow<WishCount?>
    
    /**
     * Observe recent wish counts
     * @param limit Number of records
     * @return Flow of recent wish counts
     */
    fun observeRecentWishCounts(limit: Int = 7): Flow<List<WishCount>>
}

/**
 * Statistics data class
 */
data class WishCountStatistics(
    val totalDays: Int,
    val completedDays: Int,
    val totalCount: Int,
    val totalTarget: Int,
    val averageCount: Float,
    val averageTarget: Float,
    val completionRate: Float,
    val bestDay: WishCount?,
    val worstDay: WishCount?
)

/**
 * Streak information data class
 */
data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val lastActiveDate: String?,
    val streakStartDate: String?,
    val isActiveToday: Boolean
)