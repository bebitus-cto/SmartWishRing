package com.wishring.app.presentation.wishinput.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishinput.WishInputContent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.data.model.WishDayUiState
import java.time.LocalDate
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Data loading preview states for WishInputScreen
 */

@Preview(showBackground = true, name = "Loading Existing Data")
@Composable
fun WishInputLoadingExistingPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "불러오는 중...",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 0
                )
            ),
            isLoading = true,
            existingRecord = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Loading Multiple Existing")
@Composable
fun WishInputLoadingMultiplePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "첫 번째 위시",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 100
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "두 번째 위시",
                    isCompleted = false,
                    targetCount = 2000,
                    completedCount = 200
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "세 번째 위시",
                    isCompleted = false,
                    targetCount = 3000,
                    completedCount = 300
                )
            ),
            isLoading = true,
            existingRecord = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Loading Partial Data")
@Composable
fun WishInputLoadingPartialPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "첫 번째 위시 로드됨",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 500
                ),
                WishDayUiState.empty(LocalDate.now()), // 두 번째는 비어있음
                WishDayUiState.empty(LocalDate.now())  // 세 번째도 비어있음
            ),
            isLoading = true,
            existingRecord = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Initial Loading")
@Composable
fun WishInputInitialLoadingPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(WishDayUiState.empty(LocalDate.now())),
            isLoading = true,
            existingRecord = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Loading Error")
@Composable
fun WishInputLoadingErrorPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(WishDayUiState.empty(LocalDate.now())),
            isLoading = false,
            error = "기존 위시를 불러오는데 실패했습니다"
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Loaded with Progress")
@Composable
fun WishInputLoadedWithProgressPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "진행 중인 위시 (50%)",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 500
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "시작한 위시 (10%)",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 100
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

@Preview(showBackground = true, name = "Loaded Completed Wishes")
@Composable
fun WishInputLoadedCompletedPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "완료된 위시 1",
                    isCompleted = true,
                    targetCount = 1000,
                    completedCount = 1000
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "완료된 위시 2",
                    isCompleted = true,
                    targetCount = 500,
                    completedCount = 500
                )
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

@Preview(showBackground = true, name = "Saving State")
@Composable
fun WishInputSavingPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "저장 중인 위시",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 0
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "함께 저장되는 위시",
                    isCompleted = false,
                    targetCount = 2000,
                    completedCount = 0
                )
            ),
            isLoading = false,
            isSaving = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}