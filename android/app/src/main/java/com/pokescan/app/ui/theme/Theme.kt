package com.pokescan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PokeScanYellow,
    onPrimary = PokeScanBlack,
    primaryContainer = PokeScanYellowDark,
    onPrimaryContainer = PokeScanWhite,
    surface = PokeScanSurface,
    onSurface = PokeScanBlack,
    background = PokeScanWhite,
    onBackground = PokeScanBlack,
)

private val DarkColorScheme = darkColorScheme(
    primary = PokeScanYellow,
    onPrimary = PokeScanBlack,
    primaryContainer = PokeScanYellowDark,
    onPrimaryContainer = PokeScanWhite,
    surface = PokeScanSurfaceDark,
    onSurface = PokeScanWhite,
    background = PokeScanBlack,
    onBackground = PokeScanWhite,
)

@Composable
fun PokeScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PokeScanTypography,
        content = content
    )
}
