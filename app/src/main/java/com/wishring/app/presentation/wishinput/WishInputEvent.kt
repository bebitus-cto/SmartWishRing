package com.wishring.app.presentation.wishinput

import java.time.LocalDate

/**
 * User events for WishInput screen
 * Represents user interactions and actions (supports multiple wishes)
 */
sealed class WishInputEvent {
    
    /**
     * Add new wish to the list
     * @param position Optional position to insert at
     */
    data class AddWish(val position: Int? = null) : WishInputEvent()
    
    /**
     * Remove wish from the list
     * @param index Index of the wish to remove
     */
    data class RemoveWish(val index: Int) : WishInputEvent()
    
    /**
     * Update specific wish text
     * @param index Index of the wish to update
     * @param text New wish text
     */
    data class UpdateWishText(val index: Int, val text: String) : WishInputEvent()
    
    /**
     * Update specific wish target count
     * @param index Index of the wish to update  
     * @param count New target count
     */
    data class UpdateWishCount(val index: Int, val count: Int) : WishInputEvent()
    
    /**
     * Save wish
     */
    object SaveWish : WishInputEvent()
    
    /**
     * Delete existing wish
     */
    object DeleteWish : WishInputEvent()
    
    /**
     * Show delete confirmation
     */
    object ShowDeleteConfirmation : WishInputEvent()
    
    /**
     * Hide delete confirmation
     */
    object HideDeleteConfirmation : WishInputEvent()
    
    /**
     * Confirm delete
     */
    object ConfirmDelete : WishInputEvent()
    
    /**
     * Navigate back
     */
    object NavigateBack : WishInputEvent()
    
    
    /**
     * Reset to defaults
     */
    object ResetToDefaults : WishInputEvent()
    
    /**
     * Load existing record
     * @param date Date to load
     */
    data class LoadExistingRecord(val date: String) : WishInputEvent()
    
    /**
     * Dismiss error
     */
    object DismissError : WishInputEvent()
    
    /**
     * Clear all wish text
     */
    object ClearWishText : WishInputEvent()
    
    /**
     * Validate wish text
     */
    object ValidateWish : WishInputEvent()
}