package com.wishring.app.presentation.wishinput

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.wishring.app.presentation.component.WishCard
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section title
            Text(
                text = "오늘의 소원을 등록하세요 (최대 3개)",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Text_Primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Multiple wish cards
            viewState.wishes.forEachIndexed { index, wish ->
                WishCard(
                    wishText = wish.text,
                    isInputMode = true,
                    targetCount = wish.targetCount,
                    onTextChange = { text ->
                        onEvent(WishInputEvent.UpdateWishText(wish.id, text))
                    },
                    onTargetCountChange = { count ->
                        onEvent(WishInputEvent.UpdateWishCount(wish.id, count))
                    },
                    onDelete = if (viewState.canRemoveWishes) {
                        { onEvent(WishInputEvent.RemoveWish(wish.id)) }
                    } else null,
                    showDeleteButton = viewState.canRemoveWishes,
                    placeholder = "소원 ${index + 1}을 입력하세요...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Add wish button or max message
            if (viewState.canAddMoreWishes) {
                OutlinedButton(
                    onClick = { onEvent(WishInputEvent.AddWish()) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Purple_Medium
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Purple_Medium)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add wish",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "소원 추가",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Text(
                        text = "✨ 최대 3개의 소원까지 등록할 수 있어요",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save button
            Button(
                onClick = { onEvent(WishInputEvent.SaveWish) },
                enabled = viewState.isSaveEnabled,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(160.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple_Medium,
                    disabledContainerColor = Color(0xFFCCCCCC)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp
                )
            ) {
                if (viewState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "소원 등록하기",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
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

@Preview(showBackground = true, device = "id:pixel_5", name = "Single Wish")
@Composable
fun WishInputScreenSinglePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "나는 매일 성장하고 있다",
                    targetCount = 1000
                )
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Multiple Wishes")
@Composable
fun WishInputScreenMultiplePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "나는 매일 성장하고 있다",
                    targetCount = 1000
                ),
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "건강한 습관을 만들어간다",
                    targetCount = 2000
                ),
                com.wishring.app.presentation.wishinput.model.WishItem.createEmpty()
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Max Wishes Reached")
@Composable
fun WishInputScreenMaxPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "나는 매일 성장하고 있다",
                    targetCount = 1000
                ),
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "건강한 습관을 만들어간다",
                    targetCount = 2000
                ),
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "감사하는 마음을 가진다",
                    targetCount = 1500
                )
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Empty Initial")
@Composable
fun WishInputScreenEmptyPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.createEmpty()
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Saving State")
@Composable
fun WishInputScreenSavingPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "저장 중인 소원",
                    targetCount = 1000
                )
            ),
            isSaving = true,
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Error State")
@Composable
fun WishInputScreenErrorPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "문제가 있는 소원",
                    targetCount = 1000
                )
            ),
            error = "위시 저장에 실패했습니다",
            isSaving = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Edit Mode")
@Composable
fun WishInputScreenEditModePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "수정 중인 기존 소원",
                    targetCount = 2000
                )
            ),
            isEditMode = true,
            existingRecord = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Delete Confirmation")
@Composable
fun WishInputScreenDeleteConfirmationPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "삭제할 소원",
                    targetCount = 1500
                )
            ),
            isEditMode = true,
            existingRecord = true,
            showDeleteConfirmation = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Two Wishes Partial")
@Composable
fun WishInputScreenTwoWishesPartialPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "첫 번째 소원",
                    targetCount = 1000
                ),
                com.wishring.app.presentation.wishinput.model.WishItem.createEmpty()
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Long Text")
@Composable
fun WishInputScreenLongTextPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "매우 긴 소원 텍스트입니다. 이것은 한 줄에 다 들어가지 않을 정도로 길고 복잡한 내용을 담고 있어서 텍스트 오버플로우나 줄바꿈 처리를 테스트하기 위한 예시입니다",
                    targetCount = 1000
                )
            )
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "High Target Values")
@Composable
fun WishInputScreenHighTargetPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "높은 목표의 첫 번째 위시",
                    targetCount = 5000
                ),
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "높은 목표의 두 번째 위시",
                    targetCount = 8000
                ),
                com.wishring.app.presentation.wishinput.model.WishItem.create(
                    text = "최고 목표의 세 번째 위시",
                    targetCount = 10000
                )
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}
