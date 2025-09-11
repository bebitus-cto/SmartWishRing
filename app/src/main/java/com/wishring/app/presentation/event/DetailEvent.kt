package com.wishring.app.presentation.event

/**
 * Events for Detail screen
 * 
 * 상세 기록 화면에서 발생하는 이벤트들 (UDA 패턴)
 */
sealed class DetailEvent {
    object BackPressed : DetailEvent()
    object RefreshRequested : DetailEvent()
    object ErrorDismissed : DetailEvent()
}