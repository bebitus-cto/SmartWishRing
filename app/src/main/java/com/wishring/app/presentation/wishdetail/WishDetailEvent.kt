package com.wishring.app.presentation.wishdetail

import java.time.LocalDate

/**
 * Simplified Events for WishDetail screen
 * Only essential navigation and data loading events
 */
sealed class WishDetailEvent {
    /**
     * Navigate back to previous screen
     */
    object NavigateBack : WishDetailEvent()
    
    /**
     * Load data for specific date
     */
    data class LoadDataForDate(val date: LocalDate) : WishDetailEvent()
    
    /**
     * Navigate to previous date
     */
    object NavigateToPreviousDate : WishDetailEvent()
    
    /**
     * Navigate to next date
     */
    object NavigateToNextDate : WishDetailEvent()
    
    /**
     * Retry loading data when error occurs
     */
    object RetryLoading : WishDetailEvent()
    
    /**
     * Dismiss error message
     */
    object DismissError : WishDetailEvent()
}