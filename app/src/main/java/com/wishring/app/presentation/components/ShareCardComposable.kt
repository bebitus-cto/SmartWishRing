package com.wishring.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wishring.app.ui.theme.Gray_Light
import com.wishring.app.ui.theme.Purple_Medium
import com.wishring.app.ui.theme.Text_Primary
import com.wishring.app.ui.theme.Text_Secondary
import com.wishring.app.ui.theme.WishRingTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * SNS 공유용 카드 Composable
 * 오프스크린 렌더링을 통해 이미지로 캡처됨
 */
@Composable
fun ShareCardComposable(
    count: Int,
    wishText: String,
    targetCount: Int,
    date: String = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date()),
    modifier: Modifier = Modifier
) {
    // 진행률 계산
    val progress = if (targetCount > 0) {
        (count.toFloat() / targetCount.toFloat()).coerceAtMost(1f)
    } else {
        0f
    }
    
    val isCompleted = count >= targetCount
    
    Card(
        modifier = modifier
            .size(400.dp, 600.dp)
            .background(Color.White),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 로고 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // WISH RING 로고
                Text(
                    text = "WISH RING",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Purple_Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "성공을 품은 반지",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Text_Secondary
                )
            }
            
            // 중앙 카운트 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 날짜
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Text_Secondary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 메인 카운트
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 70.sp
                        ),
                        color = if (isCompleted) {
                            Purple_Medium
                        } else {
                            Purple_Medium
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "회",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Text_Primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 진행률 게이지
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(60.dp))
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = Gray_Light.copy(alpha = 0.3f),
                            strokeWidth = 12.dp,
                            trackColor = Color.Transparent
                        )
                        
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (isCompleted) {
                                // 목표 달성 시 보라색
                                Purple_Medium
                            } else {
                                Purple_Medium
                            },
                            strokeWidth = 12.dp,
                            trackColor = Color.Transparent
                        )
                        
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Text_Primary
                            )
                            
                            Text(
                                text = "$targetCount 목표",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp
                                ),
                                color = Text_Secondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 위시 텍스트
                Text(
                    text = wishText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    ),
                    color = Text_Primary,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 하단 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCompleted) {
                    Text(
                        text = "🎉 목표 달성! 🎉",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Purple_Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = "나는 매일 성장하고 있다.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Text_Secondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // QR 코드나 앱 정보 (선택사항)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Purple_Medium,
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "W",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "WISH RING App",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Text_Secondary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareCardComposablePreview() {
    WishRingTheme {
        ShareCardComposable(
            count = 750,
            wishText = "나는 매일 운동하며 건강한 삶을 살아간다",
            targetCount = 1000
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareCardComposableCompletedPreview() {
    WishRingTheme {
        ShareCardComposable(
            count = 1000,
            wishText = "나는 매일 성장하고 발전하는 사람이다",
            targetCount = 1000
        )
    }
}