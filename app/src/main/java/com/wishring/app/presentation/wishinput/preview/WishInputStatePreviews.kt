package com.wishring.app.presentation.wishinput.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishinput.WishInputContent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.data.model.WishDayUiState
import java.time.LocalDate
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Various state preview states for WishInputScreen
 */

@Preview(showBackground = true, name = "Delete Confirmation Dialog")
@Composable
fun WishInputDeleteConfirmationPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "삭제될 위시",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 500
                )
            ),
            isLoading = false,
            showDeleteConfirmation = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Save Disabled")
@Composable
fun WishInputSaveDisabledPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
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

@Preview(showBackground = true, name = "Save Enabled")
@Composable
fun WishInputSaveEnabledPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "유효한 위시 텍스트",
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

@Preview(showBackground = true, name = "Error State")
@Composable
fun WishInputErrorStatePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "에러가 발생한 위시",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 0
                )
            ),
            isLoading = false,
            error = "위시 저장 중 오류가 발생했습니다. 네트워크 연결을 확인하고 다시 시도해주세요."
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = { }
        )
    }
}

@Preview(showBackground = true, name = "Empty with Default Values")
@Composable
fun WishInputEmptyDefaultPreview() {
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