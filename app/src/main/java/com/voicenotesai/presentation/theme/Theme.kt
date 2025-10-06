package com.voicenotesai.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Gen-Z Y2K inspired light theme - bold, vibrant, unapologetic
private val lightPrimary = Color(0xFF7C3AED) // Electric purple
private val lightOnPrimary = Color(0xFFFFFFFF)
private val lightPrimaryContainer = Color(0xFFEDE9FE)
private val lightOnPrimaryContainer = Color(0xFF3B0764)
private val lightSecondary = Color(0xFF06B6D4) // Cyber cyan
private val lightOnSecondary = Color(0xFF00171F)
private val lightSecondaryContainer = Color(0xFFCFFAFE)
private val lightOnSecondaryContainer = Color(0xFF001519)
private val lightTertiary = Color(0xFFEC4899) // Hot pink
private val lightOnTertiary = Color(0xFFFFFFFF)
private val lightTertiaryContainer = Color(0xFFFFD6EC)
private val lightOnTertiaryContainer = Color(0xFF50002B)
private val lightError = Color(0xFFEF4444)
private val lightErrorContainer = Color(0xFFFEE2E2)
private val lightOnError = Color(0xFFFFFFFF)
private val lightOnErrorContainer = Color(0xFF7F1D1D)
private val lightBackground = Color(0xFFFAFAFA) // Crisp white-ish
private val lightOnBackground = Color(0xFF0A0A0A)
private val lightSurface = Color(0xFFFFFFFF)
private val lightOnSurface = Color(0xFF0A0A0A)
private val lightSurfaceVariant = Color(0xFFF5F3FF)
private val lightOnSurfaceVariant = Color(0xFF475569)
private val lightOutline = Color(0xFF94A3B8)
private val lightInverseOnSurface = Color(0xFFFAFAFA)
private val lightInverseSurface = Color(0xFF18181B)
private val lightInversePrimary = Color(0xFFA78BFA)

// Gen-Z dark mode - deep, moody, with neon pops
private val darkPrimary = Color(0xFFA78BFA) // Bright lavender
private val darkOnPrimary = Color(0xFF270050)
private val darkPrimaryContainer = Color(0xFF5B21B6)
private val darkOnPrimaryContainer = Color(0xFFF3E8FF)
private val darkSecondary = Color(0xFF22D3EE) // Neon cyan
private val darkOnSecondary = Color(0xFF003544)
private val darkSecondaryContainer = Color(0xFF164E63)
private val darkOnSecondaryContainer = Color(0xFFCFFAFE)
private val darkTertiary = Color(0xFFF472B6) // Bubblegum pink
private val darkOnTertiary = Color(0xFF5A0031)
private val darkTertiaryContainer = Color(0xFF831843)
private val darkOnTertiaryContainer = Color(0xFFFFD6EC)
private val darkError = Color(0xFFF87171)
private val darkErrorContainer = Color(0xFF991B1B)
private val darkOnError = Color(0xFF3F0000)
private val darkOnErrorContainer = Color(0xFFFECDD3)
private val darkBackground = Color(0xFF0A0A0A) // Pure black base
private val darkOnBackground = Color(0xFFFAFAFA)
private val darkSurface = Color(0xFF18181B) // Charcoal surface
private val darkOnSurface = Color(0xFFFAFAFA)
private val darkSurfaceVariant = Color(0xFF27272A)
private val darkOnSurfaceVariant = Color(0xFFD1D5DB)
private val darkOutline = Color(0xFF6B7280)
private val darkInverseOnSurface = Color(0xFF18181B)
private val darkInverseSurface = Color(0xFFFAFAFA)
private val darkInversePrimary = Color(0xFF7C3AED)

private val LightColorScheme = lightColorScheme(
    primary = lightPrimary,
    onPrimary = lightOnPrimary,
    primaryContainer = lightPrimaryContainer,
    onPrimaryContainer = lightOnPrimaryContainer,
    secondary = lightSecondary,
    onSecondary = lightOnSecondary,
    secondaryContainer = lightSecondaryContainer,
    onSecondaryContainer = lightOnSecondaryContainer,
    tertiary = lightTertiary,
    onTertiary = lightOnTertiary,
    tertiaryContainer = lightTertiaryContainer,
    onTertiaryContainer = lightOnTertiaryContainer,
    error = lightError,
    errorContainer = lightErrorContainer,
    onError = lightOnError,
    onErrorContainer = lightOnErrorContainer,
    background = lightBackground,
    onBackground = lightOnBackground,
    surface = lightSurface,
    onSurface = lightOnSurface,
    surfaceVariant = lightSurfaceVariant,
    onSurfaceVariant = lightOnSurfaceVariant,
    outline = lightOutline,
    inverseOnSurface = lightInverseOnSurface,
    inverseSurface = lightInverseSurface,
    inversePrimary = lightInversePrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = darkPrimary,
    onPrimary = darkOnPrimary,
    primaryContainer = darkPrimaryContainer,
    onPrimaryContainer = darkOnPrimaryContainer,
    secondary = darkSecondary,
    onSecondary = darkOnSecondary,
    secondaryContainer = darkSecondaryContainer,
    onSecondaryContainer = darkOnSecondaryContainer,
    tertiary = darkTertiary,
    onTertiary = darkOnTertiary,
    tertiaryContainer = darkTertiaryContainer,
    onTertiaryContainer = darkOnTertiaryContainer,
    error = darkError,
    errorContainer = darkErrorContainer,
    onError = darkOnError,
    onErrorContainer = darkOnErrorContainer,
    background = darkBackground,
    onBackground = darkOnBackground,
    surface = darkSurface,
    onSurface = darkOnSurface,
    surfaceVariant = darkSurfaceVariant,
    onSurfaceVariant = darkOnSurfaceVariant,
    outline = darkOutline,
    inverseOnSurface = darkInverseOnSurface,
    inverseSurface = darkInverseSurface,
    inversePrimary = darkInversePrimary,
)

@Composable
fun VoiceNotesAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
