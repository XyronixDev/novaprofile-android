package com.novaprofiles.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NovaProfile v1.1 — GoLogin-style light look: white background, black text,
// green accent (the green family of the reference screenshots). Own code,
// own green — no copied assets, name or logo.
val GoGreen = Color(0xFF00A67E)
val GoGreenDark = Color(0xFF008F6B)
val GoMintBg = Color(0xFFE6F7F1)
val GoStopRed = Color(0xFFEA4335)
val GoGreyText = Color(0xFF5F6368)
val GoDivider = Color(0xFFE8EAED)
val GoBrowserBar = Color(0xFF211D19)
val GoBrowserPill = Color(0xFF3A332E)
val GoBrowserCard = Color(0xFF33291F)

private val NovaLightColors = lightColorScheme(
    primary = GoGreen,
    onPrimary = Color.White,
    secondary = GoGreyText,
    tertiary = GoGreenDark,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F4),
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
    onSurfaceVariant = GoGreyText,
    error = Color(0xFFD93025)
)

@Composable
fun NovaProfileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NovaLightColors,
        content = content
    )
}
