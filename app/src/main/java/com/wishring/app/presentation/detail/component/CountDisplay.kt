package com.wishring.app.presentation.detail.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.*

/**
 * Large count display component
 * Shows the total count with unit
 */
@Composable
fun CountDisplay(
    count: Int,
    unit: String = "회",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // Main count number
        Text(
            text = String.format("%,d", count),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp
            ),
            color = Purple_Medium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Unit text
        Text(
            text = unit,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

/**
 * Count display with description
 */
@Composable
fun CountDisplayWithDescription(
    count: Int,
    unit: String = "회",
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CountDisplay(
            count = count,
            unit = unit
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CountDisplayPreview() {
    WishRingTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            CountDisplay(count = 1000)
            
            CountDisplayWithDescription(
                count = 1000,
                description = "매일의 반복이 만든 숫자입니다.\n그 반복들이 오늘의 나를 만듭니다."
            )
        }
    }
}