package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MetroDarkColorScheme = darkColorScheme(
    primary = MetroCyan,
    onPrimary = MetroDeepBg,
    primaryContainer = MetroSurfaceBright,
    onPrimaryContainer = MetroCyan,
    secondary = CoinGold,
    onSecondary = MetroDeepBg,
    secondaryContainer = MetroSurface,
    onSecondaryContainer = CoinGold,
    tertiary = MagnetRuby,
    onTertiary = TextPrimary,
    background = MetroDeepBg,
    onBackground = TextPrimary,
    surface = MetroSurface,
    onSurface = TextPrimary,
    surfaceVariant = MetroSurfaceBright,
    onSurfaceVariant = TextSecondary,
    error = ErrorCoral,
    onError = TextPrimary
)

@Composable
fun GatinhoMetroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MetroDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
