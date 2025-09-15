package com.wishring.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishring.app.R
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.presentation.component.CircularProgress
import com.wishring.app.presentation.component.WishCard
import com.wishring.app.presentation.home.component.WishReportItem
import com.wishring.app.ui.theme.Purple_Medium
import com.wishring.app.ui.theme.Purple_Primary
import com.wishring.app.ui.theme.Text_Primary
import com.wishring.app.ui.theme.Text_Secondary
import com.wishring.app.ui.theme.Text_Tertiary
import com.wishring.app.ui.theme.WishRingTheme
import java.time.LocalDate
import com.wishring.app.presentation.components.ShareDialog
import com.wishring.app.presentation.components.PermissionExplanationDialog
import com.wishring.app.presentation.components.PermissionDeniedDialog
import com.wishring.app.presentation.components.ConnectionSuccessAnimation
import com.wishring.app.core.util.ShareUtils
import com.wishring.app.MainActivity
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToWishInput: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)
    val context = LocalContext.current
    
    // Handle navigation effects
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
                    val enableBtIntent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    context.startActivity(enableBtIntent)
                }
                
                HomeEffect.RequestBluetoothPermissions -> {
                    // MainActivity에서 권한 요청 실행
                    val activity = context as? ComponentActivity
                    if (activity is MainActivity) {
                        activity.requestBluetoothPermissions()
                    } else {
                        android.widget.Toast.makeText(context, "권한 요청 실패", android.widget.Toast.LENGTH_SHORT).show()
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
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
                
                is HomeEffect.UpdateBluetoothProgress -> {
                    // 블루투스 진행 상태 업데이트 - UI에서 처리됨
                }
                
                is HomeEffect.ShowToast -> {
                    android.widget.Toast.makeText(context, navigationEffect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                
                is HomeEffect.PlaySound -> {
                    // Handle sound effects - could implement MediaPlayer here
                    // For now, just use system notification sound for success
                    if (navigationEffect.soundType == SoundType.SUCCESS) {
                        val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = android.media.RingtoneManager.getRingtone(context, notification)
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
        viewModel.onEvent(HomeEvent.LoadData)
    }
    
    HomeScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeViewState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // 위시 리스트 섹션 (과거 기록이 있으면 표시)
            if (uiState.recentRecords.isNotEmpty()) {
                WishListSection(
                    recentRecords = uiState.recentRecords,
                    onWishClick = { date ->
                        onEvent(HomeEvent.NavigateToDetail(date))
                    }
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
            
            // 오늘의 카운트 카드
            if (uiState.todayWishCount != null) {
                TodayCountCard(
                    currentCount = uiState.currentCount,
                    targetCount = uiState.targetCount,
                    uiState = uiState,
                    onEvent = onEvent
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // 디버깅: BLE 연결 상태 표시
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔍 BLE 디버그 정보",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "상태: ${uiState.bleConnectionState}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text_Secondary
                    )
                    Text(
                        text = "연결됨: ${if (uiState.isBleConnected) "✅ 예" else "❌ 아니오"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text_Secondary
                    )
                    if (uiState.isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = Purple_Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "스캔 중...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple_Primary
                            )
                        }
                    }
                }
            }
            
            // 블루투스 상태 및 연결 버튼 (연결되지 않은 경우에만 표시)
            if (!uiState.isBleConnected) {
                BluetoothConnectionStatus(
                    onClick = { 
                        // BLE 스캔 시작 (EnableBluetooth가 아닌 StartBleScanning 호출)
                        onEvent(HomeEvent.StartBleScanning) 
                    },
                    isScanning = uiState.isLoading
                )
            } else {
                Text(
                    text = "✅ WISH RING 연결됨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Purple_Medium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            // 버튼 표시 로직
            when {
                uiState.todayWishCount == null && uiState.recentRecords.isEmpty() -> {
                    // 완전히 비어있는 상태 (0개)
                    WishRegistrationPrompt(
                        onClick = { onEvent(HomeEvent.NavigateToWishInput) },
                        remainingCount = 3
                    )
                }
                uiState.todayWishCount == null && uiState.recentRecords.size < 3 -> {
                    // 과거 기록은 있지만 3개 미만이고 오늘 위시 없음 (1-2개)
                    WishButton(
                        onClick = { onEvent(HomeEvent.NavigateToWishInput) }
                    )
                }
                // 그 외 경우: 3개 이상이거나 오늘 위시 진행중이면 버튼 없음
            }
            
            Spacer(modifier = Modifier.height(50.dp))
            
            // Report Card
            ReportCard(
                uiState = uiState,
                onEvent = onEvent
            )
            
            // Bottom spacing for floating bottom bar
            Spacer(modifier = Modifier.height(120.dp))
        }
        
        // Floating Bottom Bar
        FloatingBottomBar(
            uiState = uiState,
            onShareClick = { onEvent(HomeEvent.ShareAchievement) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
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
                onEvent(HomeEvent.DismissError)
            }
        }
        
        // Share dialog
        if (uiState.showShareDialog) {
            ShareDialog(
                count = uiState.totalCount,
                onConfirm = { message, hashtags ->
                    onEvent(HomeEvent.ConfirmShare(message, hashtags))
                },
                onDismiss = {
                    onEvent(HomeEvent.DismissShareDialog)
                }
            )
        }
        
        // Permission explanation dialog
        if (uiState.showPermissionExplanation) {
            PermissionExplanationDialog(
                explanations = uiState.permissionExplanations,
                onRequestPermissions = {
                    onEvent(HomeEvent.RequestPermissionsFromExplanation)
                },
                onDismiss = {
                    onEvent(HomeEvent.DismissPermissionExplanation)
                }
            )
        }
        
        // Permission denied dialog
        if (uiState.showPermissionDenied) {
            PermissionDeniedDialog(
                message = uiState.permissionDeniedMessage,
                onOpenSettings = {
                    onEvent(HomeEvent.OpenAppSettingsFromDialog)
                },
                onDismiss = {
                    onEvent(HomeEvent.DismissPermissionDenied)
                }
            )
        }
        
        // BLE Device Picker Dialog
        if (uiState.showBleDevicePicker) {
            BleDevicePickerDialog(
                devices = uiState.availableBleDevices,
                onDeviceSelected = { deviceAddress ->
                    onEvent(HomeEvent.SelectBleDevice(deviceAddress))
                },
                onDismiss = {
                    onEvent(HomeEvent.DismissBleDevicePicker)
                }
            )
        }
        
        // Connection success animation
        if (uiState.showConnectionSuccessAnimation) {
            ConnectionSuccessAnimation(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun TodayCountCard(
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier,
    uiState: HomeViewState? = null,
    onEvent: ((HomeEvent) -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp)
        ) {

            
            // Count and Progress Row with Divider
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Today's Count + Number
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.todays_count),
                            color = Color(0xFF333333),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentCount.toString(),
                            color = Color(0xFF333333),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                // Center: Vertical Divider
                VerticalDivider(
                    color = Color(0xFFDBDBDB),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                )
                
                // Right: Circular Progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    CircularProgress(
                        current = currentCount,
                        target = targetCount,
                        modifier = Modifier.size(120.dp),
                        showText = true
                    )
                }
            }
        }
    }
}

@Composable
private fun WishNavigationHeader(
    wishText: String,
    navigationText: String,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        IconButton(
            onClick = onPreviousClick,
            enabled = canNavigatePrevious,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous wish",
                tint = if (canNavigatePrevious) Purple_Medium else Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Center content: Wish text and navigation indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = wishText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = navigationText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Purple_Medium
            )
        }
        
        // Next button
        IconButton(
            onClick = onNextClick,
            enabled = canNavigateNext,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Next wish",
                tint = if (canNavigateNext) Purple_Medium else Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WishRegistrationPrompt(
    onClick: () -> Unit,
    remainingCount: Int = 3,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp)
        ) {
            Text(
                text = "오늘의 위시를 등록하세요",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "매일 새로운 목표를 설정하여\n꾸준히 성장해보세요",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${remainingCount}개를 더 등록할 수 있어요",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Purple_Medium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple_Medium
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "WISH 등록하기",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}



@Composable
private fun WishListSection(
    recentRecords: List<DailyRecord>,
    onWishClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        recentRecords.take(1).forEach { record ->
            WishCard(
                wishText = record.wishText,
                onClick = {
                    onWishClick(record.dateString)
                }
            )
        }
    }
}

@Composable
private fun WishButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Purple_Medium
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wish_button_text),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
    }
}

@Composable
private fun BluetoothConnectionStatus(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isScanning: Boolean = false
) {
    // 블루투스 연결하기 버튼
    Button(
        onClick = onClick,
        enabled = !isScanning,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isScanning) Color(0xFF90CAF9) else Color(0xFF2196F3),
            disabledContainerColor = Color(0xFF90CAF9)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(id = android.R.drawable.stat_sys_data_bluetooth),
                    contentDescription = "블루투스",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isScanning) "기기 검색 중..." else "WISH RING 연결하기",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun ReportCard(
    uiState: HomeViewState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFAFAFA),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("내일을 만드는 ")
                    withStyle(style = SpanStyle(color = Color(0xFF6A5ACD))) {
                        append("WISH")
                    }
                    append(" 리포트")
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Text_Primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Report content (wish list moved to top section)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Text(
                    text = "이전 WISH 데이터가 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text_Secondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FloatingBottomBar(
    uiState: HomeViewState,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp)
        ) {
            // Battery status
            // Battery level display - only show when connected and available
            if (uiState.shouldShowBatteryLevel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_battery),
                        contentDescription = stringResource(id = R.string.battery_description),
                        tint = if (uiState.deviceBatteryLevel != null && uiState.deviceBatteryLevel!! < 20) Color.Red else Text_Secondary,
                        modifier = Modifier.size(width = 37.dp, height = 21.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.deviceBatteryLevel ?: 0}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (uiState.showLowBatteryWarning) Color.Red else Text_Secondary
                    )
                }
            } else {
                // Empty space when battery not available
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Share button
            IconButton(
                onClick = onShareClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera),
                    contentDescription = stringResource(id = R.string.share_description),
                    tint = Purple_Medium,
                    modifier = Modifier.size(width = 33.dp, height = 27.dp)
                )
            }
        }
    }
}

/**
 * BLE Device Picker Dialog
 * Shows available BLE devices for selection
 */
@Composable
private fun BleDevicePickerDialog(
    devices: List<DeviceInfo>,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "블루투스 기기 선택",
                style = MaterialTheme.typography.headlineSmall,
                color = Text_Primary
            )
        },
        text = {
            Column {
                Text(
                    text = "연결할 WISH RING 기기를 선택하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text_Secondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(devices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onDeviceSelected(device.address) },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Device icon
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = Purple_Medium,
                                    modifier = Modifier.size(12.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name.takeIf { it.isNotBlank() } ?: "Unknown Device",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Text_Primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Text_Secondary
                                    )
                                }
                                
                                // Signal strength
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${device.rssi} dBm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Text_Tertiary
                                    )
                                    // Signal strength bars
                                    Row {
                                        repeat(4) { index ->
                                            val isActive = when {
                                                device.rssi >= -50 -> index < 4
                                                device.rssi >= -60 -> index < 3
                                                device.rssi >= -70 -> index < 2
                                                else -> index < 1
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(((index + 1) * 3).dp)
                                                    .background(
                                                        if (isActive) Purple_Medium else Color.Gray.copy(alpha = 0.3f),
                                                        RoundedCornerShape(1.dp)
                                                    )
                                            )
                                            if (index < 3) Spacer(modifier = Modifier.width(1.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "취소",
                    color = Text_Secondary
                )
            }
        },
        containerColor = Color.White,
        modifier = Modifier.padding(16.dp)
    )
}
