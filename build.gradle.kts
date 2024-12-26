// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val kotlin = "2.1.0"
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version kotlin apply false
    id("org.jetbrains.kotlin.plugin.serialization") version kotlin apply false
//    id("org.jetbrains.kotlin.plugin.compose") version kotlin apply false
}
