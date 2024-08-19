import com.offmind.runtimeshaders.*
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.offmind.runtimeshaders"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.offmind.runtimeshaders"
        minSdk = 33
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
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

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.register("generateShaderDependencyMap") {
    doLast {
        fun extractDependencies(functionName: String, functionCode: String, functionNames: List<String>): List<String> {
            val dependencies = mutableListOf<String>()
            for (name in functionNames) {
                if (name != functionName) { // Exclude the function itself
                    val regex = Regex("\\b$name\\b")
                    if (regex.containsMatchIn(functionCode)) {
                        dependencies.add(name)
                    }
                }
            }
            return dependencies
        }

        fun generateDependencyMap(functions: Map<String, String>): Map<String, List<String>> {
            val functionNames = functions.keys.toList()
            val dependencyMap = mutableMapOf<String, List<String>>()

            for ((functionName, functionCode) in functions) {
                val dependencies = extractDependencies(functionName, functionCode, functionNames)
                dependencyMap[functionName] = dependencies
            }

            return dependencyMap
        }

        val functionDependencies = generateDependencyMap(allFunctions)

        val kotlinCode = """
            package com.offmind.runtimeshaders.generated
            
            enum class ShaderFunction(val value: String) {
                ${allFunctions.keys.joinToString(",\n") { "${it.uppercase(Locale.getDefault())}(\"$it\")" }}
            }

            object ShaderDependencyMap {
                val dependencies: Map<ShaderFunction, List<ShaderFunction>> = mapOf(
                    ${functionDependencies.entries.joinToString(",\n") {
            "ShaderFunction.${it.key.uppercase(Locale.getDefault())} to listOf(${it.value.joinToString(", ") { "ShaderFunction.${it.uppercase(Locale.getDefault())}" }})"
        }}
                )
                
                val functions: Map<ShaderFunction, String> = mapOf(
                    ${allFunctions.entries.joinToString(",\n") {
            "ShaderFunction.${it.key.uppercase(Locale.getDefault())} to \"\"\"${it.value}\"\"\""
        }}
                )
            }
        """.trimIndent()

        val outputDir = File("${project.buildDir}/generated/src/main/java/com/offmind/runtimeshaders/generated")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "ShaderDependencyMap.kt")
        outputFile.writeText(kotlinCode)

        println("Kotlin file generated at ${outputFile.absolutePath}")
    }
}

tasks.named("preBuild") {
    dependsOn("generateShaderDependencyMap")
}
