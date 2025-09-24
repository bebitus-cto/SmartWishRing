package com.wishring.app.presentation.home

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.wishring.app.core.base.BaseViewModel
// import com.wishring.app.core.util.BlePermissionChecker
import com.wishring.app.core.util.Constants
// BLE imports removed - moved to MainViewModel
// import com.wishring.app.data.repository.BleConnectionState
// import com.wishring.app.data.repository.BleDevice
// import com.wishring.app.data.repository.BleRepository
import com.wishring.app.data.repository.WishRepository
import dagger.hilt.android.lifecycle.HiltViewModel
// import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wishRepository: WishRepository
) : BaseViewModel<HomeViewState, HomeEvent, HomeEffect>() {

    override val _uiState = MutableStateFlow(HomeViewState())

    init {
        seedTestData()
        loadInitialData()
        observeTodayWishCount()
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadData -> loadInitialData()
            is HomeEvent.NavigateToWishInput -> navigateToWishInput()
            is HomeEvent.NavigateToDetail -> navigateToDetail(event.date)
            is HomeEvent.ShareAchievement -> shareAchievement()
            is HomeEvent.ConfirmShare -> confirmShare(event.message, event.hashtags)
            is HomeEvent.DismissShareDialog -> dismissShareDialog()
            is HomeEvent.DismissError -> dismissError()
            is HomeEvent.HandleDeviceButtonPress -> {}
            is HomeEvent.UpdateBatteryLevel -> updateBatteryLevel(event.level)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }

            try {
                val todayWishCount = wishRepository.getTodayWish()
                val recentRecords = wishRepository.getDailyRecords(limit = 50)

                updateState {
                    copy(
                        isLoading = false,
                        todayWishUiState = todayWishCount,  // Can be null if no wish for today
                        recentRecords = recentRecords
                    )
                }

                // Check if today's wish is completed
                if (todayWishCount?.isCompleted == true && !currentState.showCompletionAnimation) {
                    checkAndShowCompletionAnimation()
                }

            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "데이터를 불러오는 중 오류가 발생했습니다"
                    )
                }
            }
        }
    }

    /**
     * Seed dummy data for testing purposes
     * Only runs once when database is empty
     */
    private fun seedTestData() {
        viewModelScope.launch {
            try {
                val existingRecords = wishRepository.getDailyRecords(limit = 1)
                if (existingRecords.isEmpty()) {
                    wishRepository.seedDummyData()
                    Log.d("HomeViewModel", "Seeded database with 30 days of dummy data")
                } else {
                    Log.d("HomeViewModel", "Database already has data, skipping seed")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to seed dummy data", e)
            }
        }
    }

    private fun observeTodayWishCount() {
        wishRepository.observeTodayWishCount()
            .onEach { wishCount ->
                updateState { copy(todayWishUiState = wishCount) }
            }
            .launchIn(viewModelScope)
    }

    private fun checkAndShowCompletionAnimation() {
        updateState { copy(showCompletionAnimation = true) }
        sendEffect(HomeEffect.PlayCompletionAnimation)

        viewModelScope.launch {
            delay(3000)
            updateState { copy(showCompletionAnimation = false) }
        }
    }

    private fun shareAchievement() {
        val wishCount = currentState.todayWishUiState ?: return

        val shareText = String.format(
            java.util.Locale.getDefault(),
            Constants.SHARE_MESSAGE_TEMPLATE,
            wishCount.targetCount
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


    private fun navigateToWishInput() {
        sendEffect(HomeEffect.NavigateToWishInput)
    }

    private fun navigateToDetail(date: String) {
        sendEffect(HomeEffect.NavigateToDetail(date))
    }


    private fun updateBatteryLevel(level: Int?) {
        _uiState.value = _uiState.value.copy(deviceBatteryLevel = level)
    }

    private fun dismissError() {
        updateState { copy(error = null) }
    }


    private fun confirmShare(message: String, hashtags: String) {
        viewModelScope.launch {
            updateState { copy(showShareDialog = false) }
            // TODO: Implement actual sharing logic
            sendEffect(HomeEffect.ShowToast("공유 기능 준비 중입니다"))
        }
    }

    private fun dismissShareDialog() {
        updateState { copy(showShareDialog = false) }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}