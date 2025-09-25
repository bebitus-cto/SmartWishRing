package com.wishring.app.presentation.wishinput.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishinput.WishInputScreen
import com.wishring.app.presentation.wishinput.WishInputContent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.presentation.wishinput.WishInputEvent
import com.wishring.app.data.model.WishDayUiState
import java.time.LocalDate
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Preview states for WishInputScreen component
 */

// ====== Individual WishCardItem Test ======

@Preview(showBackground = true, name = "WishCardItem Edit Mode - Empty")
@Composable
private fun WishCardItemEditEmptyPreview() {
    WishRingTheme {
        com.wishring.app.presentation.component.WishCardItem(
            wishText = "",
            isEditMode = true,
            showTargetCount = false,
            placeholder = "(예: 확언문장, 기도문, 이루고 싶은 목표)",
            onTextChange = { /* Preview - no action */ },
            showDeleteButton = false
        )
    }
}

@Preview(showBackground = true, name = "WishCardItem Edit Mode - With Delete")
@Composable
private fun WishCardItemEditWithDeletePreview() {
    WishRingTheme {
        com.wishring.app.presentation.component.WishCardItem(
            wishText = "나는 매일 성장하는 사람이다",
            isEditMode = true,
            showTargetCount = false,
            placeholder = "(예: 확언문장, 기도문, 이루고 싶은 목표)",
            onTextChange = { /* Preview - no action */ },
            onDelete = { /* Preview - no action */ },
            showDeleteButton = true
        )
    }
}

@Preview(showBackground = true, name = "WishCardItem Edit Mode - Long Text")
@Composable
private fun WishCardItemEditLongTextPreview() {
    WishRingTheme {
        com.wishring.app.presentation.component.WishCardItem(
            wishText = "나는 매일 아침 일찍 일어나서 운동을 하고 건강한 식사를 하며 새로운 것을 배우고 성장하는 사람이다",
            isEditMode = true,
            showTargetCount = false,
            placeholder = "(예: 확언문장, 기도문, 이루고 싶은 목표)",
            onTextChange = { /* Preview - no action */ },
            onDelete = { /* Preview - no action */ },
            showDeleteButton = true
        )
    }
}

// ====== Screen Level Previews ======

@Preview(showBackground = true, name = "WishInput Screen - Default")
@Composable
private fun WishInputScreenPreview() {
    WishRingTheme {
        WishInputScreen()
    }
}

// ====== Content Level Previews ======

@Preview(showBackground = true, name = "Empty State")
@Composable
private fun WishInputContentEmptyPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(WishDayUiState.empty(LocalDate.now())),
                isLoading = false,
                existingRecord = false,
                error = null
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Single Wish Filled")
@Composable
private fun WishInputContentSingleWishPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "나는 매일 성장하는 사람이다",
                    isCompleted = false,
                    targetCount = 1000,
                    completedCount = 0
                )),
                isLoading = false,
                existingRecord = false,
                error = null
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Multiple Wishes with Delete")
@Composable
private fun WishInputContentMultipleWishesPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(
                    WishDayUiState(
                        date = LocalDate.now(),
                        wishText = "내 꿈을 이루겠다",
                        isCompleted = false,
                        targetCount = 1000,
                        completedCount = 0
                    ),
                    WishDayUiState(
                        date = LocalDate.now(),
                        wishText = "건강하게 살자",
                        isCompleted = false,
                        targetCount = 500,
                        completedCount = 0
                    ),
                    WishDayUiState.empty(LocalDate.now())
                ),
                isLoading = false,
                existingRecord = true,
                error = null
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Long Text Wishes")
@Composable
private fun WishInputContentLongTextPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(
                    WishDayUiState(
                        date = LocalDate.now(),
                        wishText = "나는 매일 아침 일찍 일어나서 운동을 하고 건강한 식사를 하며 새로운 것을 배우고 성장하는 사람이다",
                        isCompleted = false,
                        targetCount = 1000,
                        completedCount = 0
                    ),
                    WishDayUiState.empty(LocalDate.now())
                ),
                isLoading = false,
                existingRecord = true,
                error = null
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "High Target Count")
@Composable
private fun WishInputContentHighTargetPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(WishDayUiState(
                    date = LocalDate.now(),
                    wishText = "매일 감사하며 살기",
                    isCompleted = false,
                    targetCount = 10000,
                    completedCount = 0
                )),
                isLoading = false,
                existingRecord = false,
                error = null
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun WishInputContentLoadingPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(WishDayUiState.empty(LocalDate.now())),
                isLoading = true,
                existingRecord = true,
                error = null
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Save Loading State")
@Composable
private fun WishInputContentSaveLoadingPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(WishDayUiState.empty(LocalDate.now())),
                isLoading = true,
                existingRecord = false,
                error = null
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
private fun WishInputContentErrorPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(WishDayUiState.empty(LocalDate.now())),
                isLoading = false,
                existingRecord = false,
                error = "위시를 저장하는데 실패했습니다. 다시 시도해주세요."
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Existing Record Error")
@Composable
private fun WishInputContentLoadErrorPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(WishDayUiState.empty(LocalDate.now())),
                isLoading = false,
                existingRecord = false,
                error = "기존 위시를 불러오는데 실패했습니다."
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Mixed Content State")
@Composable
private fun WishInputContentMixedPreview() {
    WishRingTheme {
        WishInputContent(
            viewState = WishInputViewState(
                wishes = listOf(
                    WishDayUiState(
                        date = LocalDate.now(),
                        wishText = "내 꿈을 이루겠다",
                        isCompleted = false,
                        targetCount = 1000,
                        completedCount = 0
                    ),
                    WishDayUiState(
                        date = LocalDate.now(),
                        wishText = "건강하게 살자",
                        isCompleted = false,
                        targetCount = 500,
                        completedCount = 0
                    ),
                    WishDayUiState.empty(LocalDate.now())
                ),
                isLoading = false,
                existingRecord = true,
                error = null
            ),
            onEvent = { /* Preview - no action */ }
        )
    }
}