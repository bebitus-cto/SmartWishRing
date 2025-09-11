package com.wishring.app.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.*

// 간격 상수
private const val GAP_WIDTH = 3f // 직사각형 가로 길이 (픽셀)



/**
 * Circular progress indicator with text display
 * Used in Home screen to show wish count progress
 */
@Composable
fun CircularProgress(
    current: Int,
    target: Int,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    backgroundColor: Color = LocalWishRingColors.current.progressBackground,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    animationDuration: Int = 1000,
    showText: Boolean = true
) {
    val progress = if (target > 0) current.toFloat() / target.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = animationDuration),
        label = "progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        // Progress and background arcs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progressAngle = animatedProgress * 360f

            // 배경 호 (전체 원)
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Butt
                )
            )

            // 진행률 호 (덮어쓰기)
            if (progressAngle > 0) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = progressAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Butt
                    )
                )
            }
            
            // 간격용 직사각형들  
            val center = this.center
            val radius = this.size.width.coerceAtMost(this.size.height) / 2f
            
            // 1. 12시 방향 직사각형 (회전 없음)
            // rotate(-90f, pivot = center) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(center.x - GAP_WIDTH/2, center.y - radius - strokeWidth.toPx()/2 - 4f),
                    size = Size(GAP_WIDTH, strokeWidth.toPx())
                )
            // }
            
            // 2. 진행률 끝 지점 직사각형 (해당 각도로 회전)
            if (progressAngle > 0) {
                val endAngle = -90f + progressAngle
                val endX = center.x + radius * kotlin.math.cos(Math.toRadians(endAngle.toDouble())).toFloat()
                val endY = center.y + radius * kotlin.math.sin(Math.toRadians(endAngle.toDouble())).toFloat()
                
                rotate(endAngle + 90f, pivot = Offset(endX, endY)) {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(endX - GAP_WIDTH/2, endY - strokeWidth.toPx()/2 - 4f),
                        size = Size(GAP_WIDTH, strokeWidth.toPx())
                    )
                }
            }
        }

        // Text display
        if (showText) {
            Text(
                text = "$current/$target",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6A5ACD)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FF)
@Composable
fun CircularProgressPreview() {
    WishRingTheme {
        CircularProgress(
            current = 750,
            target = 1000
        )
    }
}