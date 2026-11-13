package com.kuzmix.ai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand colors — identical in both modes (identity, not surface).
val KuzmixOrange = Color(0xFFFF8C00)
val KuzmixOrangeDeep = Color(0xFFFF6D00)
val KuzmixYellow = Color(0xFFFFC107)
val KuzmixYellowSoft = Color(0xFFFFD54F)
val KuzmixOrangeContainer = Color(0x33FF8C00)
val KuzmixUserBubble = Color(0xFFFF8C00)

/** Semantic palette for the whole app — dark and light variants. */
data class KuzmixColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnAccent: Color,
    val error: Color,
    val success: Color,
    val assistantBubble: Color
)

internal val KuzmixDarkColors = KuzmixColors(
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF14141C),
    surfaceElevated = Color(0xFF1C1C28),
    surfaceVariant = Color(0xFF222230),
    border = Color(0xFF2D2D3E),
    borderSubtle = Color(0xFF1D1D2A),
    textPrimary = Color(0xFFF9FAFB),
    textSecondary = Color(0xFF9CA3AF),
    textTertiary = Color(0xFF6B7280),
    textOnAccent = Color(0xFF0A0A0F),
    error = Color(0xFFF87171),
    success = Color(0xFF34D399),
    assistantBubble = Color(0xFF161622)
)

internal val KuzmixLightColors = KuzmixColors(
    background = Color(0xFFF6F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEFF5),
    border = Color(0xFFDCE0E8),
    borderSubtle = Color(0xFFE8EBF2),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textTertiary = Color(0xFF9CA3AF),
    textOnAccent = Color(0xFFFFFFFF),
    error = Color(0xFFDC2626),
    success = Color(0xFF059669),
    assistantBubble = Color(0xFFEDEFF5)
)

// Chat code blocks — Monospace surface used by the chat renderer.
internal val KuzmixCodeBlockDark = Color(0xFF101018)
internal val KuzmixCodeBlockLight = Color(0xFFE5E7EB)

val KuzmixCodeBlock: Color
    @Composable @ReadOnlyComposable
    get() =
        if (LocalKuzmixColors.current.background.luminance() < 0.5f) KuzmixCodeBlockDark
        else KuzmixCodeBlockLight

private fun Color.luminance(): Float =
    (0.2126f * red + 0.7152f * green + 0.0722f * blue)

val LocalKuzmixColors = staticCompositionLocalOf { KuzmixDarkColors }

// Semantic accessors. Composable getters: screens keep reading the SAME
// names they always did (KuzmixBackground, KuzmixTextPrimary, ...) and
// now recompose onto the active palette when the user switches theme.
val KuzmixBackground: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.background
val KuzmixSurface: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.surface
val KuzmixSurfaceElevated: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.surfaceElevated
val KuzmixSurfaceVariant: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.surfaceVariant
val KuzmixBorder: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.border
val KuzmixBorderSubtle: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.borderSubtle
val KuzmixTextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.textPrimary
val KuzmixTextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.textSecondary
val KuzmixTextTertiary: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.textTertiary
val KuzmixTextOnAccent: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.textOnAccent
val KuzmixError: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.error
val KuzmixSuccess: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.success
val KuzmixAssistantBubble: Color
    @Composable @ReadOnlyComposable get() = LocalKuzmixColors.current.assistantBubble
