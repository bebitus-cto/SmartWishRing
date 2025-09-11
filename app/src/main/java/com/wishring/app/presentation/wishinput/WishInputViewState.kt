package com.wishring.app.presentation.wishinput

/**
 * ViewState for WishInput screen
 * Represents the UI state of wish input
 */
data class WishInputViewState(
    val isLoading: Boolean = false,
    val wishText: String = "",
    val targetCount: Int = 1000,
    val date: String = "",
    val isEditMode: Boolean = false,
    val existingRecord: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val suggestedWishes: List<String> = listOf(
        "나는 매일 성장하고 있다",
        "나는 충분히 잘하고 있다",
        "나는 사랑받을 자격이 있다",
        "나는 내 꿈을 이룰 수 있다",
        "나는 강하고 건강하다"
    ),
    val showSuggestions: Boolean = false,
    val isWishValid: Boolean = false,
    val maxWishLength: Int = 100,
    val minTargetCount: Int = 1,
    val maxTargetCount: Int = 10000,
    val showTargetCountPicker: Boolean = false
) {
    /**
     * Check if save is enabled
     */
    val isSaveEnabled: Boolean
        get() = wishText.isNotBlank() && targetCount > 0 && !isSaving
    
    /**
     * Remaining character count
     */
    val remainingCharacters: Int
        get() = maxWishLength - wishText.length
    
    /**
     * Character count display
     */
    val characterCountDisplay: String
        get() = "${wishText.length}/$maxWishLength"
    
    /**
     * Target count display
     */
    val targetCountDisplay: String
        get() = "$targetCount 회"
}