package com.wishring.app.presentation.home

import androidx.lifecycle.viewModelScope
import com.wishring.app.core.base.BaseViewModel
import com.wishring.app.core.util.Constants
import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.domain.repository.BleDevice
import com.wishring.app.domain.repository.BleRepository
import com.wishring.app.domain.repository.ButtonPressEvent
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for Home screen
 * Manages UI state and business logic for the main screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wishCountRepository: WishCountRepository,
    private val bleRepository: BleRepository,
    private val preferencesRepository: PreferencesRepository
) : BaseViewModel<HomeViewState, HomeEvent, HomeEffect>() {

    override val _uiState = MutableStateFlow(HomeViewState())

    init {
        loadInitialData()
        observeTodayWishCount()
        observeBleConnectionState()
        observeBleButtonPress()
        observeHealthDataUpdates()
        observeDeviceStatus()
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadData -> loadInitialData()
            is HomeEvent.RefreshData -> refreshData()
            is HomeEvent.IncrementCount -> incrementCount(event.amount)
            is HomeEvent.ResetCount -> showResetConfirmation(event.reason)
            is HomeEvent.NavigateToWishInput -> navigateToWishInput()
            is HomeEvent.NavigateToDetail -> navigateToDetail(event.date)
            is HomeEvent.NavigateToSettings -> navigateToSettings()
            is HomeEvent.StartBleScanning -> startBleScanning()
            is HomeEvent.ConnectBleDevice -> connectBleDevice(event.deviceAddress)
            is HomeEvent.DisconnectBleDevice -> disconnectBleDevice()
            is HomeEvent.SyncWithDevice -> syncWithDevice()
            is HomeEvent.ShareAchievement -> shareAchievement()
            is HomeEvent.ShowStreakDetails -> showStreakDetails()
            is HomeEvent.DismissError -> dismissError()
            is HomeEvent.HandleDeviceButtonPress -> handleDeviceButtonPress(event.pressCount)
            is HomeEvent.BackgroundSyncCompleted -> handleBackgroundSync()
            is HomeEvent.HandleDeepLink -> handleDeepLink(event.action)
            is HomeEvent.ToggleCompletionAnimation -> toggleCompletionAnimation()
            is HomeEvent.RequestNotificationPermission -> requestNotificationPermission()
            is HomeEvent.RequestBlePermission -> requestBlePermission()

            // MRD SDK 건강 데이터 이벤트 처리
            is HomeEvent.LoadHealthData -> loadHealthData()
            is HomeEvent.StartRealTimeHeartRate -> startRealTimeHeartRate()
            is HomeEvent.StopRealTimeHeartRate -> stopRealTimeHeartRate()
            is HomeEvent.StartRealTimeEcg -> startRealTimeEcg()
            is HomeEvent.StopRealTimeEcg -> stopRealTimeEcg()
            is HomeEvent.UpdateUserProfile -> updateUserProfile(event.userProfile)
            is HomeEvent.SetSportTarget -> setSportTarget(event.steps)
            is HomeEvent.SendAppNotification -> sendAppNotification(event.notification)
            is HomeEvent.UpdateDeviceSettings -> updateDeviceSettings(event.units)
            is HomeEvent.FindDevice -> findDevice()
            is HomeEvent.ShowHealthDetails -> showHealthDetails(event.type)
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

                // Auto-connect BLE if enabled
                viewModelScope.launch {
                    if (preferencesRepository.isBleAutoConnectEnabled()) {
                        val lastDevice = preferencesRepository.getLastBleDeviceAddress()
                        lastDevice?.let { connectBleDevice(it) }
                    }
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
                sendEffect(HomeEffect.Vibrate(VibrationPattern.SHORT))

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
                sendEffect(HomeEffect.Vibrate(VibrationPattern.LONG))

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
        bleRepository.getConnectionState()
            .onEach { connectionState ->
                updateState { copy(bleConnectionState = connectionState) }

                // Get battery level when connected
                if (connectionState == BleConnectionState.CONNECTED) {
                    val batteryLevel = bleRepository.getBatteryLevel()
                    updateState { copy(deviceBatteryLevel = batteryLevel) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBleButtonPress() {
        bleRepository.subscribeToButtonPress()
            .onEach { event ->
                handleDeviceButtonPress(event.pressCount)
            }
            .launchIn(viewModelScope)
    }

    private fun handleDeviceButtonPress(pressCount: Int) {
        incrementCount(pressCount)
        sendEffect(HomeEffect.ShowToast("디바이스에서 $pressCount 카운트 추가"))
    }

    private fun startBleScanning() {
        viewModelScope.launch {
            val devices = mutableListOf<BleDevice>()

            bleRepository.startScanning(timeout = 10000)
                .collect { device ->
                    devices.add(device)
                }

            if (devices.isNotEmpty()) {
                sendEffect(
                    HomeEffect.ShowBleDevicePicker(
                        devices = devices.map {
                            DeviceInfo(it.name, it.address, it.rssi)
                        }
                    )
                )
            } else {
                sendEffect(HomeEffect.ShowToast("디바이스를 찾을 수 없습니다"))
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
                    val firmwareVersion = bleRepository.getFirmwareVersion()

                    updateState {
                        copy(
                            bleConnectionState = BleConnectionState.CONNECTED,
                            deviceBatteryLevel = batteryLevel
                        )
                    }

                    sendEffect(HomeEffect.ShowToast(Constants.SuccessMessages.DEVICE_CONNECTED))

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

    private fun handleGoalCompletion() {
        updateState { copy(showCompletionAnimation = true) }
        sendEffect(HomeEffect.PlayCompletionAnimation)
        sendEffect(HomeEffect.PlaySound(SoundType.SUCCESS))
        sendEffect(HomeEffect.Vibrate(VibrationPattern.SUCCESS))
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
        sendEffect(HomeEffect.NavigateToSettings)
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

    private fun dismissError() {
        updateState { copy(error = null) }
    }

    // ===== MRD SDK 건강 데이터 기능들 =====

    /**
     * Load health data from device
     */
    private fun loadHealthData() {
        if (!currentState.isBleConnected) {
            sendEffect(HomeEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        viewModelScope.launch {
            updateState { copy(healthDataLoading = true) }

            try {
                // Load all health data in parallel
                val heartRateDeferred = async { bleRepository.getLatestHeartRate() }
                val stepDataDeferred = async { bleRepository.getStepData() }
                val sleepDataDeferred = async { bleRepository.getSleepData() }
                val temperatureDeferred = async { bleRepository.getTemperatureData() }
                val bloodPressureDeferred = async { bleRepository.getLatestBloodPressure() }
                val userProfileDeferred = async { bleRepository.getUserProfile() }

                val heartRate = heartRateDeferred.await()
                val stepData = stepDataDeferred.await()
                val sleepData = sleepDataDeferred.await()
                val temperature = temperatureDeferred.await()
                val bloodPressure = bloodPressureDeferred.await()
                val userProfile = userProfileDeferred.await()

                updateState {
                    copy(
                        healthDataLoading = false,
                        heartRateData = heartRate,
                        stepData = stepData,
                        sleepData = sleepData,
                        temperatureData = temperature,
                        bloodPressureData = bloodPressure,
                        userProfile = userProfile
                    )
                }

                sendEffect(HomeEffect.ShowToast("건강 데이터 로드 완료"))

            } catch (e: Exception) {
                updateState { copy(healthDataLoading = false) }
                sendEffect(HomeEffect.ShowToast("건강 데이터 로드 실패: ${e.message}"))
            }
        }
    }

    /**
     * Start real-time heart rate monitoring
     */
    private fun startRealTimeHeartRate() {
        if (!currentState.isBleConnected) {
            sendEffect(HomeEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        updateState { copy(isRealTimeHeartRateActive = true) }

        bleRepository.startRealTimeHeartRate()
            .onEach { heartRateData ->
                updateState { copy(heartRateData = heartRateData) }
                sendEffect(HomeEffect.ShowToast("심박수: ${heartRateData.bpm} BPM"))
            }
            .catch { e ->
                updateState { copy(isRealTimeHeartRateActive = false) }
                sendEffect(HomeEffect.ShowToast("실시간 심박수 측정 오류: ${e.message}"))
            }
            .launchIn(viewModelScope)
    }

    /**
     * Stop real-time heart rate monitoring
     */
    private fun stopRealTimeHeartRate() {
        viewModelScope.launch {
            val success = bleRepository.stopRealTimeHeartRate()
            updateState { copy(isRealTimeHeartRateActive = false) }

            if (success) {
                sendEffect(HomeEffect.ShowToast("실시간 심박수 측정 중지"))
            } else {
                sendEffect(HomeEffect.ShowToast("측정 중지 실패"))
            }
        }
    }

    /**
     * Start real-time ECG monitoring
     */
    private fun startRealTimeEcg() {
        if (!currentState.isBleConnected) {
            sendEffect(HomeEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        bleRepository.startRealTimeEcg()
            .onEach { ecgData ->
                sendEffect(
                    HomeEffect.ShowEcgData(
                        EcgDisplayData(
                            data = ecgData.data,
                            heartRate = ecgData.heartRate,
                            timestamp = ecgData.timestamp,
                            quality = ecgData.quality.name
                        )
                    )
                )
            }
            .catch { e ->
                sendEffect(HomeEffect.ShowToast("실시간 ECG 측정 오류: ${e.message}"))
            }
            .launchIn(viewModelScope)
    }

    /**
     * Stop real-time ECG monitoring
     */
    private fun stopRealTimeEcg() {
        viewModelScope.launch {
            val success = bleRepository.stopRealTimeEcg()

            if (success) {
                sendEffect(HomeEffect.ShowToast("실시간 ECG 측정 중지"))
            } else {
                sendEffect(HomeEffect.ShowToast("ECG 측정 중지 실패"))
            }
        }
    }

    /**
     * Update user profile
     */
    private fun updateUserProfile(userProfile: UserProfile) {
        if (!currentState.isBleConnected) {
            sendEffect(HomeEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        viewModelScope.launch {
            try {
                val success = bleRepository.setUserProfile(userProfile)

                if (success) {
                    updateState { copy(userProfile = userProfile) }
                    sendEffect(HomeEffect.ShowToast("사용자 정보 업데이트 완료"))

                    // Save to preferences for future use
                    // TODO: Save user profile to preferences
                    // preferencesRepository.setUserProfile(userProfile)
                } else {
                    sendEffect(HomeEffect.ShowToast("사용자 정보 업데이트 실패"))
                }

            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowToast("사용자 정보 업데이트 오류: ${e.message}"))
            }
        }
    }

    /**
     * Set sport target (daily step goal)
     */
    private fun setSportTarget(steps: Int) {
        if (!currentState.isBleConnected) {
            sendEffect(HomeEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        viewModelScope.launch {
            try {
                val success = bleRepository.setSportTarget(steps)

                if (success) {
                    sendEffect(HomeEffect.ShowToast("목표 걸음 수 설정 완료: ${steps}걸음"))

                    // Save to preferences
                    // TODO: Save daily step target to preferences
                    // preferencesRepository.setDailyStepTarget(steps)
                } else {
                    sendEffect(HomeEffect.ShowToast("목표 설정 실패"))
                }

            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowToast("목표 설정 오류: ${e.message}"))
            }
        }
    }

    /**
     * Send app notification to device
     */
    private fun sendAppNotification(notification: AppNotification) {
        if (!currentState.isBleConnected) {
            sendEffect(HomeEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        viewModelScope.launch {
            try {
                val success = bleRepository.sendAppNotification(notification)

                if (success) {
                    sendEffect(HomeEffect.ShowToast("알림 전송 완료"))
                } else {
                    sendEffect(HomeEffect.ShowToast("알림 전송 실패"))
                }

            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowToast("알림 전송 오류: ${e.message}"))
            }
        }
    }

    /**
     * Update device settings
     */
    private fun updateDeviceSettings(units: UnitPreferences) {
        if (!currentState.isBleConnected) {
            sendEffect(HomeEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        viewModelScope.launch {
            try {
                val success = bleRepository.setUnitPreferences(units)

                if (success) {
                    sendEffect(HomeEffect.ShowToast("디바이스 설정 업데이트 완료"))
                    // TODO: preferencesRepository.setUnitPreferences(units)
                } else {
                    sendEffect(HomeEffect.ShowToast("설정 업데이트 실패"))
                }

            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowToast("설정 업데이트 오류: ${e.message}"))
            }
        }
    }

    /**
     * Find device (make it vibrate/beep)
     */
    private fun findDevice() {
        if (!currentState.isBleConnected) {
            sendEffect(HomeEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        viewModelScope.launch {
            try {
                val success = bleRepository.findDevice(true)

                if (success) {
                    sendEffect(HomeEffect.ShowToast("디바이스를 찾는 중..."))

                    // Stop after 5 seconds
                    delay(5000)
                    bleRepository.findDevice(false)
                } else {
                    sendEffect(HomeEffect.ShowToast("디바이스 찾기 실패"))
                }

            } catch (e: Exception) {
                sendEffect(HomeEffect.ShowToast("디바이스 찾기 오류: ${e.message}"))
            }
        }
    }

    /**
     * Show health data details
     */
    private fun showHealthDetails(type: HealthDataType) {
        when (type) {
            HealthDataType.HEART_RATE -> {
                val heartRate = currentState.heartRateData
                if (heartRate != null) {
                    sendEffect(
                        HomeEffect.ShowHealthDetailDialog(
                            HealthDetailInfo(
                                type = type,
                                title = "심박수 데이터",
                                currentValue = "${heartRate.bpm} BPM",
                                timestamp = heartRate.timestamp,
                                quality = heartRate.quality.name
                            )
                        )
                    )
                } else {
                    sendEffect(HomeEffect.ShowToast("심박수 데이터가 없습니다"))
                }
            }

            HealthDataType.STEPS -> {
                val steps = currentState.stepData
                if (steps != null) {
                    sendEffect(
                        HomeEffect.ShowHealthDetailDialog(
                            HealthDetailInfo(
                                type = type,
                                title = "걸음 데이터",
                                currentValue = "${steps.steps} 걸음",
                                additionalInfo = "거리: ${steps.distance}km, 칼로리: ${steps.calories}kcal",
                                timestamp = System.currentTimeMillis(),
                                quality = "GOOD"
                            )
                        )
                    )
                } else {
                    sendEffect(HomeEffect.ShowToast("걸음 데이터가 없습니다"))
                }
            }

            HealthDataType.SLEEP -> {
                val sleep = currentState.sleepData
                if (sleep != null) {
                    sendEffect(
                        HomeEffect.ShowHealthDetailDialog(
                            HealthDetailInfo(
                                type = type,
                                title = "수면 데이터",
                                currentValue = "${sleep.totalSleepMinutes / 60}시간 ${sleep.totalSleepMinutes % 60}분",
                                additionalInfo = "깊은 잠: ${sleep.deepSleepMinutes}분, 얕은 잠: ${sleep.lightSleepMinutes}분",
                                timestamp = System.currentTimeMillis(),
                                quality = sleep.sleepQuality.name
                            )
                        )
                    )
                } else {
                    sendEffect(HomeEffect.ShowToast("수면 데이터가 없습니다"))
                }
            }

            else -> {
                sendEffect(HomeEffect.ShowToast("해당 데이터는 아직 지원되지 않습니다"))
            }
        }
    }

    /**
     * Observe health data updates from device
     */
    private fun observeHealthDataUpdates() {
        bleRepository.subscribeToHealthDataUpdates()
            .onEach { healthUpdate ->
                when (healthUpdate.type) {
                    HealthDataType.HEART_RATE -> {
                        val heartRateData = healthUpdate.data as? HeartRateData
                        heartRateData?.let { updateState { copy(heartRateData = it) } }
                    }

                    HealthDataType.STEPS -> {
                        val stepData = healthUpdate.data as? StepData
                        stepData?.let { updateState { copy(stepData = it) } }
                    }

                    HealthDataType.SLEEP -> {
                        val sleepData = healthUpdate.data as? SleepData
                        sleepData?.let { updateState { copy(sleepData = it) } }
                    }

                    HealthDataType.TEMPERATURE -> {
                        val temperatureData = healthUpdate.data as? TemperatureData
                        temperatureData?.let { updateState { copy(temperatureData = it) } }
                    }

                    HealthDataType.BLOOD_PRESSURE -> {
                        val bloodPressureData = healthUpdate.data as? BloodPressureData
                        bloodPressureData?.let { updateState { copy(bloodPressureData = it) } }
                    }

                    else -> { /* Handle other types */
                    }
                }

                sendEffect(HomeEffect.ShowToast("건강 데이터 업데이트됨"))
            }
            .launchIn(viewModelScope)
    }

    /**
     * Observe device status updates
     */
    private fun observeDeviceStatus() {
        bleRepository.subscribeToDeviceStatus()
            .onEach { deviceStatus ->
                updateState { copy(deviceSettings = deviceStatus) }

                // Update battery level
                updateState { copy(deviceBatteryLevel = deviceStatus.batteryLevel) }

                // Show low battery warning if needed
                if (deviceStatus.batteryLevel < 15 && !currentState.showLowBatteryWarning) {
                    sendEffect(
                        HomeEffect.ShowToast("배터리가 부족합니다 (${deviceStatus.batteryLevel}%)")
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}