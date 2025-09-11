package com.wishring.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wishring.app.core.util.Constants
import com.wishring.app.core.util.DateUtils

/**
 * Entity representing BLE device reset events
 * Tracks when the WISH RING device was reset
 */
@Entity(tableName = Constants.TABLE_RESET_LOGS)
data class ResetLogEntity(
    /**
     * Auto-generated primary key
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    /**
     * Date when reset occurred (yyyy-MM-dd format)
     */
    @ColumnInfo(name = "date")
    val date: String = DateUtils.getTodayString(),
    
    /**
     * Timestamp when reset occurred
     */
    @ColumnInfo(name = "reset_time")
    val resetTime: Long = DateUtils.getCurrentTimestamp(),
    
    /**
     * Counter value before reset
     */
    @ColumnInfo(name = "count_before_reset")
    val countBeforeReset: Int = 0,
    
    /**
     * Reset type/reason (manual, auto, error, etc.)
     */
    @ColumnInfo(name = "reset_type")
    val resetType: String = ResetType.MANUAL.name,
    
    /**
     * Additional notes or context
     */
    @ColumnInfo(name = "notes")
    val notes: String? = null
) {
    
    /**
     * Get formatted reset time for display
     * @return Formatted time string
     */
    fun getFormattedResetTime(): String {
        return DateUtils.formatTime(resetTime)
    }
    
    /**
     * Get formatted reset datetime for display
     * @return Formatted datetime string
     */
    fun getFormattedResetDateTime(): String {
        return DateUtils.formatTimestamp(resetTime)
    }
    
    /**
     * Check if this was a significant reset (lost progress)
     * @return True if count before reset was > 0
     */
    fun wasSignificantReset(): Boolean {
        return countBeforeReset > 0
    }
    
    /**
     * Get human-readable reset type
     * @return Localized reset type string
     */
    fun getResetTypeDisplay(): String {
        return when (resetType) {
            ResetType.MANUAL.name -> "수동 리셋"
            ResetType.AUTO.name -> "자동 리셋"
            ResetType.ERROR.name -> "오류로 인한 리셋"
            ResetType.BATTERY.name -> "배터리 방전"
            ResetType.MIDNIGHT.name -> "자정 리셋"
            else -> "알 수 없음"
        }
    }
    
    companion object {
        /**
         * Create a reset log entry
         * @param countBeforeReset Count value before reset
         * @param resetType Type of reset
         * @param notes Optional notes
         * @return New ResetLogEntity
         */
        fun create(
            countBeforeReset: Int,
            resetType: ResetType = ResetType.MANUAL,
            notes: String? = null
        ): ResetLogEntity {
            return ResetLogEntity(
                date = DateUtils.getTodayString(),
                resetTime = DateUtils.getCurrentTimestamp(),
                countBeforeReset = countBeforeReset,
                resetType = resetType.name,
                notes = notes
            )
        }
    }
}

/**
 * Enum representing different types of reset events
 */
enum class ResetType {
    /**
     * Manual reset by user pressing device button
     */
    MANUAL,
    
    /**
     * Daily automatic reset (once per day)
     */
    DAILY,
    
    /**
     * Automatic reset at midnight
     */
    MIDNIGHT,
    
    /**
     * Reset due to battery depletion
     */
    BATTERY,
    
    /**
     * Automatic reset by system
     */
    AUTO,
    
    /**
     * Emergency reset due to critical condition
     */
    EMERGENCY,
    
    /**
     * Reset due to error condition
     */
    ERROR,
    
    /**
     * Unknown reset reason
     */
    UNKNOWN
}