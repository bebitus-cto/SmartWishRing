package com.wishring.app.presentation.wishdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishring.app.R
import com.wishring.app.presentation.component.WishCardItem
import com.wishring.app.ui.theme.*

/**
 * Simplified WishDetail screen matching Figma design
 * Shows count, wish text, and motivational messages for a specific date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishDetailScreen(
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
                displayDate = uiState.displayDate,
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
internal fun WishDetailContent(
    uiState: WishDetailViewState,
    onEvent: (WishDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Secondary)
    ) {
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
                WishCardItem(
                    wishText = message,
                    isEditMode = false
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
            CircularProgressIndicator(color = Text_Primary)
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
    displayDate: String,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Text_Primary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Text_Primary
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Background_Secondary
        )
    )
}

@Composable
private fun CountDisplayCard(
    count: String,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(5.dp),
                spotColor = Color(0x1A000000)
            ),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Text_Primary)
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
private fun CharacterImagesRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(84.dp, 115.dp),
            shape = RoundedCornerShape(5.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.character_white),
                contentDescription = "White Character",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(
            modifier = Modifier.size(89.dp, 115.dp),
            shape = RoundedCornerShape(5.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

// Preview moved to preview/ package