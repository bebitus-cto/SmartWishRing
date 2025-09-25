package com.wishring.app.data.model

import com.wishring.app.core.util.Constants
import com.wishring.app.core.util.DateUtils
import com.wishring.app.data.local.database.entity.WishEntity

/**
 * Domain model for daily wish count
 * Represents a single day's wish count and progress
 */
data class WishUiState(
    val date: String,
    val wishText: String,
    val targetCount: Int,
    val currentCount: Int,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {


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
    fun incrementCount(by: Int = 1): WishUiState {
        val newCount = (targetCount + by).coerceAtMost(Constants.MAX_DAILY_COUNT)
        return copy(
            targetCount = newCount,
            isCompleted = newCount >= currentCount,
            updatedAt = DateUtils.getCurrentTimestamp()
        )
    }

    /**
     * Update wish text and target
     */
    fun updateWishAndTarget(newWishText: String? = null, newTargetCount: Int? = null): WishUiState {
        return copy(
            wishText = newWishText ?: wishText,
            currentCount = newTargetCount ?: currentCount,
            isCompleted = targetCount >= (newTargetCount ?: currentCount),
            updatedAt = DateUtils.getCurrentTimestamp()
        )
    }

    companion object {
        /**
         * Create default WishCount for today
         */
        fun createDefault(
            date: String = DateUtils.getTodayString(),
            wishText: String = Constants.DEFAULT_WISH_TEXT
        ): WishUiState {
            val now = DateUtils.getCurrentTimestamp()
            return WishUiState(
                date = date,
                wishText = wishText,
                isCompleted = false,
                createdAt = now,
                updatedAt = now,
                targetCount = 1000,
                currentCount = 0
            )
        }

        /**
         * Create from Entity
         */
        fun fromEntity(entity: WishEntity): WishUiState {
            return WishUiState(
                date = entity.date,
                targetCount = entity.totalCount,
                wishText = entity.wishText,
                currentCount = entity.targetCount,
                isCompleted = entity.isCompleted,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }

        /**
         * Create from PastWishUiState
         */
        fun fromWishDay(wishDay: WishDayUiState): WishUiState {
            val now = DateUtils.getCurrentTimestamp()
            return WishUiState(
                date = wishDay.dateString,
                wishText = wishDay.wishText,
                targetCount = wishDay.targetCount,
                currentCount = wishDay.completedCount,
                isCompleted = wishDay.isCompleted,
                createdAt = now,
                updatedAt = now
            )
        }

    }
}

/**
 * Extension function to convert Domain Model to Entity
 */
fun WishUiState.toEntity(): WishEntity {
    return WishEntity(
        date = date,
        totalCount = targetCount,
        wishText = wishText,
        targetCount = currentCount,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}