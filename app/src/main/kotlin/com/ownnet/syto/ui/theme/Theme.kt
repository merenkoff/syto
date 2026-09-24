package com.ownnet.syto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ink palette from the brand kit (assets/icon/syto-icon.svg).
private val Ink = Color(0xFF0E1A3C)
private val InkLight = Color(0xFF1C3068)
private val Sky = Color(0xFF6FB3FF)
private val Paper = Color(0xFFDCE9FF)

private val SytoColorScheme = darkColorScheme(
    primary = Sky,
    onPrimary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = InkLight,
    onSurfaceVariant = Paper.copy(alpha = 0.72f),
)

@Composable
fun SytoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SytoColorScheme, content = content)
}
