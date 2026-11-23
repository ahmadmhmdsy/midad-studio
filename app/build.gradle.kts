import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.midad.studio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.midad.studio"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables { useSupportLibrary = true }

        ndk {
            // Real devices first, plus x86_64 so desktop emulators can run the app.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        resourceConfigurations += listOf("en", "ar")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    // A release job must never quietly produce an unsigned artifact: Play would reject it,
    // and an unsigned APK cannot be installed over the closed-test build.
    val releaseArtifactRequested = gradle.startParameter.taskNames.any { task ->
        val name = task.substringAfterLast(':')
        name.equals("assembleRelease", ignoreCase = true) ||
            name.equals("bundleRelease", ignoreCase = true) ||
            name.startsWith("assembleRelease", ignoreCase = true)
    }
    check(!releaseArtifactRequested || keystorePropertiesFile.exists()) {
        "keystore.properties is missing: a release build would be unsigned. " +
            "Restore the keystore credentials before building a release artifact."
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // LiteRT 2.3.2 is compiled with a newer Kotlin metadata version than the
        // project's compiler. We only consume its Java API (org.tensorflow.lite.*),
        // so the Kotlin metadata version is irrelevant for compilation.
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Apache-2.0 notice text must travel with the APK/AAB; do not strip AL2.0.
            excludes += "/META-INF/LGPL2.1"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    bundle {
        language {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    lint {
        // Media3's Transformer/Effect surface is annotated @UnstableApi on purpose; every use is
        // intentional and pinned to the declared media3 version.
        disable += "UnsafeOptInUsageError"
        abortOnError = true
        checkReleaseBuilds = true
    }

    // JVM-side Compose screenshot rendering for UI review (Robolectric native Skia).
    // Test-only: gives unit tests the merged resources and manifest of the debug variant.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Core / lifecycle / activity
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // EXIF metadata (photo orientation)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Video composition / transcoding (Apache-2.0)
    implementation("androidx.media3:media3-transformer:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    // On-device inference runtime (Apache-2.0). LiteRT 2.3.2 ships 16 KB-aligned
    // native libraries and keeps the org.tensorflow.lite.Interpreter API.
    implementation("com.google.ai.edge.litert:litert:2.3.2")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Instrumented checks run on the emulator against real MediaStore/storage.
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // JVM screenshot rendering: Robolectric renders the real Compose tree with native Skia.
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    // Test-only host activity for ComposeTestRule (debug variant manifest only).
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// JVM screenshot rendering (UI review): the render test writes PNGs into tmp/ui-render.
tasks.withType<Test>().configureEach {
    maxHeapSize = "4g"
    // Robolectric 4.16 keeps its native Skia runtime in static state; re-creating a sandbox in
    // one JVM (different @Config/GraphicsMode classes) races its fonts zip filesystem and fails
    // with FileSystemAlreadyExistsException. One JVM per test class isolates the native runtime
    // and keeps the whole suite deterministic on the command line and in CI.
    forkEvery = 1
    systemProperty(
        "midad.renderDir",
        rootProject.layout.projectDirectory.dir("tmp/ui-render").asFile.absolutePath,
    )
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
