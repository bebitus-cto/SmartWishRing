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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.presentation.wishinput.CustomNumberPicker

/**
 * Reusable wish card item component
 * Used in WishDetailScreen (Normal mode) and WishInputScreen (Edit mode)
 * Supports both display and input modes
 */
@Composable
fun WishCardItem(
    wishText: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    targetCount: Int = 1000,
    onTextChange: ((String) -> Unit)? = null,
    onTargetCountChange: ((Int) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showDeleteButton: Boolean = false,
    placeholder: String = "소원을 입력하세요...",
    showTargetCount: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(5.dp),
                spotColor = Color(0x1A000000)
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(5.dp)
            )
            .then(
                if (!isEditMode) Modifier.clickable { onClick() }
                else Modifier
            )
    ) {
        if (isEditMode) {
            WishEditCard(
                wishText = wishText,
                targetCount = targetCount,
                onTextChange = onTextChange ?: {},
                onTargetCountChange = onTargetCountChange ?: {},
                onDelete = onDelete,
                showDeleteButton = showDeleteButton,
                placeholder = placeholder,
                showTargetCount = showTargetCount
            )
        } else {
            WishNormalCard(wishText = wishText)
        }
    }
}

@Composable
private fun WishNormalCard(
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
private fun WishEditCard(
    wishText: String,
    targetCount: Int,
    onTextChange: (String) -> Unit,
    onTargetCountChange: (Int) -> Unit,
    onDelete: (() -> Unit)?,
    showDeleteButton: Boolean,
    placeholder: String,
    showTargetCount: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Text input field with delete button in the same row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Text input field
            Box(
                modifier = Modifier
                    .weight(1f)
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
            
            // Delete button on the right
            if (showDeleteButton && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete wish",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        if (showTargetCount) {
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
}