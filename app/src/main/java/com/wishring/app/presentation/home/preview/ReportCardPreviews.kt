package com.wishring.app.presentation.home.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.presentation.home.HomeViewState
import com.wishring.app.presentation.home.PageInfo
import com.wishring.app.presentation.home.component.WishHistorySection
import com.wishring.app.ui.theme.WishRingTheme
import java.time.LocalDate

@Preview(showBackground = true, name = "ReportCard - Empty")
@Composable
private fun ReportCardEmptyPreview() {
    WishRingTheme {
        WishHistorySection(
            uiState = HomeViewState.ConnectedNoWishes(
                wishHistory = emptyList(),
                todayWish = null,
                isLoading = false,
                error = null,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 0),
                deviceBatteryLevel = 80
            ),
            onEvent = { /* Preview - no action */ },
            onLoadMore = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "ReportCard - 3 Items")
@Composable
private fun ReportCardThreeItemsPreview() {
    WishRingTheme {
        WishHistorySection(
            uiState = HomeViewState.ConnectedPartialWishes(
                wishHistory = listOf(
                    WishDayUiState(
                        date = LocalDate.now().minusDays(1),
                        completedCount = 1000,
                        wishText = "매일 아침 6시에 일어나서 운동하기",
                        targetCount = 1000,
                        isCompleted = true
                    ),
                    WishDayUiState(
                        date = LocalDate.now().minusDays(2),
                        completedCount = 750,
                        wishText = "하루에 책 30페이지 읽기",
                        targetCount = 1000,
                        isCompleted = false
                    ),
                    WishDayUiState(
                        date = LocalDate.now().minusDays(3),
                        completedCount = 500,
                        wishText = "가족과 저녁 식사 함께하기",
                        targetCount = 800,
                        isCompleted = false
                    )
                ),
                todayWish = null,
                isLoading = false,
                error = null,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 3),
                deviceBatteryLevel = 75
            ),
            onEvent = { /* Preview - no action */ },
            onLoadMore = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "ReportCard - 20 Items")
@Composable
private fun ReportCardTwentyItemsPreview() {
    WishRingTheme {
        val wishHistory = (0..19).map { index ->
            WishDayUiState(
                date = LocalDate.now().minusDays(index.toLong()),
                completedCount = (50..1000).random(),
                wishText = when (index % 5) {
                    0 -> "매일 운동하고 건강한 생활 습관 만들기"
                    1 -> "영어 공부 30분씩 꾸준히 하기"
                    2 -> "일기 쓰며 하루 돌아보기"
                    3 -> "부모님께 안부 전화 드리기"
                    else -> "새로운 취미 활동 시작하고 꾸준히 실천하기"
                },
                targetCount = 1000,
                isCompleted = index % 3 == 0
            )
        }
        
        WishHistorySection(
            uiState = HomeViewState.ConnectedFullWishes(
                wishHistory = wishHistory,
                todayWish = null,
                isLoading = false,
                error = null,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = true, totalItems = 50),
                deviceBatteryLevel = 90
            ),
            onEvent = { /* Preview - no action */ },
            onLoadMore = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "ReportCard - Loading More")
@Composable
private fun ReportCardLoadingPreview() {
    WishRingTheme {
        val wishHistory = (0..9).map { index ->
            WishDayUiState(
                date = LocalDate.now().minusDays(index.toLong()),
                completedCount = (300..900).random(),
                wishText = "매일 꾸준히 실천하는 좋은 습관 만들기 #${index + 1}",
                targetCount = 1000,
                isCompleted = index % 2 == 0
            )
        }
        
        WishHistorySection(
            uiState = HomeViewState.ConnectedFullWishes(
                wishHistory = wishHistory,
                todayWish = null,
                isLoading = true,
                error = null,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = true, totalItems = 30),
                deviceBatteryLevel = 85
            ),
            onEvent = { /* Preview - no action */ },
            onLoadMore = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "ReportCard - Long Text")
@Composable
private fun ReportCardLongTextPreview() {
    WishRingTheme {
        WishHistorySection(
            uiState = HomeViewState.ConnectedPartialWishes(
                wishHistory = listOf(
                    WishDayUiState(
                        date = LocalDate.now().minusDays(1),
                        completedCount = 850,
                        wishText = "나는 매일 아침 일찍 일어나서 운동을 하고, 건강한 아침 식사를 먹고, 독서를 통해 새로운 지식을 습득하며, 가족과 소중한 시간을 보내고, 일에서도 최선을 다하여 더 나은 내가 되기 위해 끊임없이 노력하고 성장하는 사람이 되고 싶다.",
                        targetCount = 1000,
                        isCompleted = false
                    ),
                    WishDayUiState(
                        date = LocalDate.now().minusDays(2),
                        completedCount = 1000,
                        wishText = "짧은 위시",
                        targetCount = 1000,
                        isCompleted = true
                    )
                ),
                todayWish = null,
                isLoading = false,
                error = null,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 2),
                deviceBatteryLevel = 70
            ),
            onEvent = { /* Preview - no action */ },
            onLoadMore = { /* Preview - no action */ }
        )
    }
}