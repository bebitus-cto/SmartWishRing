package com.wishring.app.presentation.event

/**
 * Events for Wish Input screen
 * 
 * 위시 입력 화면에서 발생하는 이벤트들 (UDA 패턴)
 */
sealed class WishInputEvent {
    data class WishTextChanged(val text: String) : WishInputEvent()
    data class TargetCountChanged(val count: String) : WishInputEvent()
    object SaveClicked : WishInputEvent()
    object BackPressed : WishInputEvent()
    object ErrorDismissed : WishInputEvent()
}