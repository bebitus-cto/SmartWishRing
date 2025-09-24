package com.wishring.app.presentation.wishdetail

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Simplified ViewState for WishDetail screen
 * Focused only on essential data needed for the UI
 */
data class WishDetailViewState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val targetCount: Int = 0,
    val wishText: String = "",
    val motivationalMessages: List<String> = emptyList(),
    val error: String? = null
) {
    /**
     * Formatted date for display (yyyy.MM.dd)
     */
    val displayDate: String
        get() = selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    
    /**
     * Formatted count with comma separator
     */
    val displayCount: String
        get() = targetCount.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,")
    
    /**
     * All messages to display (wish text + motivational messages)
     */
    val allMessages: List<String>
        get() = if (wishText.isNotEmpty()) {
            listOf(wishText) + motivationalMessages
        } else {
            motivationalMessages
        }
}