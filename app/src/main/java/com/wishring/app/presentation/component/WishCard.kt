package com.wishring.app.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.WishRingTheme

/**
 * Reusable wish card component
 * Used in Home screen and Wish Input screen
 * Supports both display and input modes
 */
@Composable
fun WishCard(
    wishText: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isInputMode: Boolean = false,
    targetCount: Int = 1000,
    onTextChange: ((String) -> Unit)? = null,
    onTargetCountChange: ((Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showDeleteButton: Boolean = false,
    placeholder: String = "소원을 입력하세요..."
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isInputMode) Modifier.clickable { onClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(5.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        if (isInputMode) {
            WishInputCard(
                wishText = wishText,
                targetCount = targetCount,
                onTextChange = onTextChange ?: {},
                onTargetCountChange = onTargetCountChange ?: {},
                onDelete = onDelete,
                showDeleteButton = showDeleteButton,
                placeholder = placeholder
            )
        } else {
            WishDisplayCard(wishText = wishText)
        }
    }
}

@Composable
private fun WishDisplayCard(
    wishText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = wishText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF333333),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun WishInputCard(
    wishText: String,
    targetCount: Int,
    onTextChange: (String) -> Unit,
    onTargetCountChange: (Int) -> Unit,
    onDelete: (() -> Unit)?,
    showDeleteButton: Boolean,
    placeholder: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "소원",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF666666)
            )
            
            if (showDeleteButton && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete wish",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Text input field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFF9FBFF),
                    shape = RoundedCornerShape(5.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFFF0F0F0),
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(12.dp)
        ) {
            if (wishText.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF999999)
                )
            }
            
            BasicTextField(
                value = wishText,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF333333)
                ),
                maxLines = 2
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "목표 횟수",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF666666)
            )
            
            // Custom Number Picker for target count
            CustomNumberPicker(
                selectedValue = targetCount,
                onValueChange = onTargetCountChange,
                range = 100..10000,
                step = 100,
                modifier = Modifier.size(width = 80.dp, height = 120.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Display Mode")
@Composable
private fun WishCardDisplayPreview() {
    WishRingTheme {
        WishCard(
            wishText = "매일 운동하기",
            onClick = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Input Mode - Empty")
@Composable
private fun WishCardInputEmptyPreview() {
    WishRingTheme {
        WishCard(
            wishText = "",
            isInputMode = true,
            targetCount = 1000,
            onTextChange = { /* Preview - no action */ },
            onTargetCountChange = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Input Mode - Filled")
@Composable
private fun WishCardInputFilledPreview() {
    WishRingTheme {
        WishCard(
            wishText = "나는 매일 성장하고 있다",
            isInputMode = true,
            targetCount = 2000,
            showDeleteButton = true,
            onTextChange = { /* Preview - no action */ },
            onTargetCountChange = { /* Preview - no action */ },
            onDelete = { /* Preview - no action */ }
        )
    }
}

@Preview(showBackground = true, name = "Long Text Display")
@Composable
private fun WishCardLongTextPreview() {
    WishRingTheme {
        WishCard(
            wishText = "나는 매일 아침 일찍 일어나서 운동을 하고, 건강한 아침 식사를 먹고, 독서를 통해 새로운 지식을 습득하며, 가족과 소중한 시간을 보내고, 일에서도 최선을 다하여 더 나은 내가 되기 위해 끊임없이 노력하고 성장하는 사람이 되고 싶다.",
            onClick = { /* Preview - no action */ }
        )
    }
}