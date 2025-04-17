package com.example.reflectai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Screen width breakpoints
sealed class WindowSize(val size: String) {
    object Compact : WindowSize("Compact")  // Phone portrait
    object Medium : WindowSize("Medium")    // Phone landscape or tablet portrait
    object Expanded : WindowSize("Expanded") // Tablet landscape
}

// Get current window size class based on width
@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    return when {
        screenWidth < 600.dp -> WindowSize.Compact
        screenWidth < 840.dp -> WindowSize.Medium
        else -> WindowSize.Expanded
    }
}

// Determine if we're in landscape mode
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

// Combines window size and orientation
@Composable
fun rememberWindowInfo(): WindowInfo {
    val windowSize = rememberWindowSize()
    val isLandscape = isLandscape()
    
    return remember(windowSize, isLandscape) {
        WindowInfo(windowSize, isLandscape)
    }
}

// Window information for responsive design
data class WindowInfo(
    val windowSize: WindowSize,
    val isLandscape: Boolean
) {
    val shouldShowSidebar: Boolean
        get() = isLandscape && windowSize != WindowSize.Compact
}