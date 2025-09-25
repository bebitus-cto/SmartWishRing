package com.wishring.app.presentation.home

import com.wishring.app.core.base.BaseViewModel
import com.wishring.app.core.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * HomeViewModel - UI 효과만 처리
 * 데이터 로직은 모두 MainViewModel에서 관리
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    // UI 효과만 처리, 데이터는 MainViewModel에서 관리
) : BaseViewModel<HomeViewState, HomeEvent, HomeEffect>() {

    // 더미 초기 상태 (실제 데이터는 MainViewModel에서)
    override val _uiState = MutableStateFlow<HomeViewState>(HomeViewState.BluetoothDisconnected())

    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.NavigateToWishInput -> navigateToWishInput()
            is HomeEvent.NavigateToDetail -> navigateToDetail(event.date)
            is HomeEvent.ShareAchievement -> shareAchievement()
        }
    }

    private fun navigateToWishInput() {
        sendEffect(HomeEffect.NavigateToWishInput)
    }

    private fun navigateToDetail(date: String) {
        sendEffect(HomeEffect.NavigateToDetail(date))
    }

    private fun shareAchievement() {
        // ShareContent는 HomeEffect에서 정의되어야 함 
        // 현재 currentState가 없으므로 기본값으로 처리
        val shareText = String.format(
            java.util.Locale.getDefault(),
            Constants.SHARE_MESSAGE_TEMPLATE,
            Constants.DEFAULT_TARGET_COUNT // 기본값 사용
        )

        val hashtags = listOf(
            Constants.SHARE_HASHTAG_1,
            Constants.SHARE_HASHTAG_2,
            Constants.SHARE_HASHTAG_3,
            Constants.SHARE_HASHTAG_4
        )

        sendEffect(
            HomeEffect.ShowShareSheet(
                ShareContent(
                    text = shareText,
                    hashtags = hashtags
                )
            )
        )
    }
}