package com.wishring.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToWishInput: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)
    
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
                HomeEffect.NavigateToSettings -> {
                    onNavigateToSettings()
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
private fun HomeScreenContent(
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
            
            // 오늘의 카운트 카드 (진행중인 위시가 있으면 표시)
            if (uiState.todayWishCount != null) {
                TodayCountCard(
                    currentCount = uiState.todayWishCount.totalCount,
                    targetCount = uiState.todayWishCount.targetCount
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            
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
            modifier = Modifier.align(Alignment.BottomCenter)
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
    }
}

@Composable
private fun TodayCountCard(
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishCount = WishCount(
                    date = "2024-01-15",
                    totalCount = 700,
                    wishText = "매일 운동하기",
                    targetCount = 1000,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                recentRecords = generateDummyRecords(),
                deviceBatteryLevel = 76,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Zero Wishes State")
@Composable
private fun HomeScreenZeroWishesPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishCount = null,
                recentRecords = emptyList(),
                deviceBatteryLevel = 15,
                bleConnectionState = BleConnectionState.DISCONNECTED
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "One Wish With Button")
@Composable
private fun HomeScreenOneWishWithButtonPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishCount = null,
                recentRecords = generateDummyRecords().take(1),
                deviceBatteryLevel = 80,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Two Wishes With Button")
@Composable
private fun HomeScreenTwoWishesWithButtonPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishCount = null,
                recentRecords = generateDummyRecords().take(2),
                deviceBatteryLevel = 60,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "One Wish 100% Complete")
@Composable
private fun HomeScreenOneWishCompletePreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishCount = WishCount(
                    date = "2024-01-15",
                    totalCount = 1000, // 목표 달성
                    wishText = "매일 운동하기",
                    targetCount = 1000,
                    isCompleted = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                recentRecords = generateDummyRecords().take(1),
                deviceBatteryLevel = 85,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Long Wish Text")
@Composable
private fun HomeScreenLongWishTextPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishCount = null,
                recentRecords = listOf(
                    DailyRecord(
                        date = java.time.LocalDate.now(),
                        totalCount = 750,
                        wishText = "나는 매일 아침 일찍 일어나서 운동을 하고, 건강한 아침 식사를 먹고, 독서를 통해 새로운 지식을 습득하며, 가족과 소중한 시간을 보내고, 일에서도 최선을 다하여 더 나은 내가 되기 위해 끊임없이 노력하고 성장하는 사람이 되고 싶다.",
                        targetCount = 1000,
                        isCompleted = false
                    )
                ),
                deviceBatteryLevel = 70,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}



@Preview(showBackground = true, name = "Three Wishes State")
@Composable
private fun HomeScreenThreeWishesPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishCount = null,
                recentRecords = generateDummyRecords().take(3),
                deviceBatteryLevel = 90,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun HomeScreenLoadingPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = true,
                todayWishCount = WishCount(
                    date = "2024-01-15",
                    totalCount = 5,
                    wishText = "매일 운동하기",
                    targetCount = 10,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                deviceBatteryLevel = 50,
                bleConnectionState = BleConnectionState.CONNECTING
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

/**
 * Generate 50 dummy records for scroll testing
 */
private fun generateDummyRecords(): List<DailyRecord> {
    val wishTexts = listOf(
        "나는 어제보다 더 나은 내가 되고 있다.",
        "매일 조금씩, 나는 내 가능성을 확장하고 있다.",
        "나는 매일 배우고, 이해하고, 발전하고 있다.",
        "변화는 두렵지 않다, 나는 변화 속에서 자란다.",
        "나는 매일 감사한 마음으로 살아간다.",
        "오늘 하루도 최선을 다해 살아가고 있다.",
        "나는 끊임없이 성장하고 발전하는 사람이다.",
        "매 순간 긍정적인 에너지를 발산하고 있다.",
        "나는 내 꿈을 향해 한 걸음씩 나아가고 있다.",
        "오늘도 새로운 것을 배우며 성장한다.",
        "나는 도전을 두려워하지 않는 용기있는 사람이다.",
        "매일 건강하고 행복한 하루를 만들어간다.",
        "나는 주변 사람들에게 좋은 영향을 주는 사람이다.",
        "작은 일상에서도 기쁨을 찾아 살아간다.",
        "나는 끝까지 포기하지 않는 끈기있는 사람이다."
    )
    
    return (1..50).map { index ->
        val daysAgo = index - 1
        val date = LocalDate.now().minusDays(daysAgo.toLong())
        val wishText = wishTexts[index % wishTexts.size]
        val isCompleted = index % 3 != 0 // 2/3 확률로 완료
        val totalCount = if (isCompleted) 1000 else (200..950).random()
        
        DailyRecord(
            date = date,
            totalCount = totalCount,
            wishText = wishText,
            targetCount = 1000,
            isCompleted = isCompleted
        )
    }
}
