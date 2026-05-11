package com.offmind.runtimeshaders.gl.geometry

object CubeGeometry {
    fun createGlassCube(): GlMesh = GlMesh(
        vertices = createCubeVertices(),
        vertexLayout = VertexLayout()
    )

    private fun createCubeVertices(): FloatArray {
        val vertices = mutableListOf<Float>()

        fun addFace(normal: FloatArray, color: FloatArray, corners: Array<FloatArray>) {
            val indices = intArrayOf(0, 1, 2, 0, 2, 3)
            indices.forEach { index ->
                vertices += corners[index].toList()
                vertices += normal.toList()
                vertices += color.toList()
            }
        }

        addFace(
            normal = floatArrayOf(0f, 0f, 1f),
            color = floatArrayOf(0.72f, 0.9f, 1f, 1f),
            corners = arrayOf(
                floatArrayOf(-1f, -1f, 1f),
                floatArrayOf(1f, -1f, 1f),
                floatArrayOf(1f, 1f, 1f),
                floatArrayOf(-1f, 1f, 1f)
            )
        )
        addFace(
            normal = floatArrayOf(0f, 0f, -1f),
            color = floatArrayOf(0.50f, 0.72f, 0.86f, 1f),
            corners = arrayOf(
                floatArrayOf(1f, -1f, -1f),
                floatArrayOf(-1f, -1f, -1f),
                floatArrayOf(-1f, 1f, -1f),
                floatArrayOf(1f, 1f, -1f)
            )
        )
        addFace(
            normal = floatArrayOf(-1f, 0f, 0f),
            color = floatArrayOf(0.62f, 0.86f, 0.92f, 1f),
            corners = arrayOf(
                floatArrayOf(-1f, -1f, -1f),
                floatArrayOf(-1f, -1f, 1f),
                floatArrayOf(-1f, 1f, 1f),
                floatArrayOf(-1f, 1f, -1f)
            )
        )
        addFace(
            normal = floatArrayOf(1f, 0f, 0f),
            color = floatArrayOf(0.86f, 0.94f, 1f, 1f),
            corners = arrayOf(
                floatArrayOf(1f, -1f, 1f),
                floatArrayOf(1f, -1f, -1f),
                floatArrayOf(1f, 1f, -1f),
                floatArrayOf(1f, 1f, 1f)
            )
        )
        addFace(
            normal = floatArrayOf(0f, 1f, 0f),
            color = floatArrayOf(0.78f, 0.88f, 1f, 1f),
            corners = arrayOf(
                floatArrayOf(-1f, 1f, 1f),
                floatArrayOf(1f, 1f, 1f),
                floatArrayOf(1f, 1f, -1f),
                floatArrayOf(-1f, 1f, -1f)
            )
        )
        addFace(
            normal = floatArrayOf(0f, -1f, 0f),
            color = floatArrayOf(0.45f, 0.68f, 0.82f, 1f),
            corners = arrayOf(
                floatArrayOf(-1f, -1f, -1f),
                floatArrayOf(1f, -1f, -1f),
                floatArrayOf(1f, -1f, 1f),
                floatArrayOf(-1f, -1f, 1f)
            )
        )

        return vertices.toFloatArray()
    }
}
