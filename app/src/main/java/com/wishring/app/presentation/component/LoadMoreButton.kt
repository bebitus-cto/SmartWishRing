package com.wishring.app.presentation.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.Purple_Medium

/**
 * Reusable "더보기" (Load More) button component
 * Used throughout the app for loading additional content
 */
@Composable
fun LoadMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "더보기",
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .width(100.dp)
            .height(36.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Purple_Medium)
    ) {
        Text(text, fontSize = 12.sp)
    }
}