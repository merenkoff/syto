// Root build script. All versions live in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 compiles Kotlin itself (built-in Kotlin) and must not have kotlin-android applied to a module.
    // Declaring the plugin here with `apply false` only pins the Kotlin Gradle Plugin version on the
    // build classpath, so AGP's built-in Kotlin and the Compose compiler plugin agree on one Kotlin version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
