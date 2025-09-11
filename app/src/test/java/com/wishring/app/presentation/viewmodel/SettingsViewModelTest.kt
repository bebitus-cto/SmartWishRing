package com.wishring.app.presentation.viewmodel

import com.wishring.app.data.ble.model.BleConnectionState
import com.wishring.app.data.local.repository.PreferencesRepository
import com.wishring.app.domain.model.UserProfile
import com.wishring.app.domain.repository.BleRepository
import com.wishring.app.presentation.settings.SettingsViewModel
import com.wishring.app.presentation.settings.SettingsViewState
import com.wishring.app.presentation.settings.SettingsEvent
import com.wishring.app.presentation.settings.SettingsEffect
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

/**
 * SettingsViewModel 테스트
 * 
 * 설정 화면의 비즈니스 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SettingsViewModel 테스트")
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var bleRepository: BleRepository
    private lateinit var testScope: TestScope
    
    @BeforeEach
    fun setup() {
        preferencesRepository = mockk(relaxed = true)
        bleRepository = mockk(relaxed = true)
        testScope = TestScope()
        
        viewModel = SettingsViewModel(
            preferencesRepository = preferencesRepository,
            bleRepository = bleRepository
        )
    }
    
    @Nested
    @DisplayName("사용자 프로필 관리")
    inner class UserProfileManagementTest {
        
        @Test
        @DisplayName("프로필 로드 성공")
        fun testLoadUserProfile() = testScope.runTest {
            // Given
            val profile = UserProfile(
                name = "테스트 사용자",
                targetCount = 108,
                level = 5,
                totalWishesCompleted = 42,
                currentStreak = 7,
                maxStreak = 14,
                createdAt = LocalDateTime.now()
            )
            
            coEvery { preferencesRepository.getUserProfile() } returns flowOf(profile)
            
            // When
            viewModel.handleEvent(SettingsEvent.LoadSettings)
            advanceUntilIdle()
            
            // Then
            val state = viewModel.viewState.value
            state.userProfile shouldBe profile
            state.isLoading shouldBe false
        }
        
        @Test
        @DisplayName("프로필 업데이트")
        fun testUpdateUserProfile() = testScope.runTest {
            // Given
            val updatedName = "새로운 이름"
            val updatedTarget = 216
            
            coEvery { preferencesRepository.saveUserProfile(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.UpdateUserName(updatedName))
            viewModel.handleEvent(SettingsEvent.UpdateTargetCount(updatedTarget))
            advanceUntilIdle()
            
            // Then
            coVerify { 
                preferencesRepository.saveUserProfile(
                    withArg { profile ->
                        profile.name shouldBe updatedName
                        profile.targetCount shouldBe updatedTarget
                    }
                )
            }
        }
        
        @Test
        @DisplayName("프로필 초기화")
        fun testResetProfile() = testScope.runTest {
            // Given
            val effects = mutableListOf<SettingsEffect>()
            viewModel.effect.toList(effects)
            
            coEvery { preferencesRepository.clearUserProfile() } just Runs
            coEvery { preferencesRepository.clearAllData() } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ResetAllData)
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.clearAllData() }
            effects.any { it is SettingsEffect.ShowResetConfirmation } shouldBe true
        }
    }
    
    @Nested
    @DisplayName("BLE 설정 관리")
    inner class BleSettingsTest {
        
        @Test
        @DisplayName("BLE 디바이스 연결 상태 표시")
        fun testBleConnectionStatus() = testScope.runTest {
            // Given
            val deviceAddress = "00:11:22:33:44:55"
            coEvery { bleRepository.connectionState } returns MutableStateFlow(
                BleConnectionState.Connected(deviceAddress)
            )
            coEvery { bleRepository.batteryLevel } returns MutableStateFlow(85)
            
            // When
            viewModel.handleEvent(SettingsEvent.LoadSettings)
            advanceUntilIdle()
            
            // Then
            val state = viewModel.viewState.value
            state.bleConnectionState shouldBe BleConnectionState.Connected(deviceAddress)
            state.deviceBatteryLevel shouldBe 85
        }
        
        @Test
        @DisplayName("BLE 디바이스 연결 해제")
        fun testDisconnectBleDevice() = testScope.runTest {
            // Given
            coEvery { bleRepository.disconnect() } returns true
            coEvery { bleRepository.connectionState } returns MutableStateFlow(
                BleConnectionState.Disconnected
            )
            
            // When
            viewModel.handleEvent(SettingsEvent.DisconnectDevice)
            advanceUntilIdle()
            
            // Then
            coVerify { bleRepository.disconnect() }
            viewModel.viewState.value.bleConnectionState shouldBe BleConnectionState.Disconnected
        }
        
        @Test
        @DisplayName("자동 재연결 설정 변경")
        fun testToggleAutoReconnect() = testScope.runTest {
            // Given
            coEvery { preferencesRepository.setAutoReconnectEnabled(any()) } just Runs
            coEvery { bleRepository.enableAutoReconnect(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ToggleAutoReconnect(true))
            advanceUntilIdle()
            
            // Then
            coVerify { 
                preferencesRepository.setAutoReconnectEnabled(true)
                bleRepository.enableAutoReconnect(true)
            }
            viewModel.viewState.value.autoReconnectEnabled shouldBe true
        }
    }
    
    @Nested
    @DisplayName("알림 설정")
    inner class NotificationSettingsTest {
        
        @Test
        @DisplayName("알림 권한 설정")
        fun testNotificationPermission() = testScope.runTest {
            // Given
            coEvery { preferencesRepository.setNotificationEnabled(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ToggleNotifications(true))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.setNotificationEnabled(true) }
            viewModel.viewState.value.notificationsEnabled shouldBe true
        }
        
        @Test
        @DisplayName("자정 리셋 알림 설정")
        fun testMidnightResetNotification() = testScope.runTest {
            // Given
            coEvery { preferencesRepository.setMidnightResetNotificationEnabled(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ToggleMidnightResetNotification(true))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.setMidnightResetNotificationEnabled(true) }
            viewModel.viewState.value.midnightResetNotificationEnabled shouldBe true
        }
        
        @Test
        @DisplayName("목표 달성 알림 설정")
        fun testGoalAchievementNotification() = testScope.runTest {
            // Given
            coEvery { preferencesRepository.setGoalAchievementNotificationEnabled(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ToggleGoalAchievementNotification(false))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.setGoalAchievementNotificationEnabled(false) }
            viewModel.viewState.value.goalAchievementNotificationEnabled shouldBe false
        }
    }
    
    @Nested
    @DisplayName("테마 및 UI 설정")
    inner class ThemeSettingsTest {
        
        @Test
        @DisplayName("다크 모드 토글")
        fun testToggleDarkMode() = testScope.runTest {
            // Given
            coEvery { preferencesRepository.setDarkModeEnabled(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ToggleDarkMode(true))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.setDarkModeEnabled(true) }
            viewModel.viewState.value.darkModeEnabled shouldBe true
        }
        
        @Test
        @DisplayName("언어 설정 변경")
        fun testChangeLanguage() = testScope.runTest {
            // Given
            val newLanguage = "en"
            coEvery { preferencesRepository.setLanguage(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ChangeLanguage(newLanguage))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.setLanguage(newLanguage) }
            viewModel.viewState.value.currentLanguage shouldBe newLanguage
        }
        
        @Test
        @DisplayName("햅틱 피드백 설정")
        fun testToggleHapticFeedback() = testScope.runTest {
            // Given
            coEvery { preferencesRepository.setHapticFeedbackEnabled(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ToggleHapticFeedback(true))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.setHapticFeedbackEnabled(true) }
            viewModel.viewState.value.hapticFeedbackEnabled shouldBe true
        }
    }
    
    @Nested
    @DisplayName("데이터 관리")
    inner class DataManagementTest {
        
        @Test
        @DisplayName("데이터 백업")
        fun testBackupData() = testScope.runTest {
            // Given
            val effects = mutableListOf<SettingsEffect>()
            viewModel.effect.toList(effects)
            
            coEvery { preferencesRepository.exportData() } returns "backup_data_json"
            
            // When
            viewModel.handleEvent(SettingsEvent.BackupData)
            advanceUntilIdle()
            
            // Then
            effects.any { 
                it is SettingsEffect.ShowBackupSuccess && 
                it.backupData == "backup_data_json" 
            } shouldBe true
        }
        
        @Test
        @DisplayName("데이터 복원")
        fun testRestoreData() = testScope.runTest {
            // Given
            val backupData = "backup_data_json"
            val effects = mutableListOf<SettingsEffect>()
            viewModel.effect.toList(effects)
            
            coEvery { preferencesRepository.importData(any()) } returns true
            
            // When
            viewModel.handleEvent(SettingsEvent.RestoreData(backupData))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.importData(backupData) }
            effects.any { it is SettingsEffect.ShowRestoreSuccess } shouldBe true
        }
        
        @Test
        @DisplayName("캐시 삭제")
        fun testClearCache() = testScope.runTest {
            // Given
            val effects = mutableListOf<SettingsEffect>()
            viewModel.effect.toList(effects)
            
            coEvery { preferencesRepository.clearCache() } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ClearCache)
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.clearCache() }
            effects.any { it is SettingsEffect.ShowCacheClearedMessage } shouldBe true
        }
    }
    
    @Nested
    @DisplayName("개발자 설정")
    inner class DeveloperSettingsTest {
        
        @Test
        @DisplayName("디버그 모드 활성화")
        fun testEnableDebugMode() = testScope.runTest {
            // Given
            coEvery { preferencesRepository.setDebugModeEnabled(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ToggleDebugMode(true))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.setDebugModeEnabled(true) }
            viewModel.viewState.value.debugModeEnabled shouldBe true
        }
        
        @Test
        @DisplayName("로그 레벨 변경")
        fun testChangeLogLevel() = testScope.runTest {
            // Given
            val logLevel = "VERBOSE"
            coEvery { preferencesRepository.setLogLevel(any()) } just Runs
            
            // When
            viewModel.handleEvent(SettingsEvent.ChangeLogLevel(logLevel))
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.setLogLevel(logLevel) }
            viewModel.viewState.value.currentLogLevel shouldBe logLevel
        }
        
        @Test
        @DisplayName("개발자 통계 표시")
        fun testShowDeveloperStats() = testScope.runTest {
            // Given
            val stats = mapOf(
                "totalQueries" to 1234,
                "cacheHitRate" to 0.85,
                "avgResponseTime" to 45.6
            )
            
            coEvery { preferencesRepository.getDeveloperStats() } returns stats
            
            // When
            viewModel.handleEvent(SettingsEvent.ShowDeveloperStats)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.value.developerStats shouldBe stats
        }
    }
    
    @Nested
    @DisplayName("오류 처리")
    inner class ErrorHandlingTest {
        
        @Test
        @DisplayName("설정 로드 실패")
        fun testLoadSettingsError() = testScope.runTest {
            // Given
            val error = Exception("Failed to load settings")
            coEvery { preferencesRepository.getUserProfile() } throws error
            
            // When
            viewModel.handleEvent(SettingsEvent.LoadSettings)
            advanceUntilIdle()
            
            // Then
            val state = viewModel.viewState.value
            state.error shouldNotBe null
            state.error?.message shouldBe "Failed to load settings"
            state.isLoading shouldBe false
        }
        
        @Test
        @DisplayName("BLE 연결 해제 실패")
        fun testDisconnectError() = testScope.runTest {
            // Given
            val effects = mutableListOf<SettingsEffect>()
            viewModel.effect.toList(effects)
            
            coEvery { bleRepository.disconnect() } returns false
            
            // When
            viewModel.handleEvent(SettingsEvent.DisconnectDevice)
            advanceUntilIdle()
            
            // Then
            effects.any { 
                it is SettingsEffect.ShowError && 
                it.message.contains("disconnect") 
            } shouldBe true
        }
        
        @Test
        @DisplayName("데이터 복원 실패")
        fun testRestoreDataError() = testScope.runTest {
            // Given
            val effects = mutableListOf<SettingsEffect>()
            viewModel.effect.toList(effects)
            
            coEvery { preferencesRepository.importData(any()) } returns false
            
            // When
            viewModel.handleEvent(SettingsEvent.RestoreData("invalid_data"))
            advanceUntilIdle()
            
            // Then
            effects.any { 
                it is SettingsEffect.ShowError && 
                it.message.contains("restore") 
            } shouldBe true
        }
    }
}

// Settings 관련 클래스들이 없는 경우를 위한 임시 정의
data class SettingsViewState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val bleConnectionState: BleConnectionState = BleConnectionState.Disconnected,
    val deviceBatteryLevel: Int = 0,
    val autoReconnectEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val midnightResetNotificationEnabled: Boolean = true,
    val goalAchievementNotificationEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val currentLanguage: String = "ko",
    val hapticFeedbackEnabled: Boolean = true,
    val debugModeEnabled: Boolean = false,
    val currentLogLevel: String = "INFO",
    val developerStats: Map<String, Any>? = null,
    val error: Throwable? = null
)

sealed class SettingsEvent {
    object LoadSettings : SettingsEvent()
    data class UpdateUserName(val name: String) : SettingsEvent()
    data class UpdateTargetCount(val count: Int) : SettingsEvent()
    object ResetAllData : SettingsEvent()
    object DisconnectDevice : SettingsEvent()
    data class ToggleAutoReconnect(val enabled: Boolean) : SettingsEvent()
    data class ToggleNotifications(val enabled: Boolean) : SettingsEvent()
    data class ToggleMidnightResetNotification(val enabled: Boolean) : SettingsEvent()
    data class ToggleGoalAchievementNotification(val enabled: Boolean) : SettingsEvent()
    data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent()
    data class ChangeLanguage(val language: String) : SettingsEvent()
    data class ToggleHapticFeedback(val enabled: Boolean) : SettingsEvent()
    object BackupData : SettingsEvent()
    data class RestoreData(val data: String) : SettingsEvent()
    object ClearCache : SettingsEvent()
    data class ToggleDebugMode(val enabled: Boolean) : SettingsEvent()
    data class ChangeLogLevel(val level: String) : SettingsEvent()
    object ShowDeveloperStats : SettingsEvent()
}

sealed class SettingsEffect {
    object ShowResetConfirmation : SettingsEffect()
    data class ShowBackupSuccess(val backupData: String) : SettingsEffect()
    object ShowRestoreSuccess : SettingsEffect()
    object ShowCacheClearedMessage : SettingsEffect()
    data class ShowError(val message: String) : SettingsEffect()
}