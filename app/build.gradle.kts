plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

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

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
