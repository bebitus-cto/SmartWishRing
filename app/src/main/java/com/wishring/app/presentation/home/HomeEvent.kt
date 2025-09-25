package com.wishring.app.presentation.home


/**
 * User events for Home screen
 * Represents user interactions and actions
 */
sealed class HomeEvent {

    /**
     * Navigate to wish input screen
     */
    object NavigateToWishInput : HomeEvent()

    /**
     * Navigate to detail screen
     * @param date Date to show details for
     */
    data class NavigateToDetail(val date: String) : HomeEvent()


    /**
     * Share achievement (show dialog)
     */
    object ShareAchievement : HomeEvent()
}