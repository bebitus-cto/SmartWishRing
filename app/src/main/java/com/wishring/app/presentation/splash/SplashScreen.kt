package com.wishring.app.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.R
import com.wishring.app.ui.theme.Purple_Medium
import com.wishring.app.ui.theme.WishRingTheme
import kotlinx.coroutines.delay

/**
 * Splash screen based on Figma design
 * Shows WISH RING text positioned on the left side of the circular ring
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit = {}
) {
    // 2초 후 자동으로 다음 화면으로 이동
    LaunchedEffect(Unit) {
        delay(2000) // 2초 지연
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // 흰색 배경
        contentAlignment = Alignment.Center
    ) {
        // 원형 링 이미지 - 중앙에 위치
        Image(
            painter = painterResource(id = R.drawable.splash_ring),
            contentDescription = "Ring",
            modifier = Modifier
                .size(200.dp) // 링 크기 조정
        )
        
        // WISH RING 텍스트 - 링의 왼쪽에 위치
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .offset(x = (-40).dp, y = 0.dp), // 링의 왼쪽으로 이동
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "WISH RING",
                fontSize = 30.sp,           // 요구사항: 30sp
                fontWeight = FontWeight.SemiBold, // 요구사항: SemiBold
                color = Purple_Medium,      // 요구사항: #6A5ACD
                textAlign = TextAlign.Center,
                letterSpacing = 0.6.sp,     // 요구사항: 2% letter spacing
                lineHeight = 36.sp          // 줄 간격 조정
            )
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