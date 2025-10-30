package com.voicenotesai.presentation.theme

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for managing theme engine and theme-related state.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    val themeEngine: ThemeEngine
) : ViewModel()