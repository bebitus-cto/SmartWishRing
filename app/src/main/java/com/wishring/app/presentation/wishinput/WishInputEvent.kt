package com.wishring.app.presentation.wishinput

/**
 * User events for WishInput screen
 * Represents user interactions and actions
 */
sealed class WishInputEvent {
    
    /**
     * Update wish text
     * @param text New wish text
     */
    data class UpdateWishText(val text: String) : WishInputEvent()
    
    /**
     * Update target count
     * @param count New target count
     */
    data class UpdateTargetCount(val count: Int) : WishInputEvent()
    
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
     * Show target count picker
     */
    object ShowTargetCountPicker : WishInputEvent()
    
    /**
     * Hide target count picker
     */
    object HideTargetCountPicker : WishInputEvent()
    
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
     * Clear wish text
     */
    object ClearWishText : WishInputEvent()
    
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
     * Validate wish text
     */
    object ValidateWish : WishInputEvent()
}