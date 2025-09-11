package com.wishring.app.domain.model

import com.wishring.app.core.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Domain model for daily record display
 * Represents aggregated daily data for UI presentation
 * 
 * 일별 기록을 표시하기 위한 도메인 모델
 * UI 표시용 집계된 일별 데이터를 나타냄
 */
data class DailyRecord(
    val date: LocalDate,
    val totalCount: Int,
    val wishText: String,
    val targetCount: Int,
    val isCompleted: Boolean,
    val resetCount: Int = 0,
    val lostCount: Int = 0
) {
    /**
     * Format date for display (yyyy.MM.dd)
     */
    val displayDate: String
        get() = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    
    /**
     * Get date string in standard format (yyyy-MM-dd)
     */
    val dateString: String
        get() = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    
    /**
     * Calculate completion rate as percentage (0-100)
     */
    val completionRate: Int
        get() = if (targetCount > 0) {
            ((totalCount.toFloat() / targetCount) * 100).toInt().coerceIn(0, 100)
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
        get() = date == LocalDate.now()
    
    /**
     * Check if this is yesterday's record
     */
    val isYesterday: Boolean
        get() = date == LocalDate.now().minusDays(1)
    
    /**
     * Get relative date string (오늘, 어제, N일 전)
     */
    val relativeDateString: String
        get() = when {
            isToday -> "오늘"
            isYesterday -> "어제"
            else -> {
                val daysAgo = LocalDate.now().toEpochDay() - date.toEpochDay()
                "${daysAgo}일 전"
            }
        }
    
    /**
     * Get actual count (total count without lost count)
     */
    val actualCount: Int
        get() = totalCount + lostCount
    
    /**
     * Check if there were any resets
     */
    val hasResets: Boolean
        get() = resetCount > 0
    
    /**
     * Get achievement status text
     */
    val statusText: String
        get() = when {
            isCompleted -> "목표 달성 ✨"
            progress > 0.8f -> "거의 다 왔어요!"
            progress > 0.5f -> "절반 이상 달성!"
            progress > 0 -> "시작이 반이에요"
            else -> "오늘도 화이팅!"
        }
    
    companion object {
        /**
         * Create from WishCount model
         */
        fun fromWishCount(
            wishCount: WishCount,
            resetCount: Int = 0,
            lostCount: Int = 0
        ): DailyRecord {
            val localDate = LocalDate.parse(wishCount.date)
            return DailyRecord(
                date = localDate,
                totalCount = wishCount.totalCount,
                wishText = wishCount.wishText,
                targetCount = wishCount.targetCount,
                isCompleted = wishCount.isCompleted,
                resetCount = resetCount,
                lostCount = lostCount
            )
        }
        
        /**
         * Create empty record for date
         */
        fun empty(date: LocalDate): DailyRecord {
            return DailyRecord(
                date = date,
                totalCount = 0,
                wishText = "",
                targetCount = 0,
                isCompleted = false,
                resetCount = 0,
                lostCount = 0
            )
        }
    }
}