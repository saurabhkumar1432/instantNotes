package com.voicenotesai.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.voicenotesai.presentation.theme.color.EnhancedColorTheme
import com.voicenotesai.presentation.theme.color.EnhancedDarkColorScheme
import com.voicenotesai.presentation.theme.color.EnhancedLightColorScheme

@Composable
fun VoiceNotesAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    highContrastMode: Boolean = false,
    colorBlindnessSupport: Boolean = false,
    reducedTransparency: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> EnhancedDarkColorScheme
        else -> EnhancedLightColorScheme
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
            shapes = AppShapes,
            content = content
        )
    }
}
