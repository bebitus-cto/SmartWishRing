 package com.wishring.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishring.app.presentation.settings.component.*
import com.wishring.app.ui.theme.*

/**
 * Settings screen composable
 * Manages app preferences and device settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.NavigateBack -> onNavigateBack()
                is SettingsEffect.NavigateToAbout -> onNavigateToAbout()
                is SettingsEffect.NavigateToSupport -> onNavigateToSupport()
                is SettingsEffect.ShowToast -> {
                    // Show toast
                }
                is SettingsEffect.RestartRequired -> {
                    // Show restart dialog
                }
                else -> {
                    // Handle other effects
                }
            }
        }
    }
    
    Scaffold(
        containerColor = Background_Secondary,
        topBar = {
            SettingsTopBar(
                onBackClick = { viewModel.onEvent(SettingsEvent.NavigateBack) }
            )
        }
    ) { paddingValues ->
        SettingsContent(
            viewState = viewState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun SettingsContent(
    viewState: SettingsViewState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Secondary)
            .verticalScroll(rememberScrollState())
    ) {
        // Device Settings Section
        SettingsSection(
            title = "디바이스 설정",
            modifier = Modifier.padding(top = 8.dp)
        ) {
            // Bluetooth connection
            BluetoothSettingItem(
                isConnected = viewState.isDeviceConnected,
                deviceName = viewState.connectedDeviceName,
                onConnectClick = { onEvent(SettingsEvent.ConnectDevice) },
                onDisconnectClick = { onEvent(SettingsEvent.DisconnectDevice) }
            )
            
            SettingsDivider()
            
            // Vibration settings
            ToggleSettingItem(
                title = "진동 알림",
                description = "목표 달성 시 진동으로 알림",
                icon = Icons.Default.Vibration,
                isChecked = viewState.vibrationEnabled,
                onCheckedChange = { enabled ->
                    onEvent(SettingsEvent.UpdateVibration(enabled))
                }
            )
            
            SettingsDivider()
            
            // LED settings
            ToggleSettingItem(
                title = "LED 표시",
                description = "상태를 LED로 표시",
                icon = Icons.Default.LightMode,
                isChecked = viewState.ledEnabled,
                onCheckedChange = { enabled ->
                    onEvent(SettingsEvent.UpdateLed(enabled))
                }
            )
        }
        
        // App Settings Section
        SettingsSection(title = "앱 설정") {
            // Notification settings
            ToggleSettingItem(
                title = "푸시 알림",
                description = "목표 달성 및 리마인더 알림",
                icon = Icons.Default.Notifications,
                isChecked = viewState.notificationsEnabled,
                onCheckedChange = { enabled ->
                    onEvent(SettingsEvent.UpdateNotifications(enabled))
                }
            )
            
            SettingsDivider()
            
            // Dark mode
            ToggleSettingItem(
                title = "다크 모드",
                description = "어두운 테마 사용",
                icon = Icons.Default.DarkMode,
                isChecked = viewState.darkModeEnabled,
                onCheckedChange = { enabled ->
                    onEvent(SettingsEvent.UpdateDarkMode(enabled))
                }
            )
            
            SettingsDivider()
            
            // Sound settings
            ToggleSettingItem(
                title = "효과음",
                description = "앱 내 효과음 사용",
                icon = Icons.Default.VolumeUp,
                isChecked = viewState.soundEnabled,
                onCheckedChange = { enabled ->
                    onEvent(SettingsEvent.UpdateSound(enabled))
                }
            )
        }
        
        // Data Management Section
        SettingsSection(title = "데이터 관리") {
            // Backup
            ClickableSettingItem(
                title = "데이터 백업",
                description = "클라우드에 데이터 백업",
                icon = Icons.Default.CloudUpload,
                onClick = { onEvent(SettingsEvent.BackupData) }
            )
            
            SettingsDivider()
            
            // Restore
            ClickableSettingItem(
                title = "데이터 복원",
                description = "백업된 데이터 복원",
                icon = Icons.Default.CloudDownload,
                onClick = { onEvent(SettingsEvent.RestoreData) }
            )
            
            SettingsDivider()
            
            // Clear data
            ClickableSettingItem(
                title = "데이터 초기화",
                description = "모든 데이터 삭제",
                icon = Icons.Default.DeleteForever,
                onClick = { onEvent(SettingsEvent.ClearData) },
                tintColor = Error_Medium
            )
        }
        
        // About Section
        SettingsSection(title = "정보") {
            // Version info
            InfoSettingItem(
                title = "버전 정보",
                value = viewState.appVersion,
                icon = Icons.Default.Info
            )
            
            SettingsDivider()
            
            // About
            ClickableSettingItem(
                title = "앱 정보",
                description = "WISH RING에 대하여",
                icon = Icons.Default.Info,
                onClick = { onEvent(SettingsEvent.NavigateToAbout) }
            )
            
            SettingsDivider()
            
            // Support
            ClickableSettingItem(
                title = "고객 지원",
                description = "문의하기",
                icon = Icons.Default.HelpOutline,
                onClick = { onEvent(SettingsEvent.NavigateToSupport) }
            )
            
            SettingsDivider()
            
            // Privacy policy
            ClickableSettingItem(
                title = "개인정보 처리방침",
                description = "개인정보 보호 정책 확인",
                icon = Icons.Default.PrivacyTip,
                onClick = { onEvent(SettingsEvent.OpenPrivacyPolicy) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background_Secondary
        )
    )
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun SettingsScreenPreview() {
    WishRingTheme {
        val previewState = SettingsViewState(
            isDeviceConnected = true,
            connectedDeviceName = "WISH RING #1234",
            vibrationEnabled = true,
            ledEnabled = true,
            notificationsEnabled = true,
            darkModeEnabled = false,
            soundEnabled = true,
            appVersion = "1.0.0",
            isLoading = false
        )
        
        SettingsContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}