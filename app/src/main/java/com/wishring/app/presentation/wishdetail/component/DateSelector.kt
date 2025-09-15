package com.wishring.app.presentation.wishdetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wishring.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date selector component
 * Allows navigation between dates
 */
@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = { 
                val previousDate = selectedDate.minusDays(1)
                if (minDate == null || !previousDate.isBefore(minDate)) {
                    onDateChange(previousDate)
                }
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Previous day",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Date display
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = selectedDate.format(dateFormatter),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        
        // Forward button
        IconButton(
            onClick = { 
                val nextDate = selectedDate.plusDays(1)
                if (maxDate == null || !nextDate.isAfter(maxDate)) {
                    onDateChange(nextDate)
                }
            },
            modifier = Modifier.padding(end = 8.dp),
            enabled = maxDate == null || !selectedDate.isEqual(maxDate)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Next day",
                tint = if (maxDate != null && selectedDate.isEqual(maxDate)) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
    
    // Bottom divider
    Divider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline
    )
}

/**
 * Date selector with calendar picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorWithPicker(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        DateSelector(
            selectedDate = selectedDate,
            onDateChange = onDateChange,
            minDate = minDate,
            maxDate = maxDate
        )
        
        // Date picker button
        TextButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("날짜 선택")
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                onDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            selectedDate = selectedDate,
            minDate = minDate,
            maxDate = maxDate
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    selectedDate: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?
) {
    // This is a simplified version - in production, you'd use a proper date picker
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("날짜 선택") },
        text = {
            Text("Calendar picker would go here")
        },
        confirmButton = {
            TextButton(onClick = { onDateSelected(selectedDate) }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DateSelectorPreview() {
    WishRingTheme {
        var selectedDate by remember { mutableStateOf(LocalDate.of(2025, 8, 21)) }
        
        DateSelector(
            selectedDate = selectedDate,
            onDateChange = { selectedDate = it },
            maxDate = LocalDate.now()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DateSelectorWithPickerPreview() {
    WishRingTheme {
        var selectedDate by remember { mutableStateOf(LocalDate.now()) }
        
        DateSelectorWithPicker(
            selectedDate = selectedDate,
            onDateChange = { selectedDate = it },
            maxDate = LocalDate.now()
        )
    }
}