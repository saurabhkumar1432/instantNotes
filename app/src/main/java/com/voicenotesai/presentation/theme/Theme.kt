package com.voicenotesai.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.voicenotesai.presentation.theme.color.EnhancedColorTheme
import com.voicenotesai.presentation.theme.color.EnhancedDarkColorScheme
import com.voicenotesai.presentation.theme.color.EnhancedLightColorScheme

@Composable
fun VoiceNotesAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastMode: Boolean = false,
    colorBlindnessSupport: Boolean = false,
    reducedTransparency: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        EnhancedDarkColorScheme
    } else {
        EnhancedLightColorScheme
    }

    EnhancedColorTheme(
        darkTheme = darkTheme,
        highContrastMode = highContrastMode,
        colorBlindnessSupport = colorBlindnessSupport,
        reducedTransparency = reducedTransparency
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
