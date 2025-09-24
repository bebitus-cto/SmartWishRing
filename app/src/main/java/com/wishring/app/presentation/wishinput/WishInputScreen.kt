package com.wishring.app.presentation.wishinput

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishring.app.R
import com.wishring.app.presentation.component.WishCard
import com.wishring.app.presentation.wishinput.component.NumberPickerDialog

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
        containerColor = Color(0xFFFAF5FF),
        topBar = {
            WishInputTopBar(
                onBackClick = { viewModel.onEvent(WishInputEvent.NavigateBack) }
            )
        },
        bottomBar = {
            // 하단 고정 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { viewModel.onEvent(WishInputEvent.SaveWish) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A5ACD)
                    ),
                    shape = RoundedCornerShape(50.dp),
                    enabled = viewState.isSaveEnabled
                ) {
                    Text(
                        text = "닫기",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        WishInputContent(
            viewState = viewState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishInputTopBar(
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "오늘의 위시 (Today's WISH)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF333333)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color(0xFF333333)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
internal fun WishInputContent(
    viewState: WishInputViewState,
    onEvent: (WishInputEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF5FF))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Section 1: 위시 입력 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(1.dp),
                shape = RoundedCornerShape(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section title
                    Text(
                        text = "1. 원하는 것을 적어보세요",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF333333)
                    )
                    
                    // Wish cards using the actual WishCard component
                    viewState.wishes.forEachIndexed { index, wish ->
                        WishCard(
                            wishText = wish.text,
                            isInputMode = true,
                            showTargetCount = false,
                            placeholder = "소원 ${index + 1}을 입력하세요... (예: 확언문장, 기도문, 이루고 싶은 목표)",
                            onTextChange = { text ->
                                onEvent(WishInputEvent.UpdateWishText(wish.id, text))
                            },
                            onDelete = if (viewState.canRemoveWishes) {
                                { onEvent(WishInputEvent.RemoveWish(wish.id)) }
                            } else null,
                            showDeleteButton = viewState.canRemoveWishes,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Add wish button
                    if (viewState.canAddMoreWishes) {
                        OutlinedButton(
                            onClick = { onEvent(WishInputEvent.AddWish()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(5.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6A5ACD)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF0F0F0))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add wish",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF6A5ACD)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "위시 추가",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF6A5ACD)
                            )
                        }
                    }
                }
            }
            
            // Section 2: 목표 횟수 입력 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(1.dp),
                shape = RoundedCornerShape(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section title
                    Text(
                        text = "2. 목표 횟수를 입력하세요.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF333333)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Target count input
                    TargetCountInput(
                        targetCount = if (viewState.wishes.isNotEmpty()) viewState.wishes.first().targetCount else 1000,
                        onTargetCountChange = { count ->
                            // Update target count for all wishes
                            viewState.wishes.forEach { wish ->
                                onEvent(WishInputEvent.UpdateWishCount(wish.id, count))
                            }
                        },
                        placeholder = "작은 반복이 큰 변화를 만듭니다. (예: 100회, 1,000회, 10,000회)"
                    )
                }
            }
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        // Background image at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(127.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.input_background_32926f),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(169.dp, 127.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun TargetCountInput(
    targetCount: Int,
    onTargetCountChange: (Int) -> Unit,
    placeholder: String = "1000"
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = targetCount.toString(),
            onValueChange = { /* Read-only */ },
            placeholder = {
                Text(
                    text = "(작은 반복이 큰 변화를 만듭니다. / 예: 100회, 1,000회, 10,000회)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF333333).copy(alpha = 0.5f)
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = Color(0xFF333333)
            ),
            singleLine = true,
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE0E0E0),
                unfocusedBorderColor = Color(0xFFF0F0F0),
                focusedContainerColor = Color(0xFFF9FBFF),
                unfocusedContainerColor = Color(0xFFF9FBFF),
                disabledBorderColor = Color(0xFFF0F0F0),
                disabledContainerColor = Color(0xFFF9FBFF),
                disabledTextColor = Color(0xFF333333)
            ),
            shape = RoundedCornerShape(5.dp)
        )
    }
    
    if (showDialog) {
        NumberPickerDialog(
            currentValue = targetCount,
            onValueSelected = onTargetCountChange,
            onDismiss = { showDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WishInputScreenPreview() {
    WishInputScreen()
}