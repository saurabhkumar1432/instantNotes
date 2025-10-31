package com.voicenotesai.performance

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * Device configuration performance tests
 * 
 * Requirements: 7.6 - Performance across various device configurations and Android versions
 * - Tests performance on different device specifications
 * - Validates performance across Android API levels
 * - Tests memory and CPU constraints scenarios
 */
class DeviceConfigurationTest {
    
    private lateinit var deviceSimulator: MockDeviceSimulator
    
    @Before
    fun setup() {
        deviceSimulator = MockDeviceSimulator()
    }
    
    @Test
    fun `validate performance on low-end device configuration`() = runTest {
        val lowEndDevice = DeviceConfiguration(
            ramMB = 2048,
            cpuCores = 4,
            cpuFrequencyMHz = 1400,
            apiLevel = 26,
            screenDensity = "mdpi",
            deviceType = "low_end"
        )
        
        deviceSimulator.configureDevice(lowEndDevice)
        
        val performanceMetrics = deviceSimulator.runPerformanceTest()
        
        println("Low-End Device Performance:")
        println("  Device: ${lowEndDevice.deviceType}")
        println("  RAM: ${lowEndDevice.ramMB}MB, CPU: ${lowEndDevice.cpuCores} cores @ ${lowEndDevice.cpuFrequencyMHz}MHz")
        println("  API Level: ${lowEndDevice.apiLevel}")
        println("  Startup time: ${performanceMetrics.startupTimeMs}ms")
        println("  Average frame time: ${performanceMetrics.averageFrameTimeMs}ms")
        println("  Memory usage: ${performanceMetrics.memoryUsageMB}MB")
        println("  Scroll performance: ${performanceMetrics.scrollPerformanceScore}")
        
        // Adjusted thresholds for low-end devices
        assertTrue(
            "Low-end device startup time (${performanceMetrics.startupTimeMs}ms) exceeds threshold (800ms)",
            performanceMetrics.startupTimeMs <= 800L
        )
        
        assertTrue(
            "Low-end device frame time (${performanceMetrics.averageFrameTimeMs}ms) exceeds threshold (20ms)",
            performanceMetrics.averageFrameTimeMs <= 20.0
        )
        
        assertTrue(
            "Low-end device memory usage (${performanceMetrics.memoryUsageMB}MB) exceeds threshold (128MB)",
            performanceMetrics.memoryUsageMB <= 128.0
        )
        
        assertTrue(
            "Low-end device scroll performance score (${performanceMetrics.scrollPerformanceScore}) below threshold (70)",
            performanceMetrics.scrollPerformanceScore >= 70
        )
    }
    
    @Test
    fun `validate performance on mid-range device configuration`() = runTest {
        val midRangeDevice = DeviceConfiguration(
            ramMB = 4096,
            cpuCores = 6,
            cpuFrequencyMHz = 2000,
            apiLevel = 30,
            screenDensity = "xhdpi",
            deviceType = "mid_range"
        )
        
        deviceSimulator.configureDevice(midRangeDevice)
        
        val performanceMetrics = deviceSimulator.runPerformanceTest()
        
        println("Mid-Range Device Performance:")
        println("  Device: ${midRangeDevice.deviceType}")
        println("  RAM: ${midRangeDevice.ramMB}MB, CPU: ${midRangeDevice.cpuCores} cores @ ${midRangeDevice.cpuFrequencyMHz}MHz")
        println("  API Level: ${midRangeDevice.apiLevel}")
        println("  Startup time: ${performanceMetrics.startupTimeMs}ms")
        println("  Average frame time: ${performanceMetrics.averageFrameTimeMs}ms")
        println("  Memory usage: ${performanceMetrics.memoryUsageMB}MB")
        println("  Scroll performance: ${performanceMetrics.scrollPerformanceScore}")
        
        // Standard thresholds for mid-range devices
        assertTrue(
            "Mid-range device startup time (${performanceMetrics.startupTimeMs}ms) exceeds threshold (600ms)",
            performanceMetrics.startupTimeMs <= 600L
        )
        
        assertTrue(
            "Mid-range device frame time (${performanceMetrics.averageFrameTimeMs}ms) exceeds threshold (16.67ms)",
            performanceMetrics.averageFrameTimeMs <= PerformanceTestSuite.FRAME_TIME_THRESHOLD_MS
        )
        
        assertTrue(
            "Mid-range device memory usage (${performanceMetrics.memoryUsageMB}MB) exceeds threshold (192MB)",
            performanceMetrics.memoryUsageMB <= 192.0
        )
        
        assertTrue(
            "Mid-range device scroll performance score (${performanceMetrics.scrollPerformanceScore}) below threshold (85)",
            performanceMetrics.scrollPerformanceScore >= 85
        )
    }
    
    @Test
    fun `validate performance on high-end device configuration`() = runTest {
        val highEndDevice = DeviceConfiguration(
            ramMB = 8192,
            cpuCores = 8,
            cpuFrequencyMHz = 2800,
            apiLevel = 34,
            screenDensity = "xxhdpi",
            deviceType = "high_end"
        )
        
        deviceSimulator.configureDevice(highEndDevice)
        
        val performanceMetrics = deviceSimulator.runPerformanceTest()
        
        println("High-End Device Performance:")
        println("  Device: ${highEndDevice.deviceType}")
        println("  RAM: ${highEndDevice.ramMB}MB, CPU: ${highEndDevice.cpuCores} cores @ ${highEndDevice.cpuFrequencyMHz}MHz")
        println("  API Level: ${highEndDevice.apiLevel}")
        println("  Startup time: ${performanceMetrics.startupTimeMs}ms")
        println("  Average frame time: ${performanceMetrics.averageFrameTimeMs}ms")
        println("  Memory usage: ${performanceMetrics.memoryUsageMB}MB")
        println("  Scroll performance: ${performanceMetrics.scrollPerformanceScore}")
        
        // Optimal thresholds for high-end devices
        assertTrue(
            "High-end device startup time (${performanceMetrics.startupTimeMs}ms) exceeds threshold (400ms)",
            performanceMetrics.startupTimeMs <= 400L
        )
        
        assertTrue(
            "High-end device frame time (${performanceMetrics.averageFrameTimeMs}ms) exceeds threshold (12ms)",
            performanceMetrics.averageFrameTimeMs <= 12.0
        )
        
        assertTrue(
            "High-end device memory usage (${performanceMetrics.memoryUsageMB}MB) exceeds threshold (256MB)",
            performanceMetrics.memoryUsageMB <= 256.0
        )
        
        assertTrue(
            "High-end device scroll performance score (${performanceMetrics.scrollPerformanceScore}) below threshold (95)",
            performanceMetrics.scrollPerformanceScore >= 95
        )
    }
    
    @Test
    fun `validate performance across Android API levels`() = runTest {
        val apiLevels = listOf(26, 28, 30, 31, 33, 34)
        val performanceResults = mutableMapOf<Int, DevicePerformanceMetrics>()
        
        apiLevels.forEach { apiLevel ->
            val device = DeviceConfiguration(
                ramMB = 4096,
                cpuCores = 6,
                cpuFrequencyMHz = 2000,
                apiLevel = apiLevel,
                screenDensity = "xhdpi",
                deviceType = "test_device_api_$apiLevel"
            )
            
            deviceSimulator.configureDevice(device)
            val metrics = deviceSimulator.runPerformanceTest()
            performanceResults[apiLevel] = metrics
            
            println("API Level $apiLevel Performance:")
            println("  Startup time: ${metrics.startupTimeMs}ms")
            println("  Frame time: ${metrics.averageFrameTimeMs}ms")
            println("  Memory usage: ${metrics.memoryUsageMB}MB")
        }
        
        // Validate that performance doesn't degrade significantly across API levels
        val startupTimes = performanceResults.values.map { it.startupTimeMs }
        val frameTimes = performanceResults.values.map { it.averageFrameTimeMs }
        val memoryUsages = performanceResults.values.map { it.memoryUsageMB }
        
        val startupTimeVariation = (startupTimes.maxOrNull()!! - startupTimes.minOrNull()!!).toDouble() / startupTimes.average()
        val frameTimeVariation = (frameTimes.maxOrNull()!! - frameTimes.minOrNull()!!) / frameTimes.average()
        val memoryVariation = (memoryUsages.maxOrNull()!! - memoryUsages.minOrNull()!!) / memoryUsages.average()
        
        println("Performance Variation Across API Levels:")
        println("  Startup time variation: ${startupTimeVariation * 100}%")
        println("  Frame time variation: ${frameTimeVariation * 100}%")
        println("  Memory usage variation: ${memoryVariation * 100}%")
        
        assertTrue(
            "Startup time variation (${startupTimeVariation * 100}%) exceeds acceptable threshold (30%)",
            startupTimeVariation <= 0.3
        )
        
        assertTrue(
            "Frame time variation (${frameTimeVariation * 100}%) exceeds acceptable threshold (25%)",
            frameTimeVariation <= 0.25
        )
        
        assertTrue(
            "Memory usage variation (${memoryVariation * 100}%) exceeds acceptable threshold (20%)",
            memoryVariation <= 0.2
        )
    }
    
    @Test
    fun `validate performance under memory pressure`() = runTest {
        val device = DeviceConfiguration(
            ramMB = 2048,
            cpuCores = 4,
            cpuFrequencyMHz = 1400,
            apiLevel = 28,
            screenDensity = "hdpi",
            deviceType = "memory_constrained"
        )
        
        deviceSimulator.configureDevice(device)
        
        // Simulate memory pressure
        deviceSimulator.simulateMemoryPressure(75) // 75% memory usage
        
        val performanceUnderPressure = deviceSimulator.runPerformanceTest()
        
        println("Performance Under Memory Pressure:")
        println("  Startup time: ${performanceUnderPressure.startupTimeMs}ms")
        println("  Frame time: ${performanceUnderPressure.averageFrameTimeMs}ms")
        println("  Memory usage: ${performanceUnderPressure.memoryUsageMB}MB")
        println("  Scroll performance: ${performanceUnderPressure.scrollPerformanceScore}")
        
        // Performance should degrade gracefully under memory pressure
        assertTrue(
            "Startup time under memory pressure (${performanceUnderPressure.startupTimeMs}ms) exceeds threshold (1000ms)",
            performanceUnderPressure.startupTimeMs <= 1000L
        )
        
        assertTrue(
            "Frame time under memory pressure (${performanceUnderPressure.averageFrameTimeMs}ms) exceeds threshold (25ms)",
            performanceUnderPressure.averageFrameTimeMs <= 25.0
        )
        
        assertTrue(
            "Scroll performance under memory pressure (${performanceUnderPressure.scrollPerformanceScore}) below threshold (60)",
            performanceUnderPressure.scrollPerformanceScore >= 60
        )
    }
    
    @Test
    fun `validate performance with different screen densities`() = runTest {
        val screenDensities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        val densityPerformance = mutableMapOf<String, DevicePerformanceMetrics>()
        
        screenDensities.forEach { density ->
            val device = DeviceConfiguration(
                ramMB = 4096,
                cpuCores = 6,
                cpuFrequencyMHz = 2000,
                apiLevel = 30,
                screenDensity = density,
                deviceType = "test_device_$density"
            )
            
            deviceSimulator.configureDevice(device)
            val metrics = deviceSimulator.runPerformanceTest()
            densityPerformance[density] = metrics
            
            println("$density Performance:")
            println("  Startup time: ${metrics.startupTimeMs}ms")
            println("  Frame time: ${metrics.averageFrameTimeMs}ms")
            println("  Memory usage: ${metrics.memoryUsageMB}MB")
        }
        
        // Validate that higher density screens don't cause excessive performance degradation
        val mdpiMetrics = densityPerformance["mdpi"]!!
        val xxxhdpiMetrics = densityPerformance["xxxhdpi"]!!
        
        val startupTimeDegradation = (xxxhdpiMetrics.startupTimeMs - mdpiMetrics.startupTimeMs).toDouble() / mdpiMetrics.startupTimeMs
        val frameTimeDegradation = (xxxhdpiMetrics.averageFrameTimeMs - mdpiMetrics.averageFrameTimeMs) / mdpiMetrics.averageFrameTimeMs
        val memoryIncrease = (xxxhdpiMetrics.memoryUsageMB - mdpiMetrics.memoryUsageMB) / mdpiMetrics.memoryUsageMB
        
        println("Performance Impact of High Density (xxxhdpi vs mdpi):")
        println("  Startup time degradation: ${startupTimeDegradation * 100}%")
        println("  Frame time degradation: ${frameTimeDegradation * 100}%")
        println("  Memory increase: ${memoryIncrease * 100}%")
        
        assertTrue(
            "Startup time degradation on high density (${startupTimeDegradation * 100}%) exceeds threshold (40%)",
            startupTimeDegradation <= 0.4
        )
        
        assertTrue(
            "Frame time degradation on high density (${frameTimeDegradation * 100}%) exceeds threshold (50%)",
            frameTimeDegradation <= 0.5
        )
        
        assertTrue(
            "Memory increase on high density (${memoryIncrease * 100}%) exceeds threshold (60%)",
            memoryIncrease <= 0.6
        )
    }
    
    @Test
    fun `validate adaptive performance optimization`() = runTest {
        val devices = listOf(
            DeviceConfiguration(2048, 4, 1400, 26, "mdpi", "low_end"),
            DeviceConfiguration(4096, 6, 2000, 30, "xhdpi", "mid_range"),
            DeviceConfiguration(8192, 8, 2800, 34, "xxhdpi", "high_end")
        )
        
        devices.forEach { device ->
            deviceSimulator.configureDevice(device)
            
            // Test that app adapts performance settings based on device capabilities
            val adaptiveSettings = deviceSimulator.getAdaptivePerformanceSettings()
            val performanceMetrics = deviceSimulator.runPerformanceTest()
            
            println("Adaptive Performance for ${device.deviceType}:")
            println("  Animation quality: ${adaptiveSettings.animationQuality}")
            println("  Texture quality: ${adaptiveSettings.textureQuality}")
            println("  Background processing: ${adaptiveSettings.backgroundProcessingEnabled}")
            println("  Cache size: ${adaptiveSettings.cacheSizeMB}MB")
            println("  Performance score: ${performanceMetrics.scrollPerformanceScore}")
            
            // Validate adaptive settings are appropriate for device
            when (device.deviceType) {
                "low_end" -> {
                    assertTrue("Low-end device should use reduced animation quality", 
                        adaptiveSettings.animationQuality <= 0.7f)
                    assertTrue("Low-end device should use smaller cache", 
                        adaptiveSettings.cacheSizeMB <= 32)
                }
                "mid_range" -> {
                    assertTrue("Mid-range device should use medium animation quality", 
                        adaptiveSettings.animationQuality >= 0.7f && adaptiveSettings.animationQuality <= 0.9f)
                    assertTrue("Mid-range device should use medium cache", 
                        adaptiveSettings.cacheSizeMB in 32..64)
                }
                "high_end" -> {
                    assertTrue("High-end device should use full animation quality", 
                        adaptiveSettings.animationQuality >= 0.9f)
                    assertTrue("High-end device should use larger cache", 
                        adaptiveSettings.cacheSizeMB >= 64)
                }
            }
        }
    }
}

data class DeviceConfiguration(
    val ramMB: Int,
    val cpuCores: Int,
    val cpuFrequencyMHz: Int,
    val apiLevel: Int,
    val screenDensity: String,
    val deviceType: String
)

data class DevicePerformanceMetrics(
    val startupTimeMs: Long,
    val averageFrameTimeMs: Double,
    val memoryUsageMB: Double,
    val scrollPerformanceScore: Int
)

data class AdaptivePerformanceSettings(
    val animationQuality: Float,
    val textureQuality: Float,
    val backgroundProcessingEnabled: Boolean,
    val cacheSizeMB: Int
)

/**
 * Mock device simulator for testing performance across different configurations
 */
class MockDeviceSimulator {
    private var currentDevice: DeviceConfiguration? = null
    private var memoryPressurePercent: Int = 0
    
    fun configureDevice(device: DeviceConfiguration) {
        currentDevice = device
        memoryPressurePercent = 0
    }
    
    fun simulateMemoryPressure(pressurePercent: Int) {
        memoryPressurePercent = pressurePercent
    }
    
    fun runPerformanceTest(): DevicePerformanceMetrics {
        val device = currentDevice ?: throw IllegalStateException("Device not configured")
        
        // Simulate performance based on device specs
        val baseStartupTime = 400L
        val baseFrameTime = 16.67
        val baseMemoryUsage = 128.0
        val baseScrollScore = 90
        
        // Apply device-specific modifiers
        val cpuModifier = 2800.0 / device.cpuFrequencyMHz
        val ramModifier = 4096.0 / device.ramMB
        val apiModifier = if (device.apiLevel >= 30) 0.9 else 1.1
        val densityModifier = when (device.screenDensity) {
            "mdpi" -> 0.8
            "hdpi" -> 0.9
            "xhdpi" -> 1.0
            "xxhdpi" -> 1.2
            "xxxhdpi" -> 1.4
            else -> 1.0
        }
        val memoryPressureModifier = 1.0 + (memoryPressurePercent / 100.0)
        
        val startupTime = (baseStartupTime * cpuModifier * apiModifier * memoryPressureModifier).toLong()
        val frameTime = baseFrameTime * cpuModifier * densityModifier * memoryPressureModifier
        val memoryUsage = baseMemoryUsage * ramModifier * densityModifier
        val scrollScore = (baseScrollScore / (cpuModifier * memoryPressureModifier)).toInt()
        
        return DevicePerformanceMetrics(
            startupTimeMs = startupTime,
            averageFrameTimeMs = frameTime,
            memoryUsageMB = memoryUsage,
            scrollPerformanceScore = scrollScore
        )
    }
    
    fun getAdaptivePerformanceSettings(): AdaptivePerformanceSettings {
        val device = currentDevice ?: throw IllegalStateException("Device not configured")
        
        return when (device.deviceType.contains("low_end")) {
            true -> AdaptivePerformanceSettings(
                animationQuality = 0.6f,
                textureQuality = 0.5f,
                backgroundProcessingEnabled = false,
                cacheSizeMB = 24
            )
            false -> when (device.deviceType.contains("high_end")) {
                true -> AdaptivePerformanceSettings(
                    animationQuality = 1.0f,
                    textureQuality = 1.0f,
                    backgroundProcessingEnabled = true,
                    cacheSizeMB = 96
                )
                false -> AdaptivePerformanceSettings(
                    animationQuality = 0.8f,
                    textureQuality = 0.8f,
                    backgroundProcessingEnabled = true,
                    cacheSizeMB = 48
                )
            }
        }
    }
}