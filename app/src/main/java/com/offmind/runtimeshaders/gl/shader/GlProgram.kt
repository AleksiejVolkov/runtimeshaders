package com.offmind.runtimeshaders.gl.shader

import android.opengl.GLES20

class GlProgram(
    vertexShaderSource: String,
    fragmentShaderSource: String
) {
    val id: Int = createProgram(vertexShaderSource, fragmentShaderSource)

    fun use() {
        GLES20.glUseProgram(id)
    }

    fun getAttribute(name: String): Int {
        val handle = GLES20.glGetAttribLocation(id, name)
        check(handle >= 0) { "Missing GL attribute: $name" }
        return handle
    }

    fun setFloat(name: String, value: Float) {
        GLES20.glUniform1f(getUniform(name), value)
    }

    fun setInt(name: String, value: Int) {
        GLES20.glUniform1i(getUniform(name), value)
    }

    fun setVec2(name: String, value1: Float, value2: Float) {
        GLES20.glUniform2f(getUniform(name), value1, value2)
    }

    fun setMat4(name: String, value: FloatArray) {
        GLES20.glUniformMatrix4fv(getUniform(name), 1, false, value, 0)
    }

    fun release() {
        if (id != 0) {
            GLES20.glDeleteProgram(id)
        }
    }

    private fun getUniform(name: String): Int {
        val handle = GLES20.glGetUniformLocation(id, name)
        check(handle >= 0) { "Missing GL uniform: $name" }
        return handle
    }

    private fun createProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            error("Program link failed: $log")
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Shader compile failed: $log")
        }
        return shader
    }
}
