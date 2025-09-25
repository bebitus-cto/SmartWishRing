package com.wishring.app.presentation.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.wishring.app.data.model.WishDayUiState

@Composable
fun LatestWishCard(
    latestRecord: WishDayUiState?,
    onWishClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    latestRecord?.let { record ->
        Text(
            text = record.wishText,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF333333),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
                .fillMaxWidth()
                .clickable { onWishClick(record.dateString) }
        )
    }
}