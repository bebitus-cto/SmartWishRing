package com.wishring.app.domain.model

import com.wishring.app.core.util.Constants
import com.wishring.app.core.util.DateUtils
import com.wishring.app.data.local.database.entity.WishCountEntity

/**
 * Domain model for daily wish count
 * Represents a single day's wish count and progress
 */
data class WishCount(
    val date: String,
    val totalCount: Int,
    val wishText: String,
    val targetCount: Int,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * Calculate progress as percentage (0-100)
     */
    val progressPercentage: Int
        get() = if (targetCount > 0) {
            ((totalCount.toFloat() / targetCount) * 100).coerceIn(0f, 100f).toInt()
        } else {
            0
        }
    
    /**
     * Calculate progress as float (0.0-1.0)
     */
    val progress: Float
        get() = if (targetCount > 0) {
            (totalCount.toFloat() / targetCount).coerceIn(0f, 1f)
        } else {
            0f
        }
    
    /**
     * Get remaining count to reach target
     */
    val remainingCount: Int
        get() = (targetCount - totalCount).coerceAtLeast(0)
    
    /**
     * Check if this is today's record
     */
    val isToday: Boolean
        get() = DateUtils.isToday(date)
    
    /**
     * Get formatted date for display
     */
    val displayDate: String
        get() = DateUtils.toDisplayFormat(date)
    
    /**
     * Get relative date string (오늘, 어제, N일 전, etc.)
     */
    val relativeDateString: String
        get() = DateUtils.getRelativeDateString(date)
    
    /**
     * Increment count by specified amount
     */
    fun incrementCount(by: Int = 1): WishCount {
        val newCount = (totalCount + by).coerceAtMost(Constants.MAX_DAILY_COUNT)
        return copy(
            totalCount = newCount,
            isCompleted = newCount >= targetCount,
            updatedAt = DateUtils.getCurrentTimestamp()
        )
    }
    
    /**
     * Update wish text and target
     */
    fun updateWishAndTarget(newWishText: String? = null, newTargetCount: Int? = null): WishCount {
        return copy(
            wishText = newWishText ?: wishText,
            targetCount = newTargetCount ?: targetCount,
            isCompleted = totalCount >= (newTargetCount ?: targetCount),
            updatedAt = DateUtils.getCurrentTimestamp()
        )
    }
    
    companion object {
        /**
         * Create default WishCount for today
         */
        fun createDefault(
            date: String = DateUtils.getTodayString(),
            wishText: String = Constants.DEFAULT_WISH_TEXT,
            targetCount: Int = Constants.DEFAULT_TARGET_COUNT
        ): WishCount {
            val now = DateUtils.getCurrentTimestamp()
            return WishCount(
                date = date,
                totalCount = 0,
                wishText = wishText,
                targetCount = targetCount,
                isCompleted = false,
                createdAt = now,
                updatedAt = now
            )
        }
        
        /**
         * Create from Entity
         */
        fun fromEntity(entity: WishCountEntity): WishCount {
            return WishCount(
                date = entity.date,
                totalCount = entity.totalCount,
                wishText = entity.wishText,
                targetCount = entity.targetCount,
                isCompleted = entity.isCompleted,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

/**
 * Extension function to convert Domain Model to Entity
 */
fun WishCount.toEntity(): WishCountEntity {
    return WishCountEntity(
        date = date,
        totalCount = totalCount,
        wishText = wishText,
        targetCount = targetCount,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}