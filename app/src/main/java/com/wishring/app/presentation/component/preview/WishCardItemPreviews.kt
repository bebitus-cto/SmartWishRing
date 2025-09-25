package com.wishring.app.presentation.component.preview

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.presentation.component.WishCardItem
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Preview states for WishCardItem component
 */

// ====== Normal Mode Previews ======

@Preview(showBackground = true, name = "Normal Mode - Short Text")
@Composable
private fun WishCardItemNormalShortPreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "매일 운동하기",
            isEditMode = false,
            onClick = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Normal Mode - Long Text")
@Composable
private fun WishCardItemNormalLongPreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "나는 매일 아침 일찍 일어나서 운동을 하고, 건강한 아침 식사를 먹고, 독서를 통해 새로운 지식을 습득하며, 가족과 소중한 시간을 보내고, 일에서도 최선을 다하여 더 나은 내가 되기 위해 끊임없이 노력하고 성장하는 사람이 되고 싶다.",
            isEditMode = false,
            onClick = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Normal Mode - Medium Text")
@Composable
private fun WishCardItemNormalMediumPreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "건강하고 행복한 삶을 살며, 매일 감사하는 마음을 가지고 살아간다.",
            isEditMode = false,
            onClick = { /* Preview - no action */ }
        )
    }
}

// ====== Edit Mode Previews ======

@Preview(showBackground = true, name = "Edit Mode - Empty")
@Composable
private fun WishCardItemEditEmptyPreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "",
            isEditMode = true,
            targetCount = 1000,
            placeholder = "소원을 입력하세요... (예: 확언문장, 기도문, 이루고 싶은 목표)",
            onTextChange = { /* Preview - no action */ },
            onTargetCountChange = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Edit Mode - With Text")
@Composable
private fun WishCardItemEditFilledPreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "나는 매일 성장하고 있다",
            isEditMode = true,
            targetCount = 2000,
            onTextChange = { /* Preview - no action */ },
            onTargetCountChange = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Edit Mode - With Delete Button")
@Composable
private fun WishCardItemEditWithDeletePreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "매일 운동을 하며 건강을 관리한다",
            isEditMode = true,
            targetCount = 1500,
            showDeleteButton = true,
            onTextChange = { /* Preview - no action */ },
            onTargetCountChange = { /* Preview - no action */ },
            onDelete = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Edit Mode - Long Text")
@Composable
private fun WishCardItemEditLongTextPreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "나는 건강하고 행복하며 성공적인 삶을 살아가는 감사한 사람이다",
            isEditMode = true,
            targetCount = 3000,
            showDeleteButton = true,
            onTextChange = { /* Preview - no action */ },
            onTargetCountChange = { /* Preview - no action */ },
            onDelete = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Edit Mode - Without Target Count")
@Composable
private fun WishCardItemEditNoTargetPreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "간단한 위시 텍스트만 표시",
            isEditMode = true,
            showTargetCount = false,
            showDeleteButton = true,
            onTextChange = { /* Preview - no action */ },
            onDelete = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Edit Mode - High Target Count")
@Composable
private fun WishCardItemEditHighTargetPreview() {
    WishRingTheme {
        WishCardItem(
            wishText = "큰 목표를 가진 위시",
            isEditMode = true,
            targetCount = 10000,
            showDeleteButton = true,
            onTextChange = { /* Preview - no action */ },
            onTargetCountChange = { /* Preview - no action */ },
            onDelete = { /* Preview - no action */ }
        )
    }
}