package com.voicenotesai.presentation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voicenotesai.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun recordButton_hasContentDescription() {
        // The idle state should expose a semantic description for the record button
        val expected = composeTestRule.activity.getString(R.string.start_voice_recording_description)
        composeTestRule.onNodeWithContentDescription(expected).assertExists()
    }
}
