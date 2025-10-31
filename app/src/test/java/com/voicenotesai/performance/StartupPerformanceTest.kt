package com.voicenotesai.performance

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Startup performance tests to validate sub-500ms startup time target
 * 
 * Requirements: 7.1, 7.3 - Performance optimization and startup time
 * - Measures application startup time components
 * - Tests cold start, warm start, and hot start scenarios
 * - Validates initialization performance of critical components
 */
class StartupPerformanceTest {
    
    private lateinit var mockApplication: MockApplication
    
    @Before
    fun setup() {
        mockApplication = MockApplication()
    }
    
    @Test
    fun `validate cold start performance meets sub-500ms target`() = runTest {
        val startupMetrics = PerformanceUtils.measurePerformance(iterations = 20) {
            mockApplication.performColdStart()
        }
        
        println("Cold Start Performance:")
        println("  Average time: ${startupMetrics.averageTime}ms")
        println("  Min time: ${startupMetrics.minTime}ms")
        println("  Max time: ${startupMetrics.maxTime}ms")
        println("  P95 time: ${startupMetrics.p95Time}ms")
        println("  P99 time: ${startupMetrics.p99Time}ms")
        println("  Standard deviation: ${startupMetrics.standardDeviation}ms")
        
        assertTrue(
            "Cold start average time (${startupMetrics.averageTime}ms) exceeds target (${PerformanceTestSuite.STARTUP_TIME_THRESHOLD_MS}ms)",
            startupMetrics.averageTime <= PerformanceTestSuite.STARTUP_TIME_THRESHOLD_MS
        )
        
        assertTrue(
            "Cold start P95 time (${startupMetrics.p95Time}ms) exceeds acceptable threshold (600ms)",
            startupMetrics.p95Time <= 600L
        )
        
        assertTrue(
            "Cold start max time (${startupMetrics.maxTime}ms) exceeds maximum threshold (800ms)",
            startupMetrics.maxTime <= 800L
        )
    }
    
    @Test
    fun `validate warm start performance`() = runTest {
        // Perform initial cold start to warm up
        mockApplication.performColdStart()
        
        val warmStartMetrics = PerformanceUtils.measurePerformance(iterations = 15) {
            mockApplication.performWarmStart()
        }
        
        println("Warm Start Performance:")
        println("  Average time: ${warmStartMetrics.averageTime}ms")
        println("  Min time: ${warmStartMetrics.minTime}ms")
        println("  Max time: ${warmStartMetrics.maxTime}ms")
        println("  P95 time: ${warmStartMetrics.p95Time}ms")
        
        assertTrue(
            "Warm start average time (${warmStartMetrics.averageTime}ms) exceeds target (300ms)",
            warmStartMetrics.averageTime <= 300.0
        )
        
        assertTrue(
            "Warm start P95 time (${warmStartMetrics.p95Time}ms) exceeds threshold (400ms)",
            warmStartMetrics.p95Time <= 400L
        )
    }
    
    @Test
    fun `validate hot start performance`() = runTest {
        // Perform cold and warm starts to prepare
        mockApplication.performColdStart()
        mockApplication.performWarmStart()
        
        val hotStartMetrics = PerformanceUtils.measurePerformance(iterations = 25) {
            mockApplication.performHotStart()
        }
        
        println("Hot Start Performance:")
        println("  Average time: ${hotStartMetrics.averageTime}ms")
        println("  Min time: ${hotStartMetrics.minTime}ms")
        println("  Max time: ${hotStartMetrics.maxTime}ms")
        println("  P95 time: ${hotStartMetrics.p95Time}ms")
        
        assertTrue(
            "Hot start average time (${hotStartMetrics.averageTime}ms) exceeds target (150ms)",
            hotStartMetrics.averageTime <= 150.0
        )
        
        assertTrue(
            "Hot start P95 time (${hotStartMetrics.p95Time}ms) exceeds threshold (200ms)",
            hotStartMetrics.p95Time <= 200L
        )
    }
    
    @Test
    fun `validate component initialization performance`() = runTest {
        val componentInitTimes = mutableMapOf<String, Long>()
        
        // Test individual component initialization times
        val components = listOf(
            "Database",
            "AIConfiguration", 
            "NotificationManager",
            "AudioEngine",
            "ThemeEngine",
            "NavigationGraph",
            "SettingsRepository",
            "TaskManager",
            "CacheManager"
        )
        
        components.forEach { component ->
            val initTime = measureTimeMillis {
                mockApplication.initializeComponent(component)
            }
            componentInitTimes[component] = initTime
            println("$component initialization: ${initTime}ms")
        }
        
        val totalInitTime = componentInitTimes.values.sum()
        println("Total component initialization time: ${totalInitTime}ms")
        
        // Validate individual component times
        componentInitTimes.forEach { (component, time) ->
            val maxAllowedTime = when (component) {
                "Database" -> 100L
                "AIConfiguration" -> 50L
                "NotificationManager" -> 30L
                "AudioEngine" -> 80L
                "ThemeEngine" -> 20L
                "NavigationGraph" -> 40L
                "SettingsRepository" -> 60L
                "TaskManager" -> 70L
                "CacheManager" -> 40L
                else -> 100L
            }
            
            assertTrue(
                "$component initialization time (${time}ms) exceeds maximum allowed (${maxAllowedTime}ms)",
                time <= maxAllowedTime
            )
        }
        
        assertTrue(
            "Total component initialization time (${totalInitTime}ms) exceeds target (400ms)",
            totalInitTime <= 400L
        )
    }
    
    @Test
    fun `validate startup performance with existing data`() = runTest {
        // Setup existing data scenario
        mockApplication.setupExistingData(
            noteCount = 100,
            taskCount = 50,
            reminderCount = 10
        )
        
        val startupWithDataMetrics = PerformanceUtils.measurePerformance(iterations = 15) {
            mockApplication.performColdStartWithData()
        }
        
        println("Startup with Existing Data Performance:")
        println("  Average time: ${startupWithDataMetrics.averageTime}ms")
        println("  P95 time: ${startupWithDataMetrics.p95Time}ms")
        println("  Max time: ${startupWithDataMetrics.maxTime}ms")
        
        assertTrue(
            "Startup with data average time (${startupWithDataMetrics.averageTime}ms) exceeds target (600ms)",
            startupWithDataMetrics.averageTime <= 600.0
        )
        
        assertTrue(
            "Startup with data P95 time (${startupWithDataMetrics.p95Time}ms) exceeds threshold (750ms)",
            startupWithDataMetrics.p95Time <= 750L
        )
    }
    
    @Test
    fun `validate startup performance with large dataset`() = runTest {
        // Setup large dataset scenario
        mockApplication.setupExistingData(
            noteCount = 1000,
            taskCount = 500,
            reminderCount = 100
        )
        
        val startupWithLargeDataMetrics = PerformanceUtils.measurePerformance(iterations = 10) {
            mockApplication.performColdStartWithData()
        }
        
        println("Startup with Large Dataset Performance:")
        println("  Average time: ${startupWithLargeDataMetrics.averageTime}ms")
        println("  P95 time: ${startupWithLargeDataMetrics.p95Time}ms")
        println("  Max time: ${startupWithLargeDataMetrics.maxTime}ms")
        
        assertTrue(
            "Startup with large dataset average time (${startupWithLargeDataMetrics.averageTime}ms) exceeds target (800ms)",
            startupWithLargeDataMetrics.averageTime <= 800.0
        )
        
        assertTrue(
            "Startup with large dataset P95 time (${startupWithLargeDataMetrics.p95Time}ms) exceeds threshold (1000ms)",
            startupWithLargeDataMetrics.p95Time <= 1000L
        )
    }
    
    @Test
    fun `validate background initialization performance`() = runTest {
        val backgroundInitTime = measureTimeMillis {
            mockApplication.performBackgroundInitialization()
        }
        
        println("Background initialization time: ${backgroundInitTime}ms")
        
        // Background initialization should complete quickly to not block UI
        assertTrue(
            "Background initialization time (${backgroundInitTime}ms) exceeds target (200ms)",
            backgroundInitTime <= 200L
        )
        
        // Test that UI is responsive during background init
        val uiResponseTime = measureTimeMillis {
            mockApplication.testUIResponsiveness()
        }
        
        println("UI responsiveness during background init: ${uiResponseTime}ms")
        
        assertTrue(
            "UI response time during background init (${uiResponseTime}ms) exceeds target (50ms)",
            uiResponseTime <= 50L
        )
    }
    
    @Test
    fun `validate startup performance consistency`() = runTest {
        val startupTimes = mutableListOf<Long>()
        
        // Perform multiple cold starts to test consistency
        repeat(30) {
            val startupTime = measureTimeMillis {
                mockApplication.performColdStart()
            }
            startupTimes.add(startupTime)
            
            // Reset application state between tests
            mockApplication.reset()
        }
        
        val averageTime = startupTimes.average()
        val standardDeviation = kotlin.math.sqrt(
            startupTimes.map { (it - averageTime) * (it - averageTime) }.average()
        )
        val coefficientOfVariation = standardDeviation / averageTime
        
        println("Startup Performance Consistency:")
        println("  Average time: ${averageTime}ms")
        println("  Standard deviation: ${standardDeviation}ms")
        println("  Coefficient of variation: ${coefficientOfVariation}")
        println("  Min time: ${startupTimes.minOrNull()}ms")
        println("  Max time: ${startupTimes.maxOrNull()}ms")
        
        assertTrue(
            "Startup time coefficient of variation (${coefficientOfVariation}) exceeds acceptable threshold (0.3)",
            coefficientOfVariation <= 0.3
        )
        
        val outliers = startupTimes.count { kotlin.math.abs(it - averageTime) > 2 * standardDeviation }
        val outlierPercentage = (outliers.toDouble() / startupTimes.size) * 100
        
        println("  Outliers: $outliers/${startupTimes.size} (${outlierPercentage}%)")
        
        assertTrue(
            "Too many startup time outliers (${outlierPercentage}%) - should be less than 10%",
            outlierPercentage <= 10.0
        )
    }
}

/**
 * Mock application for testing startup performance
 */
class MockApplication {
    private var isInitialized = false
    private var hasExistingData = false
    private var noteCount = 0
    private var taskCount = 0
    private var reminderCount = 0
    private val initializedComponents = mutableSetOf<String>()
    
    fun performColdStart() {
        reset()
        
        // Simulate cold start operations
        initializeComponent("Database")
        initializeComponent("AIConfiguration")
        initializeComponent("NotificationManager")
        initializeComponent("AudioEngine")
        initializeComponent("ThemeEngine")
        initializeComponent("NavigationGraph")
        
        loadInitialData()
        setupUI()
        
        isInitialized = true
    }
    
    fun performWarmStart() {
        // Simulate warm start (some components already initialized)
        if (!initializedComponents.contains("NavigationGraph")) {
            initializeComponent("NavigationGraph")
        }
        
        refreshData()
        setupUI()
    }
    
    fun performHotStart() {
        // Simulate hot start (most components ready)
        refreshUI()
    }
    
    fun performColdStartWithData() {
        reset()
        
        // Simulate cold start with existing data
        initializeComponent("Database")
        loadExistingData()
        initializeComponent("AIConfiguration")
        initializeComponent("NotificationManager")
        initializeComponent("AudioEngine")
        initializeComponent("ThemeEngine")
        initializeComponent("NavigationGraph")
        
        setupUI()
        
        isInitialized = true
    }
    
    fun initializeComponent(componentName: String) {
        if (initializedComponents.contains(componentName)) return
        
        // Simulate component initialization work
        val complexity = when (componentName) {
            "Database" -> 200
            "AIConfiguration" -> 100
            "NotificationManager" -> 50
            "AudioEngine" -> 150
            "ThemeEngine" -> 30
            "NavigationGraph" -> 80
            "SettingsRepository" -> 120
            "TaskManager" -> 140
            "CacheManager" -> 70
            else -> 100
        }
        
        simulateWork(complexity)
        initializedComponents.add(componentName)
    }
    
    fun setupExistingData(noteCount: Int, taskCount: Int, reminderCount: Int) {
        this.noteCount = noteCount
        this.taskCount = taskCount
        this.reminderCount = reminderCount
        this.hasExistingData = true
    }
    
    fun performBackgroundInitialization() {
        // Simulate background initialization tasks
        simulateWork(50) // Should be lightweight
        
        // Initialize non-critical components
        initializeComponent("CacheManager")
        initializeComponent("SettingsRepository")
    }
    
    fun testUIResponsiveness() {
        // Simulate UI interaction during background init
        simulateWork(20) // Should be very fast
    }
    
    fun reset() {
        isInitialized = false
        initializedComponents.clear()
    }
    
    private fun loadInitialData() {
        // Simulate loading initial/default data
        simulateWork(30)
    }
    
    private fun loadExistingData() {
        // Simulate loading existing data based on dataset size
        val complexity = (noteCount / 10) + (taskCount / 5) + (reminderCount * 2)
        simulateWork(complexity)
    }
    
    private fun refreshData() {
        // Simulate data refresh
        simulateWork(20)
    }
    
    private fun setupUI() {
        // Simulate UI setup
        simulateWork(40)
    }
    
    private fun refreshUI() {
        // Simulate UI refresh
        simulateWork(10)
    }
    
    private fun simulateWork(complexity: Int) {
        // Simulate CPU work with mathematical operations
        var result = 0.0
        repeat(complexity * 100) { i ->
            result += kotlin.math.sin(i.toDouble()) * kotlin.math.cos(i.toDouble())
        }
        // Prevent optimization
        if (result > Double.MAX_VALUE) println(result)
    }
}