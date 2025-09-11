package com.wishring.app.presentation.wishinput

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishring.app.R
import com.wishring.app.presentation.wishinput.component.*
import com.wishring.app.ui.theme.*

/**
 * Wish input screen composable
 * Allows users to set their wish text and target count
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishInputScreen(
    onNavigateBack: () -> Unit = {},
    onWishSaved: () -> Unit = {},
    viewModel: WishInputViewModel = hiltViewModel()
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WishInputEffect.NavigateBack -> onNavigateBack()
                is WishInputEffect.NavigateToHomeWithResult -> onWishSaved()
                is WishInputEffect.ShowToast -> {
                    // Show toast message
                }
                is WishInputEffect.ShowValidationError -> {
                    // Show validation error
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
            WishInputTopBar(
                onBackClick = { viewModel.onEvent(WishInputEvent.NavigateBack) }
            )
        }
    ) { paddingValues ->
        WishInputContent(
            viewState = viewState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun WishInputContent(
    viewState: WishInputViewState,
    onEvent: (WishInputEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Secondary)
    ) {
        // Background image with opacity
        Image(
            painter = painterResource(id = R.drawable.input_background_32926f),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(169.dp, 127.dp)
                .alpha(0.5f),
            contentScale = ContentScale.Fit
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section 1: Wish text input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "1. 원하는 것을 적어보세요",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Text_Primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                color = Color(0xFFF9FBFF),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFF0F0F0),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .padding(8.dp)
                    ) {
                        if (viewState.wishText.isEmpty()) {
                            Text(
                                text = "(예: 확언문장, 기도문, 이루고 싶은 목표 / 최대 10개)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = Text_Primary.copy(alpha = 0.5f)
                            )
                        }
                        
                        BasicTextField(
                            value = viewState.wishText,
                            onValueChange = { text ->
                                onEvent(WishInputEvent.UpdateWishText(text))
                            },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = Text_Primary
                            ),
                            maxLines = 5
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "(예: 확언문장, 기도문, 이루고 싶은 목표 /  최대 10개)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Text_Primary.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Section 2: Target count input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "2. 목표 횟수를 입력하세요.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Text_Primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                color = Color(0xFFF9FBFF),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFF0F0F0),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .padding(8.dp)
                    ) {
                        if (viewState.targetCount == 0) {
                            Text(
                                text = "(작은 반복이 큰 변화를 만듭니다. / 예: 100회, 1,000회, 10,000회)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = Text_Primary.copy(alpha = 0.5f)
                            )
                        }
                        
                        BasicTextField(
                            value = if (viewState.targetCount > 0) viewState.targetCount.toString() else "",
                            onValueChange = { text ->
                                text.toIntOrNull()?.let { count ->
                                    onEvent(WishInputEvent.UpdateTargetCount(count))
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = Text_Primary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Close button
            Button(
                onClick = { onEvent(WishInputEvent.SaveWish) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(150.dp)
                    .height(28.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple_Medium
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp
                ),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                if (viewState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "닫기",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishInputTopBar(
    onBackClick: () -> Unit
) {
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
            onClick = onBackClick,
            modifier = Modifier.padding(start = 17.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Text_Primary
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "오늘의 위시 (Today's WISH)",
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

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun WishInputScreenPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishText = "나는 매일 성장하고 있다",
            targetCount = 1000,
            isLoading = false,
            showSuggestions = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}