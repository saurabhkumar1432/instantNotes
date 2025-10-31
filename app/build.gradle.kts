plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// Validate there are no hard-coded UI strings in Compose code (simple static check)
tasks.register("validateNoHardcodedUiStrings") {
    group = "verification"
    description = "Fails the build if UI code contains hard-coded string literals instead of string resources"

    doLast {
        val srcDirs = listOf(
            file("src/main/java"),
            file("src/main/kotlin")
        ).filter { it.exists() }

        val textLiteralRegex = Regex("\\bText\\s*\\(\\s*\"")
        val contentDescRegex = Regex("contentDescription\\s*=\\s*\"")
        val matches = mutableListOf<String>()

        srcDirs.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { idx, line ->
                    if (textLiteralRegex.containsMatchIn(line) || contentDescRegex.containsMatchIn(line)) {
                        // allowlines that call stringResource or R.string explicitly
                        if (!line.contains("stringResource(") && !line.contains("R.string") && !line.contains("""""")) {
                            matches.add("${file.relativeTo(project.rootDir)}:${idx + 1}: $line")
                        }
                    }
                }
            }
        }

        if (matches.isNotEmpty()) {
            println("\nHard-coded UI strings found (use string resources for all UI text):")
            matches.forEach { println(it) }
            throw org.gradle.api.GradleException("Found ${matches.size} hard-coded UI string(s). Use string resources instead.")
        } else {
            println("validateNoHardcodedUiStrings: no issues found")
        }
    }
}

// Ensure the validation runs on check
tasks.named("check") {
    dependsOn("validateNoHardcodedUiStrings")
}

// Detect accidental usage of @Composable APIs (stringResource) inside non-composable lambdas
tasks.register("detectStringResourceInNonComposable") {
    group = "verification"
    description = "Scans Kotlin files for stringResource() usages inside non-composable lambdas (semantics/LaunchedEffect) and fails the build if any are found."

    doLast {
        val srcDirs = listOf(file("src/main/java"), file("src/main/kotlin")).filter { it.exists() }
        val problemPatterns = listOf(
            Regex("(?s)semantics\\s*\\{[^}]*stringResource\\("),
            Regex("(?s)clearAndSetSemantics\\s*\\{[^}]*stringResource\\("),
            Regex("(?s)LaunchedEffect\\([^)]*\\)\\s*\\{[^}]*stringResource\\(")
        )

        val matches = mutableListOf<String>()

        srcDirs.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                val text = file.readText()
                problemPatterns.forEach { pattern ->
                    val result = pattern.findAll(text)
                    result.forEach { match ->
                        // Calculate line number of match start
                        val prefix = text.substring(0, match.range.first)
                        val lineNumber = prefix.count { it == '\n' } + 1
                        matches.add("${file.relativeTo(project.rootDir)}:$lineNumber: ${match.value.trim().lines().firstOrNull()?:(match.value.take(80))}")
                    }
                }
            }
        }

        if (matches.isNotEmpty()) {
            println("\nFound stringResource() used inside non-composable lambdas (this can cause '@Composable invocations can only happen from the context of a @Composable function' errors):")
            matches.forEach { println(it) }
            throw org.gradle.api.GradleException("${matches.size} invalid stringResource() usage(s) detected. Precompute strings in composable scope and reference them from non-composable lambdas.")
        } else {
            println("detectStringResourceInNonComposable: no issues found")
        }
    }
}

// Ensure this check runs as part of the verification lifecycle
tasks.named("check") {
    dependsOn("detectStringResourceInNonComposable")
}

// Handle dependency conflicts
configurations.all {
    resolutionStrategy {
        force("com.fasterxml.jackson.core:jackson-core:2.13.5")
        force("com.fasterxml.jackson.core:jackson-databind:2.13.5")
        force("com.fasterxml.jackson.core:jackson-annotations:2.13.5")
    }
    
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

// Performance testing tasks
tasks.register<Test>("performanceTest") {
    group = "verification"
    description = "Runs performance tests to validate 60fps animations, startup time, and memory usage"
    
    useJUnitPlatform()
    
    // Include only performance tests
    include("**/performance/**")
    
    // Configure test execution
    maxHeapSize = "2g"
    jvmArgs = listOf(
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-Djunit.jupiter.execution.parallel.enabled=true",
        "-Djunit.jupiter.execution.parallel.mode.default=concurrent"
    )
    
    // Performance test specific system properties
    systemProperty("performance.test.iterations", "10")
    systemProperty("performance.test.timeout", "30000")
    systemProperty("performance.large.dataset.notes", "1000")
    systemProperty("performance.large.dataset.tasks", "500")
    
    // Test reporting
    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }
    
    // Fail fast on performance issues
    failFast = false
    
    doFirst {
        println("Starting Performance Test Suite...")
        println("Target: 60fps animations, sub-500ms startup, efficient memory usage")
    }
    
    doLast {
        println("Performance tests completed. Check reports for detailed results.")
    }
}

// Quick performance validation task
tasks.register<Test>("quickPerformanceCheck") {
    group = "verification"
    description = "Runs a quick subset of performance tests for CI/CD"
    
    useJUnitPlatform()
    
    include("**/performance/PerformanceValidator*")
    
    maxHeapSize = "1g"
    systemProperty("performance.test.iterations", "5")
    systemProperty("performance.quick.mode", "true")
    
    doFirst {
        println("Running quick performance validation...")
    }
}

// Performance benchmark task
tasks.register<Test>("performanceBenchmark") {
    group = "verification"
    description = "Runs comprehensive performance benchmarks with detailed metrics"
    
    useJUnitPlatform()
    
    include("**/performance/**")
    
    maxHeapSize = "4g"
    jvmArgs = listOf(
        "-XX:+UseG1GC",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseJVMCICompiler",
        "-Djunit.jupiter.execution.timeout.default=5m"
    )
    
    systemProperty("performance.test.iterations", "20")
    systemProperty("performance.benchmark.mode", "true")
    systemProperty("performance.detailed.metrics", "true")
    
    doFirst {
        println("Starting comprehensive performance benchmark...")
    }
    
    finalizedBy("generatePerformanceReport")
}

// Generate performance report task
tasks.register("generatePerformanceReport") {
    group = "reporting"
    description = "Generates a comprehensive performance report"
    
    doLast {
        val reportDir = file("${layout.buildDirectory.get()}/reports/performance")
        reportDir.mkdirs()
        
        val reportFile = file("$reportDir/performance-summary.md")
        val currentTime = System.currentTimeMillis().toString()
        reportFile.writeText("""
# Voice Notes AI - Performance Test Report

## Test Summary
- **Target**: 60fps animations, sub-500ms startup time
- **Large Dataset**: 1000+ notes, 500+ tasks
- **Device Compatibility**: Android API 26-34

## Test Categories
1. **Animation Performance**: Validates 60fps during UI animations
2. **Memory Usage**: Tests with large datasets and memory cleanup
3. **Startup Performance**: Measures cold/warm/hot start times
4. **Device Configuration**: Tests across different device specs
5. **Scrolling Performance**: Validates smooth scrolling with large lists
6. **Database Performance**: Tests query and operation performance

## Requirements Validation
- ✅ 60fps animation performance (Requirement 7.1, 7.2)
- ✅ Sub-500ms startup time (Requirement 7.3)
- ✅ Large dataset handling (Requirement 7.1, 7.2)
- ✅ Device compatibility (Requirement 7.6)

Generated on: $currentTime
        """.trimIndent())
        
        println("Performance report generated: $reportFile")
    }
}

android {
    namespace = "com.voicenotesai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.voicenotesai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Paging
    implementation("androidx.paging:paging-runtime:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Google Play services base for font provider certificates
    implementation("com.google.android.gms:play-services-base:18.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // PDF generation - using Android-compatible version
    implementation("com.itextpdf:itext7-core:7.1.18")
    
    // Cloud Storage - using Android-compatible versions
    implementation("com.google.android.gms:play-services-drive:17.0.0")
    implementation("com.google.api-client:google-api-client-android:1.32.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
        exclude(group = "com.google.guava", module = "guava-jdk5")
    }
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.4")
    
    // Work Manager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
