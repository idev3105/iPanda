package com.ipanda.desktop.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Premium Dark Color Palette ──────────────────────────────────────

val md_primary = Color(0xFFBB86FC)
val md_onPrimary = Color(0xFF1C1B1F)
val md_primaryContainer = Color(0xFF4A148C)
val md_onPrimaryContainer = Color(0xFFEADDFF)

val md_secondary = Color(0xFF03DAC6)
val md_onSecondary = Color(0xFF00332E)
val md_secondaryContainer = Color(0xFF005048)
val md_onSecondaryContainer = Color(0xFF70EFDE)

val md_surface = Color(0xFF121212)
val md_onSurface = Color(0xFFE6E1E5)
val md_surfaceVariant = Color(0xFF1E1E2E)
val md_onSurfaceVariant = Color(0xFFCAC4D0)

val md_background = Color(0xFF0D0D0D)
val md_onBackground = Color(0xFFE6E1E5)

val md_error = Color(0xFFCF6679)
val md_onError = Color(0xFF370B1E)

val md_outline = Color(0xFF938F99)

private val DarkColorScheme = darkColorScheme(
    primary = md_primary,
    onPrimary = md_onPrimary,
    primaryContainer = md_primaryContainer,
    onPrimaryContainer = md_onPrimaryContainer,
    secondary = md_secondary,
    onSecondary = md_onSecondary,
    secondaryContainer = md_secondaryContainer,
    onSecondaryContainer = md_onSecondaryContainer,
    surface = md_surface,
    onSurface = md_onSurface,
    surfaceVariant = md_surfaceVariant,
    onSurfaceVariant = md_onSurfaceVariant,
    background = md_background,
    onBackground = md_onBackground,
    error = md_error,
    onError = md_onError,
    outline = md_outline
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
