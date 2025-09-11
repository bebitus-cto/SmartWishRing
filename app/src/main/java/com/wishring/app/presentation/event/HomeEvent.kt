package com.wishring.app.presentation.event

/**
 * Events for Home screen
 * 
 * 홈 화면에서 발생하는 이벤트들 (UDA 패턴)
 */
sealed class HomeEvent {
    object WishButtonClicked : HomeEvent()
    object ShareClicked : HomeEvent()
    data class RecordClicked(val date: String) : HomeEvent()
    object RefreshRequested : HomeEvent()
    object RetryConnection : HomeEvent()
    object BatteryClicked : HomeEvent()
    object ErrorDismissed : HomeEvent()
}