package com.wishring.app.presentation.wishdetail.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishdetail.WishDetailContent
import com.wishring.app.presentation.wishdetail.WishDetailViewState
import com.wishring.app.ui.theme.WishRingTheme
import java.time.LocalDate

/**
 * State-based preview scenarios for WishDetail screen
 */

@Preview(showBackground = true, device = "id:pixel_5", name = "Loading State")
@Composable
fun WishDetailLoadingPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 0,
            wishText = "",
            motivationalMessages = emptyList(),
            isLoading = true
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Error State")
@Composable
fun WishDetailErrorPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 500,
            wishText = "데이터 로딩 중 오류가 발생한 경우",
            motivationalMessages = listOf(
                "실패는 성공의 어머니다.",
                "다시 시도하면 된다."
            ),
            isLoading = false,
            error = "데이터를 불러오는 중 오류가 발생했습니다"
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Empty Data")
@Composable
fun WishDetailEmptyPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 0,
            wishText = "",
            motivationalMessages = emptyList(),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Partial Data")
@Composable
fun WishDetailPartialDataPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 1200,
            wishText = "", // 위시는 없고 카운트만 있는 경우
            motivationalMessages = listOf(
                "시작이 반이다.",
                "작은 시도가 큰 변화를 만든다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Only Messages")
@Composable
fun WishDetailOnlyMessagesPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 0,
            wishText = "", // 위시와 카운트는 없고 메시지만 있는 경우
            motivationalMessages = listOf(
                "오늘도 화이팅!",
                "당신은 할 수 있습니다.",
                "포기하지 마세요."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Single Message")
@Composable
fun WishDetailSingleMessagePreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 750,
            wishText = "하나의 메시지만 있는 경우",
            motivationalMessages = listOf(
                "집중하면 반드시 이룰 수 있다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}