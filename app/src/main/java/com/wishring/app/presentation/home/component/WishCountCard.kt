package com.wishring.app.presentation.home.component

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.wishring.app.ui.theme.*

@Composable
fun WishCountCard(
    currentCount: Int,
    targetCount: Int,
    wishText: String,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = Color(0x0D000000)
            )
            .clickable { onCardClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 33.dp, vertical = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Text content
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Today's Count",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Text_Primary
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = currentCount.toString().padStart(1, '0'),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Text_Primary
                    )
                }
                
                // Right side - Circular Gauge
                CircularGauge(
                    currentCount = currentCount,
                    targetCount = targetCount
                )
            }
            
            // Vertical divider line
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(125.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFFDBDBDB))
            )
        }
    }
}

/**
 * Simple wish count display without card
 */
@Composable
fun WishCountDisplay(
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularGauge(
            currentCount = currentCount,
            targetCount = targetCount,
            modifier = Modifier.size(ProgressIndicatorSize)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "$currentCount / $targetCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WishCountCardPreview() {
    WishRingTheme {
        WishCountCard(
            currentCount = 700,
            targetCount = 1000,
            wishText = "나는 매일 성장하고 있다"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WishCountDisplayPreview() {
    WishRingTheme {
        WishCountDisplay(
            currentCount = 700,
            targetCount = 1000
        )
    }
}