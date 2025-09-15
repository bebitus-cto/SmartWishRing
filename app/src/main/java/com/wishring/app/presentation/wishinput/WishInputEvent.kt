package com.wishring.app.presentation.wishinput

import java.util.UUID

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
     * @param wishId ID of the wish to remove
     */
    data class RemoveWish(val wishId: UUID) : WishInputEvent()
    
    /**
     * Update specific wish text
     * @param wishId ID of the wish to update
     * @param text New wish text
     */
    data class UpdateWishText(val wishId: UUID, val text: String) : WishInputEvent()
    
    /**
     * Update specific wish target count
     * @param wishId ID of the wish to update  
     * @param count New target count
     */
    data class UpdateWishCount(val wishId: UUID, val count: Int) : WishInputEvent()
    
    /**
     * Select suggested wish
     * @param wish Suggested wish text
     */
    data class SelectSuggestedWish(val wish: String) : WishInputEvent()
    
    /**
     * Toggle suggestions visibility
     */
    object ToggleSuggestions : WishInputEvent()
    
    
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