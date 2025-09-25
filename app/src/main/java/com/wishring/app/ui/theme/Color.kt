package com.wishring.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors
val Purple_Primary = Color(0xFFA613FE)  // 메인 보라색 (#A613FE from Figma)
val Purple_Light = Color(0xFFE4B5FF)    // 연한 보라색
val Purple_Dark = Color(0xFF7B00C7)     // 진한 보라색
val Purple_Medium = Color(0xFF6A5ACD)   // 중간 보라색 (로고 색상)

// Background Colors
val Background_Primary = Color(0xFFFFFFFF)  // 흰색 배경
val Background_Secondary = Color(0xFFF6F7FF)  // 연한 보라 배경
val Background_Card = Color(0xFFFFFFFF)     // 카드 배경

// Text Colors
val Text_Primary = Color(0xFF333333)    // 주요 텍스트 (#333333 from Figma)
val Text_Secondary = Color(0xFF666666)  // 보조 텍스트
val Gray_Light = Color(0xFFE0E0E0)      // 연한 회색 (UI 배경, 구분선 등)
val Text_OnPrimary = Color(0xFFFFFFFF)  // 보라색 배경 위 텍스트
val Text_Disabled = Color(0xFFC0C0C0)   // 비활성 텍스트
val Text_Tertiary = Color(0xFF9E9E9E)   // 3차 텍스트

// Surface Colors
val Surface_Card = Color(0xFFFFFFFF)
val Surface_Shadow = Color(0x1A000000)   // 10% 검정 그림자

// Border & Divider
val Divider_Color = Color(0xFFE0E0E0)
val Border_Light = Color(0xFFF0F0F0)
val Border_Default = Color(0xFFDBDBDB)
val Border_Dark = Color(0xFFC0C0C0)

// Progress Colors
val Progress_Background = Color(0xFFC5C5C5)  // 프로그레스 배경
val Progress_Foreground = Purple_Primary      // 프로그레스 전경

// Battery Indicator
val Battery_Full = Color(0xFF4CAF50)
val Battery_Medium = Color(0xFFFF9800)
val Battery_Low = Color(0xFFE91E63)
val Battery_Icon = Color(0xFF424243)

// Status Colors
val Success_Color = Color(0xFF4CAF50)
val Success_Light = Color(0xFF81C784)
val Success_Medium = Color(0xFF66BB6A)
val Success_Dark = Color(0xFF388E3C)
val Error_Color = Color(0xFFE91E63)
val Error_Medium = Color(0xFFEF5350)
val Warning_Color = Color(0xFFFF9800)
val Info_Color = Color(0xFF2196F3)

// Gradient Colors (for loading screen)
val Gradient_Start = Purple_Primary
val Gradient_End = Purple_Light