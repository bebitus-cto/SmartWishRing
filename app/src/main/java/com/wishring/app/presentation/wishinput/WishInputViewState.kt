package com.wishring.app.presentation.wishinput

import com.wishring.app.data.model.WishDayUiState
import java.time.LocalDate

/**
 * ViewState for WishInput screen
 * Represents the UI state of wish input (supports multiple wishes)
 */
data class WishInputViewState(
    val isLoading: Boolean = false,
    val wishes: List<WishDayUiState> = listOf(WishDayUiState.empty(LocalDate.now())),
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
        get() = wishes.any { it.wishText.isNotBlank() && it.targetCount > 0 } && !isSaving
    
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
        get() = wishes.count { it.wishText.isNotBlank() && it.targetCount > 0 }
    
    /**
     * Check if maximum wishes reached
     */
    val isMaxWishesReached: Boolean
        get() = wishes.size >= maxWishCount
}