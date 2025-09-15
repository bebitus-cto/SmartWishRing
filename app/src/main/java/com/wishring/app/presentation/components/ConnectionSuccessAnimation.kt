package com.wishring.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.Purple_Primary

/**
 * Connection success animation
 * Shows a celebratory animation when bluetooth connection is established
 */
@Composable
fun ConnectionSuccessAnimation(
    modifier: Modifier = Modifier
) {
    var animationStarted by remember { mutableStateOf(false) }
    
    // Scale animation for the main circle
    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Rotation animation for bluetooth icon
    val rotation by animateFloatAsState(
        targetValue = if (animationStarted) 360f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "rotation"
    )
    
    // Alpha animation for check icon
    val checkAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 400
        ),
        label = "checkAlpha"
    )
    
    // Start animation when component appears
    LaunchedEffect(Unit) {
        animationStarted = true
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .size(200.dp)
                .scale(scale),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Bluetooth icon with rotation
                    Box(
                        modifier = Modifier.size(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer { rotationZ = rotation },
                            tint = Purple_Primary
                        )
                        
                        // Check mark overlay
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { alpha = checkAlpha }
                                .offset(x = 15.dp, y = (-15).dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "연결 성공!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Purple_Primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "WISH RING과 연결되었습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Ripple effect circles
        repeat(3) { index ->
            val rippleScale by animateFloatAsState(
                targetValue = if (animationStarted) 2f else 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        delayMillis = index * 200
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ripple$index"
            )
            
            val rippleAlpha by animateFloatAsState(
                targetValue = if (animationStarted) 0f else 0.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        delayMillis = index * 200
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleAlpha$index"
            )
            
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(rippleScale)
                    .background(
                        color = Purple_Primary.copy(alpha = rippleAlpha),
                        shape = CircleShape
                    )
            )
        }
    }
}