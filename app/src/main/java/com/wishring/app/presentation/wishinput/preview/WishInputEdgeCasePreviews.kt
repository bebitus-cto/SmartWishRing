package com.wishring.app.presentation.wishinput.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.wishinput.WishInputContent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.presentation.wishinput.model.WishItem
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Edge case and extreme scenario previews for WishInput screen
 */

@Preview(showBackground = true, device = "id:pixel_5", name = "Long Text")
@Composable
fun WishInputScreenLongTextPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "매우 긴 소원 텍스트입니다. 이것은 한 줄에 다 들어가지 않을 정도로 길고 복잡한 내용을 담고 있어서 텍스트 오버플로우나 줄바꿈 처리를 테스트하기 위한 예시입니다",
                    targetCount = 1000
                )
            )
        )
        
        WishInputContent(
            viewState = previewState,
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "High Target Values")
@Composable
fun WishInputScreenHighTargetPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "높은 목표의 첫 번째 위시",
                    targetCount = 5000
                ),
                WishItem.create(
                    text = "높은 목표의 두 번째 위시",
                    targetCount = 8000
                ),
                WishItem.create(
                    text = "최고 목표의 세 번째 위시",
                    targetCount = 10000
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

@Preview(showBackground = true, device = "id:pixel_5", name = "Mixed Valid Invalid")
@Composable
fun WishInputScreenMixedValidityPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "유효한 첫 번째 위시",
                    targetCount = 1000
                ),
                WishItem.createEmpty(), // 빈 위시 (무효)
                WishItem.create(
                    text = "유효한 세 번째 위시",
                    targetCount = 2000
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

@Preview(showBackground = true, device = "id:pixel_5", name = "Special Characters")
@Composable
fun WishInputScreenSpecialCharsPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "나는 100% 성공할 수 있다! 💪✨",
                    targetCount = 1000
                ),
                WishItem.create(
                    text = "행복한 하루 보내기 😊🌈",
                    targetCount = 500
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

@Preview(showBackground = true, device = "id:pixel_5", name = "Very Short Text")
@Composable
fun WishInputScreenShortTextPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "짧음",
                    targetCount = 100
                ),
                WishItem.create(
                    text = "행복",
                    targetCount = 50
                ),
                WishItem.create(
                    text = "성공",
                    targetCount = 200
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

@Preview(showBackground = true, device = "id:pixel_5", name = "Low Target Values")
@Composable
fun WishInputScreenLowTargetPreview() {
    WishRingTheme {
        val previewState = WishInputViewState(
            wishes = listOf(
                WishItem.create(
                    text = "작은 목표 첫 번째",
                    targetCount = 1
                ),
                WishItem.create(
                    text = "작은 목표 두 번째",
                    targetCount = 5
                ),
                WishItem.create(
                    text = "작은 목표 세 번째",
                    targetCount = 10
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