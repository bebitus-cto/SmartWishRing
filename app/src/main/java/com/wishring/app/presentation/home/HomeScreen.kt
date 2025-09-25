package com.wishring.app.presentation.home

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishring.app.MainActivity
import com.wishring.app.core.util.ShareUtils
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.presentation.home.component.BleDevicePickerDialog
import com.wishring.app.presentation.home.component.BluetoothConnectionStatus
import com.wishring.app.presentation.home.component.FloatingBottomBar
import com.wishring.app.presentation.home.component.WishHistorySection
import com.wishring.app.presentation.home.component.TodayCountCard
import com.wishring.app.presentation.home.component.WishButton
import com.wishring.app.presentation.home.component.LatestWishCard
import com.wishring.app.presentation.home.component.WishRegistrationPrompt

import com.wishring.app.presentation.main.BlePhase
import com.wishring.app.presentation.main.DeviceInfo
import com.wishring.app.presentation.main.MainViewModel
import com.wishring.app.presentation.main.MainViewModel.Companion.WR_EVENT
import com.wishring.app.ui.theme.Purple_Primary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToWishInput: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel<MainViewModel>()
) {
    val bleCommand by mainViewModel.bleCommand.collectAsStateWithLifecycle()
    
    // MainViewModel에서 위시 데이터 가져오기
    val wishHistory = bleCommand.wishHistory
    val todayWish = bleCommand.todayWish
    val pageInfo = bleCommand.pageInfo
    val isWishDataLoading = bleCommand.isWishDataLoading
    val wishDataError = bleCommand.wishDataError
    val deviceBatteryLevel = bleCommand.batteryLevel
    
    val isConnected = bleCommand.isConnected
    val scannedDevices = bleCommand.scannedDevices
    val showDevicePicker = bleCommand.shouldShowDevicePicker
    val blePhase = bleCommand.phase
    
    // HomeViewState를 MainViewModel 데이터로 구성
    val uiState = when {
        !isConnected -> HomeViewState.BluetoothDisconnected(
            wishHistory = wishHistory,
            todayWish = todayWish,
            isLoading = isWishDataLoading,
            error = wishDataError,
            pageInfo = null, // 연결 해제 시에는 null
            deviceBatteryLevel = deviceBatteryLevel
        )
        todayWish == null || (todayWish.targetCount == 0) -> HomeViewState.ConnectedNoWishes(
            wishHistory = wishHistory,
            todayWish = todayWish,
            isLoading = isWishDataLoading,
            error = wishDataError,
            pageInfo = pageInfo,
            deviceBatteryLevel = deviceBatteryLevel
        )
        todayWish.currentCount < todayWish.targetCount && todayWish.currentCount < (todayWish.targetCount * 0.8f) -> HomeViewState.ConnectedPartialWishes(
            wishHistory = wishHistory,
            todayWish = todayWish,
            isLoading = isWishDataLoading,
            error = wishDataError,
            pageInfo = pageInfo,
            deviceBatteryLevel = deviceBatteryLevel
        )
        else -> HomeViewState.ConnectedFullWishes(
            wishHistory = wishHistory,
            todayWish = todayWish,
            isLoading = isWishDataLoading,
            error = wishDataError,
            pageInfo = pageInfo,
            deviceBatteryLevel = deviceBatteryLevel
        )
    }
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)
    val context = LocalContext.current



    val activity = context as? MainActivity

    LaunchedEffect(effect) {
        effect?.let { navigationEffect ->
            when (navigationEffect) {
                is HomeEffect.NavigateToDetail -> {
                    onNavigateToDetail(navigationEffect.date)
                }

                HomeEffect.NavigateToWishInput -> {
                    onNavigateToWishInput()
                }

                is HomeEffect.ShareImageWithIntent -> {
                    ShareUtils.shareImageWithText(
                        context = context,
                        imageFile = navigationEffect.imageFile,
                        message = navigationEffect.message,
                        hashtags = navigationEffect.hashtags
                    )
                }

                HomeEffect.EnableBluetooth -> {
                    // 블루투스 활성화 시스템 다이얼로그 표시
                    val enableBtIntent =
                        android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    context.startActivity(enableBtIntent)
                }

                HomeEffect.RequestBluetoothPermissions -> {
                    // MainActivity에서 BLE 스캔 시작 (권한 체크 포함)
                    val activity = context as? ComponentActivity
                    if (activity is MainActivity) {
                        activity.startBleScan()
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "권한 요청 실패",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                is HomeEffect.ShowPermissionExplanation -> {
                    // 권한 설명 다이얼로그 표시 - UI에서 처리됨
                }

                is HomeEffect.ShowPermissionDenied -> {
                    // 권한 거부 안내 다이얼로그 표시 - UI에서 처리됨  
                }

                HomeEffect.OpenAppSettings -> {
                    // 앱 설정 화면으로 이동
                    val intent =
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .apply {
                                data =
                                    android.net.Uri.fromParts("package", context.packageName, null)
                            }
                    context.startActivity(intent)
                }

                is HomeEffect.UpdateBluetoothProgress -> {
                    // 블루투스 진행 상태 업데이트 - UI에서 처리됨
                }

                is HomeEffect.ShowToast -> {
                    android.widget.Toast.makeText(
                        context,
                        navigationEffect.message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                is HomeEffect.PlaySound -> {
                    // Handle sound effects - could implement MediaPlayer here
                    // For now, just use system notification sound for success
                    if (navigationEffect.soundType == SoundType.SUCCESS) {
                        val notification =
                            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone =
                            android.media.RingtoneManager.getRingtone(context, notification)
                        ringtone?.play()
                    }
                }


                HomeEffect.ShowConnectionSuccessAnimation -> {
                    // Connection success animation handled in UI state
                }

                is HomeEffect.ShowBleDevicePicker -> {
                    // ShowBleDevicePicker effect는 더 이상 필요없음 - 상태 기반으로 처리
                }

                else -> {
                    // Handle other effects like sharing, errors, etc.
                }
            }
        }
    }

    // Load initial data
    LaunchedEffect(Unit) {
        mainViewModel.loadInitialWishData()
        
        // 연결된 상태에서 HomeScreen 진입 시 배터리 정보 적극적으로 요청
        if (isConnected) {
            Log.d("HomeScreen", "[배터리] HomeScreen 진입 - 연결된 상태에서 배터리 정보 요청")
            activity?.refreshBatteryLevel()
        }

    // Battery level is already handled by MainViewModel
    // BLE connection state is already handled by MainViewModel
    }

    // Event handler that delegates to MainViewModel
    val onEvent: (HomeEvent) -> Unit = { event ->
        when (event) {
            is HomeEvent.NavigateToWishInput -> {
                viewModel.onEvent(event) // UI navigation은 HomeViewModel에서
            }
            is HomeEvent.NavigateToDetail -> {
                viewModel.onEvent(event) // UI navigation은 HomeViewModel에서  
            }
            is HomeEvent.ShareAchievement -> {
                viewModel.onEvent(event) // UI effects는 HomeViewModel에서
            }
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onEvent = onEvent,
        scannedDevices = scannedDevices,
        showDevicePicker = showDevicePicker,
        blePhase = blePhase,
        activity = activity,
        mainViewModel = mainViewModel,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeViewState,
    onEvent: (HomeEvent) -> Unit,
    scannedDevices: List<DeviceInfo>,
    showDevicePicker: Boolean,
    blePhase: BlePhase,
    activity: MainActivity?,
    mainViewModel: MainViewModel = hiltViewModel<MainViewModel>(),
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is HomeViewState.BluetoothDisconnected -> {
            BluetoothDisconnectedContent(
                uiState = uiState,
                onEvent = onEvent,
                activity = activity,
                mainViewModel = mainViewModel,
                blePhase = blePhase,
                scannedDevices = scannedDevices,
                showDevicePicker = showDevicePicker,
                modifier = modifier
            )
        }

        is HomeViewState.ConnectedNoWishes -> {
            ConnectedNoWishesContent(
                uiState = uiState,
                onEvent = onEvent,
                mainViewModel = mainViewModel,
                modifier = modifier
            )
        }

        is HomeViewState.ConnectedPartialWishes -> {
            ConnectedPartialWishesContent(
                uiState = uiState,
                onEvent = onEvent,
                mainViewModel = mainViewModel,
                modifier = modifier
            )
        }

        is HomeViewState.ConnectedFullWishes -> {
            ConnectedFullWishesContent(
                uiState = uiState,
                onEvent = onEvent,
                mainViewModel = mainViewModel,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun BluetoothDisconnectedContent(
    uiState: HomeViewState.BluetoothDisconnected,
    onEvent: (HomeEvent) -> Unit,
    activity: MainActivity?,
    mainViewModel: MainViewModel,
    blePhase: BlePhase,
    scannedDevices: List<DeviceInfo>,
    showDevicePicker: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FF))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {

        // 블루투스 상태 및 연결 버튼 (연결되지 않은 경우에만 표시)
        // 현재 시간
        val currentTime = System.currentTimeMillis()
        // 3초 제한 체크 (마지막 스캔으로부터 3초 경과했는지)
        val canScan = (currentTime - uiState.lastBleScanTime) >= 3000L

        BluetoothConnectionStatus(
            onClick = {
                Log.i(WR_EVENT, "[HomeScreen] ========== 수동 연결 버튼 클릭 ==========")
                Log.i(WR_EVENT, "[HomeScreen] [MANUAL_CONNECT_DEBUG] === 수동 연결 플로우 시작 ===")

                // 1. 현재 상태 로깅
                Log.i(
                    WR_EVENT,
                    "[HomeScreen] [MANUAL_CONNECT_DEBUG] 1. 현재 시간: $currentTime"
                )
                Log.i(
                    WR_EVENT,
                    "[HomeScreen] [MANUAL_CONNECT_DEBUG] 2. 마지막 스캔 시간: ${uiState.lastBleScanTime}"
                )
                Log.i(WR_EVENT, "[HomeScreen] [MANUAL_CONNECT_DEBUG] 5. canScan: $canScan")

                // 연결 시도 상태가 아닐 때만 실행
                if (!uiState.isScanning && !uiState.isAttemptingConnection) {

                    if (canScan) {
                        Log.i(
                            WR_EVENT,
                            "[HomeScreen] [MANUAL_CONNECT_DEBUG] 7. ✅ 스캔 가능 - 스캔 시작"
                        )

                        // MainActivity 액세스 확인
                        if (activity == null) {
                            Log.e(
                                WR_EVENT,
                                "[HomeScreen] [MANUAL_CONNECT_DEBUG] ❌ MainActivity 참조 없음!"
                            )
                            return@BluetoothConnectionStatus
                        }

                        try {
                            activity.startBleScan()
                        } catch (e: Exception) {
                            Log.e(
                                WR_EVENT,
                                "[HomeScreen] [MANUAL_CONNECT_DEBUG] ❌ 스캔 시작 실패",
                                e
                            )
                        }

                    } else {
                        val remainingTime =
                            ((3000L - (currentTime - uiState.lastBleScanTime)) / 1000L).toInt() + 1
                        Log.w(
                            WR_EVENT,
                            "[HomeScreen] [MANUAL_CONNECT_DEBUG] 7. ⏱️ 스캔 쿨다운: ${remainingTime}초 대기 필요"
                        )

                        android.widget.Toast.makeText(
                            activity,
                            "${remainingTime}초 후에 다시 시도해주세요",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            uiState = uiState,
            blePhase = blePhase
        )

        // Show loading overlay
        if (uiState.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { /* Block clicks */ }
            ) {
                CircularProgressIndicator(
                    color = Purple_Primary
                )
            }
        }

        // Show error snackbar
        uiState.error?.let { errorMessage ->
            LaunchedEffect(errorMessage) {
                // Show error snackbar or dialog
                // For now, just log the error
                println("Home Error: $errorMessage")
                mainViewModel.dismissWishDataError()
            }
        }

        if (showDevicePicker && scannedDevices.isNotEmpty()) {
            Log.i(WR_EVENT, "[HomeScreen] ===== BLE 기기 선택 다이얼로그 표시 =====")
            BleDevicePickerDialog(
                devices = scannedDevices.map {
                    DeviceInfo(it.name, it.address, it.rssi)
                },
                onDeviceSelected = { deviceAddress ->
                    try {
                        if (activity == null) {
                            return@BleDevicePickerDialog
                        }

                        activity.connectToDeviceByAddress(deviceAddress)
                        mainViewModel.selectDevice()

                    } catch (e: Exception) {
                        Log.e(WR_EVENT, "[HomeScreen] [DEVICE_CONNECT_DEBUG] ❌ 기기 연결 시작 실패", e)
                    }
                },
                onDismiss = {
                    mainViewModel.dismissDevicePicker()
                }
            )
        }
    }
}

@Composable
private fun ConnectedNoWishesContent(
    uiState: HomeViewState.ConnectedNoWishes,
    onEvent: (HomeEvent) -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FF))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // 오늘의 위시 카드 (오늘의 위시가 있을 때만 표시)
            uiState.todayWish?.let { todayWish ->
                LatestWishCard(
                    latestRecord = WishDayUiState(
                        date = LocalDate.now(),
                        wishText = todayWish.wishText,
                        isCompleted = todayWish.isCompleted,
                        targetCount = todayWish.targetCount,
                        completedCount = todayWish.currentCount
                    ),
                    onWishClick = { date ->
                        onEvent(HomeEvent.NavigateToDetail(date))
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 오늘의 카운트 카드
            if (uiState.todayWish != null) {
                TodayCountCard(
                    currentCount = uiState.todayWish.currentCount,
                    targetCount = uiState.todayWish.targetCount,
                    uiState = uiState,
                    onEvent = onEvent
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 완전히 비어있는 상태 - WishRegistrationPrompt
            WishRegistrationPrompt(
                onClick = { onEvent(HomeEvent.NavigateToWishInput) },
                remainingCount = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Report Card (with infinite scroll)
            WishHistorySection(
                uiState = uiState,
                onEvent = onEvent,
                onLoadMore = { mainViewModel.loadMoreWishes() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom spacing for floating bottom bar
            Spacer(modifier = Modifier.height(100.dp))
        }

        FloatingBottomBar(
            uiState = uiState,
            isConnected = true,
            onShareClick = { onEvent(HomeEvent.ShareAchievement) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )

        // Common overlays
        ConnectedContentOverlays(uiState = uiState, onEvent = onEvent, onDismissError = { mainViewModel.dismissWishDataError() })

    }
}

@Composable
private fun ConnectedPartialWishesContent(
    uiState: HomeViewState.ConnectedPartialWishes,
    onEvent: (HomeEvent) -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FF))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // 오늘의 위시 카드 (오늘의 위시가 있을 때만 표시)
            uiState.todayWish?.let { todayWish ->
                LatestWishCard(
                    latestRecord = WishDayUiState(
                        date = LocalDate.now(),
                        wishText = todayWish.wishText,
                        isCompleted = todayWish.isCompleted,
                        targetCount = todayWish.targetCount,
                        completedCount = todayWish.currentCount
                    ),
                    onWishClick = { date ->
                        onEvent(HomeEvent.NavigateToDetail(date))
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 오늘의 카운트 카드
            if (uiState.todayWish != null) {
                TodayCountCard(
                    currentCount = uiState.todayWish.currentCount,
                    targetCount = uiState.todayWish.targetCount,
                    uiState = uiState,
                    onEvent = onEvent
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 부분적인 상태 - WishButton
            WishButton(
                onClick = { onEvent(HomeEvent.NavigateToWishInput) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Report Card (with infinite scroll)
            WishHistorySection(
                uiState = uiState,
                onEvent = onEvent,
                onLoadMore = { mainViewModel.loadMoreWishes() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom spacing for floating bottom bar
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Floating Bottom Bar
        FloatingBottomBar(
            uiState = uiState,
            isConnected = true,
            onShareClick = { onEvent(HomeEvent.ShareAchievement) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )

        // Common overlays
        ConnectedContentOverlays(uiState = uiState, onEvent = onEvent, onDismissError = { mainViewModel.dismissWishDataError() })

    }
}

@Composable
private fun ConnectedFullWishesContent(
    uiState: HomeViewState.ConnectedFullWishes,
    onEvent: (HomeEvent) -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FF))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // 오늘의 위시 카드 (오늘의 위시가 있을 때만 표시)
            uiState.todayWish?.let { todayWish ->
                LatestWishCard(
                    latestRecord = WishDayUiState(
                        date = LocalDate.now(),
                        wishText = todayWish.wishText,
                        isCompleted = todayWish.isCompleted,
                        targetCount = todayWish.targetCount,
                        completedCount = todayWish.currentCount
                    ),
                    onWishClick = { date ->
                        onEvent(HomeEvent.NavigateToDetail(date))
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 오늘의 카운트 카드
            if (uiState.todayWish != null) {
                TodayCountCard(
                    currentCount = uiState.todayWish.currentCount,
                    targetCount = uiState.todayWish.targetCount,
                    uiState = uiState,
                    onEvent = onEvent
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Report Card (with infinite scroll)
            WishHistorySection(
                uiState = uiState,
                onEvent = onEvent,
                onLoadMore = { mainViewModel.loadMoreWishes() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom spacing for floating bottom bar
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Floating Bottom Bar
        FloatingBottomBar(
            uiState = uiState,
            isConnected = true,
            onShareClick = { onEvent(HomeEvent.ShareAchievement) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )

        // Common overlays
        ConnectedContentOverlays(uiState = uiState, onEvent = onEvent, onDismissError = { mainViewModel.dismissWishDataError() })

    }
}

@Composable
private fun ConnectedContentOverlays(
    uiState: HomeViewState,
    onEvent: (HomeEvent) -> Unit,
    onDismissError: () -> Unit
) {
    if (uiState.isLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { /* Block clicks */ }
        ) {
            CircularProgressIndicator(
                color = Purple_Primary
            )
        }
    }

    uiState.error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            println("Home Error: $errorMessage")
            onDismissError()
        }
    }
}