package com.wishring.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = Purple_Dark,
    onPrimaryContainer = Text_OnPrimary,
    secondary = Purple_Light,
    onSecondary = Text_Primary,
    secondaryContainer = Purple_Dark,
    onSecondaryContainer = Text_OnPrimary,
    tertiary = Purple_Medium,
    onTertiary = Text_OnPrimary,
    background = Background_Primary,
    onBackground = Text_Primary,
    surface = Surface_Card,
    onSurface = Text_Primary,
    surfaceVariant = Background_Secondary,
    onSurfaceVariant = Text_Secondary,
    error = Error_Color,
    onError = Text_OnPrimary,
    errorContainer = Error_Color,
    onErrorContainer = Text_OnPrimary,
    outline = Divider_Color,
    outlineVariant = Border_Light
)

private val LightColorScheme = lightColorScheme(
    primary = Purple_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = Purple_Light,
    onPrimaryContainer = Purple_Dark,
    secondary = Purple_Light,
    onSecondary = Text_Primary,
    secondaryContainer = Background_Secondary,
    onSecondaryContainer = Text_Primary,
    tertiary = Purple_Medium,
    onTertiary = Text_OnPrimary,
    background = Background_Primary,
    onBackground = Text_Primary,
    surface = Surface_Card,
    onSurface = Text_Primary,
    surfaceVariant = Background_Secondary,
    onSurfaceVariant = Text_Secondary,
    error = Error_Color,
    onError = Text_OnPrimary,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Error_Color,
    outline = Divider_Color,
    outlineVariant = Border_Light
)

// Custom color scheme for additional colors
data class WishRingColors(
    val gradientStart: Color = Gradient_Start,
    val gradientEnd: Color = Gradient_End,
    val progressBackground: Color = Progress_Background,
    val progressForeground: Color = Progress_Foreground,
    val batteryFull: Color = Battery_Full,
    val batteryMedium: Color = Battery_Medium,
    val batteryLow: Color = Battery_Low,
    val batteryIcon: Color = Battery_Icon,
    val borderDefault: Color = Border_Default,
    val borderDark: Color = Border_Dark,
    val textDisabled: Color = Text_Disabled,
    val cardBackground: Color = Background_Card,
    val shadowColor: Color = Surface_Shadow,
    val success: Color = Success_Color,
    val warning: Color = Warning_Color,
    val info: Color = Info_Color
)

val LocalWishRingColors = staticCompositionLocalOf { WishRingColors() }

@Composable
fun WishRingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color to white always
            window.statusBarColor = Color.White.toArgb()
            // Set status bar icons color to dark (since background is white)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    CompositionLocalProvider(
        LocalWishRingColors provides WishRingColors()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}