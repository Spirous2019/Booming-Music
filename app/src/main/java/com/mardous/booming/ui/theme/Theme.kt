package com.mardous.booming.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.mardous.booming.R
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.util.Preferences

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@Composable
fun BoomingMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    blackTheme: Boolean = Preferences.blackTheme,
    content: @Composable () -> Unit
) {
    var colorScheme = when {
        darkTheme -> darkScheme
        else -> lightScheme
    }

    if (darkTheme && blackTheme) {
        colorScheme = colorScheme.copy(
            primary = Color(0xFF3A86F5),
            onPrimary = Color.White,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF0E0E10),
            onSurfaceVariant = Color(0xFFA2A2A8),
            surfaceContainer = Color(0xFF0E0E10),
            surfaceContainerLow = Color(0xFF08080A),
            surfaceContainerLowest = Color.Black,
            surfaceContainerHigh = Color(0xFF141416),
            surfaceContainerHighest = Color(0xFF1C1C1F),
            secondaryContainer = Color(0xFF0F253C),
            onSecondaryContainer = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}

@Composable
fun PlayerTheme(
    playerColorScheme: PlayerColorScheme,
    content: @Composable () -> Unit
) {
    val base = MaterialTheme.colorScheme

    val scheme = remember(playerColorScheme) {
        if (playerColorScheme.mode == PlayerColorScheme.Mode.AppTheme || playerColorScheme == PlayerColorScheme.Unspecified) {
            base
        } else {
            base.copy(
                surface = playerColorScheme.surface,
                primary = playerColorScheme.primary,
                onPrimary = playerColorScheme.onPrimary,
                onSurface = playerColorScheme.onSurface,
                onSurfaceVariant = playerColorScheme.onSurfaceVariant
            )
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = customTypography,
        content = content
    )
}