package com.wishring.app.presentation.wishinput

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
internal fun WishInputContent(
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
            // Section title with suggestions toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "오늘의 소원을 등록하세요 (최대 3개)",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Text_Primary,
                    modifier = Modifier.weight(1f)
                )
                
                // Suggestions toggle button
                TextButton(
                    onClick = { onEvent(WishInputEvent.ToggleSuggestions) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Purple_Medium
                    )
                ) {
                    Text(
                        text = if (viewState.showSuggestions) "숨기기" else "추천",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            // Suggested wishes section
            if (viewState.showSuggestions) {
                WishSuggestionsSection(
                    suggestions = viewState.suggestedWishes,
                    onSuggestionClick = { suggestion ->
                        onEvent(WishInputEvent.SelectSuggestedWish(suggestion))
                    }
                )
            }
            
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
            
            // Progress indicator
            WishProgressIndicator(
                validWishCount = viewState.validWishCount,
                maxWishCount = viewState.maxWishCount
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                // Clear button (only show if there are wishes with text)
                if (viewState.wishes.any { it.text.isNotEmpty() }) {
                    OutlinedButton(
                        onClick = { onEvent(WishInputEvent.ClearWishText) },
                        modifier = Modifier.width(100.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        )
                    ) {
                        Text(
                            text = "초기화",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                
                // Save button
                Button(
                    onClick = { onEvent(WishInputEvent.SaveWish) },
                    enabled = viewState.isSaveEnabled,
                    modifier = Modifier.width(160.dp).height(40.dp),
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
                            text = if (viewState.isEditMode) "소원 수정하기" else "소원 등록하기",
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
        
        // Error handling
        if (viewState.error != null) {
            ErrorSnackbar(
                message = viewState.error,
                onDismiss = { onEvent(WishInputEvent.DismissError) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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

@Composable
private fun WishSuggestionsSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💡 추천 소원",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Purple_Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    text = suggestion,
                    onClick = { onSuggestionClick(suggestion) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            ),
            color = Color(0xFF333333),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun WishProgressIndicator(
    validWishCount: Int,
    maxWishCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxWishCount) { index ->
            val isCompleted = index < validWishCount
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isCompleted) Purple_Medium else Color(0xFFE0E0E0),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            
            if (index < maxWishCount - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = "$validWishCount/$maxWishCount 완성",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = if (validWishCount > 0) Purple_Medium else Color(0xFF999999)
        )
    }
}

@Composable
private fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
            
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFC62828)
                )
            ) {
                Text(
                    text = "닫기",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

