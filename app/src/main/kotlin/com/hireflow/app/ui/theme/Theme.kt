package com.hireflow.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Navy = Color(0xFF0D2B57)
val Azure = Color(0xFF1769E8)
val Teal = Color(0xFF1C8494)
val IceBlue = Color(0xFFEAF3FF)
val AppBackground = Color(0xFFF7F9FC)
val Ink = Color(0xFF172033)
val Muted = Color(0xFF667085)
val Border = Color(0xFFE5EAF1)
val Success = Color(0xFF189568)
val Warning = Color(0xFFE18A18)
val Danger = Color(0xFFD9485F)
val Purple = Color(0xFF7155C6)

private val LightColors = lightColorScheme(
    primary = Azure,
    onPrimary = Color.White,
    primaryContainer = IceBlue,
    onPrimaryContainer = Navy,
    secondary = Teal,
    tertiary = Purple,
    background = AppBackground,
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F3F8),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    outline = Border,
    error = Danger
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BB8FF),
    onPrimary = Color(0xFF00316D),
    primaryContainer = Color(0xFF173D72),
    secondary = Color(0xFF69CBD7),
    tertiary = Color(0xFFC5B5FF),
    background = Color(0xFF0D1118),
    surface = Color(0xFF151B24),
    surfaceVariant = Color(0xFF202835),
    onBackground = Color(0xFFF2F4F7),
    onSurface = Color(0xFFF2F4F7),
    onSurfaceVariant = Color(0xFFAAB4C3),
    outline = Color(0xFF344054),
    error = Color(0xFFFF8B9D)
)

private val HireFlowTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

@Composable
fun HireFlowTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = HireFlowTypography, content = content)
}
