package com.wishring.app.presentation.wishinput.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishinput.WishInputContent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.presentation.wishinput.model.WishItem
import com.wishring.app.ui.theme.WishRingTheme

/**
 * State-related previews for WishInput screen
 */

@Preview(showBackground = true, device = "id:pixel_5", name = "Saving State")
@Composable
fun WishInputScreenSavingPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "저장 중인 소원",
                    targetCount = 1000
                )
            ),
            isSaving = true,
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Error State")
@Composable
fun WishInputScreenErrorPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "문제가 있는 소원",
                    targetCount = 1000
                )
            ),
            error = "위시 저장에 실패했습니다",
            isSaving = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Edit Mode")
@Composable
fun WishInputScreenEditModePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "수정 중인 기존 소원",
                    targetCount = 2000
                )
            ),
            isEditMode = true,
            existingRecord = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Delete Confirmation")
@Composable
fun WishInputScreenDeleteConfirmationPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "삭제할 소원",
                    targetCount = 1500
                )
            ),
            isEditMode = true,
            existingRecord = true,
            showDeleteConfirmation = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Loading State")
@Composable
fun WishInputScreenLoadingPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(WishItem.createEmpty()),
            isLoading = true
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}