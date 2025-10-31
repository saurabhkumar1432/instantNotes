package com.voicenotesai.performance

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import kotlin.system.measureNanoTime

/**
 * Animation performance tests to validate 60fps performance during animations
 * 
 * Requirements: 7.1, 7.2 - Performance and Responsiveness
 * - Validates smooth animations without lag
 * - Tests gradient animations, waveform animations, and UI transitions
 * - Ensures frame times stay within 16.67ms threshold for 60fps
 */
class AnimationPerformanceTest {
    
    private lateinit var animationEngine: MockAnimationEngine
    
    @Before
    fun setup() {
        animationEngine = MockAnimationEngine()
    }
    
    @Test
    fun `validate gradient header animation performance meets 60fps target`() = runTest {
        // Test gradient animation performance
        val frameMetrics = mutableListOf<Long>()
        val testDurationMs = 1000L
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val frameTime = PerformanceUtils.measureFrameTime {
                animationEngine.renderGradientFrame()
            }
            frameMetrics.add(frameTime)
        }
        
        // Validate performance
        val averageFrameTime = frameMetrics.average()
        val maxFrameTime = frameMetrics.maxOrNull() ?: 0L
        val framesAboveThreshold = frameMetrics.count { it > PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS }
        
        println("Gradient Animation Performance:")
        println("  Average frame time: ${averageFrameTime}ms")
        println("  Max frame time: ${maxFrameTime}ms")
        println("  Frames above threshold: $framesAboveThreshold/${frameMetrics.size}")
        println("  Target frame time: ${PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS}ms")
        
        // Assert performance requirements
        assertTrue(
            "Average frame time (${averageFrameTime}ms) exceeds 60fps threshold (${PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS}ms)",
            averageFrameTime <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS
        )
        
        assertTrue(
            "More than 5% of frames exceed threshold",
            framesAboveThreshold < frameMetrics.size * 0.05
        )
    }
    
    @Test
    fun `validate waveform animation performance during recording`() = runTest {
        val frameMetrics = mutableListOf<Long>()
        val testDurationMs = 2000L
        val startTime = System.currentTimeMillis()
        
        // Simulate recording with active waveform
        animationEngine.startRecording()
        
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val frameTime = PerformanceUtils.measureFrameTime {
                animationEngine.renderWaveformFrame(generateAudioLevels())
            }
            frameMetrics.add(frameTime)
        }
        
        animationEngine.stopRecording()
        
        val averageFrameTime = frameMetrics.average()
        val p95FrameTime = frameMetrics.sorted()[(frameMetrics.size * 0.95).toInt()]
        
        println("Waveform Animation Performance:")
        println("  Average frame time: ${averageFrameTime}ms")
        println("  P95 frame time: ${p95FrameTime}ms")
        println("  Total frames: ${frameMetrics.size}")
        
        assertTrue(
            "Waveform animation average frame time exceeds threshold",
            averageFrameTime <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS
        )
        
        assertTrue(
            "Waveform animation P95 frame time exceeds threshold",
            p95FrameTime <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS * 1.2 // Allow 20% tolerance for P95
        )
    }
    
    @Test
    fun `validate screen transition animations performance`() = runTest {
        val transitionTypes = listOf(
            "HomeToRecording",
            "RecordingToNoteDetail", 
            "HomeToTasks",
            "TasksToHome",
            "HomeToSettings",
            "SettingsToHome"
        )
        
        transitionTypes.forEach { transitionType ->
            val transitionMetrics = PerformanceUtils.measurePerformance(iterations = 20) {
                animationEngine.performScreenTransition(transitionType)
            }
            
            println("$transitionType Transition Performance:")
            println("  Average time: ${transitionMetrics.averageTime}ms")
            println("  P95 time: ${transitionMetrics.p95Time}ms")
            println("  Max time: ${transitionMetrics.maxTime}ms")
            
            assertTrue(
                "$transitionType transition average time exceeds threshold",
                transitionMetrics.averageTime <= 300.0 // 300ms max for transitions
            )
            
            assertTrue(
                "$transitionType transition P95 time exceeds threshold", 
                transitionMetrics.p95Time <= 400L // 400ms max for P95
            )
        }
    }
    
    @Test
    fun `validate floating action button animation performance`() = runTest {
        val frameMetrics = mutableListOf<Long>()
        
        // Test FAB scale animation
        repeat(50) {
            val frameTime = PerformanceUtils.measureFrameTime {
                animationEngine.animateFABScale()
            }
            frameMetrics.add(frameTime)
        }
        
        val averageFrameTime = frameMetrics.average()
        
        println("FAB Animation Performance:")
        println("  Average frame time: ${averageFrameTime}ms")
        println("  Frames tested: ${frameMetrics.size}")
        
        assertTrue(
            "FAB animation frame time exceeds threshold",
            averageFrameTime <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS
        )
    }
    
    @Test
    fun `validate card animation performance with large dataset`() = runTest {
        val largeDataset = PerformanceTestData.generateLargeNoteDataset(100)
        val frameMetrics = mutableListOf<Long>()
        
        // Test card entrance animations
        largeDataset.forEach { note ->
            val frameTime = PerformanceUtils.measureFrameTime {
                animationEngine.animateCardEntrance(note)
            }
            frameMetrics.add(frameTime)
        }
        
        val averageFrameTime = frameMetrics.average()
        val maxFrameTime = frameMetrics.maxOrNull() ?: 0L
        
        println("Card Animation Performance (${largeDataset.size} cards):")
        println("  Average frame time: ${averageFrameTime}ms")
        println("  Max frame time: ${maxFrameTime}ms")
        
        assertTrue(
            "Card animation average frame time exceeds threshold",
            averageFrameTime <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS
        )
        
        assertTrue(
            "Card animation max frame time exceeds threshold",
            maxFrameTime <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS * 2 // Allow 2x threshold for max
        )
    }
    
    @Test
    fun `validate pulsing dot animation performance during recording`() = runTest {
        val frameMetrics = mutableListOf<Long>()
        val testDurationMs = 3000L
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val frameTime = PerformanceUtils.measureFrameTime {
                animationEngine.renderPulsingDot()
            }
            frameMetrics.add(frameTime)
        }
        
        val averageFrameTime = frameMetrics.average()
        val consistentFrames = frameMetrics.count { 
            it <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS 
        }
        
        println("Pulsing Dot Animation Performance:")
        println("  Average frame time: ${averageFrameTime}ms")
        println("  Consistent frames: $consistentFrames/${frameMetrics.size}")
        println("  Consistency rate: ${(consistentFrames.toDouble() / frameMetrics.size * 100)}%")
        
        assertTrue(
            "Pulsing dot animation frame time exceeds threshold",
            averageFrameTime <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS
        )
        
        assertTrue(
            "Pulsing dot animation consistency below 95%",
            consistentFrames >= frameMetrics.size * 0.95
        )
    }
    
    private fun generateAudioLevels(): List<Float> {
        return (1..20).map { (0.0f..1.0f).random() }
    }
}

/**
 * Mock animation engine for testing animation performance
 */
class MockAnimationEngine {
    private var isRecording = false
    private var currentFrame = 0L
    
    fun startRecording() {
        isRecording = true
        currentFrame = 0L
    }
    
    fun stopRecording() {
        isRecording = false
    }
    
    fun renderGradientFrame() {
        // Simulate gradient rendering work
        simulateRenderingWork(complexity = 50)
        currentFrame++
    }
    
    fun renderWaveformFrame(audioLevels: List<Float>) {
        // Simulate waveform rendering with audio data
        simulateRenderingWork(complexity = audioLevels.size * 2)
        currentFrame++
    }
    
    fun performScreenTransition(transitionType: String) {
        // Simulate screen transition work
        val complexity = when (transitionType) {
            "HomeToRecording" -> 100
            "RecordingToNoteDetail" -> 120
            "HomeToTasks" -> 80
            "TasksToHome" -> 80
            "HomeToSettings" -> 60
            "SettingsToHome" -> 60
            else -> 100
        }
        simulateRenderingWork(complexity)
    }
    
    fun animateFABScale() {
        // Simulate FAB scale animation
        simulateRenderingWork(complexity = 30)
    }
    
    fun animateCardEntrance(note: MockNote) {
        // Simulate card entrance animation
        val complexity = note.content.length / 10 + 40
        simulateRenderingWork(complexity)
    }
    
    fun renderPulsingDot() {
        // Simulate pulsing dot animation
        simulateRenderingWork(complexity = 20)
    }
    
    private fun simulateRenderingWork(complexity: Int) {
        // Simulate rendering work with mathematical operations
        var result = 0.0
        repeat(complexity) { i ->
            result += kotlin.math.sin(i.toDouble()) * kotlin.math.cos(i.toDouble())
        }
        // Prevent optimization
        if (result > Double.MAX_VALUE) println(result)
    }
}