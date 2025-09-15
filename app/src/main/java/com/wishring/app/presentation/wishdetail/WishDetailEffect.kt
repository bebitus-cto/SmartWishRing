package com.wishring.app.presentation.wishdetail

/**
 * Simplified Effects for WishDetail screen
 * Only essential one-time UI actions
 */
sealed class WishDetailEffect {
    /**
     * Navigate back to previous screen
     */
    object NavigateBack : WishDetailEffect()
    
    /**
     * Show toast message
     * @param message Message to display
     */
    data class ShowToast(val message: String) : WishDetailEffect()
}