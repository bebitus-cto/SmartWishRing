package com.wishring.app.presentation.settings

import androidx.lifecycle.viewModelScope
import com.wishring.app.core.base.BaseViewModel
import com.wishring.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 * Manages app settings and preferences
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val bleRepository: BleRepository,
    private val wishCountRepository: WishCountRepository,
    private val resetLogRepository: ResetLogRepository
) : BaseViewModel<SettingsViewState, SettingsEvent, SettingsEffect>() {

    override val _uiState = MutableStateFlow(SettingsViewState())

    init {
        loadInitialData()
        observePreferences()
        observeBleConnection()
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadData -> loadInitialData()

            // General Settings
            is SettingsEvent.UpdateThemeMode -> updateThemeMode(event.mode)
            is SettingsEvent.UpdateLanguage -> updateLanguage(event.language)
            is SettingsEvent.UpdateDefaultWishText -> updateDefaultWishText(event.text)
            is SettingsEvent.UpdateDefaultTargetCount -> updateDefaultTargetCount(event.count)

            // Notification Settings
            is SettingsEvent.ToggleNotification -> toggleNotification()
            is SettingsEvent.ToggleDailyReminder -> toggleDailyReminder()
            is SettingsEvent.UpdateDailyReminderTime -> updateDailyReminderTime(event.time)
            is SettingsEvent.ToggleAchievementNotification -> toggleAchievementNotification()

            // Sound & Vibration
            is SettingsEvent.ToggleSound -> toggleSound()
            is SettingsEvent.ToggleVibration -> toggleVibration()

            // BLE Settings
            is SettingsEvent.StartBleScanning -> startBleScanning()
            is SettingsEvent.ConnectBleDevice -> connectBleDevice(event.deviceAddress)
            is SettingsEvent.DisconnectBleDevice -> disconnectBleDevice()
            is SettingsEvent.ToggleBleAutoConnect -> toggleBleAutoConnect()
            is SettingsEvent.UpdateBleSyncInterval -> updateBleSyncInterval(event.minutes)
            is SettingsEvent.TestBleConnection -> testBleConnection()
            is SettingsEvent.UpdateDeviceFirmware -> updateDeviceFirmware()

            // Data & Backup
            is SettingsEvent.ToggleAutoBackup -> toggleAutoBackup()
            is SettingsEvent.BackupNow -> backupNow()
            is SettingsEvent.RestoreFromBackup -> restoreFromBackup()
            is SettingsEvent.ExportData -> exportData(event.format)
            is SettingsEvent.ImportData -> importData()
            is SettingsEvent.ClearAllData -> showClearAllDataConfirmation()
            is SettingsEvent.ClearOldData -> clearOldData(event.beforeDays)

            // UI Events
            is SettingsEvent.ToggleSectionExpansion -> toggleSectionExpansion(event.section)
            is SettingsEvent.ShowResetConfirmation -> showResetConfirmation()
            is SettingsEvent.HideResetConfirmation -> hideResetConfirmation()
            is SettingsEvent.ConfirmReset -> confirmReset()
            is SettingsEvent.ShowDeleteDataConfirmation -> showDeleteDataConfirmation()
            is SettingsEvent.HideDeleteDataConfirmation -> hideDeleteDataConfirmation()
            is SettingsEvent.ConfirmDeleteData -> confirmDeleteData()

            // Navigation
            is SettingsEvent.NavigateBack -> navigateBack()
            is SettingsEvent.NavigateToAbout -> navigateToAbout()
            is SettingsEvent.NavigateToPrivacyPolicy -> navigateToPrivacyPolicy()
            is SettingsEvent.NavigateToTermsOfService -> navigateToTermsOfService()
            is SettingsEvent.NavigateToLicenses -> navigateToLicenses()

            // Other Events
            is SettingsEvent.ResetToDefaults -> resetToDefaults()
            is SettingsEvent.SendFeedback -> sendFeedback()
            is SettingsEvent.RateApp -> rateApp()
            is SettingsEvent.ShareApp -> shareApp()
            is SettingsEvent.CheckForUpdates -> checkForUpdates()
            is SettingsEvent.DismissError -> dismissError()
            SettingsEvent.BackupData -> TODO()
            SettingsEvent.ClearData -> TODO()
            SettingsEvent.ConnectDevice -> TODO()
            SettingsEvent.DisconnectDevice -> TODO()
            SettingsEvent.NavigateToSupport -> TODO()
            SettingsEvent.OpenPrivacyPolicy -> TODO()
            SettingsEvent.RestoreData -> TODO()
            is SettingsEvent.UpdateDarkMode -> TODO()
            is SettingsEvent.UpdateLed -> TODO()
            is SettingsEvent.UpdateNotifications -> TODO()
            is SettingsEvent.UpdateSound -> TODO()
            is SettingsEvent.UpdateVibration -> TODO()
            else -> {
                // Handle other events
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            try {
                // Load current preferences
                val themeMode = preferencesRepository.getThemeMode()
                val language = preferencesRepository.getLanguage()
                val defaultWishText = preferencesRepository.getDefaultWishText()
                val defaultTargetCount = preferencesRepository.getDefaultTargetCount()
                val notificationEnabled = preferencesRepository.isNotificationEnabled()
                val dailyReminderTime = preferencesRepository.getDailyReminderTime()
                val achievementNotificationEnabled =
                    preferencesRepository.isAchievementNotificationEnabled()
                val soundEnabled = preferencesRepository.isSoundEnabled()
                val vibrationEnabled = preferencesRepository.isVibrationEnabled()
                val bleAutoConnect = preferencesRepository.isBleAutoConnectEnabled()
                val lastBleDevice = preferencesRepository.getLastBleDeviceAddress()
                val bleSyncInterval = preferencesRepository.getBleSyncInterval()
                val autoBackupEnabled = preferencesRepository.isAutoBackupEnabled()
                val lastBackupTime = preferencesRepository.getLastBackupTime()

                // Load data statistics
                val recentRecords = wishCountRepository.getRecentWishCounts(100)
                val totalRecordsCount = recentRecords.size

                // Get device info if connected
                val isConnected = bleRepository.isDeviceConnected()
                val batteryLevel = if (isConnected) bleRepository.getBatteryLevel() else null
                val firmwareVersion = if (isConnected) bleRepository.getFirmwareVersion() else null

                updateState {
                    copy(
                        isLoading = false,
                        themeMode = themeMode,
                        language = language,
                        defaultWishText = defaultWishText,
                        defaultTargetCount = defaultTargetCount,
                        notificationEnabled = notificationEnabled,
                        dailyReminderEnabled = dailyReminderTime != null,
                        dailyReminderTime = dailyReminderTime,
                        achievementNotificationEnabled = achievementNotificationEnabled,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        bleAutoConnect = bleAutoConnect,
                        lastConnectedDevice = lastBleDevice,
                        bleSyncInterval = bleSyncInterval,
                        autoBackupEnabled = autoBackupEnabled,
                        lastBackupTime = lastBackupTime,
                        totalRecordsCount = totalRecordsCount,
                        deviceBatteryLevel = batteryLevel,
                        deviceFirmwareVersion = firmwareVersion,
                        bleConnectionState = if (isConnected) BleConnectionState.CONNECTED else BleConnectionState.DISCONNECTED,
                        appVersion = getAppVersion(),
                        buildNumber = getBuildNumber()
                    )
                }

            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "설정을 불러오는 중 오류가 발생했습니다"
                    )
                }
            }
        }
    }

    private fun observePreferences() {
        // Observe theme mode changes
        preferencesRepository.observeThemeMode()
            .onEach { themeMode ->
                updateState { copy(themeMode = themeMode) }
            }
            .launchIn(viewModelScope)

        // Observe notification settings
        preferencesRepository.observeNotificationEnabled()
            .onEach { enabled ->
                updateState { copy(notificationEnabled = enabled) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBleConnection() {
        bleRepository.getConnectionState()
            .onEach { connectionState ->
                updateState { copy(bleConnectionState = connectionState) }

                // Update device info when connected
                if (connectionState == BleConnectionState.CONNECTED) {
                    viewModelScope.launch {
                        val batteryLevel = bleRepository.getBatteryLevel()
                        val firmwareVersion = bleRepository.getFirmwareVersion()
                        updateState {
                            copy(
                                deviceBatteryLevel = batteryLevel,
                                deviceFirmwareVersion = firmwareVersion
                            )
                        }
                    }
                } else if (connectionState == BleConnectionState.DISCONNECTED) {
                    updateState {
                        copy(
                            deviceBatteryLevel = null,
                            deviceFirmwareVersion = null
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    // General Settings

    private fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
            sendEffect(SettingsEffect.ApplyTheme(mode))
        }
    }

    private fun updateLanguage(language: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
            sendEffect(SettingsEffect.ApplyLanguage(language))
            sendEffect(SettingsEffect.RestartApp)
        }
    }

    private fun updateDefaultWishText(text: String) {
        viewModelScope.launch {
            preferencesRepository.setDefaultWishText(text)
            updateState { copy(defaultWishText = text) }
        }
    }

    private fun updateDefaultTargetCount(count: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultTargetCount(count)
            updateState { copy(defaultTargetCount = count) }
        }
    }

    // Notification Settings

    private fun toggleNotification() {
        viewModelScope.launch {
            val newValue = !currentState.notificationEnabled
            preferencesRepository.setNotificationEnabled(newValue)

            if (newValue) {
                sendEffect(
                    SettingsEffect.RequestPermission(Permission.NOTIFICATION) {
                        // Permission granted callback
                    }
                )
            }
        }
    }

    private fun toggleDailyReminder() {
        if (currentState.dailyReminderEnabled) {
            // Disable reminder
            viewModelScope.launch {
                preferencesRepository.setDailyReminderTime(null)
                updateState { copy(dailyReminderEnabled = false, dailyReminderTime = null) }
                sendEffect(SettingsEffect.CancelScheduledNotification)
            }
        } else {
            // Show time picker to enable reminder
            sendEffect(
                SettingsEffect.ShowTimePicker(
                    currentTime = "09:00",
                    onTimeSelected = { time ->
                        updateDailyReminderTime(time)
                    }
                )
            )
        }
    }

    private fun updateDailyReminderTime(time: String) {
        viewModelScope.launch {
            preferencesRepository.setDailyReminderTime(time)
            updateState { copy(dailyReminderEnabled = true, dailyReminderTime = time) }

            sendEffect(
                SettingsEffect.ScheduleNotification(
                    time = time,
                    message = "오늘의 위시를 실천해보세요!"
                )
            )
        }
    }

    private fun toggleAchievementNotification() {
        viewModelScope.launch {
            val newValue = !currentState.achievementNotificationEnabled
            preferencesRepository.setAchievementNotificationEnabled(newValue)
            updateState { copy(achievementNotificationEnabled = newValue) }
        }
    }

    // Sound & Vibration

    private fun toggleSound() {
        viewModelScope.launch {
            val newValue = !currentState.soundEnabled
            preferencesRepository.setSoundEnabled(newValue)
            updateState { copy(soundEnabled = newValue) }

            if (newValue) {
                sendEffect(SettingsEffect.PlayTestSound)
            }
        }
    }

    private fun toggleVibration() {
        viewModelScope.launch {
            val newValue = !currentState.vibrationEnabled
            preferencesRepository.setVibrationEnabled(newValue)
            updateState { copy(vibrationEnabled = newValue) }

            if (newValue) {
                sendEffect(SettingsEffect.VibrateTest)
            }
        }
    }

    // BLE Settings

    private fun startBleScanning() {
        viewModelScope.launch {
            try {
                val devices = mutableListOf<BleDevice>()

                bleRepository.startScanning()
                    .collect { device ->
                        devices.add(device)
                    }

                sendEffect(
                    SettingsEffect.ShowBleDevicePicker(
                        devices = devices.map {
                            BleDeviceInfo(it.name, it.address, it.rssi)
                        },
                        onDeviceSelected = { address ->
                            connectBleDevice(address)
                        }
                    )
                )

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("스캔 실패: ${e.message}"))
            }
        }
    }

    private fun connectBleDevice(deviceAddress: String) {
        viewModelScope.launch {
            try {
                val connected = bleRepository.connectDevice(deviceAddress)

                if (connected) {
                    preferencesRepository.setLastBleDeviceAddress(deviceAddress)
                    sendEffect(SettingsEffect.ShowToast("디바이스 연결 성공"))
                } else {
                    sendEffect(SettingsEffect.ShowToast("디바이스 연결 실패"))
                }

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("연결 오류: ${e.message}"))
            }
        }
    }

    private fun disconnectBleDevice() {
        viewModelScope.launch {
            bleRepository.disconnectDevice()
            sendEffect(SettingsEffect.ShowToast("디바이스 연결 해제됨"))
        }
    }

    private fun toggleBleAutoConnect() {
        viewModelScope.launch {
            val newValue = !currentState.bleAutoConnect
            preferencesRepository.setBleAutoConnectEnabled(newValue)
            updateState { copy(bleAutoConnect = newValue) }
        }
    }

    private fun updateBleSyncInterval(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setBleSyncInterval(minutes)
            updateState { copy(bleSyncInterval = minutes) }
        }
    }

    private fun testBleConnection() {
        if (!currentState.isBleConnected) {
            sendEffect(SettingsEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        viewModelScope.launch {
            try {
                val isResponding = bleRepository.testConnection()

                if (isResponding) {
                    sendEffect(SettingsEffect.ShowToast("연결 테스트 성공"))
                    sendEffect(SettingsEffect.VibrateTest)
                } else {
                    sendEffect(SettingsEffect.ShowToast("연결 테스트 실패"))
                }

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("테스트 실패: ${e.message}"))
            }
        }
    }

    private fun updateDeviceFirmware() {
        if (!currentState.isBleConnected) {
            sendEffect(SettingsEffect.ShowToast("디바이스가 연결되지 않았습니다"))
            return
        }

        viewModelScope.launch {
            try {
                val currentVersion = bleRepository.getFirmwareVersion() ?: "Unknown"
                val newVersion = "1.2.0" // Mock new version

                sendEffect(
                    SettingsEffect.ShowFirmwareUpdateDialog(
                        currentVersion = currentVersion,
                        newVersion = newVersion,
                        onConfirm = {
                            // Perform firmware update
                            sendEffect(SettingsEffect.ShowToast("펌웨어 업데이트가 시작되었습니다"))
                        }
                    )
                )

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("펌웨어 업데이트 실패: ${e.message}"))
            }
        }
    }

// Data & Backup

    private fun toggleAutoBackup() {
        viewModelScope.launch {
            val newValue = !currentState.autoBackupEnabled
            preferencesRepository.setAutoBackupEnabled(newValue)
            updateState { copy(autoBackupEnabled = newValue) }

            if (newValue && currentState.totalRecordsCount > 0) {
                backupNow()
            }
        }
    }

    private fun backupNow() {
        if (!currentState.canBackupNow) return

        viewModelScope.launch {
            try {
                // Create backup data
                val records = wishCountRepository.getRecentWishCounts(1000)
                val resetLogs = resetLogRepository.getRecentResetLogs(1000)

                // Mock backup process
                val backupData = createBackupData(records, resetLogs)
                val timestamp = System.currentTimeMillis()

                // Save backup timestamp
                preferencesRepository.setLastBackupTime(timestamp)
                updateState { copy(lastBackupTime = timestamp) }

                sendEffect(SettingsEffect.ShowToast("백업이 완료되었습니다"))

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("백업 실패: ${e.message}"))
            }
        }
    }

    private fun restoreFromBackup() {
        sendEffect(
            SettingsEffect.ShowRestoreConfirmation(
                backupInfo = BackupInfo(
                    date = "2024-01-01",
                    recordCount = 100,
                    fileSize = 1024L,
                    version = "1.0.0"
                ),
                onConfirm = {
                    performRestore()
                }
            )
        )
    }

    private fun performRestore() {
        viewModelScope.launch {
            try {
                // Mock restore process
                sendEffect(SettingsEffect.ShowToast("데이터 복원이 완료되었습니다"))
                sendEffect(SettingsEffect.RestartApp)

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("복원 실패: ${e.message}"))
            }
        }
    }

    private fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            try {
                val records = wishCountRepository.getRecentWishCounts(1000)

                val exportData = when (format) {
                    ExportFormat.CSV -> generateCsvData(records)
                    ExportFormat.JSON -> generateJsonData(records)
                    ExportFormat.EXCEL -> generateExcelData(records)
                }

                val fileName = "wish_data_export.${format.name.lowercase()}"
                val mimeType = when (format) {
                    ExportFormat.CSV -> "text/csv"
                    ExportFormat.JSON -> "application/json"
                    ExportFormat.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                }

                sendEffect(
                    SettingsEffect.SaveFile(
                        fileName = fileName,
                        content = exportData.toByteArray(),
                        mimeType = mimeType
                    )
                )

                sendEffect(SettingsEffect.ShowToast("데이터 내보내기 완료"))

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("내보내기 실패: ${e.message}"))
            }
        }
    }

    private fun importData() {
        sendEffect(
            SettingsEffect.ShowFilePicker(
                mimeType = "*/*",
                onFileSelected = { filePath ->
                    performImport(filePath)
                }
            )
        )
    }

    private fun performImport(filePath: String) {
        viewModelScope.launch {
            try {
                // Mock import process
                sendEffect(SettingsEffect.ShowToast("데이터 가져오기 완료"))

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("가져오기 실패: ${e.message}"))
            }
        }
    }

    private fun showClearAllDataConfirmation() {
        updateState { copy(showDeleteDataConfirmation = true) }
    }

    private fun showDeleteDataConfirmation() {
        updateState { copy(showDeleteDataConfirmation = true) }
    }

    private fun hideDeleteDataConfirmation() {
        updateState { copy(showDeleteDataConfirmation = false) }
    }

    private fun confirmDeleteData() {
        viewModelScope.launch {
            try {
                wishCountRepository.deleteOldRecords("1970-01-01")
                resetLogRepository.deleteOldResetLogs("1970-01-01")

                updateState {
                    copy(
                        showDeleteDataConfirmation = false,
                        totalRecordsCount = 0
                    )
                }

                sendEffect(SettingsEffect.ShowToast("모든 데이터가 삭제되었습니다"))

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("삭제 실패: ${e.message}"))
            }
        }
    }

    private fun clearOldData(beforeDays: Int) {
        viewModelScope.launch {
            try {
                val cutoffDate = java.time.LocalDate.now().minusDays(beforeDays.toLong()).toString()
                val deletedCount = wishCountRepository.deleteOldRecords(cutoffDate)

                sendEffect(SettingsEffect.ShowToast("${deletedCount}개의 오래된 기록이 삭제되었습니다"))

                // Reload data count
                val remainingRecords = wishCountRepository.getRecentWishCounts(1000)
                updateState { copy(totalRecordsCount = remainingRecords.size) }

            } catch (e: Exception) {
                sendEffect(SettingsEffect.ShowToast("삭제 실패: ${e.message}"))
            }
        }
    }

// UI Events

    private fun toggleSectionExpansion(section: SettingsSection) {
        updateState {
            copy(
                expandedSection = if (expandedSection == section) null else section
            )
        }
    }

    private fun showResetConfirmation() {
        updateState { copy(showResetConfirmation = true) }
    }

    private fun hideResetConfirmation() {
        updateState { copy(showResetConfirmation = false) }
    }

    private fun confirmReset() {
        updateState { copy(showResetConfirmation = false) }
        resetToDefaults()
    }

// Navigation

    private fun navigateBack() {
        sendEffect(SettingsEffect.NavigateBack)
    }

    private fun navigateToAbout() {
        sendEffect(SettingsEffect.NavigateToAbout)
    }

    private fun navigateToPrivacyPolicy() {
        sendEffect(SettingsEffect.OpenUrl("https://wishring.app/privacy"))
    }

    private fun navigateToTermsOfService() {
        sendEffect(SettingsEffect.OpenUrl("https://wishring.app/terms"))
    }

    private fun navigateToLicenses() {
        sendEffect(SettingsEffect.OpenUrl("https://wishring.app/licenses"))
    }

// Other Events

    private fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
            loadInitialData()
            sendEffect(SettingsEffect.ShowToast("설정이 기본값으로 초기화되었습니다"))
            sendEffect(SettingsEffect.RestartApp)
        }
    }

    private fun sendFeedback() {
        sendEffect(
            SettingsEffect.SendFeedbackEmail(
                email = "support@wishring.app",
                subject = "WISH RING 앱 피드백",
                body = "안녕하세요, WISH RING 앱에 대한 피드백을 보내드립니다:\n\n"
            )
        )
    }

    private fun rateApp() {
        sendEffect(SettingsEffect.ShowRateAppDialog)
    }

    private fun shareApp() {
        sendEffect(
            SettingsEffect.ShareApp(
                "WISH RING - 매일의 위시를 실천하세요!\n\n" +
                        "https://play.google.com/store/apps/details?id=com.wishring.app"
            )
        )
    }

    private fun checkForUpdates() {
        sendEffect(SettingsEffect.ShowToast("최신 버전입니다"))
    }

    private fun dismissError() {
        updateState { copy(error = null) }
    }

// Helper methods

    private fun getAppVersion(): String = "1.0.0"

    private fun getBuildNumber(): String = "100"

    private fun createBackupData(
        records: List<com.wishring.app.domain.model.WishCount>,
        resetLogs: List<com.wishring.app.domain.model.ResetLog>
    ): String {
        // Create backup data structure
        return "{}" // Simplified
    }

    private fun generateCsvData(records: List<com.wishring.app.domain.model.WishCount>): String {
        val header = "Date,Wish Text,Target Count,Total Count,Completed\n"
        val rows = records.joinToString("\n") { record ->
            "${record.date},\"${record.wishText}\",${record.targetCount},${record.totalCount},${record.isCompleted}"
        }
        return header + rows
    }

    private fun generateJsonData(records: List<com.wishring.app.domain.model.WishCount>): String {
        // Convert to JSON
        return "[]" // Simplified
    }

    private fun generateExcelData(records: List<com.wishring.app.domain.model.WishCount>): String {
        // Convert to Excel format
        return "" // Simplified
    }
}