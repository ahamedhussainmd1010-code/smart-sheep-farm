package com.example.myapplication2sheepfarm.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    onSurface = White
)

@Composable
fun MyApplication2Theme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        var context = view.context
        while (context is android.content.ContextWrapper) {
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
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