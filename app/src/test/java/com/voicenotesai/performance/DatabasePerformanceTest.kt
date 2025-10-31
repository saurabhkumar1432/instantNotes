package com.voicenotesai.performance

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert.*

/**
 * Database performance tests for large datasets
 * 
 * Requirements: 7.1, 7.2, 7.3 - Database performance with large datasets
 * - Tests database operations with 1000+ notes and 500+ tasks
 * - Validates query performance and indexing efficiency
 * - Tests pagination and search performance
 */
class DatabasePerformanceTest {
    
    private lateinit var mockDatabase: MockDatabase
    
    @Before
    fun setup() {
        mockDatabase = MockDatabase()
        mockDatabase.initialize()
    }
    
    @After
    fun cleanup() {
        mockDatabase.cleanup()
    }
    
    @Test
    fun `validate database insertion performance with large dataset`() = runTest {
        val largeNotesDataset = PerformanceTestData.generateLargeNoteDataset(
            PerformanceTestSuite.LARGE_DATASET_NOTES
        )
        val largeTasksDataset = PerformanceTestData.generateLargeTaskDataset(
            PerformanceTestSuite.LARGE_DATASET_TASKS
        )
        
        // Test bulk note insertion
        val noteInsertionMetrics = PerformanceUtils.measurePerformance(iterations = 5) {
            mockDatabase.insertNotesBatch(largeNotesDataset)
        }
        
        println("Note Insertion Performance (${largeNotesDataset.size} notes):")
        println("  Average time: ${noteInsertionMetrics.averageTime}ms")
        println("  P95 time: ${noteInsertionMetrics.p95Time}ms")
        println("  Max time: ${noteInsertionMetrics.maxTime}ms")
        println("  Throughput: ${largeNotesDataset.size / (noteInsertionMetrics.averageTime / 1000)} notes/sec")
        
        // Test bulk task insertion
        val taskInsertionMetrics = PerformanceUtils.measurePerformance(iterations = 5) {
            mockDatabase.insertTasksBatch(largeTasksDataset)
        }
        
        println("Task Insertion Performance (${largeTasksDataset.size} tasks):")
        println("  Average time: ${taskInsertionMetrics.averageTime}ms")
        println("  P95 time: ${taskInsertionMetrics.p95Time}ms")
        println("  Throughput: ${largeTasksDataset.size / (taskInsertionMetrics.averageTime / 1000)} tasks/sec")
        
        // Validate performance thresholds
        assertTrue(
            "Note insertion average time (${noteInsertionMetrics.averageTime}ms) exceeds threshold (2000ms)",
            noteInsertionMetrics.averageTime <= 2000.0
        )
        
        assertTrue(
            "Task insertion average time (${taskInsertionMetrics.averageTime}ms) exceeds threshold (1000ms)",
            taskInsertionMetrics.averageTime <= 1000.0
        )
        
        val noteThroughput = largeNotesDataset.size / (noteInsertionMetrics.averageTime / 1000)
        assertTrue(
            "Note insertion throughput (${noteThroughput} notes/sec) below threshold (500 notes/sec)",
            noteThroughput >= 500
        )
    }
    
    @Test
    fun `validate database query performance with large dataset`() = runTest {
        // Setup large dataset
        val notes = PerformanceTestData.generateLargeNoteDataset(1000)
        val tasks = PerformanceTestData.generateLargeTaskDataset(500)
        
        mockDatabase.insertNotesBatch(notes)
        mockDatabase.insertTasksBatch(tasks)
        
        // Test various query types
        val queryTests = mapOf(
            "getAllNotes" to { mockDatabase.getAllNotes() },
            "getRecentNotes" to { mockDatabase.getRecentNotes(50) },
            "searchNotes" to { mockDatabase.searchNotes("test") },
            "getNotesByTag" to { mockDatabase.getNotesByTag("tag1") },
            "getAllTasks" to { mockDatabase.getAllTasks() },
            "getPendingTasks" to { mockDatabase.getPendingTasks() },
            "getTasksByNote" to { mockDatabase.getTasksByNote("note_1") },
            "getTasksWithNotes" to { mockDatabase.getTasksWithNotes() }
        )
        
        queryTests.forEach { (queryName, queryFunction) ->
            val queryMetrics = PerformanceUtils.measurePerformance(iterations = 20) {
                queryFunction()
            }
            
            println("$queryName Performance:")
            println("  Average time: ${queryMetrics.averageTime}ms")
            println("  P95 time: ${queryMetrics.p95Time}ms")
            println("  Max time: ${queryMetrics.maxTime}ms")
            
            val threshold = when (queryName) {
                "getAllNotes", "getAllTasks" -> 200.0
                "getTasksWithNotes" -> 300.0
                "searchNotes" -> 150.0
                else -> 100.0
            }
            
            assertTrue(
                "$queryName average time (${queryMetrics.averageTime}ms) exceeds threshold (${threshold}ms)",
                queryMetrics.averageTime <= threshold
            )
        }
    }
    
    @Test
    fun `validate database pagination performance`() = runTest {
        val largeDataset = PerformanceTestData.generateLargeNoteDataset(2000)
        mockDatabase.insertNotesBatch(largeDataset)
        
        val pageSize = 50
        val totalPages = largeDataset.size / pageSize
        val paginationMetrics = mutableListOf<Double>()
        
        // Test pagination performance across all pages
        repeat(totalPages) { page ->
            val pageMetrics = PerformanceUtils.measurePerformance(iterations = 10) {
                mockDatabase.getNotesPaginated(page, pageSize)
            }
            paginationMetrics.add(pageMetrics.averageTime)
        }
        
        val averagePaginationTime = paginationMetrics.average()
        val maxPaginationTime = paginationMetrics.maxOrNull() ?: 0.0
        val paginationVariance = paginationMetrics.map { (it - averagePaginationTime) * (it - averagePaginationTime) }.average()
        val paginationStdDev = kotlin.math.sqrt(paginationVariance)
        
        println("Pagination Performance (${totalPages} pages, ${pageSize} items/page):")
        println("  Average page load time: ${averagePaginationTime}ms")
        println("  Max page load time: ${maxPaginationTime}ms")
        println("  Standard deviation: ${paginationStdDev}ms")
        println("  Consistency coefficient: ${paginationStdDev / averagePaginationTime}")
        
        assertTrue(
            "Average pagination time (${averagePaginationTime}ms) exceeds threshold (50ms)",
            averagePaginationTime <= 50.0
        )
        
        assertTrue(
            "Max pagination time (${maxPaginationTime}ms) exceeds threshold (100ms)",
            maxPaginationTime <= 100.0
        )
        
        assertTrue(
            "Pagination consistency coefficient (${paginationStdDev / averagePaginationTime}) exceeds threshold (0.3)",
            paginationStdDev / averagePaginationTime <= 0.3
        )
    }
    
    @Test
    fun `validate database search performance`() = runTest {
        val dataset = PerformanceTestData.generateLargeNoteDataset(1500)
        mockDatabase.insertNotesBatch(dataset)
        
        val searchQueries = listOf(
            "test",
            "note",
            "content",
            "important",
            "meeting",
            "task",
            "action",
            "follow up",
            "reminder",
            "project"
        )
        
        searchQueries.forEach { query ->
            val searchMetrics = PerformanceUtils.measurePerformance(iterations = 15) {
                mockDatabase.searchNotes(query)
            }
            
            println("Search Performance for '$query':")
            println("  Average time: ${searchMetrics.averageTime}ms")
            println("  P95 time: ${searchMetrics.p95Time}ms")
            
            assertTrue(
                "Search for '$query' average time (${searchMetrics.averageTime}ms) exceeds threshold (100ms)",
                searchMetrics.averageTime <= 100.0
            )
        }
        
        // Test complex search queries
        val complexQueries = listOf(
            "test AND important",
            "meeting OR task",
            "content NOT archived"
        )
        
        complexQueries.forEach { query ->
            val complexSearchMetrics = PerformanceUtils.measurePerformance(iterations = 10) {
                mockDatabase.complexSearch(query)
            }
            
            println("Complex Search Performance for '$query':")
            println("  Average time: ${complexSearchMetrics.averageTime}ms")
            
            assertTrue(
                "Complex search for '$query' average time (${complexSearchMetrics.averageTime}ms) exceeds threshold (200ms)",
                complexSearchMetrics.averageTime <= 200.0
            )
        }
    }
    
    @Test
    fun `validate database update performance`() = runTest {
        val dataset = PerformanceTestData.generateLargeNoteDataset(800)
        mockDatabase.insertNotesBatch(dataset)
        
        // Test single note updates
        val singleUpdateMetrics = PerformanceUtils.measurePerformance(iterations = 50) {
            val randomNote = dataset.random()
            mockDatabase.updateNote(randomNote.copy(content = "Updated content"))
        }
        
        println("Single Note Update Performance:")
        println("  Average time: ${singleUpdateMetrics.averageTime}ms")
        println("  P95 time: ${singleUpdateMetrics.p95Time}ms")
        
        // Test bulk updates
        val bulkUpdateMetrics = PerformanceUtils.measurePerformance(iterations = 10) {
            val notesToUpdate = dataset.take(100).map { 
                it.copy(content = "Bulk updated content") 
            }
            mockDatabase.updateNotesBatch(notesToUpdate)
        }
        
        println("Bulk Update Performance (100 notes):")
        println("  Average time: ${bulkUpdateMetrics.averageTime}ms")
        println("  P95 time: ${bulkUpdateMetrics.p95Time}ms")
        
        assertTrue(
            "Single update average time (${singleUpdateMetrics.averageTime}ms) exceeds threshold (20ms)",
            singleUpdateMetrics.averageTime <= 20.0
        )
        
        assertTrue(
            "Bulk update average time (${bulkUpdateMetrics.averageTime}ms) exceeds threshold (500ms)",
            bulkUpdateMetrics.averageTime <= 500.0
        )
    }
    
    @Test
    fun `validate database deletion performance`() = runTest {
        val dataset = PerformanceTestData.generateLargeNoteDataset(600)
        mockDatabase.insertNotesBatch(dataset)
        
        // Test single deletions
        val singleDeleteMetrics = PerformanceUtils.measurePerformance(iterations = 30) {
            val randomNote = dataset.random()
            mockDatabase.deleteNote(randomNote.id)
        }
        
        println("Single Note Deletion Performance:")
        println("  Average time: ${singleDeleteMetrics.averageTime}ms")
        
        // Test bulk deletions
        val idsToDelete = dataset.take(50).map { it.id }
        val bulkDeleteMetrics = PerformanceUtils.measurePerformance(iterations = 10) {
            mockDatabase.deleteNotesBatch(idsToDelete)
        }
        
        println("Bulk Deletion Performance (50 notes):")
        println("  Average time: ${bulkDeleteMetrics.averageTime}ms")
        
        assertTrue(
            "Single deletion average time (${singleDeleteMetrics.averageTime}ms) exceeds threshold (15ms)",
            singleDeleteMetrics.averageTime <= 15.0
        )
        
        assertTrue(
            "Bulk deletion average time (${bulkDeleteMetrics.averageTime}ms) exceeds threshold (200ms)",
            bulkDeleteMetrics.averageTime <= 200.0
        )
    }
    
    @Test
    fun `validate database concurrent access performance`() = runTest {
        val dataset = PerformanceTestData.generateLargeNoteDataset(500)
        mockDatabase.insertNotesBatch(dataset)
        
        // Simulate concurrent read/write operations
        val concurrentOperationsMetrics = PerformanceUtils.measurePerformance(iterations = 20) {
            mockDatabase.performConcurrentOperations()
        }
        
        println("Concurrent Operations Performance:")
        println("  Average time: ${concurrentOperationsMetrics.averageTime}ms")
        println("  P95 time: ${concurrentOperationsMetrics.p95Time}ms")
        println("  Max time: ${concurrentOperationsMetrics.maxTime}ms")
        
        assertTrue(
            "Concurrent operations average time (${concurrentOperationsMetrics.averageTime}ms) exceeds threshold (300ms)",
            concurrentOperationsMetrics.averageTime <= 300.0
        )
        
        assertTrue(
            "Concurrent operations P95 time (${concurrentOperationsMetrics.p95Time}ms) exceeds threshold (500ms)",
            concurrentOperationsMetrics.p95Time <= 500L
        )
    }
    
    @Test
    fun `validate database memory usage during operations`() = runTest {
        val initialMemory = getCurrentMemoryUsage()
        
        // Load large dataset
        val largeDataset = PerformanceTestData.generateLargeNoteDataset(1000)
        val memoryAfterLoad = PerformanceUtils.measureMemoryUsage {
            mockDatabase.insertNotesBatch(largeDataset)
        }
        
        // Perform various operations
        val memoryDuringOperations = PerformanceUtils.measureMemoryUsage {
            mockDatabase.getAllNotes()
            mockDatabase.searchNotes("test")
            mockDatabase.getNotesPaginated(0, 100)
            mockDatabase.updateNote(largeDataset.first().copy(content = "Updated"))
        }
        
        println("Database Memory Usage:")
        println("  Memory for loading ${largeDataset.size} notes: ${PerformanceUtils.bytesToMB(memoryAfterLoad)}MB")
        println("  Memory during operations: ${PerformanceUtils.bytesToMB(memoryDuringOperations)}MB")
        
        assertTrue(
            "Database loading memory usage (${PerformanceUtils.bytesToMB(memoryAfterLoad)}MB) exceeds threshold (50MB)",
            PerformanceUtils.bytesToMB(memoryAfterLoad) <= 50.0
        )
        
        assertTrue(
            "Database operations memory usage (${PerformanceUtils.bytesToMB(memoryDuringOperations)}MB) exceeds threshold (20MB)",
            PerformanceUtils.bytesToMB(memoryDuringOperations) <= 20.0
        )
    }
    
    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}

/**
 * Mock database for testing database performance
 */
class MockDatabase {
    private val notes = mutableListOf<MockNote>()
    private val tasks = mutableListOf<MockTask>()
    private val indices = mutableMapOf<String, MutableSet<String>>() // Simple indexing simulation
    
    fun initialize() {
        notes.clear()
        tasks.clear()
        indices.clear()
        buildIndices()
    }
    
    fun cleanup() {
        notes.clear()
        tasks.clear()
        indices.clear()
    }
    
    fun insertNotesBatch(notesList: List<MockNote>) {
        // Simulate batch insertion with transaction
        simulateDatabaseWork(notesList.size * 2)
        notes.addAll(notesList)
        updateIndices(notesList)
    }
    
    fun insertTasksBatch(tasksList: List<MockTask>) {
        simulateDatabaseWork(tasksList.size * 2)
        tasks.addAll(tasksList)
    }
    
    fun getAllNotes(): List<MockNote> {
        simulateDatabaseWork(notes.size / 10)
        return notes.toList()
    }
    
    fun getRecentNotes(limit: Int): List<MockNote> {
        simulateDatabaseWork(limit / 5)
        return notes.sortedByDescending { it.timestamp }.take(limit)
    }
    
    fun searchNotes(query: String): List<MockNote> {
        simulateDatabaseWork(notes.size / 20) // Simulate index lookup
        return notes.filter { 
            it.content.contains(query, ignoreCase = true) ||
            it.transcribedText.contains(query, ignoreCase = true)
        }
    }
    
    fun getNotesByTag(tag: String): List<MockNote> {
        simulateDatabaseWork(notes.size / 30) // Simulate index lookup
        return notes.filter { it.tags.contains(tag) }
    }
    
    fun getNotesPaginated(page: Int, pageSize: Int): List<MockNote> {
        simulateDatabaseWork(pageSize / 5)
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, notes.size)
        return if (startIndex < notes.size) {
            notes.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }
    
    fun getAllTasks(): List<MockTask> {
        simulateDatabaseWork(tasks.size / 10)
        return tasks.toList()
    }
    
    fun getPendingTasks(): List<MockTask> {
        simulateDatabaseWork(tasks.size / 15)
        return tasks.filter { !it.isCompleted }
    }
    
    fun getTasksByNote(noteId: String): List<MockTask> {
        simulateDatabaseWork(tasks.size / 20)
        return tasks.filter { it.sourceNoteId == noteId }
    }
    
    fun getTasksWithNotes(): List<Pair<MockTask, MockNote?>> {
        simulateDatabaseWork(tasks.size / 5) // More complex join operation
        return tasks.map { task ->
            val note = task.sourceNoteId?.let { noteId ->
                notes.find { it.id == noteId }
            }
            task to note
        }
    }
    
    fun updateNote(note: MockNote) {
        simulateDatabaseWork(5)
        val index = notes.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            notes[index] = note
        }
    }
    
    fun updateNotesBatch(notesList: List<MockNote>) {
        simulateDatabaseWork(notesList.size * 3)
        notesList.forEach { note ->
            val index = notes.indexOfFirst { it.id == note.id }
            if (index >= 0) {
                notes[index] = note
            }
        }
    }
    
    fun deleteNote(noteId: String) {
        simulateDatabaseWork(3)
        notes.removeAll { it.id == noteId }
    }
    
    fun deleteNotesBatch(noteIds: List<String>) {
        simulateDatabaseWork(noteIds.size * 2)
        notes.removeAll { it.id in noteIds }
    }
    
    fun complexSearch(query: String): List<MockNote> {
        simulateDatabaseWork(notes.size / 10) // More complex search
        // Simplified complex search simulation
        return when {
            query.contains("AND") -> {
                val terms = query.split(" AND ")
                notes.filter { note ->
                    terms.all { term -> 
                        note.content.contains(term.trim(), ignoreCase = true) 
                    }
                }
            }
            query.contains("OR") -> {
                val terms = query.split(" OR ")
                notes.filter { note ->
                    terms.any { term -> 
                        note.content.contains(term.trim(), ignoreCase = true) 
                    }
                }
            }
            query.contains("NOT") -> {
                val parts = query.split(" NOT ")
                val includeTerm = parts[0].trim()
                val excludeTerm = parts.getOrNull(1)?.trim() ?: ""
                notes.filter { note ->
                    note.content.contains(includeTerm, ignoreCase = true) &&
                    !note.content.contains(excludeTerm, ignoreCase = true)
                }
            }
            else -> searchNotes(query)
        }
    }
    
    fun performConcurrentOperations() {
        // Simulate concurrent read/write operations
        simulateDatabaseWork(50)
        
        // Simulate multiple operations happening concurrently
        getAllNotes()
        searchNotes("concurrent")
        updateNote(notes.firstOrNull() ?: return)
        getPendingTasks()
    }
    
    private fun buildIndices() {
        // Simulate building database indices
        simulateDatabaseWork(100)
    }
    
    private fun updateIndices(notesList: List<MockNote>) {
        // Simulate updating indices for new notes
        simulateDatabaseWork(notesList.size)
    }
    
    private fun simulateDatabaseWork(complexity: Int) {
        // Simulate database I/O and processing work
        var result = 0.0
        repeat(complexity * 10) { i ->
            result += kotlin.math.sin(i.toDouble()) * kotlin.math.cos(i.toDouble())
        }
        // Prevent optimization
        if (result > Double.MAX_VALUE) println(result)
    }
}