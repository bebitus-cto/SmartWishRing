package com.wishring.app.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wishring.app.presentation.main.DeviceInfo
import com.wishring.app.ui.theme.Purple_Medium
import com.wishring.app.ui.theme.Text_Primary
import com.wishring.app.ui.theme.Text_Secondary
import com.wishring.app.ui.theme.Text_Tertiary

/**
 * BLE Device Picker Dialog
 * Shows available BLE devices for selection
 */
@Composable
fun BleDevicePickerDialog(
    devices: List<DeviceInfo>,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "블루투스 기기 선택",
                style = MaterialTheme.typography.headlineSmall,
                color = Text_Primary
            )
        },
        text = {
            Column {
                Text(
                    text = "연결할 WISH RING 기기를 선택하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text_Secondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn {
                    items(devices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onDeviceSelected(device.address) },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Device icon
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = Purple_Medium,
                                    modifier = Modifier.size(12.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name.takeIf { it.isNotBlank() }
                                            ?: "Unknown Device",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Text_Primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Text_Secondary
                                    )
                                }

                                // Signal strength
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${device.rssi} dBm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Text_Tertiary
                                    )
                                    // Signal strength bars
                                    Row {
                                        repeat(4) { index ->
                                            val isActive = when {
                                                device.rssi >= -50 -> index < 4
                                                device.rssi >= -60 -> index < 3
                                                device.rssi >= -70 -> index < 2
                                                else -> index < 1
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(((index + 1) * 3).dp)
                                                    .background(
                                                        if (isActive) Purple_Medium else Color.Gray.copy(
                                                            alpha = 0.3f
                                                        ),
                                                        RoundedCornerShape(1.dp)
                                                    )
                                            )
                                            if (index < 3) Spacer(modifier = Modifier.width(1.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "취소",
                    color = Text_Secondary
                )
            }
        },
        containerColor = Color.White,
        modifier = Modifier.padding(16.dp)
    )
}