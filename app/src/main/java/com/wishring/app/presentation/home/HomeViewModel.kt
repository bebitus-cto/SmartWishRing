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
import com.wishring.app.data.repository.PreferencesRepository
import com.wishring.app.data.repository.WishCountRepository
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
    private val wishCountRepository: WishCountRepository,
    private val preferencesRepository: PreferencesRepository
) : BaseViewModel<HomeViewState, HomeEvent, HomeEffect>() {

    override val _uiState = MutableStateFlow(HomeViewState())

    init {
        // Seed dummy data for testing (개발 중에만 사용)
        seedTestData()
        loadInitialData()
        observeTodayWishCount()
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadData -> loadInitialData()
            is HomeEvent.RefreshData -> refreshData()
            is HomeEvent.IncrementCount -> {}
            is HomeEvent.ResetCount -> {}
            is HomeEvent.NavigateToWishInput -> navigateToWishInput()
            is HomeEvent.NavigateToDetail -> navigateToDetail(event.date)
            is HomeEvent.ShareAchievement -> shareAchievement()
            is HomeEvent.ConfirmShare -> confirmShare(event.message, event.hashtags)
            is HomeEvent.DismissShareDialog -> dismissShareDialog()
            is HomeEvent.ShowStreakDetails -> showStreakDetails()
            is HomeEvent.DismissError -> dismissError()
            is HomeEvent.HandleDeviceButtonPress -> {}
            is HomeEvent.BackgroundSyncCompleted -> handleBackgroundSync()
            is HomeEvent.UpdateBatteryLevel -> updateBatteryLevel(event.level)
            is HomeEvent.UpdateLastBleScanTime -> updateLastBleScanTime(event.time)
            is HomeEvent.HandleDeepLink -> {}
            is HomeEvent.ToggleCompletionAnimation -> toggleCompletionAnimation()
            is HomeEvent.RequestNotificationPermission -> requestNotificationPermission()
            is HomeEvent.DismissPermissionExplanation -> dismissPermissionExplanation()
            is HomeEvent.RequestPermissionsFromExplanation -> requestPermissionsFromExplanation()
            is HomeEvent.DismissPermissionDenied -> dismissPermissionDenied()
            is HomeEvent.OpenAppSettingsFromDialog -> openAppSettingsFromDialog()
            is HomeEvent.StartScanning -> startScanning()
            is HomeEvent.ScanCompleted -> scanCompleted()
            is HomeEvent.StartConnectionAttempt -> startConnectionAttempt()
            is HomeEvent.ConnectionAttemptCompleted -> connectionAttemptCompleted()
            is HomeEvent.ConnectionFailed -> connectionFailed(event.error)

            is HomeEvent.ToggleDebugPanel -> toggleDebugPanel()
            is HomeEvent.ClearDebugHistory -> clearDebugHistory()
            is HomeEvent.RequestBlePermission,
            is HomeEvent.EnableBluetooth,
            is HomeEvent.StartBleScanning,
            is HomeEvent.ConnectBleDevice,
            is HomeEvent.DisconnectBleDevice,
            is HomeEvent.SyncWithDevice,
            is HomeEvent.SelectBleDevice,
            is HomeEvent.DismissBleDevicePicker -> {
                Log.d(TAG, "BLE event received but handled by MainViewModel: $event")
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }

            try {
                // Get today's wish count (may be null if no wish exists)
                val todayWishCount = wishCountRepository.getTodayWishCount()

                // Get recent records from actual database
                val recentRecords = wishCountRepository.getDailyRecords(limit = 50)

                // Get streak info
                val streakInfo = wishCountRepository.getStreakInfo()

                updateState {
                    copy(
                        isLoading = false,
                        todayWishUiState = todayWishCount,  // Can be null if no wish for today
                        recentRecords = recentRecords,
                        streakInfo = streakInfo
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
                // Check if database is empty (no records exist)
                val existingRecords = wishCountRepository.getDailyRecords(limit = 1)
                if (existingRecords.isEmpty()) {
                    // Database is empty, seed with dummy data
                    wishCountRepository.seedDummyData()
                    Log.d("HomeViewModel", "Seeded database with 30 days of dummy data")
                } else {
                    Log.d("HomeViewModel", "Database already has data, skipping seed")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to seed dummy data", e)
                // Continue normally even if seeding fails
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }

            try {
                val todayWishCount = wishCountRepository.getTodayWishCount()
                val recentRecords = wishCountRepository.getDailyRecords(limit = 50)
                val streakInfo = wishCountRepository.getStreakInfo()

                updateState {
                    copy(
                        isRefreshing = false,
                        todayWishUiState = todayWishCount,
                        recentRecords = recentRecords,
                        streakInfo = streakInfo
                    )
                }

            } catch (e: Exception) {
                updateState { copy(isRefreshing = false) }
                sendEffect(HomeEffect.ShowToast("새로고침 실패: ${e.message}"))
            }
        }
    }

    private fun observeTodayWishCount() {
        wishCountRepository.observeTodayWishCount()
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

    private fun showStreakDetails() {
        val streakInfo = currentState.streakInfo ?: return

        sendEffect(
            HomeEffect.ShowStreakDetailsDialog(
                StreakDetailsInfo(
                    currentStreak = streakInfo.currentStreak,
                    bestStreak = streakInfo.bestStreak,
                    streakHistory = emptyList(), // Would load from repository
                    achievements = emptyList() // Would load from repository
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

    private fun handleBackgroundSync() {
        loadInitialData()
        sendEffect(HomeEffect.ShowToast("백그라운드 동기화 완료"))
    }

    private fun updateBatteryLevel(level: Int?) {
        _uiState.value = _uiState.value.copy(deviceBatteryLevel = level)
    }

    private fun updateLastBleScanTime(time: Long) {
        _uiState.value = _uiState.value.copy(lastBleScanTime = time)
    }

    private fun toggleCompletionAnimation() {
        updateState { copy(showCompletionAnimation = !showCompletionAnimation) }
    }

    private fun requestNotificationPermission() {
        sendEffect(HomeEffect.RequestPermission(PermissionType.NOTIFICATION))
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

    private fun dismissPermissionExplanation() {
        updateState {
            copy(
                showPermissionExplanation = false,
                permissionExplanations = emptyMap(),
                bluetoothProgressMessage = ""
            )
        }
    }

    private fun requestPermissionsFromExplanation() {
        updateState {
            copy(
                showPermissionExplanation = false,
                permissionExplanations = emptyMap()
            )
        }
        sendEffect(HomeEffect.RequestBluetoothPermissions)
    }

    private fun dismissPermissionDenied() {
        updateState {
            copy(
                showPermissionDenied = false,
                permissionDeniedMessage = "",
                bluetoothProgressMessage = ""
            )
        }
    }

    private fun openAppSettingsFromDialog() {
        updateState {
            copy(
                showPermissionDenied = false,
                permissionDeniedMessage = ""
            )
        }
        sendEffect(HomeEffect.OpenAppSettings)
    }


    /**
     * 디버그 패널 토글
     */
    private fun toggleDebugPanel() {
        updateState { copy(showDebugPanel = !showDebugPanel) }
    }

    /**
     * 디버그 히스토리 클리어
     */
    private fun clearDebugHistory() {
        updateState { copy(debugEventHistory = emptyList()) }
        sendEffect(HomeEffect.ShowToast("디버그 히스토리가 클리어되었습니다"))
        Log.d(TAG, "Debug history cleared")
    }

    /**
     * 연결 시도 시작 처리
     */
    /**
     * 스캔 시작 처리
     */
    private fun startScanning() {
        updateState {
            copy(isScanning = true)
        }

        Log.d(TAG, "Device scanning started")
    }

    /**
     * 스캔 완료 처리 (기기 발견 및 다이얼로그 표시)
     */
    private fun scanCompleted() {
        updateState {
            copy(isScanning = false)
        }

        Log.d(TAG, "Device scanning completed")
    }

    /**
     * 연결 시도 시작 처리
     */
    private fun startConnectionAttempt() {
        updateState {
            copy(
                isAttemptingConnection = true,
                connectionStartTime = System.currentTimeMillis()
            )
        }

        // 30초 타임아웃 시작
        startConnectionTimeout()

        Log.d(TAG, "Connection attempt started")
    }

    /**
     * 연결 시도 완료 처리 (성공/실패 관계없이)
     */
    private fun connectionAttemptCompleted() {
        updateState {
            copy(
                isAttemptingConnection = false,
                connectionStartTime = null
            )
        }

        Log.d(TAG, "Connection attempt completed")
    }

    /**
     * 연결 실패 처리
     */
    private fun connectionFailed(error: String) {
        connectionAttemptCompleted()
        sendEffect(HomeEffect.ShowToast("연결 실패: $error"))

        Log.e(TAG, "Connection failed: $error")
    }

    /**
     * 연결 시도 타임아웃 시작 (30초)
     */
    private fun startConnectionTimeout() {
        viewModelScope.launch {
            delay(30_000) // 30초 대기

            // 여전히 연결 시도 중이면 타임아웃 처리
            if (currentState.isAttemptingConnection) {
                handleConnectionTimeout()
            }
        }
    }

    /**
     * 연결 타임아웃 처리
     */
    private fun handleConnectionTimeout() {
        updateState {
            copy(
                isAttemptingConnection = false,
                connectionStartTime = null
            )
        }

        sendEffect(HomeEffect.ShowToast("연결 시간이 초과되었습니다"))
        Log.w(TAG, "Connection attempt timed out")
    }

    /**
     * 백그라운드 복귀 시 상태 동기화
     * MainViewModel의 실제 연결 상태와 동기화
     */
    fun syncConnectionState(actualConnectionState: com.wishring.app.data.repository.BleConnectionState) {
        val currentAttempting = currentState.isAttemptingConnection

        // 연결 시도 중이었는데 실제로는 연결되었거나 연결 해제된 경우
        if (currentAttempting && (
                    actualConnectionState == com.wishring.app.data.repository.BleConnectionState.CONNECTED ||
                            actualConnectionState == com.wishring.app.data.repository.BleConnectionState.DISCONNECTED ||
                            actualConnectionState == com.wishring.app.data.repository.BleConnectionState.ERROR)
        ) {

            connectionAttemptCompleted()

            // 연결 성공 시 피드백
            if (actualConnectionState == com.wishring.app.data.repository.BleConnectionState.CONNECTED) {
                sendEffect(HomeEffect.ShowToast("WISH RING 연결 성공!"))
                sendEffect(HomeEffect.ShowConnectionSuccessAnimation)
            }

            Log.d(TAG, "Connection state synchronized: $actualConnectionState")
        }
    }

    /**
     * 앱 복귀 시 상태 확인 (onResume 등에서 호출)
     */
    fun checkConnectionState() {
        // 연결 시도가 30초 이상 지속된 경우 강제 타임아웃
        if (currentState.isConnectionAttemptTimedOut) {
            handleConnectionTimeout()
        }

        Log.d(TAG, "Connection state checked on app resume")
    }


    companion object {
        private const val TAG = "HomeViewModel"
    }
}