package com.wishring.app.presentation.wishinput

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.Purple_Medium
import com.wishring.app.ui.theme.WishRingTheme
import kotlinx.coroutines.delay

/**
 * Custom Number Picker Component
 * Compose-native wheel/roulette picker for target count selection
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomNumberPicker(
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 1..10000,
    step: Int = 1,
    visibleItemsCount: Int = 5
) {
    val values = remember(range, step) {
        range.step(step).toList()
    }
    
    val selectedIndex = remember(selectedValue, values) {
        values.indexOf(selectedValue).coerceAtLeast(0)
    }
    
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - visibleItemsCount / 2).coerceAtLeast(0)
    )
    
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // Track scroll changes and update selected value
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            delay(100) // Small delay to ensure scroll has fully stopped
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val centerIndex = firstVisibleIndex + visibleItemsCount / 2
            if (centerIndex in values.indices) {
                val newValue = values[centerIndex]
                if (newValue != selectedValue) {
                    onValueChange(newValue)
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .height(160.dp)
            .width(80.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Add padding items at the beginning
            items(visibleItemsCount / 2) {
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            items(values) { value ->
                NumberPickerItem(
                    value = value,
                    isSelected = value == selectedValue
                )
            }
            
            // Add padding items at the end
            items(visibleItemsCount / 2) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NumberPickerItem(
    value: Int,
    isSelected: Boolean
) {
    val alpha = if (isSelected) 1f else 0.4f
    val textColor = if (isSelected) Purple_Medium else Color(0xFF666666)
    val fontSize = if (isSelected) 18.sp else 14.sp
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    
    Text(
        text = "${value}회",
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            fontWeight = fontWeight
        ),
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .height(32.dp)
            .fillMaxWidth()
            .alpha(alpha)
            .wrapContentHeight(Alignment.CenterVertically)
    )
}

@Preview(showBackground = true, name = "Number Picker - Default")
@Composable
private fun CustomNumberPickerPreview() {
    WishRingTheme {
        var selectedValue by remember { mutableIntStateOf(1000) }
        
        CustomNumberPicker(
            selectedValue = selectedValue,
            onValueChange = { selectedValue = it },
            range = 100..5000,
            step = 100
        )
    }
}

@Preview(showBackground = true, name = "Number Picker - Small Range")
@Composable
private fun CustomNumberPickerSmallRangePreview() {
    WishRingTheme {
        var selectedValue by remember { mutableIntStateOf(5) }
        
        CustomNumberPicker(
            selectedValue = selectedValue,
            onValueChange = { selectedValue = it },
            range = 1..20,
            step = 1
        )
    }
}

@Preview(showBackground = true, name = "Number Picker - Large Values")
@Composable
private fun CustomNumberPickerLargeValuesPreview() {
    WishRingTheme {
        var selectedValue by remember { mutableIntStateOf(10000) }
        
        CustomNumberPicker(
            selectedValue = selectedValue,
            onValueChange = { selectedValue = it },
            range = 1000..50000,
            step = 1000
        )
    }
}