package com.wishring.app.presentation.wishdetail.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishdetail.WishDetailContent
import com.wishring.app.presentation.wishdetail.WishDetailViewState
import com.wishring.app.ui.theme.WishRingTheme
import java.time.LocalDate

/**
 * Basic preview states for WishDetail screen
 */

@Preview(showBackground = true, device = "id:pixel_5", name = "Normal Count Display")
@Composable
fun WishDetailNormalPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 1500,
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

@Preview(showBackground = true, device = "id:pixel_5", name = "High Count (10K)")
@Composable
fun WishDetailHighCountPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 10000,
            wishText = "나는 끊임없이 발전하는 사람이다",
            motivationalMessages = listOf(
                "만 번의 반복이 나를 완전히 다른 사람으로 만들었다.",
                "포기하지 않은 내가 자랑스럽다.",
                "오늘도 한 걸음 더 나아간다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Low Count")
@Composable
fun WishDetailLowCountPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 25,
            wishText = "작은 시작이지만 의미있는 여정이다",
            motivationalMessages = listOf(
                "작은 발걸음도 큰 여행의 시작이다.",
                "모든 위대함은 작은 것에서 시작된다.",
                "지금 이 순간이 나의 시작점이다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Today's Date")
@Composable
fun WishDetailTodayPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.now(),
            targetCount = 2500,
            wishText = "오늘도 최선을 다해 살아간다",
            motivationalMessages = listOf(
                "오늘은 새로운 가능성의 날이다.",
                "현재에 집중하며 미래를 만든다.",
                "매 순간이 성장의 기회다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Long Wish Text")
@Composable
fun WishDetailLongTextPreview() {
    WishRingTheme {
        val previewState = WishDetailViewState(
            selectedDate = LocalDate.of(2025, 1, 15),
            targetCount = 3000,
            wishText = "나는 건강하고 행복하며 성공적인 삶을 살아가는 감사한 사람이다",
            motivationalMessages = listOf(
                "긴 여정도 한 걸음씩 걸어가면 언젠가는 도착한다.",
                "꾸준함이 나의 가장 큰 무기다.",
                "복잡한 목표도 단순한 반복으로 이룰 수 있다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}