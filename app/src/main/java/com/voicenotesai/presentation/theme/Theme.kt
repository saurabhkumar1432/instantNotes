package com.voicenotesai.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme palette inspired by neon gradients and soft glass surfaces
private val lightPrimary = Color(0xFF6C63FF)
private val lightOnPrimary = Color(0xFFFFFFFF)
private val lightPrimaryContainer = Color(0xFFE6E0FF)
private val lightOnPrimaryContainer = Color(0xFF251A65)
private val lightSecondary = Color(0xFF2DD4BF)
private val lightOnSecondary = Color(0xFF003730)
private val lightSecondaryContainer = Color(0xFFA9FFF3)
private val lightOnSecondaryContainer = Color(0xFF00201A)
private val lightTertiary = Color(0xFFFF6FB5)
private val lightOnTertiary = Color(0xFF4A082A)
private val lightTertiaryContainer = Color(0xFFFFD7EA)
private val lightOnTertiaryContainer = Color(0xFF2F001B)
private val lightError = Color(0xFFFF4D6D)
private val lightErrorContainer = Color(0xFFFFD3DC)
private val lightOnError = Color(0xFFFFFFFF)
private val lightOnErrorContainer = Color(0xFF41020E)
private val lightBackground = Color(0xFFF7F7FF)
private val lightOnBackground = Color(0xFF0D1220)
private val lightSurface = Color(0xFFFFFFFF)
private val lightOnSurface = Color(0xFF0D1220)
private val lightSurfaceVariant = Color(0xFFE6E8F5)
private val lightOnSurfaceVariant = Color(0xFF3E4A63)
private val lightOutline = Color(0xFF74809A)
private val lightInverseOnSurface = Color(0xFFF0F3FF)
private val lightInverseSurface = Color(0xFF1C2337)
private val lightInversePrimary = Color(0xFFCBC0FF)

// Dark theme palette keeping the neon glow while embracing deep midnight tones
private val darkPrimary = Color(0xFFB8A1FF)
private val darkOnPrimary = Color(0xFF1F0E5C)
private val darkPrimaryContainer = Color(0xFF3C2A82)
private val darkOnPrimaryContainer = Color(0xFFE8DEFF)
private val darkSecondary = Color(0xFF5EEAD4)
private val darkOnSecondary = Color(0xFF053F34)
private val darkSecondaryContainer = Color(0xFF0F5B4D)
private val darkOnSecondaryContainer = Color(0xFFBBFBE8)
private val darkTertiary = Color(0xFFFF8CCF)
private val darkOnTertiary = Color(0xFF4B0033)
private val darkTertiaryContainer = Color(0xFF6E1B55)
private val darkOnTertiaryContainer = Color(0xFFFFD7EC)
private val darkError = Color(0xFFFF748C)
private val darkErrorContainer = Color(0xFF81102C)
private val darkOnError = Color(0xFF2C000A)
private val darkOnErrorContainer = Color(0xFFFFD8E4)
private val darkBackground = Color(0xFF040812)
private val darkOnBackground = Color(0xFFE4E8FF)
private val darkSurface = Color(0xFF0B1224)
private val darkOnSurface = Color(0xFFE4E8FF)
private val darkSurfaceVariant = Color(0xFF1D2439)
private val darkOnSurfaceVariant = Color(0xFFC3C8E5)
private val darkOutline = Color(0xFF545C75)
private val darkInverseOnSurface = Color(0xFF0B1224)
private val darkInverseSurface = Color(0xFFE4E8FF)
private val darkInversePrimary = Color(0xFF6F5FFF)

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
