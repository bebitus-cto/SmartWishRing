package com.wishring.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wishring.app.data.local.entity.BleEventLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for BLE event logging operations
 * Primarily used for RESET event logging as per RFP.md requirements
 */
@Dao
interface BleEventDao {
    
    /**
     * Insert a new BLE event log entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventLog(eventLog: BleEventLogEntity): Long
    
    /**
     * Get all RESET events for a specific date (YYYY-MM-DD format)
     */
    @Query("""
        SELECT * FROM ble_event_logs 
        WHERE eventType = :eventType 
        AND date(timestamp / 1000, 'unixepoch', 'localtime') = :date
        ORDER BY timestamp DESC
    """)
    suspend fun getResetEventsByDate(
        date: String,
        eventType: String = BleEventLogEntity.EVENT_TYPE_RESET
    ): List<BleEventLogEntity>
    
    /**
     * Get recent RESET events (last N events)
     */
    @Query("""
        SELECT * FROM ble_event_logs 
        WHERE eventType = :eventType 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentResetEvents(
        limit: Int = 10,
        eventType: String = BleEventLogEntity.EVENT_TYPE_RESET
    ): List<BleEventLogEntity>
    
    /**
     * Get all RESET events as Flow for reactive updates
     */
    @Query("""
        SELECT * FROM ble_event_logs 
        WHERE eventType = :eventType 
        ORDER BY timestamp DESC
    """)
    fun getResetEventsFlow(
        eventType: String = BleEventLogEntity.EVENT_TYPE_RESET
    ): Flow<List<BleEventLogEntity>>
    
    /**
     * Count RESET events for a specific date
     */
    @Query("""
        SELECT COUNT(*) FROM ble_event_logs 
        WHERE eventType = :eventType 
        AND date(timestamp / 1000, 'unixepoch', 'localtime') = :date
    """)
    suspend fun countResetEventsByDate(
        date: String,
        eventType: String = BleEventLogEntity.EVENT_TYPE_RESET
    ): Int
    
    /**
     * Delete old event logs (older than specified days)
     * For database cleanup and performance
     */
    @Query("""
        DELETE FROM ble_event_logs 
        WHERE timestamp < :cutoffTimestamp
    """)
    suspend fun deleteOldEventLogs(cutoffTimestamp: Long)
    
    /**
     * Get the latest RESET event
     */
    @Query("""
        SELECT * FROM ble_event_logs 
        WHERE eventType = :eventType 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestResetEvent(
        eventType: String = BleEventLogEntity.EVENT_TYPE_RESET
    ): BleEventLogEntity?
}