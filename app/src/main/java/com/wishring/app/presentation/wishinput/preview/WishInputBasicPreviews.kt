package com.wishring.app.presentation.wishinput.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishinput.WishInputContent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.data.model.WishDayUiState
import java.time.LocalDate
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Basic preview states for WishInputScreen
 */

@Preview(showBackground = true, name = "Single Wish")
@Composable
fun WishInputScreenSinglePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "나는 매일 성장하고 있다",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 0
                )
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Multiple Wishes")
@Composable
fun WishInputScreenMultiplePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "건강한 몸 만들기",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 0
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "매일 감사하기",
                    isCompleted = false,
                    targetCount = 500,
                    completedCount = 0
                ),
                WishDayUiState.empty(LocalDate.now())
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Max Wishes (3)")
@Composable
fun WishInputScreenMaxWishesPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "부자 되기",
                    isCompleted = false,
                    targetCount = 10000,
                    completedCount = 0
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "행복한 가정 만들기",
                    isCompleted = false,
                    targetCount = 5000,
                    completedCount = 0
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "세계 여행하기",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 0
                )
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
fun WishInputScreenEmptyPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(WishDayUiState.empty(LocalDate.now())),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Edit Mode with Existing")
@Composable
fun WishInputScreenEditPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "기존 위시 수정하기",
                    isCompleted = false,
                    targetCount = 2000,
                    completedCount = 500
                ),
                WishDayUiState.empty(LocalDate.now())
            ),
            isLoading = false,
            isEditMode = true,
            existingRecord = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}