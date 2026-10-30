package com.closetstudio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioColors = lightColorScheme(
    primary = Color(0xFF6B5334),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0E2CC),
    onPrimaryContainer = Color(0xFF241A08),
    secondary = Color(0xFF4F6354),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E8D6),
    onSecondaryContainer = Color(0xFF0C1F13),
    background = Color(0xFFFBF9F4),
    onBackground = Color(0xFF1D1B17),
    surface = Color(0xFFFBF9F4),
    onSurface = Color(0xFF1D1B17),
    surfaceVariant = Color(0xFFEDE6DA),
    onSurfaceVariant = Color(0xFF4C463C),
    outline = Color(0xFF7E7769),
    error = Color(0xFFBA1A1A),
)

@Composable
fun ClosetStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StudioColors, content = content)
}
