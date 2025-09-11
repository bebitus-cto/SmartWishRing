package com.wishring.app.presentation.effect

/**
 * Navigation effects for app navigation
 * 
 * 앱 내 네비게이션을 위한 이펙트들 (UDA 패턴)
 */
sealed class NavigationEffect {
    object NavigateToHome : NavigationEffect()
    object NavigateToWishInput : NavigationEffect()
    data class NavigateToDetail(val date: String) : NavigationEffect()
    object NavigateBack : NavigationEffect()
}