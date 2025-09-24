package com.wishring.app.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Number Picker Dialog for selecting target count
 * Shows a wheel-style picker with progressive step increments
 */
@Composable
fun NumberPickerDialog(
    currentValue: Int,
    onValueSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedValue by remember { mutableStateOf(currentValue) }
    
    // Calculate appropriate step based on value range
    val step = when {
        selectedValue < 100 -> 1
        selectedValue < 1000 -> 10
        selectedValue < 10000 -> 100
        else -> 1000
    }
    
    // Calculate range based on current value
    val range = when {
        selectedValue < 100 -> 1..200
        selectedValue < 1000 -> 10..2000
        selectedValue < 10000 -> 100..20000
        else -> 1000..100000
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "목표 횟수 선택",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Number Picker
                CustomNumberPicker(
                    selectedValue = selectedValue,
                    onValueChange = { selectedValue = it },
                    range = range,
                    step = step,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    visibleItemsCount = 5
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "취소",
                            color = Color(0xFF999999)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    TextButton(
                        onClick = { 
                            onValueSelected(selectedValue)
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "확인",
                            color = Color(0xFF6A5ACD),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}