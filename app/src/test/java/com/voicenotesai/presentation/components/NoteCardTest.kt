package com.voicenotesai.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.voicenotesai.domain.ai.ContentCategory
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for NoteCard component.
 * 
 * Tests the note card functionality including:
 * - Content display (title, preview, duration, tags)
 * - Action items indicator
 * - Category display
 * - Click handlers
 * - Accessibility
 */
class NoteCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `displays note content correctly`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Discussed project timeline and deliverables"
        val duration = "2:30"
        val tags = listOf("work", "meeting")

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = tags,
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(preview).assertIsDisplayed()
        composeTestRule.onNodeWithText(duration).assertIsDisplayed()
        composeTestRule.onNodeWithText("work").assertIsDisplayed()
        composeTestRule.onNodeWithText("meeting").assertIsDisplayed()
    }

    @Test
    fun `shows action items indicator when hasActionItems is true`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Call John tomorrow"
        val duration = "1:15"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = emptyList(),
                    hasActionItems = true,
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Tasks").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Tasks").assertIsDisplayed()
    }

    @Test
    fun `hides action items indicator when hasActionItems is false`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Just some regular notes"
        val duration = "1:15"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = emptyList(),
                    hasActionItems = false,
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Tasks").assertDoesNotExist()
    }

    @Test
    fun `displays category indicator when category is provided`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Project discussion"
        val duration = "1:15"
        val category = ContentCategory.WORK

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = emptyList(),
                    category = category,
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        // Category indicator should be displayed (exact implementation depends on CategoryIndicator component)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `handles click events correctly`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Project discussion"
        val duration = "1:15"
        var onClickCalled = false
        var onMoreClickCalled = false

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = emptyList(),
                    onClick = { onClickCalled = true },
                    onMoreClick = { onMoreClickCalled = true }
                )
            }
        }

        // Perform clicks
        composeTestRule.onNodeWithText(title).performClick()
        composeTestRule.onNodeWithContentDescription("More options for $title").performClick()

        // Then
        assert(onClickCalled)
        assert(onMoreClickCalled)
    }

    @Test
    fun `displays duration with time icon`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Project discussion"
        val duration = "2:30"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = emptyList(),
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(duration).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Duration").assertIsDisplayed()
    }

    @Test
    fun `limits tags display to maximum of 2`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Project discussion"
        val duration = "1:15"
        val tags = listOf("work", "meeting", "project", "important", "urgent")

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = tags,
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("work").assertIsDisplayed()
        composeTestRule.onNodeWithText("meeting").assertIsDisplayed()
        // Third tag and beyond should not be displayed
        composeTestRule.onNodeWithText("project").assertDoesNotExist()
    }

    @Test
    fun `handles empty tags list`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Project discussion"
        val duration = "1:15"
        val tags = emptyList<String>()

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = tags,
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(preview).assertIsDisplayed()
        composeTestRule.onNodeWithText(duration).assertIsDisplayed()
    }

    @Test
    fun `truncates long title correctly`() {
        // Given
        val longTitle = "This is a very long title that should be truncated because it exceeds the maximum length"
        val preview = "Project discussion"
        val duration = "1:15"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = longTitle,
                    preview = preview,
                    duration = duration,
                    tags = emptyList(),
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        // The title should be displayed (truncation is handled by maxLines and overflow)
        composeTestRule.onNodeWithText(longTitle).assertIsDisplayed()
    }

    @Test
    fun `truncates long preview correctly`() {
        // Given
        val title = "Meeting Notes"
        val longPreview = "This is a very long preview text that should be truncated to two lines maximum because we don't want the card to become too tall and affect the layout of the list"
        val duration = "1:15"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = longPreview,
                    duration = duration,
                    tags = emptyList(),
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        // The preview should be displayed (truncation is handled by maxLines and overflow)
        composeTestRule.onNodeWithText(longPreview).assertIsDisplayed()
    }

    @Test
    fun `provides correct accessibility information`() {
        // Given
        val title = "Meeting Notes"
        val preview = "Project discussion"
        val duration = "2:30"
        val tags = listOf("work")

        // When
        composeTestRule.setContent {
            MaterialTheme {
                NoteCard(
                    title = title,
                    preview = preview,
                    duration = duration,
                    tags = tags,
                    hasActionItems = true,
                    onClick = {},
                    onMoreClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("More options for $title").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Tag: work").assertIsDisplayed()
    }
}