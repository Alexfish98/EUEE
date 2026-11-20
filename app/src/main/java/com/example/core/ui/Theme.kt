package com.example.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.core.model.AccentPalette
import com.example.core.model.ThemeMode

val EueeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EueeMasteryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPalette: AccentPalette = AccentPalette.ABYSSINIAN_COBALT,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val primarySeed = Color(accentPalette.primaryHex)
    val secondarySeed = Color(accentPalette.secondaryHex)
    val tertiarySeed = Color(accentPalette.tertiaryHex)

    val colorScheme = if (useDark) {
        val lightPrimary = lerp(primarySeed, Color.White, 0.42f)
        val lightSecondary = lerp(secondarySeed, Color.White, 0.38f)
        val lightTertiary = lerp(tertiarySeed, Color.White, 0.35f)
        darkColorScheme(
            primary = lightPrimary,
            onPrimary = Color(0xFF0A1628),
            primaryContainer = lerp(primarySeed, Color.Black, 0.35f),
            onPrimaryContainer = Color(0xFFE2E8F0),
            secondary = lightSecondary,
            onSecondary = Color(0xFF052E2B),
            secondaryContainer = lerp(secondarySeed, Color.Black, 0.42f),
            onSecondaryContainer = Color(0xFFCCFBF1),
            tertiary = lightTertiary,
            onTertiary = Color(0xFF2B1800),
            tertiaryContainer = lerp(tertiarySeed, Color.Black, 0.40f),
            onTertiaryContainer = Color(0xFFFEF3C7),
            background = SlateNightBg,
            onBackground = ChalkPrimary,
            surface = SlateNightSurface,
            onSurface = ChalkPrimary,
            surfaceVariant = SlateNightSurfaceVariant,
            onSurfaceVariant = ChalkSecondary,
            surfaceContainerLowest = Color(0xFF080D14),
            surfaceContainerLow = Color(0xFF101824),
            surfaceContainer = SlateNightSurface,
            surfaceContainerHigh = Color(0xFF192436),
            surfaceContainerHighest = Color(0xFF223047),
            outline = Color(0xFF334155),
            outlineVariant = Color(0xFF1E293B),
            error = Color(0xFFF87171),
            onError = Color(0xFF450A0A),
            errorContainer = Color(0xFF7F1D1D),
            onErrorContainer = Color(0xFFFEE2E2)
        )
    } else {
        lightColorScheme(
            primary = primarySeed,
            onPrimary = Color.White,
            primaryContainer = lerp(primarySeed, Color.White, 0.86f),
            onPrimaryContainer = lerp(primarySeed, Color.Black, 0.45f),
            secondary = secondarySeed,
            onSecondary = Color.White,
            secondaryContainer = lerp(secondarySeed, Color.White, 0.85f),
            onSecondaryContainer = lerp(secondarySeed, Color.Black, 0.45f),
            tertiary = tertiarySeed,
            onTertiary = Color.White,
            tertiaryContainer = lerp(tertiarySeed, Color.White, 0.85f),
            onTertiaryContainer = lerp(tertiarySeed, Color.Black, 0.45f),
            background = AlabasterBg,
            onBackground = InkPrimary,
            surface = AlabasterSurface,
            onSurface = InkPrimary,
            surfaceVariant = AlabasterSurfaceVariant,
            onSurfaceVariant = InkSecondary,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF8FAFC),
            surfaceContainer = Color(0xFFF1F5F9),
            surfaceContainerHigh = Color(0xFFE8EEF7),
            surfaceContainerHighest = Color(0xFFDCE5F2),
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0),
            error = AttentionCoral,
            onError = Color.White,
            errorContainer = AttentionCoralContainer,
            onErrorContainer = Color(0xFF7F1D1D)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildEueeTypography(fontScale),
        shapes = EueeShapes,
        content = content
    )
}
