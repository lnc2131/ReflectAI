package com.example.reflectai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Minimalistic black and white theme with thin lines
private val DarkColorScheme = darkColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = DarkGray,
    onSecondary = White,
    tertiary = AccentGray,
    background = Black,
    onBackground = White,
    surface = AccentDark,
    onSurface = White,
    surfaceVariant = Black,
    outline = DarkGray
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = DarkGray,
    onSecondary = White,
    tertiary = AccentGray,
    background = White,
    onBackground = Black,
    surface = LightGray,
    onSurface = Black,
    surfaceVariant = AccentLight,
    outline = MediumGray
)

@Composable
fun ReflectAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to maintain consistent minimal black/white design
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // Update status bar color to match theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}