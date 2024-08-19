package com.offmind.runtimeshaders.shaders

import android.graphics.RuntimeShader
import com.offmind.runtimeshaders.generated.ShaderDependencyMap
import com.offmind.runtimeshaders.generated.ShaderFunction

class Shader(private val shaderCode: String) {
    private val allFunctions = ShaderDependencyMap.functions

    private fun getRawShader(uniforms: List<Uniform> = basicUniformList): String {
        val resolvedFunctions = mutableSetOf<ShaderFunction>()

        for (function in allFunctions.keys) {
            resolveDependencies(function, resolvedFunctions, mutableSetOf())
        }

        val functionsCode = resolvedFunctions.map { allFunctions[it] }.joinToString("\n\n")

        return uniformsToString(uniforms) + functionsCode + shaderCode
    }

    fun getRuntimeShader(uniforms: List<Uniform> = basicUniformList): RuntimeShader {
        return RuntimeShader(getRawShader(uniforms))
    }

    private fun uniformsToString(uniforms: List<Uniform>): String {
        return uniforms.joinToString(separator = "") {
            "uniform ${it.type.value} ${it.name};\n"
        }
    }

    private fun resolveDependencies(
        function: ShaderFunction,
        resolved: MutableSet<ShaderFunction>,
        unresolved: MutableSet<ShaderFunction>
    ): List<ShaderFunction> {
        unresolved.add(function)

        val dependencies = ShaderDependencyMap.dependencies[function] ?: emptyList()

        for (dependency in dependencies) {
            if (!resolved.contains(dependency)) {
                if (unresolved.contains(dependency)) {
                    throw IllegalArgumentException("Circular dependency detected: ${function.value} -> $dependency")
                }
                resolveDependencies(dependency, resolved, unresolved)
            }
        }

        unresolved.remove(function)
        resolved.add(function)

        return resolved.toList()
    }
}

data class Uniform(
    val type: Type,
    val name: String
) {
    enum class Type(val value: String) {
        FLOAT("float"),
        VEC2("vec2"),
        VEC3("vec3"),
        VEC4("vec4"),
        MAT2("mat2"),
        MAT3("mat3"),
        MAT4("mat4"),
        SHADER("shader"),
    }
}

/**
 * A list of the most common uniforms used in almost every shader.
 *
 * This list includes:
 * - `image`: A shader uniform of type `SHADER`.
 * - `resolution`: A shader uniform of type `VEC2`.
 * - `time`: A shader uniform of type `FLOAT`.
 * - `percentage`: A shader uniform of type `FLOAT`. This uniform is used to control the progress of an animation. Normally would be between 0 and 1
 *
 * You can remove any of these uniforms if needed by using the extension function `removeUniform`.
 * Additionally, new uniforms can be added to this list for a specific shader by calling the `addUniform` extension function.
 */
val basicUniformList = listOf(
    Uniform(Uniform.Type.SHADER, "image"),
    Uniform(Uniform.Type.VEC2, "resolution"),
    Uniform(Uniform.Type.FLOAT, "time"),
    Uniform(Uniform.Type.FLOAT, "percentage")
)

fun List<Uniform>.removeUniform(name: String): List<Uniform> {
    return this.filter { it.name != name }
}

fun List<Uniform>.addUniform(uniform: Pair<Uniform.Type, String>): List<Uniform> {
    assert(this.none { it.name == uniform.second }) { "Uniform with name ${uniform.second} already exists" }
    return this + Uniform(uniform.first, uniform.second)
}
