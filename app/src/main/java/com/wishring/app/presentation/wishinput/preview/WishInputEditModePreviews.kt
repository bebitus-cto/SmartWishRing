package com.wishring.app.presentation.wishinput.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishinput.WishInputContent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.data.model.WishDayUiState
import java.time.LocalDate
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Edit mode preview states for WishInputScreen
 */

@Preview(showBackground = true, name = "Edit Mode - Single Existing")
@Composable
fun WishInputEditModeSinglePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "기존 위시 텍스트",
                    isCompleted = false,
                    targetCount = 2000,
                    completedCount = 500
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "두 번째 위시",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 100
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

@Preview(showBackground = true, name = "Edit Mode - Multiple Existing")
@Composable
fun WishInputEditModeMultiplePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "첫 번째 기존 위시",
                    isCompleted = false,
                    targetCount = 3000,
                    completedCount = 1500
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "두 번째 기존 위시",
                    isCompleted = false,
                    targetCount = 2000,
                    completedCount = 800
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

@Preview(showBackground = true, name = "Edit Mode - Max Wishes")
@Composable
fun WishInputEditModeMaxPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "첫 번째 위시 (수정 중)",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 100
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "두 번째 위시 (수정 중)",
                    isCompleted = false,
                    targetCount = 2000,
                    completedCount = 200
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "세 번째 위시 (수정 중)",
                    isCompleted = false,
                    targetCount = 3000,
                    completedCount = 300
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

@Preview(showBackground = true, name = "Edit Mode - Partially Complete")
@Composable
fun WishInputEditModePartialPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "50% 진행된 위시",
                    isCompleted = false,
                    targetCount = 1000,
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

@Preview(showBackground = true, name = "Edit Mode - Nearly Complete")
@Composable
fun WishInputEditModeNearlyCompletePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "거의 완료된 위시 (90%)",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 900
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "진행 중인 위시 (30%)",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 300
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

@Preview(showBackground = true, name = "Edit Mode - Mixed Valid/Invalid")
@Composable
fun WishInputEditModeMixedPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "유효한 기존 위시",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 200
                ),
                WishDayUiState.empty(LocalDate.now()), // 빈 위시 (무효)
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "또 다른 유효한 위시",
                    isCompleted = false,
                    targetCount = 500,
                    completedCount = 50
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

@Preview(showBackground = true, name = "Edit Mode - High Progress")
@Composable
fun WishInputEditModeHighProgressPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "1000회 중 999회 완료",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 999
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "10000회 중 9500회 완료",
                    isCompleted = false,
                    targetCount = 10000,
                    completedCount = 9500
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "500회 중 450회 완료",
                    isCompleted = false,
                    targetCount = 500,
                    completedCount = 450
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

@Preview(showBackground = true, name = "Edit Mode - Completed")
@Composable
fun WishInputEditModeCompletedPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "완료된 위시",
                    isCompleted = true,
                    targetCount = 1000,
                    completedCount = 1000
                ),
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "진행 중인 위시",
                    isCompleted = false,
                    targetCount = 2000,
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