import com.offmind.runtimeshaders.scripts.CREATE_DEPENDENCY_TASK_NAME
import com.offmind.runtimeshaders.scripts.registerGenerateShaderFunctionsTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.offmind.runtimeshaders"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.offmind.runtimeshaders"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        // built-in Kotlin compiles only the kotlin source set
        getByName("main").kotlin.srcDir("build/generated/src/main/java")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

registerGenerateShaderFunctionsTask(tasks)

tasks.named("preBuild") {
    dependsOn(CREATE_DEPENDENCY_TASK_NAME)
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation3:navigation3-runtime:1.0.1")
    implementation(libs.navigation3.ui)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // no longer comes with material3
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.datastore.preferences)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.haze.glass)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}
