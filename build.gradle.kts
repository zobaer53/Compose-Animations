// Top-level build file. Versions live in gradle/libs.versions.toml.
buildscript {
    dependencies {
        // Use this Kotlin version for AGP's built-in Kotlin instead of the one AGP bundles.
        classpath(libs.kotlin.gradle.plugin)
    }
}

// Load the plugins once here so the convention plugins in build-logic can apply them by id.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
