package com.wishring.app.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.presentation.home.HomeEvent
import com.wishring.app.presentation.home.HomeViewState
import com.wishring.app.ui.theme.Text_Primary
import com.wishring.app.ui.theme.Text_Secondary

@Composable
fun WishHistorySection(
    uiState: HomeViewState,
    onEvent: (HomeEvent) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    // Detect when to load more
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            // Load more when we're within 20 items of the end
            lastVisibleItemIndex > (totalItemsNumber - 20) &&
                    uiState.pageInfo?.hasNextPage == true &&
                    !uiState.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
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

            // Report content with LazyColumn for wish history
            if (uiState.wishHistory.isEmpty()) {
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
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.wishHistory.size) { index ->
                        val record = uiState.wishHistory[index]
                        WishHistoryItem(
                            record = record,
                            onClick = {
                                onEvent(HomeEvent.NavigateToDetail(record.dateString))
                            }
                        )
                    }

                    // Loading indicator at the bottom
                    if (uiState.isLoading && uiState.pageInfo?.hasNextPage == true) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}