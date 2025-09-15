package com.wishring.app.presentation.home

import androidx.lifecycle.viewModelScope
import com.wishring.app.core.base.BaseViewModel
import com.wishring.app.core.util.Constants
import com.wishring.app.core.util.BlePermissionChecker
import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.domain.repository.BleDevice
import com.wishring.app.domain.repository.BleRepository
import com.wishring.app.domain.repository.ButtonPressEvent
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.model.*
import com.wishring.app.domain.model.HealthDataType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import android.util.Log
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * ViewModel for Home screen
 * Manages UI state and business logic for the main screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wishCountRepository: WishCountRepository,
    private val bleRepository: BleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val blePermissionChecker: BlePermissionChecker
) : BaseViewModel<HomeViewState, HomeEvent, HomeEffect>() {

    override val _uiState = MutableStateFlow(HomeViewState())

    init {
        loadInitialData()
        observeTodayWishCount()
        observeBleConnectionState()
        observeBleButtonPress()
        
        // 디버그 이벤트 히스토리 구독
        observeDebugEventHistory()
    }

    override fun onEvent(event: HomeEvent) {
        Log.d(TAG, "🎯 HomeEvent received: $event")
        when (event) {
            is HomeEvent.LoadData -> loadInitialData()
            is HomeEvent.RefreshData -> refreshData()
            is HomeEvent.IncrementCount -> incrementCount(event.amount)
            is HomeEvent.ResetCount -> showResetConfirmation(event.reason)
            is HomeEvent.NavigateToWishInput -> navigateToWishInput()
            is HomeEvent.NavigateToDetail -> navigateToDetail(event.date)

            is HomeEvent.StartBleScanning -> startBleScanning()
            is HomeEvent.ConnectBleDevice -> connectBleDevice(event.deviceAddress)
            is HomeEvent.DisconnectBleDevice -> disconnectBleDevice()
            is HomeEvent.SyncWithDevice -> syncWithDevice()
            is HomeEvent.SelectBleDevice -> selectBleDevice(event.deviceAddress)
            is HomeEvent.DismissBleDevicePicker -> dismissBleDevicePicker()
            is HomeEvent.ShareAchievement -> shareAchievement()
            is HomeEvent.ConfirmShare -> confirmShare(event.message, event.hashtags)
            is HomeEvent.DismissShareDialog -> dismissShareDialog()
            is HomeEvent.ShowStreakDetails -> showStreakDetails()
            is HomeEvent.DismissError -> dismissError()
            is HomeEvent.HandleDeviceButtonPress -> handleDeviceButtonPress(event.pressCount)
            is HomeEvent.BackgroundSyncCompleted -> handleBackgroundSync()
            is HomeEvent.HandleDeepLink -> handleDeepLink(event.action)
            is HomeEvent.ToggleCompletionAnimation -> toggleCompletionAnimation()
            is HomeEvent.RequestNotificationPermission -> requestNotificationPermission()
            is HomeEvent.RequestBlePermission -> requestBlePermission()
            is HomeEvent.EnableBluetooth -> enableBluetooth()
            is HomeEvent.DismissPermissionExplanation -> dismissPermissionExplanation()
            is HomeEvent.RequestPermissionsFromExplanation -> requestPermissionsFromExplanation()
            is HomeEvent.DismissPermissionDenied -> dismissPermissionDenied()
            is HomeEvent.OpenAppSettingsFromDialog -> openAppSettingsFromDialog()
            
            // ✅ 디버그 이벤트 처리
            is HomeEvent.ToggleDebugPanel -> toggleDebugPanel()
            is HomeEvent.ClearDebugHistory -> clearDebugHistory()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }

            try {
                // Load today's wish count
                val todayWishCount = wishCountRepository.getTodayWishCount()

                // Load recent records
                val recentRecords = wishCountRepository.getDailyRecords(limit = 50)

                // Load streak info
                val streakInfo = wishCountRepository.getStreakInfo()

                // Update state
                updateState {
                    copy(
                        isLoading = false,
                        todayWishCount = todayWishCount,
                        recentRecords = recentRecords,
                        streakInfo = streakInfo
                    )
                }

                // Check if goal completed for animation
                if (todayWishCount.isCompleted && !currentState.showCompletionAnimation) {
                    checkAndShowCompletionAnimation()
                }

                // Start BLE Auto Connect Service
                startBleAutoConnectService()

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
     * 자동 BLE 서비스 시작
     */
    private fun startBleAutoConnectService() {
        viewModelScope.launch {
            try {
                // BLE 자동 연결이 활성화된 경우에만 서비스 시작
                if (preferencesRepository.isBleAutoConnectEnabled()) {
                    com.wishring.app.ble.BleAutoConnectService.startService(
                        context = context
                    )
                    Log.d("HomeViewModel", "BLE Auto Connect Service started")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to start BLE service", e)
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
                        todayWishCount = todayWishCount,
                        recentRecords = recentRecords,
                        streakInfo = streakInfo
                    )
                }

                // Sync with device if connected
                if (currentState.isBleConnected) {
                    syncWithDevice()
                }

            } catch (e: Exception) {
                updateState { copy(isRefreshing = false) }
                sendEffect(HomeEffect.ShowToast("새로고침 실패: ${e.message}"))
            }
        }
    }

    private fun incrementCount(amount: Int) {
        if (!currentState.canIncrement) return

        viewModelScope.launch {
            try {
                val updated = wishCountRepository.incrementTodayCount(amount)

                updateState { copy(todayWishCount = updated) }

                // Play sound and vibrate
                sendEffect(HomeEffect.PlaySound(SoundType.TAP))


                // Check if goal completed
                if (updated.isCompleted && !currentState.todayWishCount?.isCompleted!!) {
                    handleGoalCompletion()
                }

                // Sync with device if connected
                if (currentState.isBleConnected) {
                    bleRepository.sendWishCount(updated.totalCount)
                }

                // Update widget
                sendEffect(
                    HomeEffect.UpdateWidget(
                        count = updated.totalCount,
                        target = updated.targetCount
                    )
                )

            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowToast("카운트 증가 실패: ${e.message}"))
            }
        }
    }

    private fun showResetConfirmation(reason: String?) {
        sendEffect(
            HomeEffect.ShowResetConfirmation { confirmedReason ->
                resetCount(confirmedReason ?: reason)
            }
        )
    }

    private fun resetCount(reason: String?) {
        viewModelScope.launch {
            try {
                val reset = wishCountRepository.resetTodayCount(reason)
                updateState { copy(todayWishCount = reset) }

                // Sync with device
                if (currentState.isBleConnected) {
                    bleRepository.sendWishCount(0)
                }

                sendEffect(HomeEffect.ShowToast("카운트가 초기화되었습니다"))


            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowToast("초기화 실패: ${e.message}"))
            }
        }
    }

    private fun observeTodayWishCount() {
        wishCountRepository.observeTodayWishCount()
            .onEach { wishCount ->
                updateState { copy(todayWishCount = wishCount) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBleConnectionState() {
        bleRepository.connectionState
            .onEach { connectionState ->
                updateState { copy(bleConnectionState = connectionState) }

                // Get battery level when connected
                if (connectionState == BleConnectionState.CONNECTED) {
                    viewModelScope.launch {
                        val batteryLevel = bleRepository.getBatteryLevel()
                        updateState { copy(deviceBatteryLevel = batteryLevel) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBleButtonPress() {
        // Updated to use MRD SDK counter increments
        viewModelScope.launch {
            bleRepository.counterIncrements.collect { increment ->
                handleMrdCounterIncrement(increment)
            }
        }
    }

    private fun handleMrdCounterIncrement(increment: Int) {
        // MRD SDK sends individual +1 increments
        incrementCount(increment)
        
        // Show more user-friendly message
        if (increment == 1) {
            sendEffect(HomeEffect.ShowToast("WISH RING 버튼 누름 감지! ✨"))
        } else {
            sendEffect(HomeEffect.ShowToast("WISH RING에서 +$increment"))
        }
        

        
        // Update battery level request after activity
        viewModelScope.launch {
            bleRepository.requestBatteryUpdate()
        }
    }
    
    // Keep legacy method for compatibility
    private fun handleDeviceButtonPress(pressCount: Int) {
        handleMrdCounterIncrement(pressCount)
    }

    private fun startBleScanning() {
        Log.d(TAG, "🔍 BLE 스캔 시작...")
        viewModelScope.launch {
            try {
                Log.d(TAG, "📡 BLE 권한 체크 중...")
                
                // 권한 확인
                if (!blePermissionChecker.hasAllBlePermissions()) {
                    Log.e(TAG, "❌ BLE 권한 없음!")
                    sendEffect(HomeEffect.RequestBluetoothPermissions)
                    return@launch
                }
                
                Log.d(TAG, "✅ 권한 확인 완료, 스캔 시작...")
                updateState { copy(isLoading = true) }
                
                val devices = mutableListOf<BleDevice>()

                // 3초로 타임아웃 단축 (더 빠른 스캔)
                bleRepository.startScanning(timeout = 3000)
                    .collect { device ->
                        Log.d(TAG, "🎯 기기 발견: ${device.name} (${device.address})")
                        devices.add(device)
                        
                        // 실시간으로 기기 목록 업데이트 (모든 기기 동등하게)
                        val deviceInfos = devices.map {
                            DeviceInfo(it.name, it.address, it.rssi)
                        }.sortedByDescending { it.rssi } // RSSI 강한 순으로 정렬
                        
                        updateState {
                            copy(
                                showBleDevicePicker = true,
                                availableBleDevices = deviceInfos,
                                isLoading = devices.isEmpty() // 첫 기기 발견하면 로딩 중지
                            )
                        }
                    }

                Log.d(TAG, "📊 스캔 완료. 발견된 기기: ${devices.size}개")
                updateState { copy(isLoading = false) }
                
                // 기기가 하나도 없을 때만 안내
                if (devices.isEmpty()) {
                    Log.w(TAG, "⚠️ 스캔 완료했지만 기기를 찾을 수 없음")
                    sendEffect(HomeEffect.ShowToast("BLE 기기를 찾을 수 없습니다. 기기 전원을 확인해주세요."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ BLE 스캔 실패: ${e.message}", e)
                updateState { copy(isLoading = false) }
                
                val errorMessage = when {
                    e.message?.contains("BLUETOOTH") == true -> "블루투스를 켜주세요"
                    e.message?.contains("LOCATION") == true -> "위치 서비스를 켜주세요"
                    e.message?.contains("PERMISSION") == true -> "권한이 필요합니다"
                    else -> "BLE 스캔 실패: ${e.message}"
                }
                sendEffect(HomeEffect.ShowToast(errorMessage))
            }
        }
    }

    private fun connectBleDevice(deviceAddress: String) {
        viewModelScope.launch {
            updateState { copy(bleConnectionState = BleConnectionState.CONNECTING) }

            try {
                val connected = bleRepository.connectDevice(deviceAddress)

                if (connected) {
                    // Save device address
                    preferencesRepository.setLastBleDeviceAddress(deviceAddress)

                    // Get device info
                    val batteryLevel = bleRepository.getBatteryLevel()
                    // TODO: Implement when getFirmwareVersion is available in BleRepository
                    val firmwareVersion = "1.0.0" // Mock firmware version

                    updateState {
                        copy(
                            bleConnectionState = BleConnectionState.CONNECTED,
                            deviceBatteryLevel = batteryLevel,
                            bluetoothProgressMessage = "" // Clear progress message
                        )
                    }

                    // Connection success feedback
                    sendEffect(HomeEffect.PlaySound(SoundType.SUCCESS))

                    sendEffect(HomeEffect.ShowConnectionSuccessAnimation)
                    sendEffect(HomeEffect.ShowToast(Constants.SuccessMessages.DEVICE_CONNECTED))
                    
                    // Show connection success animation for 2 seconds
                    updateState { copy(showConnectionSuccessAnimation = true) }
                    viewModelScope.launch {
                        delay(2000)
                        updateState { copy(showConnectionSuccessAnimation = false) }
                    }

                    // Initial sync
                    syncWithDevice()
                } else {
                    updateState { copy(bleConnectionState = BleConnectionState.DISCONNECTED) }
                    sendEffect(HomeEffect.ShowToast(Constants.ErrorMessages.CONNECTION_FAILED))
                }

            } catch (e: Exception) {
                updateState { copy(bleConnectionState = BleConnectionState.ERROR) }
                sendEffect(HomeEffect.ShowToast("연결 실패: ${e.message}"))
            }
        }
    }

    private fun disconnectBleDevice() {
        viewModelScope.launch {
            updateState { copy(bleConnectionState = BleConnectionState.DISCONNECTING) }
            bleRepository.disconnectDevice()
            updateState {
                copy(
                    bleConnectionState = BleConnectionState.DISCONNECTED,
                    deviceBatteryLevel = null
                )
            }
            sendEffect(HomeEffect.ShowToast("디바이스 연결이 해제되었습니다"))
        }
    }

    private fun syncWithDevice() {
        if (!currentState.isBleConnected) return

        viewModelScope.launch {
            try {
                val wishCount = currentState.todayWishCount ?: return@launch

                val success = bleRepository.syncAllData(
                    wishCount = wishCount.totalCount,
                    wishText = wishCount.wishText,
                    targetCount = wishCount.targetCount,
                    isCompleted = wishCount.isCompleted
                )

                if (success) {
                    updateState { copy(lastSyncTime = System.currentTimeMillis()) }
                    sendEffect(HomeEffect.ShowToast("동기화 완료"))
                } else {
                    sendEffect(HomeEffect.ShowToast("동기화 실패"))
                }

            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowToast("동기화 오류: ${e.message}"))
            }
        }
    }

    /**
     * Select BLE device from picker and connect
     */
    private fun selectBleDevice(deviceAddress: String) {
        updateState { 
            copy(
                showBleDevicePicker = false,
                availableBleDevices = emptyList()
            ) 
        }
        connectBleDevice(deviceAddress)
    }

    /**
     * Dismiss BLE device picker dialog
     */
    private fun dismissBleDevicePicker() {
        updateState { 
            copy(
                showBleDevicePicker = false,
                availableBleDevices = emptyList()
            ) 
        }
    }

    private fun handleGoalCompletion() {
        updateState { copy(showCompletionAnimation = true) }
        sendEffect(HomeEffect.PlayCompletionAnimation)
        sendEffect(HomeEffect.PlaySound(SoundType.SUCCESS))

        sendEffect(HomeEffect.ShowToast(Constants.SuccessMessages.GOAL_ACHIEVED))

        // Send achievement notification if enabled
        viewModelScope.launch {
            if (preferencesRepository.isAchievementNotificationEnabled()) {
                sendEffect(
                    HomeEffect.SendLocalNotification(
                        title = "목표 달성! 🎉",
                        body = "오늘의 목표를 달성했습니다!"
                    )
                )
            }
        }

        // Hide animation after delay
        viewModelScope.launch {
            delay(3000)
            updateState { copy(showCompletionAnimation = false) }
        }
    }

    private fun checkAndShowCompletionAnimation() {
        // Check if already shown today
        // This would ideally check a preference to avoid showing multiple times
        updateState { copy(showCompletionAnimation = true) }
        sendEffect(HomeEffect.PlayCompletionAnimation)

        viewModelScope.launch {
            delay(3000)
            updateState { copy(showCompletionAnimation = false) }
        }
    }

    private fun shareAchievement() {
        val wishCount = currentState.todayWishCount ?: return

        val shareText = String.format(
            Constants.SHARE_MESSAGE_TEMPLATE,
            wishCount.totalCount
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

    private fun navigateToSettings() {
        // TODO: Add NavigateToSettings to HomeEffect or use OpenAppSettings
        sendEffect(HomeEffect.OpenAppSettings)
    }

    private fun handleBackgroundSync() {
        loadInitialData()
        sendEffect(HomeEffect.ShowToast("백그라운드 동기화 완료"))
    }

    private fun handleDeepLink(action: String) {
        // Handle deep link actions
        when (action) {
            "increment" -> incrementCount(1)
            "share" -> shareAchievement()
            else -> { /* Unknown action */
            }
        }
    }

    private fun toggleCompletionAnimation() {
        updateState { copy(showCompletionAnimation = !showCompletionAnimation) }
    }

    private fun requestNotificationPermission() {
        sendEffect(HomeEffect.RequestPermission(PermissionType.NOTIFICATION))
    }

    private fun requestBlePermission() {
        sendEffect(HomeEffect.RequestPermission(PermissionType.BLUETOOTH))
    }

    private fun enableBluetooth() {
        viewModelScope.launch {
            try {
                updateState { copy(bluetoothProgressMessage = "블루투스 설정을 확인하는 중...") }
                
                // 1. 블루투스 지원 여부 확인
                val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                
                if (bluetoothAdapter == null) {
                    sendEffect(HomeEffect.ShowPermissionDenied(
                        deniedPermissions = listOf("Bluetooth Not Supported"),
                        solution = "이 기기는 블루투스를 지원하지 않습니다"
                    ))
                    return@launch
                }
                
                // 2. 권한 확인
                if (!blePermissionChecker.hasAllBlePermissions()) {
                    val missingPermissions = blePermissionChecker.getMissingBlePermissions()
                    
                    updateState { copy(bluetoothProgressMessage = "블루투스 권한이 필요합니다") }
                    
                    // 권한 설명 메시지 생성
                    val explanations = missingPermissions.associateWith { permission ->
                        when (permission) {
                            android.Manifest.permission.BLUETOOTH_SCAN -> "근처 WISH RING 기기를 찾기 위해 필요합니다"
                            android.Manifest.permission.BLUETOOTH_CONNECT -> "WISH RING 기기와 연결하기 위해 필요합니다"
                            android.Manifest.permission.BLUETOOTH -> "블루투스 기능 사용을 위해 필요합니다"
                            android.Manifest.permission.BLUETOOTH_ADMIN -> "블루투스 설정 관리를 위해 필요합니다"
                            else -> "앱 기능 사용을 위해 필요한 권한입니다"
                        }
                    }
                    
                    updateState { 
                        copy(
                            showPermissionExplanation = true,
                            permissionExplanations = explanations
                        )
                    }
                    return@launch
                }
                
                // 3. 블루투스 활성화 확인
                if (!bluetoothAdapter.isEnabled) {
                    updateState { copy(bluetoothProgressMessage = "블루투스 활성화가 필요합니다") }
                    sendEffect(HomeEffect.EnableBluetooth)
                    return@launch
                }
                
                // 4. 모든 조건 만족 - BLE 스캔 시작
                updateState { copy(bluetoothProgressMessage = "WISH RING 기기를 찾는 중...") }
                startBleScanning()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check Bluetooth setup", e)
                sendEffect(HomeEffect.ShowToast("블루투스 설정 확인 중 오류가 발생했습니다"))
                updateState { 
                    copy(
                        showPermissionDenied = true,
                        permissionDeniedMessage = "다시 시도해주세요. 문제가 계속되면 설정에서 수동으로 권한을 확인해주세요"
                    )
                }
            }
        }
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
     * 디버그 이벤트 히스토리 관찰
     */
    private fun observeDebugEventHistory() {
        viewModelScope.launch {
            // BleRepository에서 이벤트 히스토리를 구독하는 로직은
            // 실제 테스트에서 로그로 확인하는 것이 더 안전함
            Log.d(TAG, "Debug event history observation initialized")
        }
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
    companion object {
        private const val TAG = "HomeViewModel"
    }
}