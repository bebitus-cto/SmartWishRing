package com.wishring.app.presentation.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishring.app.R
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.presentation.detail.component.*
import com.wishring.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Detail screen composable
 * Shows detailed wish count information and motivational messages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    initialDate: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel()
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Initialize with date if provided
    LaunchedEffect(initialDate) {
        initialDate?.let {
            try {
                val date = LocalDate.parse(it)
                viewModel.onEvent(DetailEvent.SelectDate(date))
            } catch (e: Exception) {
                // Handle invalid date format
            }
        }
    }
    
    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DetailEffect.NavigateBack -> onNavigateBack()
                is DetailEffect.ShowToast -> {
                    // Show toast
                }
                is DetailEffect.ShowShareSheet -> {
                    // Handle share
                }
                else -> {}
            }
        }
    }
    
    Scaffold(
        containerColor = Background_Secondary,
        topBar = {
            DetailTopBar(
                onBackClick = { viewModel.onEvent(DetailEvent.NavigateBack) },
                onShareClick = { viewModel.onEvent(DetailEvent.ShareRecord()) }
            )
        }
    ) { paddingValues ->
        DetailContent(
            viewState = viewState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun DetailContent(
    viewState: DetailViewState,
    onEvent: (DetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Secondary)
    ) {
        // Date selector header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.White)
                .border(width = 0.dp, color = Color(0xFFC0C0C0), shape = RoundedCornerShape(0.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { /* Navigate to previous date */ },
                modifier = Modifier.padding(start = 17.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous",
                    tint = Text_Primary
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = viewState.selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Text_Primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Empty space for symmetry
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Main count display card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(162.dp),
                shape = RoundedCornerShape(5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = (viewState.currentRecord?.totalCount ?: 0).toString()
                                    .replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,"),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 58.sp
                                ),
                                color = Purple_Medium
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "회",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 36.sp
                                ),
                                color = Text_Primary,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "매일의 반복이 만든 숫자입니다.\n그 반복들이 오늘의 나를 만듭니다.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            ),
                            color = Text_Primary.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Wish text cards
            if (viewState.currentRecord != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = viewState.currentRecord.wishText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Text_Primary
                        )
                    }
                }
            }
            
            // Additional motivational messages
            viewState.motivationalMessages.take(2).forEachIndexed { index, message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (index == 0) Color(0xFFFAFAFA) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Text_Primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Character images
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.size(84.dp, 115.dp),
                    shape = RoundedCornerShape(5.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.character_white),
                        contentDescription = "White Character",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.width(26.dp))
                
                Card(
                    modifier = Modifier.size(89.dp, 115.dp),
                    shape = RoundedCornerShape(5.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.character_black),
                        contentDescription = "Black Character",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
    
    // Loading overlay
    if (viewState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun StatisticsSection(
    record: DailyRecord?,
    weeklyStats: List<DailyRecord>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "이번 주 통계",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Weekly stats summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "총 카운트",
                    value = weeklyStats.sumOf { it.totalCount }.toString()
                )
                StatItem(
                    label = "달성률",
                    value = "${calculateAchievementRate(weeklyStats)}%"
                )
                StatItem(
                    label = "연속 일수",
                    value = calculateStreak(weeklyStats).toString()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementSection(
    achievements: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "달성 뱃지",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Achievement badges would go here
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                achievements.forEach { achievement ->
                    AssistChip(
                        onClick = { },
                        label = { Text(achievement) }
                    )
                }
            }
        }
    }
}

// Helper functions
private fun calculateAchievementRate(records: List<DailyRecord>): Int {
    if (records.isEmpty()) return 0
    val achieved = records.count { it.isCompleted }
    return (achieved * 100 / records.size)
}

private fun calculateStreak(records: List<DailyRecord>): Int {
    return records.takeWhile { it.isCompleted }.size
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun DetailScreenPreview() {
    WishRingTheme {
        val previewState = DetailViewState(
            selectedDate = LocalDate.of(2025, 8, 21),
            currentRecord = DailyRecord(
                date = LocalDate.of(2025, 8, 21),
                wishText = "나는 매일 성장하고 있다",
                targetCount = 1000,
                totalCount = 1000,
                isCompleted = true
            ),
            motivationalMessages = listOf(
                "나는 어제보다 더 나은 내가 되고 있다.",
                "오늘의 선택이 나를 더 단단하게 만든다.",
                "내 안의 가능성은 멈추지 않고 자라고 있다."
            ),
            isLoading = false
        )
        
        DetailContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}