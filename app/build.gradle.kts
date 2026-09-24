plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Build identity shown on the main screen.
// CI (GitHub Actions) exports GITHUB_SHA and GITHUB_RUN_NUMBER; local builds ask git and report "local".
val gitSha: Provider<String> = providers.environmentVariable("GITHUB_SHA")
    .map { it.take(7) }
    .orElse(
        providers.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } },
    )
val buildNumber: Provider<String> = providers.environmentVariable("GITHUB_RUN_NUMBER").orElse("local")

android {
    namespace = "com.ownnet.syto"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ownnet.syto"
        minSdk = 29
        targetSdk = 37
        // versionCode stays 1 during development so any build (CI or local) installs over any other
        // without uninstalling. The commit hash below is the real build identity. See D-010.
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "GIT_SHA", "\"${gitSha.get()}\"")
        buildConfigField("String", "BUILD_NUMBER", "\"${buildNumber.get()}\"")
    }

    signingConfigs {
        // Committed debug keystore: CI and every developer machine produce the same debug signature,
        // so APKs from either source update each other on the phone. Not a release key. See D-009.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Kotlin jvmTarget follows targetCompatibility with AGP built-in Kotlin.
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
