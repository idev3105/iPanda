package com.ipanda.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Netflix-inspired dark palette
val NetflixRed = Color(0xFFE50914)
val NetflixRedDark = Color(0xFFB20710)
val BackgroundDark = Color(0xFF0A0A0A)
val SurfaceDark = Color(0xFF141414)
val SurfaceVariant = Color(0xFF1F1F1F)
val CardBackground = Color(0xFF1A1A1A)
val OnSurfaceLight = Color(0xFFE5E5E5)
val OnSurfaceMuted = Color(0xFF808080)
val AccentGold = Color(0xFFF5C518)

private val DarkColorPalette = darkColors(
    primary = NetflixRed,
    primaryVariant = NetflixRedDark,
    secondary = AccentGold,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    error = Color(0xFFCF6679)
)

@Composable
fun iPandaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarkColorPalette,
        content = content
    )
}
