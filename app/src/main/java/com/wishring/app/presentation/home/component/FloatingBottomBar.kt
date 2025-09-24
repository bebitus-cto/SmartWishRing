package com.wishring.app.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.R
import com.wishring.app.presentation.home.HomeViewState
import com.wishring.app.ui.theme.Purple_Medium
import com.wishring.app.ui.theme.Text_Secondary

@Composable
fun FloatingBottomBar(
    uiState: HomeViewState,
    isConnected: Boolean,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp)
        ) {
            // Battery status
            // Battery level display - always show icon, percentage only when connected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {

                // 배터리 아이콘은 항상 표시
                Icon(
                    painter = painterResource(id = R.drawable.ic_battery),
                    contentDescription = stringResource(id = R.string.battery_description),
                    tint = if (uiState.deviceBatteryLevel != null && uiState.deviceBatteryLevel < 20) Color.Red else Text_Secondary,
                    modifier = Modifier.size(width = 37.dp, height = 21.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))

                // 배터리 퍼센트는 연결되고 값이 있을 때만 표시
                if (isConnected && uiState.deviceBatteryLevel != null) {
                    Text(
                        text = "${uiState.deviceBatteryLevel}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (uiState.deviceBatteryLevel < 20) Color.Red else Text_Secondary
                    )
                } else if (isConnected) {
                    // 배터리 레벨 로딩 중
                    Text(
                        text = "연결중...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Text_Secondary
                    )
                }
            }

            // Share button
            IconButton(
                onClick = onShareClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera),
                    contentDescription = stringResource(id = R.string.share_description),
                    tint = Purple_Medium,
                    modifier = Modifier.size(width = 33.dp, height = 27.dp)
                )
            }
        }
    }
}