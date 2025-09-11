package com.wishring.app.presentation.wishdetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.*

/**
 * Motivation message card component
 * Displays motivational messages with card style
 */
@Composable
fun MotivationCard(
    message: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isHighlighted) {
        Background_Card
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp)
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(5.dp),
                spotColor = LocalWishRingColors.current.shadowColor
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * List of motivation cards
 */
@Composable
fun MotivationCardList(
    messages: List<String>,
    selectedIndex: Int = -1,
    onCardClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        messages.forEachIndexed { index, message ->
            MotivationCard(
                message = message,
                isHighlighted = index == selectedIndex,
                onClick = { onCardClick(index) }
            )
        }
    }
}

/**
 * Motivation section with image placeholders
 */
@Composable
fun MotivationSection(
    messages: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Motivation cards
        MotivationCardList(messages = messages)
        
        // Image placeholders (for future implementation)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ImagePlaceholder(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
            )
            ImagePlaceholder(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
            )
        }
    }
}

@Composable
private fun ImagePlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder for future image implementation
        Text(
            text = "Image",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MotivationCardPreview() {
    WishRingTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MotivationCard(
                message = "나는 어제보다 더 나은 내가 되고 있다.",
                isHighlighted = false
            )
            
            MotivationCard(
                message = "오늘의 선택이 나를 더 단단하게 만든다.",
                isHighlighted = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MotivationSectionPreview() {
    WishRingTheme {
        MotivationSection(
            messages = listOf(
                "나는 어제보다 더 나은 내가 되고 있다.",
                "오늘의 선택이 나를 더 단단하게 만든다.",
                "내 안의 가능성은 멈추지 않고 자라고 있다."
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}