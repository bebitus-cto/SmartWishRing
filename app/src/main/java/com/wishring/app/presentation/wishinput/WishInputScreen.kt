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
import com.wishring.app.presentation.component.WishCardItem
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
        containerColor = Color(0xFFF6F7FF),
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
        Box(modifier = Modifier.fillMaxSize()) {
            WishInputContent(
                viewState = viewState,
                onEvent = viewModel::onEvent,
                modifier = Modifier.padding(paddingValues)
            )

            // Loading overlay
            if (viewState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF6A5ACD),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (viewState.existingRecord) "기존 위시를 불러오는 중..." else "위시를 저장하는 중...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF333333)
                            )
                        }
                    }
                }
            }

            // Error snackbar
            viewState.error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.onEvent(WishInputEvent.DismissError) }
                        ) {
                            Text(
                                text = "닫기",
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
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
            .background(Color(0xFFF6F7FF))
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

                // Wish cards using the WishCardItem component
                viewState.wishes.forEachIndexed { index, wish ->
                    WishCardItem(
                        wishText = wish.wishText,
                        isEditMode = true,
                        showTargetCount = false,
                        placeholder = "(예: 확언문장, 기도문, 이루고 싶은 목표)",
                        onTextChange = { text ->
                            onEvent(WishInputEvent.UpdateWishText(index, text))
                        },
                        onDelete = if (viewState.canRemoveWishes) {
                            { onEvent(WishInputEvent.RemoveWish(index)) }
                        } else null,
                        showDeleteButton = viewState.canRemoveWishes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Add wish button
                if (viewState.canAddMoreWishes) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { onEvent(WishInputEvent.AddWish()) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF6A5ACD).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add wish",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF6A5ACD)
                            )
                        }
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
                        viewState.wishes.forEachIndexed { index, _ ->
                            onEvent(WishInputEvent.UpdateWishCount(index, count))
                        }
                    },
                    placeholder = "작은 반복이 큰 변화를 만듭니다. (예: 100회, 1,000회, 10,000회)"
                )
            }
        }

        // Bottom spacing before background image
        Spacer(modifier = Modifier.height(20.dp))

        // Background image at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(127.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.input_background_32926f),
                contentDescription = null,
                modifier = Modifier.size(169.dp, 127.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Additional bottom padding for bottomBar
        Spacer(modifier = Modifier.height(80.dp))
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
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
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE0E0E0),
                    unfocusedBorderColor = Color(0xFFF0F0F0),
                    focusedContainerColor = Color(0xFFF9FBFF),
                    unfocusedContainerColor = Color(0xFFF9FBFF),
                    disabledBorderColor = Color(0xFFF0F0F0),
                    disabledContainerColor = Color(0xFFF9FBFF),
                    disabledTextColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(5.dp),
                enabled = false
            )
        }
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