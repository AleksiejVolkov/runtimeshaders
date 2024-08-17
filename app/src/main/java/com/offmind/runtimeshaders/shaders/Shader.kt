package com.offmind.runtimeshaders.shaders

class Shader(private val shaderCode: String) {
    private val functions = """
        $GetViewTextureFunction
        $ChromaticAberrationFunction
        $CircleSDFFunction
""".trimIndent()

    fun getRawShader(uniforms: List<Uniform> = basicUniformList): String {
        return uniformsToString(uniforms) + functions + shaderCode
    }

    private fun uniformsToString(uniforms: List<Uniform>): String {
        return uniforms.joinToString(separator = "") {
            "uniform ${it.type.value} ${it.name};\n"
        }
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
