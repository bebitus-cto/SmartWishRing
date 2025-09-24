package com.wishring.app.data.model

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
    val wishText: String,
    val isCompleted: Boolean,
    val targetCount: Int,
    val completedCount: Int,
) {

    /**
     * Get date string in standard format (yyyy-MM-dd)
     */
    val dateString: String
        get() = date.format(DateTimeFormatter.ISO_LOCAL_DATE)


    companion object {
        /**
         * Create from WishCount model
         */
        fun fromWishCount(wishUiState: WishUiState): DailyRecord {
            val localDate = LocalDate.parse(wishUiState.date)
            return DailyRecord(
                date = localDate,
                wishText = wishUiState.wishText,
                isCompleted = wishUiState.isCompleted,
                targetCount = wishUiState.targetCount,
                completedCount = wishUiState.currentCount,
            )
        }

        /**
         * Create empty record for date
         */
        fun empty(date: LocalDate): DailyRecord {
            return DailyRecord(
                date = date,
                wishText = "",
                isCompleted = false,
                targetCount = 0,
                completedCount = 0,
            )
        }
    }
}