package com.voicenotesai.performance

import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.system.measureTimeMillis
import kotlin.system.measureNanoTime

/**
 * Comprehensive performance test suite for Voice Notes AI application
 * 
 * Tests cover:
 * - 60fps animation performance validation
 * - Memory usage with large datasets (1000+ notes, 500+ tasks)
 * - Startup time optimization (target: sub-500ms)
 * - Performance across various device configurations
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.6
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AnimationPerformanceTest::class,
    MemoryUsageTest::class,
    StartupPerformanceTest::class,
    DeviceConfigurationTest::class,
    ScrollingPerformanceTest::class,
    DatabasePerformanceTest::class
)
class PerformanceTestSuite {
    
    companion object {
        // Performance thresholds
        const val TARGET_FPS = 60
        const val FRAME_TIME_THRESHOLD_MS = 16.67 // 1000ms / 60fps
        const val STARTUP_TIME_THRESHOLD_MS = 500
        const val MAX_MEMORY_USAGE_MB = 256
        const val LARGE_DATASET_NOTES = 1000
        const val LARGE_DATASET_TASKS = 500
        
        // Test configuration
        const val PERFORMANCE_TEST_ITERATIONS = 10
        const val ANIMATION_TEST_DURATION_MS = 5000L
        const val SCROLL_TEST_ITEMS = 1000
    }
}

/**
 * Performance measurement utilities
 */
object PerformanceUtils {
    
    data class PerformanceMetrics(
        val averageTime: Double,
        val minTime: Long,
        val maxTime: Long,
        val standardDeviation: Double,
        val p95Time: Long,
        val p99Time: Long
    )
    
    /**
     * Measures execution time of a block multiple times and returns statistics
     */
    inline fun measurePerformance(
        iterations: Int = PerformanceTestSuite.PERFORMANCE_TEST_ITERATIONS,
        crossinline block: () -> Unit
    ): PerformanceMetrics {
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureTimeMillis { block() }
            times.add(time)
        }
        
        times.sort()
        val average = times.average()
        val variance = times.map { (it - average) * (it - average) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        return PerformanceMetrics(
            averageTime = average,
            minTime = times.first(),
            maxTime = times.last(),
            standardDeviation = standardDeviation,
            p95Time = times[(times.size * 0.95).toInt()],
            p99Time = times[(times.size * 0.99).toInt()]
        )
    }
    
    /**
     * Measures memory usage before and after an operation
     */
    inline fun measureMemoryUsage(crossinline block: () -> Unit): Long {
        System.gc()
        Thread.sleep(100) // Allow GC to complete
        
        val runtime = Runtime.getRuntime()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        block()
        
        System.gc()
        Thread.sleep(100)
        
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        return memoryAfter - memoryBefore
    }
    
    /**
     * Simulates frame rendering time measurement
     */
    fun measureFrameTime(renderingBlock: () -> Unit): Long {
        return measureNanoTime { renderingBlock() } / 1_000_000 // Convert to milliseconds
    }
    
    /**
     * Validates if performance meets 60fps target
     */
    fun validateFrameRate(frameTimeMs: Double): Boolean {
        return frameTimeMs <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS
    }
    
    /**
     * Converts bytes to megabytes
     */
    fun bytesToMB(bytes: Long): Double = bytes / (1024.0 * 1024.0)
}

/**
 * Mock data generators for performance testing
 */
object PerformanceTestData {
    
    fun generateLargeNoteDataset(count: Int): List<MockNote> {
        return (1..count).map { index ->
            MockNote(
                id = "note_$index",
                content = "This is a test note with content for performance testing. " +
                        "It contains multiple sentences to simulate real note content. " +
                        "The content is long enough to test rendering performance. " +
                        "Note number: $index",
                transcribedText = "Transcribed version of note $index with additional text",
                timestamp = System.currentTimeMillis() - (index * 60000),
                duration = (30000..300000).random().toLong(),
                tags = listOf("tag${index % 5}", "category${index % 3}"),
                hasActionItems = index % 3 == 0
            )
        }
    }
    
    fun generateLargeTaskDataset(count: Int): List<MockTask> {
        return (1..count).map { index ->
            MockTask(
                id = "task_$index",
                text = "Task $index: Complete this important action item",
                isCompleted = index % 4 == 0,
                sourceNoteId = if (index % 2 == 0) "note_${index / 2}" else null,
                createdAt = System.currentTimeMillis() - (index * 30000),
                priority = MockTaskPriority.values()[index % MockTaskPriority.values().size]
            )
        }
    }
}

// Mock data classes for testing
data class MockNote(
    val id: String,
    val content: String,
    val transcribedText: String,
    val timestamp: Long,
    val duration: Long,
    val tags: List<String>,
    val hasActionItems: Boolean
)

data class MockTask(
    val id: String,
    val text: String,
    val isCompleted: Boolean,
    val sourceNoteId: String?,
    val createdAt: Long,
    val priority: MockTaskPriority
)

enum class MockTaskPriority {
    LOW, NORMAL, HIGH, URGENT
}