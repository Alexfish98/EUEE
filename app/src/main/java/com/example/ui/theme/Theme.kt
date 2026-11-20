package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.model.AccentPalette
import com.example.core.model.ThemeMode

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EueeMasteryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPalette: AccentPalette = AccentPalette.ABYSSINIAN_COBALT,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val primary = Color(accentPalette.primaryHex)
    val secondary = Color(accentPalette.secondaryHex)
    val tertiary = Color(accentPalette.tertiaryHex)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary.lighten(0.32f),
            onPrimary = Color(0xFF0A1424),
            primaryContainer = primary.copy(alpha = 0.28f),
            onPrimaryContainer = Color(0xFFE0ECFF),
            secondary = secondary.lighten(0.25f),
            onSecondary = Color(0xFF05201D),
            secondaryContainer = secondary.copy(alpha = 0.24f),
            onSecondaryContainer = Color(0xFFCCFBF1),
            tertiary = tertiary.lighten(0.22f),
            onTertiary = Color(0xFF281600),
            background = SlateDarkBg,
            onBackground = Color(0xFFF1F5F9),
            surface = SlateDarkSurface,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = SlateDarkSurfaceVariant,
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = Color(0xFF334155),
            error = Color(0xFFF87171)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.12f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.13f),
            onSecondaryContainer = secondary,
            tertiary = tertiary,
            onTertiary = Color.White,
            background = AlabasterBg,
            onBackground = Color(0xFF0F172A),
            surface = CrispSurface,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = SoftSurfaceVariant,
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            error = CoralError
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildEueeTypography(fontScale),
        shapes = AppShapes,
        content = content
    )
}

private fun Color.lighten(factor: Float): Color {
    val r = red + (1f - red) * factor
    val g = green + (1f - green) * factor
    val b = blue + (1f - blue) * factor
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), alpha)
}
