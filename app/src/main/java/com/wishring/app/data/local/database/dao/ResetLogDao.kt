package com.wishring.app.data.local.database.dao

import androidx.room.*
import com.wishring.app.core.base.BaseDao
import com.wishring.app.core.util.Constants
import com.wishring.app.data.local.database.entity.ResetLogEntity
import com.wishring.app.data.local.database.entity.ResetType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reset_logs table
 * Extends BaseDao for common CRUD operations
 */
@Dao
interface ResetLogDao : BaseDao<ResetLogEntity> {
    
    /**
     * Get all reset logs
     * @return Flow of all reset logs ordered by time
     */
    @Query("SELECT * FROM ${Constants.TABLE_RESET_LOGS} ORDER BY reset_time DESC")
    fun getAllLogs(): Flow<List<ResetLogEntity>>
    
    /**
     * Get reset logs for specific date
     * @param date Date in yyyy-MM-dd format
     * @return List of reset logs for the date
     */
    @Query("SELECT * FROM ${Constants.TABLE_RESET_LOGS} WHERE date = :date ORDER BY reset_time DESC")
    suspend fun getLogsByDate(date: String): List<ResetLogEntity>
    
    /**
     * Get reset logs between dates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of reset logs between dates
     */
    @Query("SELECT * FROM ${Constants.TABLE_RESET_LOGS} WHERE date BETWEEN :startDate AND :endDate ORDER BY reset_time DESC")
    suspend fun getLogsBetween(startDate: String, endDate: String): List<ResetLogEntity>
    
    /**
     * Get recent reset logs
     * @param limit Number of logs to fetch
     * @return List of recent reset logs
     */
    @Query("SELECT * FROM ${Constants.TABLE_RESET_LOGS} ORDER BY reset_time DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 10): List<ResetLogEntity>
    
    /**
     * Get reset logs by type
     * @param resetType Type of reset
     * @return Flow of reset logs of specified type
     */
    @Query("SELECT * FROM ${Constants.TABLE_RESET_LOGS} WHERE reset_type = :resetType ORDER BY reset_time DESC")
    fun getLogsByType(resetType: String): Flow<List<ResetLogEntity>>
    
    /**
     * Get today's reset logs
     * @return List of today's reset logs
     */
    @Query("SELECT * FROM ${Constants.TABLE_RESET_LOGS} WHERE date = date('now', 'localtime') ORDER BY reset_time DESC")
    suspend fun getTodayLogs(): List<ResetLogEntity>
    
    /**
     * Get significant reset logs (where count was > 0)
     * @return Flow of significant reset logs
     */
    @Query("SELECT * FROM ${Constants.TABLE_RESET_LOGS} WHERE count_before_reset > 0 ORDER BY reset_time DESC")
    fun getSignificantLogs(): Flow<List<ResetLogEntity>>
    
    /**
     * Get last reset log
     * @return Most recent reset log or null
     */
    @Query("SELECT * FROM ${Constants.TABLE_RESET_LOGS} ORDER BY reset_time DESC LIMIT 1")
    suspend fun getLastReset(): ResetLogEntity?
    
    /**
     * Get total lost count from all resets
     * @return Sum of all count_before_reset values
     */
    @Query("SELECT SUM(count_before_reset) FROM ${Constants.TABLE_RESET_LOGS}")
    suspend fun getTotalLostCount(): Int?
    
    /**
     * Get reset count for today
     * @return Number of resets today
     */
    @Query("SELECT COUNT(*) FROM ${Constants.TABLE_RESET_LOGS} WHERE date = date('now', 'localtime')")
    suspend fun getTodayResetCount(): Int
    
    /**
     * Get reset count by type
     * @param resetType Type of reset
     * @return Number of resets of specified type
     */
    @Query("SELECT COUNT(*) FROM ${Constants.TABLE_RESET_LOGS} WHERE reset_type = :resetType")
    suspend fun getResetCountByType(resetType: String): Int
    
    /**
     * Delete logs older than specified date
     * @param date Cutoff date
     * @return Number of deleted logs
     */
    @Query("DELETE FROM ${Constants.TABLE_RESET_LOGS} WHERE date < :date")
    suspend fun deleteOlderThan(date: String): Int
    
    /**
     * Check if reset occurred today
     * @return True if any reset occurred today
     */
    @Query("SELECT EXISTS(SELECT 1 FROM ${Constants.TABLE_RESET_LOGS} WHERE date = date('now', 'localtime') LIMIT 1)")
    suspend fun hasResetToday(): Boolean
    
    /**
     * Get reset statistics
     * Returns count grouped by reset type
     */
    @Query("""
        SELECT reset_type, COUNT(*) as count 
        FROM ${Constants.TABLE_RESET_LOGS} 
        GROUP BY reset_type
    """)
    suspend fun getResetStatistics(): List<ResetStatistics>
}

/**
 * Data class for reset statistics query result
 */
data class ResetStatistics(
    @ColumnInfo(name = "reset_type")
    val resetType: String,
    
    @ColumnInfo(name = "count")
    val count: Int
)