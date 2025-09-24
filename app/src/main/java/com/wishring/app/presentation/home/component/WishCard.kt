package com.wishring.app.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.Text_Primary

@Composable
fun WishCard(
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