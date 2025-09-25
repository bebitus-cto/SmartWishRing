package com.wishring.app.presentation.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.R
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.ui.theme.Text_Primary
import com.wishring.app.ui.theme.Text_Secondary

@Composable
fun WishHistoryItem(
    record: WishDayUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Calendar icon
        Image(
            painter = painterResource(id = R.drawable.wish_calendar),
            contentDescription = "Calendar",
            modifier = Modifier.size(32.dp)
        )

        // Wish text and date
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = record.wishText,
                style = MaterialTheme.typography.bodySmall,
                color = Text_Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = record.dateString,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp
                ),
                color = Color(0xFF333333).copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Count with background
        Box(
            modifier = Modifier
                .background(
                    color = if (record.isCompleted) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${record.targetCount}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (record.isCompleted) Color(0xFF4CAF50) else Text_Secondary
            )
        }
    }
}