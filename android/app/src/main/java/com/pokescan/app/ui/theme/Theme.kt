package com.snapdex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SnapDexBlue,
    onPrimary = SnapDexWhite,
    primaryContainer = SnapDexBlueTint,
    onPrimaryContainer = SnapDexBlue,
    error = SnapDexDanger,
    onError = SnapDexWhite,
    surface = SnapDexSurface,
    onSurface = SnapDexBlack,
    background = SnapDexSurface,
    onBackground = SnapDexBlack,
)

@Composable
fun SnapDexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = SnapDexTypography,
        content = content
    )
}
