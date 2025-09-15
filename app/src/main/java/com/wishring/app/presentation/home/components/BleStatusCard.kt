package com.wishring.app.presentation.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wishring.app.R
import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.domain.repository.isConnected
import com.wishring.app.domain.model.BatteryStatus

/**
 * Extension function to get status text for BleConnectionState enum
 */
private fun BleConnectionState.statusText(): String {
    return when (this) {
        BleConnectionState.DISCONNECTED -> "연결 안됨"
        BleConnectionState.CONNECTING -> "연결 중..."
        BleConnectionState.CONNECTED -> "연결됨"
        BleConnectionState.DISCONNECTING -> "연결 해제 중..."
        BleConnectionState.ERROR -> "연결 오류"
    }
}

/**
 * BLE connection status card for HomeScreen
 * Shows connection state, battery level, and provides quick actions
 */
@Composable
fun BleStatusCard(
    connectionState: BleConnectionState,
    batteryLevel: Int,
    batteryStatus: BatteryStatus,
    onRetryConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                if (!connectionState.isConnected()) {
                    onRetryConnection()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                BleConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                BleConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                BleConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Connection status section
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            color = when (connectionState) {
                                BleConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                BleConnectionState.CONNECTING -> Color(0xFFFF9800)
                                BleConnectionState.ERROR -> Color(0xFFE91E63)
                                else -> Color(0xFF9E9E9E)
                            }
                        )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "WISH RING",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    AnimatedContent(
                        targetState = connectionState.statusText(),
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "connection_status_text"
                    ) { statusText ->
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Battery and actions section
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery indicator (only when connected)
                if (connectionState.isConnected()) {
                    BatteryIndicator(
                        level = batteryLevel,
                        status = batteryStatus
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // Action button
                when (connectionState) {
                    BleConnectionState.DISCONNECTED,
                    BleConnectionState.ERROR -> {
                        IconButton(
                            onClick = onRetryConnection
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "재연결",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    BleConnectionState.CONNECTED -> {
                        IconButton(
                            onClick = onOpenSettings
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "설정",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    BleConnectionState.CONNECTING,
                    BleConnectionState.DISCONNECTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Battery level indicator with color coding
 */
@Composable
private fun BatteryIndicator(
    level: Int,
    status: BatteryStatus,
    modifier: Modifier = Modifier
) {
    val batteryColor = when (status) {
        BatteryStatus.GOOD -> Color(0xFF4CAF50)
        BatteryStatus.HIGH -> Color(0xFF4CAF50)  // Same as GOOD (deprecated)
        BatteryStatus.MEDIUM -> Color(0xFFFF9800)
        BatteryStatus.LOW -> Color(0xFFE91E63)
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (status) {
                BatteryStatus.GOOD -> Icons.Default.BatteryFull
                BatteryStatus.HIGH -> Icons.Default.BatteryFull  // Same as GOOD (deprecated)
                BatteryStatus.MEDIUM -> Icons.Default.Battery3Bar
                BatteryStatus.LOW -> Icons.Default.Battery1Bar
            },
            contentDescription = "배터리 $level%",
            tint = batteryColor,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = "$level%",
            style = MaterialTheme.typography.bodySmall,
            color = batteryColor,
            fontWeight = FontWeight.Medium
        )
    }
}