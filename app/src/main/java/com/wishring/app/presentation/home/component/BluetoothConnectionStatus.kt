package com.wishring.app.presentation.home.component

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wishring.app.MainActivity
import com.wishring.app.R
import com.wishring.app.core.util.ShareUtils
import com.wishring.app.data.model.DailyRecord
import com.wishring.app.presentation.component.CircularProgress
import com.wishring.app.presentation.component.WishCard
import com.wishring.app.presentation.components.ConnectionSuccessAnimation
import com.wishring.app.presentation.components.ShareDialog
import com.wishring.app.presentation.home.HomeViewState
import com.wishring.app.presentation.home.component.BleDevicePickerDialog
import com.wishring.app.presentation.main.AutoConnectResult
import com.wishring.app.presentation.main.DeviceInfo
import com.wishring.app.presentation.main.MainViewModel
import com.wishring.app.ui.theme.Purple_Medium
import com.wishring.app.ui.theme.Purple_Primary
import com.wishring.app.ui.theme.Text_Primary
import com.wishring.app.ui.theme.Text_Secondary
import com.wishring.app.ui.theme.Text_Tertiary

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