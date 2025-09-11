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
            
            // Header text
            Text(
                text = "나는 매일 성장하고 있다.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Conditional UI based on today's wish registration
            if (uiState.todayWishCount != null) {
                // Show TopContentCard if today's wish exists
                TopContentCard(
                    currentCount = uiState.currentCount,
                    targetCount = uiState.targetCount
                )
            } else {
                // Show wish registration prompt if no today's wish
                WishRegistrationPrompt(
                    onClick = { onEvent(HomeEvent.NavigateToWishInput) }
                )
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
            batteryLevel = uiState.batteryLevelText,
            showLowBatteryWarning = uiState.showLowBatteryWarning,
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
private fun TopContentCard(
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
            // Header message
            Text(
                text = stringResource(id = R.string.home_header_message),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Text_Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
                        current = 420/*currentCount*/,
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
private fun WishCard(
    wishText: String,
    date: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(5.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = wishText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF333333),
                maxLines = 2,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF999999)
                )
                
                Text(
                    text = "${count}회",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Purple_Medium
                )
            }
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
            
            if (uiState.hasRecentRecords) {
                // Show top 3 wishes as fixed cards
                val topWishes = uiState.recentRecords.take(3)
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    topWishes.forEach { record ->
                        WishCard(
                            wishText = record.wishText,
                            date = record.dateString,
                            count = record.totalCount,
                            onClick = {
                                onEvent(HomeEvent.NavigateToDetail(record.dateString))
                            }
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.empty_state_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Text_Secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.empty_state_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Text_Tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomBar(
    batteryLevel: String,
    showLowBatteryWarning: Boolean,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_battery),
                    contentDescription = stringResource(id = R.string.battery_description),
                    tint = if (showLowBatteryWarning) Color.Red else Text_Secondary,
                    modifier = Modifier.size(width = 37.dp, height = 21.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (batteryLevel.isEmpty() || batteryLevel == "--") "76%" else batteryLevel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (showLowBatteryWarning) Color.Red else Text_Secondary
                )
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

@Preview(showBackground = true, name = "Empty State")
@Composable
private fun HomeScreenEmptyPreview() {
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
