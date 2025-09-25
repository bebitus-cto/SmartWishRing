package com.wishring.app.presentation.wishdetail.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishdetail.WishDetailContent
import com.wishring.app.presentation.wishdetail.WishDetailViewState
import com.wishring.app.ui.theme.WishRingTheme
import java.time.LocalDate

/**
 * Date-based preview scenarios for WishDetail screen
 */

@Preview(showBackground = true, device = "id:pixel_5", name = "Past Date - Week Ago")
@Composable
fun WishDetailPastWeekPreview() {
    WishRingTheme {
        val pastDate = LocalDate.now().minusDays(7)
        val previewState = WishDetailViewState(
            selectedDate = pastDate,
            targetCount = 1800,
            wishText = "지난주의 나는 이런 목표를 가지고 있었다",
            motivationalMessages = listOf(
                "과거의 노력이 오늘의 나를 만들었다.",
                "매일의 기록이 성장의 증거다.",
                "꾸준함의 힘을 믿는다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Past Date - Month Ago")
@Composable
fun WishDetailPastMonthPreview() {
    WishRingTheme {
        val pastDate = LocalDate.now().minusMonths(1)
        val previewState = WishDetailViewState(
            selectedDate = pastDate,
            targetCount = 5000,
            wishText = "한 달 전의 나는 이미 이 길을 걷고 있었다",
            motivationalMessages = listOf(
                "한 달간의 여정이 나를 변화시켰다.",
                "시간이 지날수록 더 강해진다.",
                "과거의 선택이 현재를 만든다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Yesterday")
@Composable
fun WishDetailYesterdayPreview() {
    WishRingTheme {
        val yesterday = LocalDate.now().minusDays(1)
        val previewState = WishDetailViewState(
            selectedDate = yesterday,
            targetCount = 2100,
            wishText = "어제의 나는 오늘을 위해 준비하고 있었다",
            motivationalMessages = listOf(
                "어제의 노력이 오늘의 바탕이 된다.",
                "매일이 새로운 도전이다.",
                "어제보다 나은 오늘을 만든다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Far Past - 3 Months")
@Composable
fun WishDetailFarPastPreview() {
    WishRingTheme {
        val farPast = LocalDate.now().minusMonths(3)
        val previewState = WishDetailViewState(
            selectedDate = farPast,
            targetCount = 8500,
            wishText = "3개월 전부터 시작된 나의 여정",
            motivationalMessages = listOf(
                "긴 여정의 시작점을 되돌아본다.",
                "지속적인 노력이 결실을 맺는다.",
                "과거의 나에게 감사한다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "New Year's Day")
@Composable
fun WishDetailNewYearPreview() {
    WishRingTheme {
        val newYear = LocalDate.of(2025, 1, 1)
        val previewState = WishDetailViewState(
            selectedDate = newYear,
            targetCount = 10000,
            wishText = "새해 첫날부터 시작한 나의 다짐",
            motivationalMessages = listOf(
                "새로운 시작은 언제나 설렌다.",
                "2025년의 첫 걸음이 여기서 시작되었다.",
                "매일이 새로운 기회다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Special Date Format")
@Composable
fun WishDetailSpecialDatePreview() {
    WishRingTheme {
        val specialDate = LocalDate.of(2024, 12, 25)
        val previewState = WishDetailViewState(
            selectedDate = specialDate,
            targetCount = 2500,
            wishText = "특별한 날에도 계속된 나의 루틴",
            motivationalMessages = listOf(
                "특별한 날일수록 더욱 의미 있는 행동을.",
                "크리스마스에도 성장은 계속된다.",
                "매일이 선물 같은 하루다."
            ),
            isLoading = false
        )
        
        WishDetailContent(
            uiState = previewState,
            onEvent = {}
        )
    }
}