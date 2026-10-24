// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val kotlin = "2.4.21"
    // AGP 9 compiles Kotlin itself (built-in Kotlin), so there is no kotlin-android plugin
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version kotlin apply false
    id("org.jetbrains.kotlin.plugin.compose") version kotlin apply false
}
