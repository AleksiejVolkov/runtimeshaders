package com.offmind.runtimeshaders.scripts

import com.offmind.runtimeshaders.functions.allColorCorrectionFunctions
import com.offmind.runtimeshaders.functions.allCommonFunctions
import com.offmind.runtimeshaders.functions.allEasingFunctions
import com.offmind.runtimeshaders.functions.allNoisesFunctions
import com.offmind.runtimeshaders.functions.allSdfFunctions
import com.offmind.runtimeshaders.functions.allTransformationsFunctions
import org.gradle.api.Task
import java.io.File
import java.util.Locale

fun createDependenciesScript(task: Task) {
    fun extractDependencies(
        functionName: String,
        functionCode: String,
        functionNames: List<String>
    ): List<String> {
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

    val allFunctions = allColorCorrectionFunctions +
            allSdfFunctions +
            allEasingFunctions +
            allNoisesFunctions +
            allCommonFunctions +
            allTransformationsFunctions

    val functionDependencies = generateDependencyMap(allFunctions)

    val kotlinCode = """
            package com.offmind.runtimeshaders.generated
            
            enum class ShaderFunction(val value: String) {
                ${allFunctions.keys.joinToString(",\n") { "${it.uppercase(Locale.getDefault())}(\"$it\")" }}
            }

            object ShaderDependencyMap {
                val dependencies: Map<ShaderFunction, List<ShaderFunction>> = mapOf(
                    ${
        functionDependencies.entries.joinToString(",\n") {
            "ShaderFunction.${it.key.uppercase(Locale.getDefault())} to listOf(${
                it.value.joinToString(", ")
                { "ShaderFunction.${it.uppercase(Locale.getDefault())}" }
            })"
        }
    }
                )
                
                val functions: Map<ShaderFunction, String> = mapOf(
                    ${
        allFunctions.entries.joinToString(",\n") {
            "ShaderFunction.${it.key.uppercase(Locale.getDefault())} to \"\"\"${it.value}\"\"\""
        }
    }
                )
            }
        """.trimIndent()

    val outputDir =
        File("${task.project.buildDir}/generated/src/main/java/com/offmind/runtimeshaders/generated")
    outputDir.mkdirs()
    val outputFile = File(outputDir, "ShaderDependencyMap.kt")
    outputFile.writeText(kotlinCode)

    println("Shader custom methods are generated at ${outputFile.absolutePath}")
}
