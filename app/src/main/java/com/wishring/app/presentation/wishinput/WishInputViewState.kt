package com.wishring.app.presentation.wishinput

import com.wishring.app.presentation.wishinput.model.WishItem

/**
 * ViewState for WishInput screen
 * Represents the UI state of wish input (supports multiple wishes)
 */
data class WishInputViewState(
    val isLoading: Boolean = false,
    val wishes: List<WishItem> = listOf(WishItem.createEmpty()),
    val date: String = "",
    val isEditMode: Boolean = false,
    val existingRecord: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val maxWishCount: Int = 3,
    val maxWishLength: Int = 100,
    val minTargetCount: Int = 1,
    val maxTargetCount: Int = 10000,
    val showTargetCountPicker: Boolean = false
) {
    /**
     * Check if save is enabled (at least one valid wish)
     */
    val isSaveEnabled: Boolean
        get() = wishes.any { it.isValid } && !isSaving
    
    /**
     * Check if can add more wishes
     */
    val canAddMoreWishes: Boolean
        get() = wishes.size < maxWishCount
    
    /**
     * Check if can remove wishes
     */
    val canRemoveWishes: Boolean
        get() = wishes.size > 1
    
    /**
     * Get count of valid wishes
     */
    val validWishCount: Int
        get() = wishes.count { it.isValid }
    
    /**
     * Check if maximum wishes reached
     */
    val isMaxWishesReached: Boolean
        get() = wishes.size >= maxWishCount
}