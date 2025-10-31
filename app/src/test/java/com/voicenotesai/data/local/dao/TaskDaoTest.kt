package com.voicenotesai.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voicenotesai.data.local.AppDatabase
import com.voicenotesai.data.local.entity.Task
import com.voicenotesai.data.local.entity.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        taskDao = database.taskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTask() = runTest {
        // Given
        val task = Task(
            id = "test-task-1",
            text = "Complete project documentation",
            isCompleted = false,
            sourceNoteId = null,
            priority = TaskPriority.HIGH
        )

        // When
        taskDao.insertTask(task)
        val retrievedTask = taskDao.getTaskById("test-task-1")

        // Then
        assertNotNull(retrievedTask)
        assertEquals(task.text, retrievedTask?.text)
        assertEquals(task.priority, retrievedTask?.priority)
        assertEquals(task.isCompleted, retrievedTask?.isCompleted)
    }

    @Test
    fun getTasksByCompletionStatus() = runTest {
        // Given
        val completedTask = Task(
            id = "completed-task",
            text = "Completed task",
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        val pendingTask = Task(
            id = "pending-task", 
            text = "Pending task",
            isCompleted = false
        )

        taskDao.insertTasks(listOf(completedTask, pendingTask))

        // When
        val completedTasks = taskDao.getTasksByCompletionStatus(true).first()
        val pendingTasks = taskDao.getTasksByCompletionStatus(false).first()

        // Then
        assertEquals(1, completedTasks.size)
        assertEquals("Completed task", completedTasks[0].text)
        
        assertEquals(1, pendingTasks.size)
        assertEquals("Pending task", pendingTasks[0].text)
    }

    @Test
    fun getPendingTasksCount() = runTest {
        // Given
        val tasks = listOf(
            Task(id = "task1", text = "Task 1", isCompleted = false),
            Task(id = "task2", text = "Task 2", isCompleted = true),
            Task(id = "task3", text = "Task 3", isCompleted = false)
        )
        taskDao.insertTasks(tasks)

        // When
        val pendingCount = taskDao.getPendingTasksCount().first()

        // Then
        assertEquals(2, pendingCount)
    }

    @Test
    fun updateTaskCompletionStatus() = runTest {
        // Given
        val task = Task(
            id = "update-task",
            text = "Task to update",
            isCompleted = false
        )
        taskDao.insertTask(task)

        // When
        val completedAt = System.currentTimeMillis()
        taskDao.updateTaskCompletionStatus("update-task", true, completedAt)
        val updatedTask = taskDao.getTaskById("update-task")

        // Then
        assertNotNull(updatedTask)
        assertTrue(updatedTask!!.isCompleted)
        assertEquals(completedAt, updatedTask.completedAt)
    }

    @Test
    fun getTasksByPriority() = runTest {
        // Given
        val tasks = listOf(
            Task(id = "high1", text = "High priority 1", priority = TaskPriority.HIGH),
            Task(id = "normal1", text = "Normal priority 1", priority = TaskPriority.NORMAL),
            Task(id = "high2", text = "High priority 2", priority = TaskPriority.HIGH),
            Task(id = "urgent1", text = "Urgent priority 1", priority = TaskPriority.URGENT)
        )
        taskDao.insertTasks(tasks)

        // When
        val highPriorityTasks = taskDao.getTasksByPriority(TaskPriority.HIGH).first()
        val urgentTasks = taskDao.getTasksByPriority(TaskPriority.URGENT).first()

        // Then
        assertEquals(2, highPriorityTasks.size)
        assertEquals(1, urgentTasks.size)
        assertEquals("Urgent priority 1", urgentTasks[0].text)
    }

    @Test
    fun searchTasks() = runTest {
        // Given
        val tasks = listOf(
            Task(id = "task1", text = "Complete project documentation"),
            Task(id = "task2", text = "Review code changes"),
            Task(id = "task3", text = "Update project README")
        )
        taskDao.insertTasks(tasks)

        // When
        val searchResults = taskDao.searchTasks("project").first()

        // Then
        assertEquals(2, searchResults.size)
        assertTrue(searchResults.any { it.text.contains("project documentation") })
        assertTrue(searchResults.any { it.text.contains("project README") })
    }

    @Test
    fun deleteTask() = runTest {
        // Given
        val task = Task(
            id = "delete-task",
            text = "Task to delete"
        )
        taskDao.insertTask(task)

        // When
        taskDao.deleteTaskById("delete-task")
        val deletedTask = taskDao.getTaskById("delete-task")

        // Then
        assertNull(deletedTask)
    }
}