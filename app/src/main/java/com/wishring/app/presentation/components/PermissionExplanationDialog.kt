package com.wishring.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wishring.app.ui.theme.Purple_Primary

/**
 * Dialog explaining why Bluetooth permissions are needed
 */
@Composable
fun PermissionExplanationDialog(
    explanations: Map<String, String>,
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header icon and title
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Purple_Primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "권한이 필요합니다",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "WISH RING과 연결하기 위해 다음 권한들이 필요합니다:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Permission explanations
                explanations.forEach { (permission, explanation) ->
                    PermissionExplanationItem(
                        permission = permission,
                        explanation = explanation,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("나중에")
                    }
                    
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple_Primary
                        )
                    ) {
                        Text("권한 허용")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionExplanationItem(
    permission: String,
    explanation: String,
    modifier: Modifier = Modifier
) {
    val icon = getPermissionIcon(permission)
    val permissionName = getPermissionDisplayName(permission)
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Purple_Primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = permissionName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getPermissionIcon(permission: String): ImageVector {
    return when {
        permission.contains("BLUETOOTH") -> Icons.Default.Bluetooth
        permission.contains("LOCATION") -> Icons.Default.LocationOn
        else -> Icons.Default.Security
    }
}

private fun getPermissionDisplayName(permission: String): String {
    return when (permission) {
        android.Manifest.permission.BLUETOOTH_SCAN -> "블루투스 스캔"
        android.Manifest.permission.BLUETOOTH_CONNECT -> "블루투스 연결"

        android.Manifest.permission.BLUETOOTH -> "블루투스 사용"
        android.Manifest.permission.BLUETOOTH_ADMIN -> "블루투스 관리"
        else -> "시스템 권한"
    }
}