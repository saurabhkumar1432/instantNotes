package com.voicenotesai.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskPriority
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for TaskCard component.
 * 
 * Tests the task card functionality including:
 * - Task content display
 * - Completion status
 * - Priority indicators
 * - Source note information
 * - Action handlers
 * - Accessibility
 */
class TaskCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTask = Task(
        id = "task1",
        text = "Call John tomorrow",
        isCompleted = false,
        priority = TaskPriority.NORMAL,
        createdAt = System.currentTimeMillis()
    )

    private val sampleNote = EnhancedNote(
        id = "note1",
        content = "Meeting notes with John about project timeline",
        transcribedText = "Meeting notes with John about project timeline",
        timestamp = System.currentTimeMillis()
    )

    @Test
    fun `displays task content correctly`() {
        // Given
        val task = sampleTask.copy(text = "Buy groceries")

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Buy groceries").assertIsDisplayed()
    }

    @Test
    fun `shows completion checkbox correctly`() {
        // Given
        val task = sampleTask.copy(isCompleted = false)

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Mark task as completed").assertIsDisplayed()
    }

    @Test
    fun `shows completed task with strikethrough`() {
        // Given
        val task = sampleTask.copy(
            text = "Completed task",
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Completed task").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Mark task as pending").assertIsDisplayed()
    }

    @Test
    fun `displays source note information`() {
        // Given
        val task = sampleTask
        val sourceNote = sampleNote

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = sourceNote,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Meeting notes with John about pr...").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Source note").assertIsDisplayed()
    }

    @Test
    fun `handles toggle completion correctly`() {
        // Given
        val task = sampleTask.copy(isCompleted = false)
        var toggledTaskId: String? = null

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = { toggledTaskId = it },
                    onDelete = {}
                )
            }
        }

        // Perform click on checkbox
        composeTestRule.onNodeWithContentDescription("Mark task as completed").performClick()

        // Then
        assert(toggledTaskId == task.id)
    }

    @Test
    fun `handles delete action correctly`() {
        // Given
        val task = sampleTask
        var deletedTaskId: String? = null

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = { deletedTaskId = it }
                )
            }
        }

        // Perform click on delete button
        composeTestRule.onNodeWithContentDescription("Delete task: ${task.text}").performClick()

        // Then
        assert(deletedTaskId == task.id)
    }

    @Test
    fun `displays priority indicator for high priority tasks`() {
        // Given
        val task = sampleTask.copy(priority = TaskPriority.HIGH)

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("High").assertIsDisplayed()
    }

    @Test
    fun `displays priority indicator for urgent priority tasks`() {
        // Given
        val task = sampleTask.copy(priority = TaskPriority.URGENT)

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Urgent").assertIsDisplayed()
    }

    @Test
    fun `hides priority indicator for normal priority tasks`() {
        // Given
        val task = sampleTask.copy(priority = TaskPriority.NORMAL)

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Normal").assertDoesNotExist()
    }

    @Test
    fun `displays due date when present`() {
        // Given
        val tomorrow = System.currentTimeMillis() + 86400000L // 24 hours
        val task = sampleTask.copy(dueDate = tomorrow)

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Due: Tomorrow").assertIsDisplayed()
    }

    @Test
    fun `shows overdue indicator for past due dates`() {
        // Given
        val yesterday = System.currentTimeMillis() - 86400000L // 24 hours ago
        val task = sampleTask.copy(
            dueDate = yesterday,
            isCompleted = false
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Due: Yesterday").assertIsDisplayed()
        // Note: Overdue styling would be tested through color/style verification if possible
    }

    @Test
    fun `displays creation date correctly`() {
        // Given
        val task = sampleTask

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Created").assertIsDisplayed()
    }

    @Test
    fun `handles task click when onTaskClick is provided`() {
        // Given
        val task = sampleTask
        var taskClicked = false

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {},
                    onTaskClick = { taskClicked = true }
                )
            }
        }

        // Perform click on task content
        composeTestRule.onNodeWithText(task.text).performClick()

        // Then
        assert(taskClicked)
    }

    @Test
    fun `provides correct accessibility information`() {
        // Given
        val task = sampleTask.copy(
            text = "Important task",
            isCompleted = false
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = sampleNote,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Task pending: Important task").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delete task: Important task").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Mark task as completed").assertIsDisplayed()
    }

    @Test
    fun `handles long task text correctly`() {
        // Given
        val longTaskText = "This is a very long task description that should be handled properly by the component and might need to wrap to multiple lines"
        val task = sampleTask.copy(text = longTaskText)

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(longTaskText).assertIsDisplayed()
    }

    @Test
    fun `displays low priority indicator`() {
        // Given
        val task = sampleTask.copy(priority = TaskPriority.LOW)

        // When
        composeTestRule.setContent {
            MaterialTheme {
                TaskCard(
                    task = task,
                    sourceNote = null,
                    onToggleComplete = {},
                    onDelete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Low").assertIsDisplayed()
    }
}