package com.kuzmix.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private fun buildDarkScheme() = darkColorScheme(
    primary = KuzmixOrange,
    onPrimary = KuzmixDarkColors.textOnAccent,
    primaryContainer = KuzmixOrangeContainer,
    onPrimaryContainer = KuzmixOrange,
    secondary = KuzmixYellow,
    onSecondary = KuzmixDarkColors.textOnAccent,
    secondaryContainer = Color(0x2EFFC107),
    onSecondaryContainer = KuzmixYellow,
    tertiary = KuzmixYellowSoft,
    onTertiary = KuzmixDarkColors.textOnAccent,
    background = KuzmixDarkColors.background,
    onBackground = KuzmixDarkColors.textPrimary,
    surface = KuzmixDarkColors.surface,
    onSurface = KuzmixDarkColors.textPrimary,
    surfaceVariant = KuzmixDarkColors.surfaceVariant,
    onSurfaceVariant = KuzmixDarkColors.textSecondary,
    surfaceContainer = KuzmixDarkColors.surface,
    surfaceContainerLow = KuzmixDarkColors.background,
    surfaceContainerHigh = KuzmixDarkColors.surfaceElevated,
    surfaceContainerHighest = KuzmixDarkColors.surfaceVariant,
    outline = KuzmixDarkColors.border,
    outlineVariant = KuzmixDarkColors.borderSubtle,
    error = KuzmixDarkColors.error,
    onError = Color.Black
)

private fun buildLightScheme() = lightColorScheme(
    primary = KuzmixOrange,
    onPrimary = KuzmixLightColors.textOnAccent,
    primaryContainer = KuzmixOrangeContainer,
    onPrimaryContainer = KuzmixOrangeDeep,
    secondary = KuzmixYellow,
    onSecondary = KuzmixLightColors.textOnAccent,
    secondaryContainer = Color(0x33FFC107),
    onSecondaryContainer = KuzmixOrangeDeep,
    tertiary = KuzmixYellowSoft,
    onTertiary = KuzmixLightColors.textOnAccent,
    background = KuzmixLightColors.background,
    onBackground = KuzmixLightColors.textPrimary,
    surface = KuzmixLightColors.surface,
    onSurface = KuzmixLightColors.textPrimary,
    surfaceVariant = KuzmixLightColors.surfaceVariant,
    onSurfaceVariant = KuzmixLightColors.textSecondary,
    surfaceContainer = KuzmixLightColors.surface,
    surfaceContainerLow = KuzmixLightColors.background,
    surfaceContainerHigh = KuzmixLightColors.surfaceElevated,
    surfaceContainerHighest = KuzmixLightColors.surfaceVariant,
    outline = KuzmixLightColors.border,
    outlineVariant = KuzmixLightColors.borderSubtle,
    error = KuzmixLightColors.error,
    onError = Color.White
)

@Composable
fun KuzmixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) KuzmixDarkColors else KuzmixLightColors
    val scheme = remember(darkTheme) {
        if (darkTheme) buildDarkScheme() else buildLightScheme()
    }
    CompositionLocalProvider(LocalKuzmixColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = KuzmixTypography,
            shapes = KuzmixShapes,
            content = content
        )
    }
}
