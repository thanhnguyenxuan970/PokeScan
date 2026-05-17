package com.snapdex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SnapDexBlue,
    onPrimary = SnapDexWhite,
    primaryContainer = SnapDexBlueTint,
    onPrimaryContainer = SnapDexBlue,
    surface = SnapDexSurface,
    onSurface = SnapDexBlack,
    background = SnapDexWhite,
    onBackground = SnapDexBlack,
)

private val DarkColorScheme = darkColorScheme(
    primary = SnapDexBlue,
    onPrimary = SnapDexWhite,
    primaryContainer = SnapDexBlueDark,
    onPrimaryContainer = SnapDexWhite,
    surface = SnapDexSurfaceDark,
    onSurface = SnapDexWhite,
    background = SnapDexBlack,
    onBackground = SnapDexWhite,
)

@Composable
fun SnapDexTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SnapDexTypography,
        content = content
    )
}
