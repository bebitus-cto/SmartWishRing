package com.wishring.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wishring.app.core.util.Constants
import com.wishring.app.core.util.DateUtils

/**
 * Entity representing daily wish count records
 * Stores user's daily progress and wish information
 */
@Entity(tableName = Constants.TABLE_WISH_COUNTS)
data class WishCountEntity(
    /**
     * Date in yyyy-MM-dd format (primary key)
     */
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String = DateUtils.getTodayString(),
    
    /**
     * Total count for the day
     */
    @ColumnInfo(name = "total_count")
    val totalCount: Int = 0,
    
    /**
     * User's wish/affirmation text
     */
    @ColumnInfo(name = "wish_text")
    val wishText: String = Constants.DEFAULT_WISH_TEXT,
    
    /**
     * Target count for the day
     */
    @ColumnInfo(name = "target_count")
    val targetCount: Int = Constants.DEFAULT_TARGET_COUNT,
    
    /**
     * Whether the target was achieved
     */
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    /**
     * Creation timestamp
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = DateUtils.getCurrentTimestamp(),
    
    /**
     * Last update timestamp
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = DateUtils.getCurrentTimestamp()
) {
    /**
     * Calculate progress percentage
     * @return Progress as percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        return if (targetCount > 0) {
            ((totalCount.toFloat() / targetCount) * 100).coerceIn(0f, 100f).toInt()
        } else {
            0
        }
    }
    
    /**
     * Calculate progress as float
     * @return Progress as float (0.0-1.0)
     */
    fun getProgress(): Float {
        return if (targetCount > 0) {
            (totalCount.toFloat() / targetCount).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * Check if target is achieved
     * @return True if current count >= target count
     */
    fun isTargetAchieved(): Boolean {
        return totalCount >= targetCount
    }
    
    /**
     * Get remaining count to target
     * @return Remaining count (0 if target achieved)
     */
    fun getRemainingCount(): Int {
        return (targetCount - totalCount).coerceAtLeast(0)
    }
    
    /**
     * Create a copy with incremented count
     * @param incrementBy Amount to increment (default 1)
     * @return Updated entity with new count
     */
    fun incrementCount(incrementBy: Int = 1): WishCountEntity {
        val newCount = (totalCount + incrementBy).coerceAtMost(Constants.MAX_DAILY_COUNT)
        return copy(
            totalCount = newCount,
            isCompleted = newCount >= targetCount,
            updatedAt = DateUtils.getCurrentTimestamp()
        )
    }
    
    /**
     * Create a copy with updated wish and target
     * @param newWishText New wish text
     * @param newTargetCount New target count
     * @return Updated entity
     */
    fun updateWishAndTarget(
        newWishText: String? = null,
        newTargetCount: Int? = null
    ): WishCountEntity {
        return copy(
            wishText = newWishText ?: wishText,
            targetCount = newTargetCount ?: targetCount,
            isCompleted = totalCount >= (newTargetCount ?: targetCount),
            updatedAt = DateUtils.getCurrentTimestamp()
        )
    }
    
    companion object {
        /**
         * Create a new entity for today
         * @param wishText Optional wish text
         * @param targetCount Optional target count
         * @return New WishCountEntity for today
         */
        fun createForToday(
            wishText: String = Constants.DEFAULT_WISH_TEXT,
            targetCount: Int = Constants.DEFAULT_TARGET_COUNT
        ): WishCountEntity {
            return WishCountEntity(
                date = DateUtils.getTodayString(),
                wishText = wishText,
                targetCount = targetCount
            )
        }
    }
}