package com.novaprofiles.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NovaProfile — own premium dark look (deep indigo + violet accent).
// Deliberately distinct from GoLogin's green and CloneX's blue.
private val NovaDarkColors = darkColorScheme(
    primary = Color(0xFF7C6CFF),
    secondary = Color(0xFF9D94FF),
    tertiary = Color(0xFF4DD0C4),
    background = Color(0xFF0F1226),
    surface = Color(0xFF1A1E3A),
    surfaceVariant = Color(0xFF232847),
    onBackground = Color(0xFFF2F3FF),
    onSurface = Color(0xFFF2F3FF),
    onPrimary = Color.White,
    error = Color(0xFFFF6B6B)
)

@Composable
fun NovaProfileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NovaDarkColors,
        content = content
    )
}
