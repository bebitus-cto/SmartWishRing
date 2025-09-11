package com.wishring.app.presentation.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.wishring.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WishReportItem(
    wishText: String,
    date: String,
    count: Int,
    isCompleted: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFFFAFAFA))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Character image
        Image(
            painter = painterResource(id = R.drawable.wish_character),
            contentDescription = null,
            modifier = Modifier
                .size(22.dp, 24.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(15.dp))

        // Text content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = wishText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 12.sp
                ),
                color = Text_Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 10.sp
                ),
                color = Text_Primary.copy(alpha = 0.5f)
            )
        }

        // Count display
        Text(
            text = count.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,"),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp
            ),
            color = Text_Primary
        )
    }

    // Bottom divider
    Divider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = Color(0xFFEEEEEE) // 더 밝은 회색으로 변경
    )
}

@Preview(showBackground = true)
@Composable
fun WishReportItemPreview() {
    WishRingTheme {
        Column {
            WishReportItem(
                wishText = "나는 어제보다 더 나은 내가 되고 있다.",
                date = "2025-08-21",
                count = 1000,
                isCompleted = true
            )
            WishReportItem(
                wishText = "매일 조금씩, 나는 내 가능성을 확장하고 있다.",
                date = "2025-08-20",
                count = 850,
                isCompleted = false
            )
        }
    }
}