package com.wishring.app.presentation.home.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular progress gauge component
 * Shows progress towards target with color change on completion
 */
@Composable
fun CircularGauge(
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 8.dp.value,
    animationDuration: Int = 1000
) {
    // Safe progress calculation to prevent NaN
    val progress = if (targetCount <= 0) {
        0f
    } else {
        (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
    }
    val isCompleted = currentCount >= targetCount && targetCount > 0
    
    // Animate progress with additional NaN validation
    val animatedProgress by animateFloatAsState(
        targetValue = if (progress.isFinite()) progress else 0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "progress"
    )
    
    // Rainbow gradient for completed state
    val rainbowColors = listOf(
        Color(0xFFFF0000), // Red
        Color(0xFFFF7F00), // Orange
        Color(0xFFFFFF00), // Yellow
        Color(0xFF00FF00), // Green
        Color(0xFF0000FF), // Blue
        Color(0xFF4B0082), // Indigo
        Color(0xFF9400D3)  // Violet
    )
    
    Box(
        modifier = modifier.size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced size validation to prevent "Offset is unspecified" crash
            if (size.width > 0 && size.height > 0 && size.width.isFinite() && size.height.isFinite()) {
                val canvasSize = size.minDimension
                val radius = (canvasSize - strokeWidth) / 2
                
                // Additional validation for radius and ensure it's positive
                if (radius > 0 && radius.isFinite() && radius > strokeWidth) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // Validate center coordinates before creating Offset
                    if (centerX.isFinite() && centerY.isFinite() && centerX > 0 && centerY > 0) {
                        try {
                            val center = Offset(centerX, centerY)
                            
                            // Background circle
                            drawCircle(
                                color = Color(0xFFC5C5C5),
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            
                            // Progress arc
                            val sweepAngle = animatedProgress * 360f
                            
                            if (sweepAngle > 0 && sweepAngle.isFinite()) {
                                val topLeftX = center.x - radius
                                val topLeftY = center.y - radius
                                
                                // Ensure topLeft coordinates are valid
                                if (topLeftX.isFinite() && topLeftY.isFinite()) {
                                    val topLeft = Offset(topLeftX, topLeftY)
                                    val arcSize = Size(radius * 2, radius * 2)
                                    
                                    if (isCompleted) {
                                        // Rainbow gradient for completed state
                                        val segmentAngle = sweepAngle / rainbowColors.size
                                        rainbowColors.forEachIndexed { index, color ->
                                            val startAngle = -90f + (index * segmentAngle)
                                            val endAngle = if (index == rainbowColors.size - 1) {
                                                sweepAngle - (index * segmentAngle)
                                            } else {
                                                segmentAngle
                                            }
                                            
                                            if (endAngle > 0 && endAngle.isFinite()) {
                                                drawArc(
                                                    color = color,
                                                    startAngle = startAngle,
                                                    sweepAngle = endAngle,
                                                    useCenter = false,
                                                    topLeft = topLeft,
                                                    size = arcSize,
                                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                                )
                                            }
                                        }
                                    } else {
                                        // Purple gradient for incomplete state
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                colors = listOf(
                                                    Purple_Primary,
                                                    Purple_Medium,
                                                    Purple_Primary
                                                ),
                                                center = center
                                            ),
                                            startAngle = -90f,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                    
                                    // Progress indicator dot
                                    if (sweepAngle > 0) {
                                        val angleInRadians = Math.toRadians(sweepAngle - 90.0)
                                        val cosValue = cos(angleInRadians).toFloat()
                                        val sinValue = sin(angleInRadians).toFloat()
                                        
                                        // Validate trigonometric results
                                        if (cosValue.isFinite() && sinValue.isFinite()) {
                                            val dotX = center.x + radius * cosValue
                                            val dotY = center.y + radius * sinValue
                                            
                                            if (dotX.isFinite() && dotY.isFinite()) {
                                                val dotOffset = Offset(dotX, dotY)
                                                drawCircle(
                                                    color = if (isCompleted) Color.White else Purple_Primary,
                                                    radius = strokeWidth * 0.8f,
                                                    center = dotOffset
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Silently handle any drawing exceptions to prevent crashes
                        }
                    }
                }
            }
        }
        
        // Progress text
        Text(
            text = "$currentCount/$targetCount",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Text_Primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = 25.dp)
        )
    }
}