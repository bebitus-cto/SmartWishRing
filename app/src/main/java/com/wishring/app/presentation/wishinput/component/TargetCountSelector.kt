package com.wishring.app.presentation.wishinput.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.Purple_Primary

@Composable
fun TargetCountSelector(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int = 1,
    maxValue: Int = 10000,
    step: Int = 100,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "목표 횟수",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrease button
                FilledIconButton(
                    onClick = {
                        val newValue = (currentValue - step).coerceAtLeast(minValue)
                        onValueChange(newValue)
                    },
                    enabled = currentValue > minValue,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Purple_Primary.copy(alpha = 0.1f),
                        contentColor = Purple_Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "감소"
                    )
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                // Current value display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentValue.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Purple_Primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "회",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                // Increase button
                FilledIconButton(
                    onClick = {
                        val newValue = (currentValue + step).coerceAtMost(maxValue)
                        onValueChange(newValue)
                    },
                    enabled = currentValue < maxValue,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Purple_Primary.copy(alpha = 0.1f),
                        contentColor = Purple_Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "증가"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick select buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(100, 500, 1000, 2000).forEach { value ->
                    TextButton(
                        onClick = { onValueChange(value) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (currentValue == value) Purple_Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "$value",
                            fontWeight = if (currentValue == value) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}