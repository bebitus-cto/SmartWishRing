package com.wishring.app.presentation.wishinput

/**
 * Side effects for WishInput screen
 * Represents one-time UI actions
 */
sealed class WishInputEffect {
    
    /**
     * Show toast message
     * @param message Message to display
     */
    data class ShowToast(val message: String) : WishInputEffect()
    
    /**
     * Show snackbar
     * @param message Message to display
     * @param actionLabel Action button label
     * @param action Action to perform
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : WishInputEffect()
    
    /**
     * Navigate back
     */
    object NavigateBack : WishInputEffect()
    
    /**
     * Navigate to home with result
     * @param message Success message
     */
    data class NavigateToHomeWithResult(val message: String) : WishInputEffect()
    
    /**
     * Show number picker
     * @param currentValue Current value
     * @param minValue Minimum value
     * @param maxValue Maximum value
     * @param onValueSelected Callback when value selected
     */
    data class ShowNumberPicker(
        val currentValue: Int,
        val minValue: Int,
        val maxValue: Int,
        val onValueSelected: (Int) -> Unit
    ) : WishInputEffect()
    
    /**
     * Show delete confirmation dialog
     * @param onConfirm Callback when confirmed
     */
    data class ShowDeleteConfirmationDialog(
        val onConfirm: () -> Unit
    ) : WishInputEffect()
    
    /**
     * Show keyboard
     */
    object ShowKeyboard : WishInputEffect()
    
    /**
     * Hide keyboard
     */
    object HideKeyboard : WishInputEffect()
    
    /**
     * Vibrate for feedback
     */
    object VibrateForFeedback : WishInputEffect()
    
    /**
     * Play sound effect
     * @param soundType Sound type
     */
    data class PlaySoundEffect(val soundType: SoundType) : WishInputEffect()
    
    /**
     * Track analytics event
     * @param eventName Event name
     * @param parameters Event parameters
     */
    data class TrackAnalyticsEvent(
        val eventName: String,
        val parameters: Map<String, Any>
    ) : WishInputEffect()
    
    /**
     * Show validation error
     * @param field Field with error
     * @param message Error message
     */
    data class ShowValidationError(
        val field: ValidationField,
        val message: String
    ) : WishInputEffect()
}

/**
 * Sound type enum
 */
enum class SoundType {
    TAP,
    SUCCESS,
    ERROR,
    SELECT
}

/**
 * Validation field enum
 */
enum class ValidationField {
    WISH_TEXT,
    TARGET_COUNT
}