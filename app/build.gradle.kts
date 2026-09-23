/*
 * This is the Gradle file for your app module. It defines the app's dependencies,
 * build configurations, and Android settings.
 */

plugins {
    // Apply the Android Application plugin to enable Android-specific build tasks.
    // AGP 9+ builds Kotlin support in directly, so the separate kotlin-android plugin
    // is no longer applied here (see https://kotl.in/gradle/agp-built-in-kotlin).
    id("com.android.application")

    // The Compose Compiler Gradle plugin is still required and works with AGP's built-in Kotlin.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // Configure the Android SDK versions for your app.
    namespace = "com.mod4.cool_lock"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mod4.cool_lock"
        minSdk = 24
        targetSdk = 36
        versionCode = 200
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Configure build types for different environments (e.g., release, debug).
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Configure compilation options.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Enable Jetpack Compose in your project.
    buildFeatures {
        compose = true
    }

    // Packaging options for the APK.
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// With the kotlin-android plugin removed (AGP 9's built-in Kotlin), the separate
// `kotlin { compilerOptions {} }` extension it used to provide no longer exists here.
// android.compileOptions above (Java 17) is what AGP's built-in Kotlin now uses for
// the Kotlin JVM target too — no extra block needed. If a later error shows this
// wasn't actually picked up, AGP 9's own DSL for it may need to go directly in
// android {} instead; paste that error and I'll adjust.

dependencies {

    // --- Android and Kotlin Core Dependencies ---
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    // FileProvider/ContextCompat come from androidx.core:core-ktx above; no separate core needed

    // --- Compose Dependencies ---
    // Core Compose libraries.
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // THIS IS THE NEW LINE ADDED TO FIX THE BUILD ERROR
    // This dependency contains a wider range of icons, including those you're using.
    implementation("androidx.compose.material:material-icons-extended")

    // The core material icons library, which you already had.
    implementation("androidx.compose.material:material-icons-core")

    // Additional foundation dependencies for compose
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")

    // --- Testing Dependencies ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // --- Debugging and Tooling Dependencies ---
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
