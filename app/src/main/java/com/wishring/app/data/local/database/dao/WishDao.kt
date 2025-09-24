package com.wishring.app.data.local.database.dao

import androidx.room.*
import com.wishring.app.core.base.BaseDao
import com.wishring.app.core.util.DateUtils
import com.wishring.app.data.local.database.entity.WishEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for wish_counts table
 * Extends BaseDao for common CRUD operations
 */
@Dao
interface WishDao : BaseDao<WishEntity> {
    
    /**
     * Get wish count by date
     * @param date Date string in yyyy-MM-dd format
     * @return WishCountEntity or null if not found
     */
    @Query("SELECT * FROM wish_counts WHERE date = :date")
    suspend fun getByDate(date: String): WishEntity?
    
    /**
     * Get today's wish count
     * @return Today's WishCountEntity or null
     */
    @Query("SELECT * FROM wish_counts WHERE date = date('now', 'localtime')")
    suspend fun getTodayRecord(): WishEntity?
    
    /**
     * Get today's count as Flow
     * @return Flow emitting today's count
     */
    @Query("SELECT total_count FROM wish_counts WHERE date = date('now', 'localtime')")
    fun getTodayCount(): Flow<Int?>
    
    /**
     * Observe wish count by date as Flow
     * @param date Date string in yyyy-MM-dd format
     * @return Flow emitting WishCountEntity for the date
     */
    @Query("SELECT * FROM wish_counts WHERE date = :date")
    fun observeByDate(date: String): Flow<WishEntity?>
    
    /**
     * Get recent records
     * @param limit Number of records to fetch
     * @return Flow of recent records
     */
    @Query("SELECT * FROM wish_counts ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 30): Flow<List<WishEntity>>
    
    /**
     * Get all records
     * @return Flow of all records
     */
    @Query("SELECT * FROM wish_counts ORDER BY date DESC")
    fun getAllRecords(): Flow<List<WishEntity>>

    
    /**
     * Get all records synchronously (suspend function)
     * @return List of all records
     */
    @Query("SELECT * FROM wish_counts ORDER BY date DESC")
    suspend fun getAllRecordsSync(): List<WishEntity>
    
    /**
     * Get records between dates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of records between dates
     */
    @Query("SELECT * FROM wish_counts WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getRecordsBetween(startDate: String, endDate: String): List<WishEntity>
    
    /**
     * Update count for specific date
     * @param date Date to update
     * @param count New count value
     * @param updatedAt Update timestamp
     */
    @Query("UPDATE wish_counts SET total_count = :count, updated_at = :updatedAt WHERE date = :date")
    suspend fun updateCount(date: String, count: Int, updatedAt: Long = DateUtils.getCurrentTimestamp())
    
    /**
     * Increment today's count
     * @param increment Amount to increment (default 1)
     */
    @Transaction
    suspend fun incrementTodayCount(increment: Int = 1) {
        val today = DateUtils.getTodayString()
        val entity = getByDate(today) ?: WishEntity.createForToday()
        insert(entity.incrementCount(increment))
    }
    
    /**
     * Update wish and target for today
     * @param wishText New wish text
     * @param targetCount New target count
     */
    @Transaction
    suspend fun updateTodayWish(wishText: String, targetCount: Int) {
        val today = DateUtils.getTodayString()
        val entity = getByDate(today) ?: WishEntity.createForToday()
        insert(entity.updateWishAndTarget(wishText, targetCount))
    }
    
    /**
     * Update completion status
     * @param date Date to update
     * @param completed Completion status
     */
    @Query("UPDATE wish_counts SET is_completed = :completed WHERE date = :date")
    suspend fun updateCompletionStatus(date: String, completed: Boolean)
    
    /**
     * Delete records older than specified date
     * @param date Cutoff date
     * @return Number of deleted records
     */
    @Query("DELETE FROM wish_counts WHERE date < :date")
    suspend fun deleteOlderThan(date: String): Int
    
    /**
     * Get total count across all days
     * @return Total count sum
     */
    @Query("SELECT SUM(total_count) FROM wish_counts")
    suspend fun getTotalCountAllTime(): Int?
    
    /**
     * Get average daily count
     * @return Average count per day
     */
    @Query("SELECT AVG(total_count) FROM wish_counts")
    suspend fun getAverageDailyCount(): Float?
    
    /**
     * Get days with completed goals
     * @return Number of days with completed goals
     */
    @Query("SELECT COUNT(*) FROM wish_counts WHERE is_completed = 1")
    suspend fun getCompletedDaysCount(): Int
    
    /**
     * Get current streak (consecutive days with records)
     * This is a simplified version - for complex streak calculation,
     * implement in repository layer
     */
    @Query("""
        SELECT COUNT(*) FROM wish_counts
        WHERE date >= date('now', '-' || (
            SELECT COUNT(*) FROM wish_counts
            WHERE date <= date('now', 'localtime')
            AND date >= date('now', '-30 days')
        ) || ' days')
    """)
    suspend fun getCurrentStreak(): Int
    
    /**
     * Delete a specific wish count record by date
     * @param date Date of the record to delete
     * @return Number of deleted records (0 or 1)
     */
    @Query("DELETE FROM wish_counts WHERE date = :date")
    suspend fun deleteWishCount(date: String): Int
}