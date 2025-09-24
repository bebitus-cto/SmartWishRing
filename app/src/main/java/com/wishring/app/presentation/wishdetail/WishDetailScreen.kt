package com.wishring.app.presentation.wishdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.wishring.app.ui.theme.*
import java.time.LocalDate

/**
 * Simplified WishDetail screen matching Figma design
 * Shows count, wish text, and motivational messages for a specific date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishDetailScreen(
    initialDate: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: WishDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WishDetailEffect.NavigateBack -> onNavigateBack()
                is WishDetailEffect.ShowToast -> {
                    // Handle toast if needed
                }
            }
        }
    }

    Scaffold(
        containerColor = Background_Secondary,
        topBar = {
            WishDetailTopBar(
                onBackClick = { viewModel.onEvent(WishDetailEvent.NavigateBack) }
            )
        }
    ) { paddingValues ->
        WishDetailContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun WishDetailContent(
    uiState: WishDetailViewState,
    onEvent: (WishDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Secondary)
    ) {
        // Date selector header
        DateSelectorHeader(
            displayDate = uiState.displayDate,
            onPreviousClick = { onEvent(WishDetailEvent.NavigateToPreviousDate) },
            onNextClick = { onEvent(WishDetailEvent.NavigateToNextDate) }
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Main count display card
            CountDisplayCard(
                count = uiState.displayCount,
                isLoading = uiState.isLoading
            )

            // Wish and motivational message cards
            uiState.allMessages.forEachIndexed { index, message ->
                MessageCard(
                    message = message,
                    backgroundColor = if (index == 2) Color(0xFFFAFAFA) else Color.White
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Character images at bottom
            CharacterImagesRow()
        }
    }

    // Loading overlay
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Purple_Medium)
        }
    }

    // Error handling
    uiState.error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Could show error dialog or snackbar here
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishDetailTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Text_Primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background_Secondary
        )
    )
}

@Composable
private fun DateSelectorHeader(
    displayDate: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
            .border(width = 0.5.dp, color = Color(0xFFC0C0C0)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.padding(start = 17.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Previous Date",
                tint = Text_Primary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = displayDate,
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
}

@Composable
private fun CountDisplayCard(
    count: String,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Purple_Medium)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = count,
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
    }
}

@Composable
private fun MessageCard(
    message: String,
    backgroundColor: Color = Color.White
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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

@Composable
private fun CharacterImagesRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(84.dp, 115.dp),
            shape = RoundedCornerShape(5.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun WishDetailScreenPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 8, 21),
            targetCount = 1000,
            wishText = "나는 매일 성장하고 있다",
            motivationalMessages = listOf(
                "나는 어제보다 더 나은 내가 되고 있다.",
                "오늘의 선택이 나를 더 단단하게 만든다.",
                "내 안의 가능성은 멈추지 않고 자라고 있다."
            ),
            isLoading = false
        )

        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}