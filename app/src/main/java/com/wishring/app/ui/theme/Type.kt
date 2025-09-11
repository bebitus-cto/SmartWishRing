package com.wishring.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Pretendard 폰트 패밀리 정의 (실제 폰트 파일이 있다면 사용)
val PretendardFamily = FontFamily.Default // TODO: Add Pretendard font files

val Typography = Typography(
    // Display styles
    displayLarge = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline styles
    headlineLarge = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title styles
    titleLarge = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body styles
    bodyLarge = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Label styles
    labelLarge = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PretendardFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Custom text styles from Figma
val WishCountTextStyle = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 38.sp,
    lineHeight = 45.sp,
    letterSpacing = 0.sp
)

val WishTextStyle = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.1.sp
)

val ReportItemTextStyle = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    letterSpacing = 0.sp
)

val DateTextStyle = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 8.sp,
    lineHeight = 10.sp,
    letterSpacing = 0.sp
)

val CountBadgeTextStyle = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.sp
)