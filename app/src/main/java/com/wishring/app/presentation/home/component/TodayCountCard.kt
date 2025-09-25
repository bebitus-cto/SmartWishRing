package com.wishring.app.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.R
import com.wishring.app.presentation.home.HomeEvent
import com.wishring.app.presentation.home.HomeViewState

@Composable
fun TodayCountCard(
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier,
    uiState: HomeViewState? = null,
    onEvent: ((HomeEvent) -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.todays_count),
                            color = Color(0xFF333333),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentCount.toString(),
                            color = Color(0xFF333333),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Center: Vertical Divider
                VerticalDivider(
                    color = Color(0xFFDBDBDB),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    CircularProgress(
                        current = currentCount,
                        target = targetCount,
                        modifier = Modifier.size(120.dp),
                        showText = true
                    )
                }
            }
        }
    }
}