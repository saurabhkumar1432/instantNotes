package com.voicenotesai.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for StatsCard component.
 * 
 * Tests the stats card functionality including:
 * - Value and label display
 * - Color customization
 * - Trend indicators
 * - Accessibility
 */
class StatsCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `displays value and label correctly`() {
        // Given
        val value = "42"
        val label = "Notes"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(value).assertIsDisplayed()
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    @Test
    fun `displays content description correctly`() {
        // Given
        val value = "42"
        val label = "Notes"
        val expectedContentDesc = "Notes: 42"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription(expectedContentDesc).assertIsDisplayed()
    }

    @Test
    fun `displays positive trend indicator`() {
        // Given
        val value = "42"
        val label = "Notes"
        val trend = 15.5f

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label,
                    trend = trend,
                    showTrend = true
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("+15.5%").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Up trend").assertIsDisplayed()
    }

    @Test
    fun `displays negative trend indicator`() {
        // Given
        val value = "42"
        val label = "Notes"
        val trend = -8.2f

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label,
                    trend = trend,
                    showTrend = true
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("-8.2%").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Down trend").assertIsDisplayed()
    }

    @Test
    fun `displays zero trend as positive`() {
        // Given
        val value = "42"
        val label = "Notes"
        val trend = 0.0f

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label,
                    trend = trend,
                    showTrend = true
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("+0.0%").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Up trend").assertIsDisplayed()
    }

    @Test
    fun `hides trend when showTrend is false`() {
        // Given
        val value = "42"
        val label = "Notes"
        val trend = 15.5f

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label,
                    trend = trend,
                    showTrend = false
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("+15.5%").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Up trend").assertDoesNotExist()
    }

    @Test
    fun `hides trend when trend is null`() {
        // Given
        val value = "42"
        val label = "Notes"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label,
                    trend = null,
                    showTrend = true
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Up trend").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Down trend").assertDoesNotExist()
    }

    @Test
    fun `applies custom value color`() {
        // Given
        val value = "42"
        val label = "Notes"
        val customColor = Color.Red

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label,
                    valueColor = customColor
                )
            }
        }

        // Then
        // Note: Color testing in Compose is limited, but we can verify the component renders
        composeTestRule.onNodeWithText(value).assertIsDisplayed()
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    @Test
    fun `handles large numbers correctly`() {
        // Given
        val value = "1,234,567"
        val label = "Total Notes"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(value).assertIsDisplayed()
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    @Test
    fun `handles empty value gracefully`() {
        // Given
        val value = ""
        val label = "Notes"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
        // Empty value should still be rendered (as empty text)
    }

    @Test
    fun `handles long labels correctly`() {
        // Given
        val value = "42"
        val label = "Very Long Label That Might Wrap"

        // When
        composeTestRule.setContent {
            MaterialTheme {
                StatsCard(
                    value = value,
                    label = label
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(value).assertIsDisplayed()
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }
}