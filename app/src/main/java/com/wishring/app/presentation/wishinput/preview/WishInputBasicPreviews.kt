package com.wishring.app.presentation.wishinput.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishinput.WishInputContent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.presentation.wishinput.model.WishItem
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Basic preview states for WishInput screen
 */

@Preview(showBackground = true, device = "id:pixel_5", name = "Single Wish")
@Composable
fun WishInputScreenSinglePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "나는 매일 성장하고 있다",
                    targetCount = 1000
                )
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Multiple Wishes")
@Composable
fun WishInputScreenMultiplePreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "나는 매일 성장하고 있다",
                    targetCount = 1000
                ),
                WishItem.create(
                    text = "건강한 습관을 만들어간다",
                    targetCount = 2000
                ),
                WishItem.createEmpty()
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Max Wishes Reached")
@Composable
fun WishInputScreenMaxPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "나는 매일 성장하고 있다",
                    targetCount = 1000
                ),
                WishItem.create(
                    text = "건강한 습관을 만들어간다",
                    targetCount = 2000
                ),
                WishItem.create(
                    text = "감사하는 마음을 가진다",
                    targetCount = 1500
                )
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Empty Initial")
@Composable
fun WishInputScreenEmptyPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.createEmpty()
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Two Wishes Partial")
@Composable
fun WishInputScreenTwoWishesPartialPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "첫 번째 소원",
                    targetCount = 1000
                ),
                WishItem.createEmpty()
            ),
            isLoading = false
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}