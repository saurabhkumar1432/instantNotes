package com.voicenotesai

import android.app.Application
// Temporarily disabled - performance modules removed
// import com.voicenotesai.presentation.performance.MemoryOptimizer
// import com.voicenotesai.presentation.performance.PerformanceOptimizer
// import com.voicenotesai.presentation.performance.SixtyFpsAnimationSystem
// import com.voicenotesai.presentation.performance.StartupOptimizer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VoiceNotesApplication : Application() {
    
    // Temporarily disabled - performance modules removed
    // @Inject
    // lateinit var performanceOptimizer: PerformanceOptimizer
    
    // @Inject
    // lateinit var memoryOptimizer: MemoryOptimizer
    
    // @Inject
    // lateinit var startupOptimizer: StartupOptimizer
    
    // @Inject
    // lateinit var animationSystem: SixtyFpsAnimationSystem
    
    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        
        // Performance optimizations temporarily disabled
        // applicationScope.launch {
        //     initializePerformanceOptimizations()
        // }
    }
    
    // Temporarily disabled - performance modules removed
    /*
    private suspend fun initializePerformanceOptimizations() {
        try {
            startupOptimizer.initialize()
            performanceOptimizer.initialize()
            memoryOptimizer.initialize()
            animationSystem.initialize()
            println("Performance optimizations initialized successfully")
        } catch (e: Exception) {
            println("Failed to initialize performance optimizations: ${e.message}")
        }
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        memoryOptimizer.onTrimMemory(level)
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        memoryOptimizer.onLowMemory()
    }
    */
}
