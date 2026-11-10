package com.termtune.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colour roles used throughout the terminal UI. */
data class TerminalPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val accent: Color,
    val accentDim: Color,
    val foreground: Color,
    val muted: Color,
    val error: Color,
)

private val GreenPalette = TerminalPalette(
    background = Color(0xFF060A07),
    surface = Color(0xFF0D140F),
    border = Color(0xFF1E3A26),
    accent = Color(0xFF39FF6A),
    accentDim = Color(0xFF1F7A3A),
    foreground = Color(0xFFC8F7D4),
    muted = Color(0xFF5F8A6D),
    error = Color(0xFFFF6B5B),
)

private val AmberPalette = TerminalPalette(
    background = Color(0xFF0A0804),
    surface = Color(0xFF16100A),
    border = Color(0xFF3D2D12),
    accent = Color(0xFFFFB000),
    accentDim = Color(0xFF8A5E00),
    foreground = Color(0xFFFFE3B3),
    muted = Color(0xFF8A7148),
    error = Color(0xFFFF6B5B),
)

private val BluePalette = TerminalPalette(
    background = Color(0xFF04070D),
    surface = Color(0xFF0A111E),
    border = Color(0xFF1B2E4A),
    accent = Color(0xFF4DA6FF),
    accentDim = Color(0xFF1E5C99),
    foreground = Color(0xFFCFE4FF),
    muted = Color(0xFF61809E),
    error = Color(0xFFFF6B81),
)

val TerminalTheme.palette: TerminalPalette
    get() = when (this) {
        TerminalTheme.GREEN -> GreenPalette
        TerminalTheme.AMBER -> AmberPalette
        TerminalTheme.BLUE -> BluePalette
    }

val LocalTerminalPalette = staticCompositionLocalOf { GreenPalette }

/** Every text style is monospace: the whole app is meant to read as a console. */
private fun monospaceTypography(): Typography {
    val base = TextStyle(fontFamily = FontFamily.Monospace)
    return Typography(
        displayLarge = base.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        titleMedium = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
        bodyLarge = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = base.copy(fontSize = 13.sp, lineHeight = 19.sp),
        bodySmall = base.copy(fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
        labelMedium = base.copy(fontSize = 12.sp),
        labelSmall = base.copy(fontSize = 11.sp),
    )
}

@Composable
fun TermTuneTheme(
    theme: TerminalTheme,
    content: @Composable () -> Unit,
) {
    val palette = theme.palette
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.background,
        secondary = palette.accentDim,
        onSecondary = palette.foreground,
        background = palette.background,
        onBackground = palette.foreground,
        surface = palette.surface,
        onSurface = palette.foreground,
        surfaceVariant = palette.surface,
        onSurfaceVariant = palette.muted,
        outline = palette.border,
        error = palette.error,
        onError = palette.background,
    )

    CompositionLocalProvider(LocalTerminalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = monospaceTypography(),
            content = content,
        )
    }
}
