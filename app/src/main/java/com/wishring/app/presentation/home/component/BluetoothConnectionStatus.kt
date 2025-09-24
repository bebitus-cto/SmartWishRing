package com.wishring.app.presentation.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.presentation.home.HomeViewState

@Composable
fun BluetoothConnectionStatus(
    onClick: () -> Unit,
    uiState: HomeViewState,
    isAutoConnecting: Boolean,
    modifier: Modifier = Modifier
) {
    // 블루투스 연결하기 버튼
    Button(
        onClick = onClick,
        enabled = uiState.isConnectionButtonEnabled(isAutoConnecting),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (uiState.shouldShowConnectionLoading(isAutoConnecting)) Color(
                0xFF90CAF9
            ) else Color(0xFF2196F3),
            disabledContainerColor = Color(0xFF90CAF9)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (uiState.shouldShowConnectionLoading(isAutoConnecting)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(id = android.R.drawable.stat_sys_data_bluetooth),
                    contentDescription = "블루투스",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = uiState.getConnectionButtonText(isAutoConnecting),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}