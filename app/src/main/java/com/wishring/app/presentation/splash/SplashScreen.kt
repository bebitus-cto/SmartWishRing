package com.wishring.app.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.R
import com.wishring.app.ui.theme.Purple_Medium
import com.wishring.app.ui.theme.Text_Secondary
import com.wishring.app.ui.theme.WishRingTheme
import kotlinx.coroutines.delay

/**
 * Splash screen based on Figma design
 * Shows WISH RING text positioned on the left side of the circular ring
 * With animations as per requirements SPL-02, SPL-03
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit = {}
) {
    // Animation states
    var logoVisible by remember { mutableStateOf(false) }
    var logoShimmer by remember { mutableStateOf(false) }
    var firstTextVisible by remember { mutableStateOf(false) }

    
    // Logo shimmer animation
    val shimmerAlpha by animateFloatAsState(
        targetValue = if (logoShimmer) 1f else 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    // Text slide animations
    val firstTextOffset by animateFloatAsState(
        targetValue = if (firstTextVisible) 0f else -100f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "first_text"
    )
    

    
    // Animation sequence
    LaunchedEffect(Unit) {
        // Step 1: Show logo with fade in
        delay(100)
        logoVisible = true
        
        // Step 2: Start shimmer effect (SPL-02)
        delay(300)
        logoShimmer = true
        
        // Step 3: First text slides in (SPL-03)
        delay(200)
        firstTextVisible = true
        
        // Step 4: Complete splash after total 2 seconds
        delay(800)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // 흰색 배경
        contentAlignment = Alignment.Center
    ) {
        // 원형 링 이미지 - 중앙에 위치 with shimmer effect
        AnimatedVisibility(
            visible = logoVisible,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300)
            )
        ) {
            Box(
                modifier = Modifier.size(200.dp)
            ) {
                // Base ring image
                Image(
                    painter = painterResource(id = R.drawable.splash_ring),
                    contentDescription = "Ring",
                    modifier = Modifier.fillMaxSize()
                )
                
                // Shimmer overlay for metallic purple foil effect (SPL-02)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = shimmerAlpha * 0.3f // Subtle shimmer
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Purple_Medium.copy(alpha = 0.6f),
                                    Color.Transparent
                                ),
                                center = Offset(0.5f, 0.5f),
                                radius = 0.8f
                            )
                        )
                )
            }
        }
        
        // WISH RING 텍스트 - 링의 왼쪽에 위치 with slide animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .offset(x = (-40).dp, y = 0.dp), // 링의 왼쪽으로 이동
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // First text: "WISH RING" sliding from left (SPL-03)
                AnimatedVisibility(
                    visible = firstTextVisible,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    Text(
                        text = "WISH RING",
                        fontSize = 30.sp,           // 요구사항: 30sp
                        fontWeight = FontWeight.SemiBold, // 요구사항: SemiBold
                        color = Purple_Medium,      // 요구사항: #6A5ACD
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.6.sp,     // 요구사항: 2% letter spacing
                        lineHeight = 36.sp,         // 줄 간격 조정
                        modifier = Modifier
                            .offset(x = firstTextOffset.dp)
                            .graphicsLayer {
                                // Add slight rotation for dynamic effect
                                rotationZ = firstTextOffset * 0.1f
                            }
                    )
                }
                
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    WishRingTheme {
        SplashScreen()
    }
}