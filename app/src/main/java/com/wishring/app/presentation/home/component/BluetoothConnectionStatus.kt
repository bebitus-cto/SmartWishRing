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
import com.wishring.app.R
import com.wishring.app.presentation.home.HomeViewState
import com.wishring.app.presentation.main.BlePhase

@Composable
fun BluetoothConnectionStatus(
    onClick: () -> Unit,
    uiState: HomeViewState.BluetoothDisconnected,
    blePhase: BlePhase,
    modifier: Modifier = Modifier
) {
    // Direct phase-based state handling
    val showLoading = when (blePhase) {
        BlePhase.Scanning,
        BlePhase.DeviceSelected,   // 기기 선택됨
        BlePhase.Connecting,
        BlePhase.Connected,        // 연결 후 초기화 대기
        BlePhase.Initializing,     // 초기화 중
        BlePhase.ReadingSettings,  // 설정 읽는 중
        BlePhase.WritingTime,      // 시간 동기화 중
        BlePhase.AutoConnecting -> true
        BlePhase.Idle,
        BlePhase.Ready -> false
    }
    
    val buttonText = when (blePhase) {
        BlePhase.Idle -> "블루투스 연결하기"
        BlePhase.Scanning -> "기기 검색중..."
        BlePhase.DeviceSelected -> "기기 연결 준비중..."
        BlePhase.Connecting -> "연결 시도중..."
        BlePhase.Connected -> "연결됨"
        BlePhase.Initializing -> "초기화중..."
        BlePhase.ReadingSettings -> "설정 읽는 중..."
        BlePhase.WritingTime -> "시간 동기화 중..."
        BlePhase.Ready -> "연결 완료"
        BlePhase.AutoConnecting -> "자동 연결 시도중..."
    }
    
    val isEnabled = blePhase == BlePhase.Idle

    // 블루투스 연결하기 버튼
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (showLoading) Color(0xFF90CAF9) else Color(0xFF2196F3),
            disabledContainerColor = Color(0xFF90CAF9)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 상태에 따라 프로그레스바 또는 블루투스 아이콘 표시
            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bluetooth),
                    contentDescription = "Bluetooth",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}