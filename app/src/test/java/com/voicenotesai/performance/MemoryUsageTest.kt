package com.voicenotesai.performance

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert.*

/**
 * Memory usage performance tests with large datasets
 * 
 * Requirements: 7.1, 7.2, 7.3 - Performance and memory optimization
 * - Tests memory usage with 1000+ notes and 500+ tasks
 * - Validates memory cleanup and garbage collection
 * - Ensures memory usage stays within acceptable limits
 */
class MemoryUsageTest {
    
    private lateinit var mockDataManager: MockDataManager
    private val runtime = Runtime.getRuntime()
    
    @Before
    fun setup() {
        mockDataManager = MockDataManager()
        // Force garbage collection before tests
        System.gc()
        Thread.sleep(100)
    }
    
    @After
    fun cleanup() {
        mockDataManager.cleanup()
        System.gc()
        Thread.sleep(100)
    }
    
    @Test
    fun `validate memory usage with 1000 notes dataset`() = runTest {
        val initialMemory = getCurrentMemoryUsage()
        println("Initial memory usage: ${PerformanceUtils.bytesToMB(initialMemory)} MB")
        
        // Load large notes dataset
        val largeNotesDataset = PerformanceTestData.generateLargeNoteDataset(
            PerformanceTestSuite.LARGE_DATASET_NOTES
        )
        
        val memoryAfterLoad = PerformanceUtils.measureMemoryUsage {
            mockDataManager.loadNotes(largeNotesDataset)
        }
        
        val totalMemoryAfterLoad = getCurrentMemoryUsage()
        println("Memory after loading ${largeNotesDataset.size} notes: ${PerformanceUtils.bytesToMB(totalMemoryAfterLoad)} MB")
        println("Memory increase: ${PerformanceUtils.bytesToMB(memoryAfterLoad)} MB")
        
        // Test memory usage during operations
        val memoryDuringOperations = PerformanceUtils.measureMemoryUsage {
            // Simulate typical operations
            mockDataManager.searchNotes("test")
            mockDataManager.filterNotesByTag("tag1")
            mockDataManager.sortNotesByDate()
            mockDataManager.paginateNotes(0, 50)
        }
        
        println("Memory usage during operations: ${PerformanceUtils.bytesToMB(memoryDuringOperations)} MB")
        
        // Validate memory constraints
        val totalMemoryUsageMB = PerformanceUtils.bytesToMB(totalMemoryAfterLoad)
        assertTrue(
            "Memory usage (${totalMemoryUsageMB} MB) exceeds maximum allowed (${PerformanceTestSuite.MAX_MEMORY_USAGE_MB} MB)",
            totalMemoryUsageMB <= PerformanceTestSuite.MAX_MEMORY_USAGE_MB
        )
        
        // Test memory cleanup
        mockDataManager.clearNotes()
        System.gc()
        Thread.sleep(200)
        
        val memoryAfterCleanup = getCurrentMemoryUsage()
        val memoryReclaimed = totalMemoryAfterLoad - memoryAfterCleanup
        println("Memory after cleanup: ${PerformanceUtils.bytesToMB(memoryAfterCleanup)} MB")
        println("Memory reclaimed: ${PerformanceUtils.bytesToMB(memoryReclaimed)} MB")
        
        assertTrue(
            "Memory cleanup ineffective - less than 70% memory reclaimed",
            memoryReclaimed >= memoryAfterLoad * 0.7
        )
    }
    
    @Test
    fun `validate memory usage with 500 tasks dataset`() = runTest {
        val initialMemory = getCurrentMemoryUsage()
        
        // Load large tasks dataset
        val largeTasksDataset = PerformanceTestData.generateLargeTaskDataset(
            PerformanceTestSuite.LARGE_DATASET_TASKS
        )
        
        val memoryAfterLoad = PerformanceUtils.measureMemoryUsage {
            mockDataManager.loadTasks(largeTasksDataset)
        }
        
        val totalMemoryAfterLoad = getCurrentMemoryUsage()
        println("Memory after loading ${largeTasksDataset.size} tasks: ${PerformanceUtils.bytesToMB(totalMemoryAfterLoad)} MB")
        
        // Test task operations memory usage
        val memoryDuringTaskOps = PerformanceUtils.measureMemoryUsage {
            mockDataManager.filterTasksByStatus(completed = false)
            mockDataManager.sortTasksByPriority()
            mockDataManager.groupTasksByDate()
            mockDataManager.searchTasks("task")
        }
        
        println("Memory usage during task operations: ${PerformanceUtils.bytesToMB(memoryDuringTaskOps)} MB")
        
        val totalMemoryUsageMB = PerformanceUtils.bytesToMB(totalMemoryAfterLoad)
        assertTrue(
            "Task dataset memory usage exceeds limit",
            totalMemoryUsageMB <= PerformanceTestSuite.MAX_MEMORY_USAGE_MB / 2 // Tasks should use less memory than notes
        )
    }
    
    @Test
    fun `validate memory usage with combined large dataset`() = runTest {
        val initialMemory = getCurrentMemoryUsage()
        
        // Load both large datasets
        val largeNotesDataset = PerformanceTestData.generateLargeNoteDataset(
            PerformanceTestSuite.LARGE_DATASET_NOTES
        )
        val largeTasksDataset = PerformanceTestData.generateLargeTaskDataset(
            PerformanceTestSuite.LARGE_DATASET_TASKS
        )
        
        val memoryAfterBothLoads = PerformanceUtils.measureMemoryUsage {
            mockDataManager.loadNotes(largeNotesDataset)
            mockDataManager.loadTasks(largeTasksDataset)
        }
        
        val totalMemoryAfterLoad = getCurrentMemoryUsage()
        println("Memory after loading combined dataset: ${PerformanceUtils.bytesToMB(totalMemoryAfterLoad)} MB")
        println("  - ${largeNotesDataset.size} notes")
        println("  - ${largeTasksDataset.size} tasks")
        
        // Test complex operations with both datasets
        val memoryDuringComplexOps = PerformanceUtils.measureMemoryUsage {
            mockDataManager.linkTasksToNotes()
            mockDataManager.generateAnalytics()
            mockDataManager.performFullTextSearch("test query")
            mockDataManager.exportData()
        }
        
        println("Memory usage during complex operations: ${PerformanceUtils.bytesToMB(memoryDuringComplexOps)} MB")
        
        val totalMemoryUsageMB = PerformanceUtils.bytesToMB(totalMemoryAfterLoad)
        assertTrue(
            "Combined dataset memory usage (${totalMemoryUsageMB} MB) exceeds maximum allowed (${PerformanceTestSuite.MAX_MEMORY_USAGE_MB} MB)",
            totalMemoryUsageMB <= PerformanceTestSuite.MAX_MEMORY_USAGE_MB
        )
        
        // Test memory stability over time
        repeat(10) {
            mockDataManager.performRandomOperations()
            Thread.sleep(50)
        }
        
        val memoryAfterStabilityTest = getCurrentMemoryUsage()
        val memoryGrowth = memoryAfterStabilityTest - totalMemoryAfterLoad
        println("Memory growth after stability test: ${PerformanceUtils.bytesToMB(memoryGrowth)} MB")
        
        assertTrue(
            "Memory growth during stability test exceeds 10% of initial usage",
            memoryGrowth <= totalMemoryAfterLoad * 0.1
        )
    }
    
    @Test
    fun `validate memory usage during pagination`() = runTest {
        val largeDataset = PerformanceTestData.generateLargeNoteDataset(2000)
        mockDataManager.loadNotes(largeDataset)
        
        val initialMemory = getCurrentMemoryUsage()
        
        // Test pagination memory usage
        val pageSize = 50
        val totalPages = largeDataset.size / pageSize
        val memoryUsagePerPage = mutableListOf<Long>()
        
        repeat(totalPages) { page ->
            val memoryBefore = getCurrentMemoryUsage()
            mockDataManager.paginateNotes(page, pageSize)
            val memoryAfter = getCurrentMemoryUsage()
            memoryUsagePerPage.add(memoryAfter - memoryBefore)
        }
        
        val averageMemoryPerPage = memoryUsagePerPage.average()
        val maxMemoryPerPage = memoryUsagePerPage.maxOrNull() ?: 0L
        
        println("Pagination Memory Usage:")
        println("  Average per page: ${PerformanceUtils.bytesToMB(averageMemoryPerPage.toLong())} MB")
        println("  Max per page: ${PerformanceUtils.bytesToMB(maxMemoryPerPage)} MB")
        println("  Total pages tested: $totalPages")
        
        assertTrue(
            "Average memory per page exceeds 5MB",
            PerformanceUtils.bytesToMB(averageMemoryPerPage.toLong()) <= 5.0
        )
        
        assertTrue(
            "Max memory per page exceeds 10MB",
            PerformanceUtils.bytesToMB(maxMemoryPerPage) <= 10.0
        )
    }
    
    @Test
    fun `validate memory cleanup after operations`() = runTest {
        val dataset = PerformanceTestData.generateLargeNoteDataset(500)
        
        // Perform operations that should be cleaned up
        val memoryUsed = PerformanceUtils.measureMemoryUsage {
            mockDataManager.loadNotes(dataset)
            mockDataManager.performBulkOperations()
            mockDataManager.generateLargeReport()
            mockDataManager.clearTemporaryData()
        }
        
        println("Memory used during operations: ${PerformanceUtils.bytesToMB(memoryUsed)} MB")
        
        // Force cleanup
        mockDataManager.cleanup()
        System.gc()
        Thread.sleep(200)
        
        val memoryAfterCleanup = getCurrentMemoryUsage()
        
        // Perform same operations again to check for memory leaks
        val memoryUsedSecondTime = PerformanceUtils.measureMemoryUsage {
            mockDataManager.loadNotes(dataset)
            mockDataManager.performBulkOperations()
            mockDataManager.generateLargeReport()
            mockDataManager.clearTemporaryData()
        }
        
        println("Memory used second time: ${PerformanceUtils.bytesToMB(memoryUsedSecondTime)} MB")
        
        val memoryDifference = kotlin.math.abs(memoryUsedSecondTime - memoryUsed)
        val memoryDifferencePercent = (memoryDifference.toDouble() / memoryUsed) * 100
        
        println("Memory difference between runs: ${PerformanceUtils.bytesToMB(memoryDifference)} MB (${memoryDifferencePercent}%)")
        
        assertTrue(
            "Memory usage difference between runs exceeds 20% - possible memory leak",
            memoryDifferencePercent <= 20.0
        )
    }
    
    private fun getCurrentMemoryUsage(): Long {
        return runtime.totalMemory() - runtime.freeMemory()
    }
}

/**
 * Mock data manager for testing memory usage
 */
class MockDataManager {
    private val notes = mutableListOf<MockNote>()
    private val tasks = mutableListOf<MockTask>()
    private val temporaryData = mutableListOf<String>()
    private val cache = mutableMapOf<String, Any>()
    
    fun loadNotes(notesList: List<MockNote>) {
        notes.clear()
        notes.addAll(notesList)
    }
    
    fun loadTasks(tasksList: List<MockTask>) {
        tasks.clear()
        tasks.addAll(tasksList)
    }
    
    fun searchNotes(query: String): List<MockNote> {
        return notes.filter { it.content.contains(query, ignoreCase = true) }
    }
    
    fun filterNotesByTag(tag: String): List<MockNote> {
        return notes.filter { it.tags.contains(tag) }
    }
    
    fun sortNotesByDate(): List<MockNote> {
        return notes.sortedByDescending { it.timestamp }
    }
    
    fun paginateNotes(page: Int, pageSize: Int): List<MockNote> {
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, notes.size)
        return if (startIndex < notes.size) {
            notes.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }
    
    fun filterTasksByStatus(completed: Boolean): List<MockTask> {
        return tasks.filter { it.isCompleted == completed }
    }
    
    fun sortTasksByPriority(): List<MockTask> {
        return tasks.sortedBy { it.priority.ordinal }
    }
    
    fun groupTasksByDate(): Map<String, List<MockTask>> {
        return tasks.groupBy { 
            java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(it.createdAt))
        }
    }
    
    fun searchTasks(query: String): List<MockTask> {
        return tasks.filter { it.text.contains(query, ignoreCase = true) }
    }
    
    fun linkTasksToNotes() {
        tasks.forEach { task ->
            task.sourceNoteId?.let { noteId ->
                notes.find { it.id == noteId }
            }
        }
    }
    
    fun generateAnalytics(): Map<String, Any> {
        return mapOf(
            "totalNotes" to notes.size,
            "totalTasks" to tasks.size,
            "completedTasks" to tasks.count { it.isCompleted },
            "averageNoteLength" to notes.map { it.content.length }.average(),
            "tagDistribution" to notes.flatMap { it.tags }.groupingBy { it }.eachCount()
        )
    }
    
    fun performFullTextSearch(query: String): List<Any> {
        val noteResults = searchNotes(query)
        val taskResults = searchTasks(query)
        return noteResults + taskResults
    }
    
    fun exportData(): String {
        val exportData = StringBuilder()
        notes.forEach { note ->
            exportData.append("Note: ${note.content}\n")
        }
        tasks.forEach { task ->
            exportData.append("Task: ${task.text}\n")
        }
        return exportData.toString()
    }
    
    fun performRandomOperations() {
        // Simulate random operations that might cause memory usage
        searchNotes("random")
        filterTasksByStatus(true)
        generateAnalytics()
        temporaryData.add("temporary_${System.currentTimeMillis()}")
        
        // Simulate caching
        cache["random_${System.currentTimeMillis()}"] = notes.take(10)
    }
    
    fun performBulkOperations() {
        // Simulate bulk operations
        repeat(100) {
            temporaryData.add("bulk_data_$it")
        }
        
        // Simulate processing
        notes.forEach { note ->
            cache["processed_${note.id}"] = note.content.uppercase()
        }
    }
    
    fun generateLargeReport(): String {
        val report = StringBuilder()
        repeat(1000) {
            report.append("Report line $it with data\n")
        }
        return report.toString()
    }
    
    fun clearTemporaryData() {
        temporaryData.clear()
        cache.clear()
    }
    
    fun clearNotes() {
        notes.clear()
    }
    
    fun clearTasks() {
        tasks.clear()
    }
    
    fun cleanup() {
        notes.clear()
        tasks.clear()
        temporaryData.clear()
        cache.clear()
    }
}