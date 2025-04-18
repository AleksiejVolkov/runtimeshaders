import com.offmind.runtimeshaders.scripts.CREATE_DEPENDENCY_TASK_NAME
import com.offmind.runtimeshaders.scripts.registerGenerateShaderFunctionsTask

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.offmind.runtimeshaders"
    compileSdk = 35

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
        getByName("main").java.srcDirs("build/generated/src/main/java")
    }
}

registerGenerateShaderFunctionsTask(tasks)

tasks.named("preBuild") {
    dependsOn(CREATE_DEPENDENCY_TASK_NAME)
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation(platform("androidx.compose:compose-bom:2025.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation(platform("io.insert-koin:koin-bom:4.0.3"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-android:4.0.3")
    implementation("io.insert-koin:koin-compose:4.0.3")
    implementation("io.insert-koin:koin-compose-viewmodel:4.0.3")
    implementation("io.insert-koin:koin-compose-viewmodel-navigation:4.0.3")
    testImplementation(kotlin("test"))
}