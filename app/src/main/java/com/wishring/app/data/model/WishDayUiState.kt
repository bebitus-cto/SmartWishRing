package com.wishring.app.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * UI model for daily wish records
 * Represents aggregated wish data for a specific day
 *
 * 일별 위시 기록을 표시하기 위한 UI 모델
 * 특정 날짜의 집계된 위시 데이터를 나타냄
 */
data class WishDayUiState(
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
         * Create from WishUiState model
         */
        fun fromWishCount(wishUiState: WishUiState): WishDayUiState {
            val localDate = LocalDate.parse(wishUiState.date)
            return WishDayUiState(
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
        fun empty(date: LocalDate): WishDayUiState {
            return WishDayUiState(
                date = date,
                wishText = "",
                isCompleted = false,
                targetCount = 0,
                completedCount = 0,
            )
        }
    }
}