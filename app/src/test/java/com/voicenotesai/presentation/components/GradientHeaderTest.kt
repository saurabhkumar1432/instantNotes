package com.voicenotesai.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for GradientHeader component.
 * 
 * Tests the gradient header functionality including:
 * - Title display
 * - User avatar display
 * - Search functionality
 * - Action buttons
 */
class GradientHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `displays title correctly`() {
        // Given
        val title = "Voice Notes"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(title = title)
            }
        }

        // Then
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `shows user avatar when enabled`() {
        // Given
        val title = "Voice Notes"
        val userInitials = "JD"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(
                    title = title,
                    showUserAvatar = true,
                    userInitials = userInitials
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(userInitials).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("User avatar: $userInitials").assertIsDisplayed()
    }

    @Test
    fun `hides user avatar when disabled`() {
        // Given
        val title = "Voice Notes"
        val userInitials = "JD"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(
                    title = title,
                    showUserAvatar = false,
                    userInitials = userInitials
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(userInitials).assertDoesNotExist()
    }

    @Test
    fun `shows search bar when enabled`() {
        // Given
        val title = "Voice Notes"
        val placeholder = "Search notes..."

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(
                    title = title,
                    showSearch = true,
                    searchPlaceholder = placeholder
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search bar").assertIsDisplayed()
    }

    @Test
    fun `hides search bar when disabled`() {
        // Given
        val title = "Voice Notes"
        val placeholder = "Search notes..."

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(
                    title = title,
                    showSearch = false,
                    searchPlaceholder = placeholder
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(placeholder).assertDoesNotExist()
    }

    @Test
    fun `handles search query changes`() {
        // Given
        val title = "Voice Notes"
        var searchQuery by mutableStateOf("")
        val testQuery = "test query"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(
                    title = title,
                    showSearch = true,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )
            }
        }

        // Perform text input
        composeTestRule.onNodeWithContentDescription("Search bar")
            .performTextInput(testQuery)

        // Then
        assert(searchQuery == testQuery)
    }

    @Test
    fun `displays search query correctly`() {
        // Given
        val title = "Voice Notes"
        val searchQuery = "existing query"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(
                    title = title,
                    showSearch = true,
                    searchQuery = searchQuery
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(searchQuery).assertIsDisplayed()
    }

    @Test
    fun `renders action buttons correctly`() {
        // Given
        val title = "Voice Notes"
        var actionClicked = false

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(
                    title = title,
                    actions = {
                        androidx.compose.material3.IconButton(
                            onClick = { actionClicked = true }
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        }

        // Perform click on action button
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .performClick()

        // Then
        assert(actionClicked)
    }

    @Test
    fun `combines all features correctly`() {
        // Given
        val title = "Voice Notes"
        val userInitials = "JD"
        val searchQuery = "test"
        val placeholder = "Search notes..."

        // When
        composeTestRule.setContent {
            MaterialTheme {
                GradientHeader(
                    title = title,
                    showUserAvatar = true,
                    userInitials = userInitials,
                    showSearch = true,
                    searchQuery = searchQuery,
                    searchPlaceholder = placeholder,
                    actions = {
                        androidx.compose.material3.IconButton(onClick = {}) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(userInitials).assertIsDisplayed()
        composeTestRule.onNodeWithText(searchQuery).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
}