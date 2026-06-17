package com.example.myapplication2sheepfarm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.myapplication2sheepfarm.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = FarmGreen,
    secondary = FarmGreenLight,
    tertiary = FarmAccent,
    background = SlateDark,
    surface = SlateCardDark,
    onPrimary = White,
    onSecondary = White,
    onTertiary = SlateDark,
    onBackground = White,
    onSurface = White,
    outline = BorderGray
)

private val LightColorScheme = lightColorScheme(
    primary = FarmGreen,
    secondary = FarmGreenLight,
    tertiary = FarmAccent,
    background = Color(0xFFF1F5F9), // Light Slate background
    surface = Color(0xFFFFFFFF),    // White card surfaces
    onPrimary = White,
    onSecondary = White,
    onTertiary = SlateDark,
    onBackground = SlateDark,        // Slate Dark text
    onSurface = SlateDark,
    outline = Color(0xFFCBD5E1)     // Soft light border
)

@Composable
fun MyApplication2Theme(
    theme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        var context = view.context
        while (context is android.content.ContextWrapper) {
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                break
            }
            context = context.baseContext
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}