package com.offmind.runtimeshaders.gl.geometry

object CubeGeometry {
    fun createCalmColorCube(): GlMesh = GlMesh(
        vertices = CUBE_VERTICES,
        vertexLayout = VertexLayout()
    )

    private val CUBE_VERTICES = floatArrayOf(
        // Front, muted coral
        -1f, -1f, 1f, 0.74f, 0.42f, 0.38f, 1f,
        1f, -1f, 1f, 0.74f, 0.42f, 0.38f, 1f,
        1f, 1f, 1f, 0.74f, 0.42f, 0.38f, 1f,
        -1f, -1f, 1f, 0.74f, 0.42f, 0.38f, 1f,
        1f, 1f, 1f, 0.74f, 0.42f, 0.38f, 1f,
        -1f, 1f, 1f, 0.74f, 0.42f, 0.38f, 1f,
        // Back, sage
        1f, -1f, -1f, 0.44f, 0.62f, 0.49f, 1f,
        -1f, -1f, -1f, 0.44f, 0.62f, 0.49f, 1f,
        -1f, 1f, -1f, 0.44f, 0.62f, 0.49f, 1f,
        1f, -1f, -1f, 0.44f, 0.62f, 0.49f, 1f,
        -1f, 1f, -1f, 0.44f, 0.62f, 0.49f, 1f,
        1f, 1f, -1f, 0.44f, 0.62f, 0.49f, 1f,
        // Left, desaturated blue
        -1f, -1f, -1f, 0.42f, 0.55f, 0.72f, 1f,
        -1f, -1f, 1f, 0.42f, 0.55f, 0.72f, 1f,
        -1f, 1f, 1f, 0.42f, 0.55f, 0.72f, 1f,
        -1f, -1f, -1f, 0.42f, 0.55f, 0.72f, 1f,
        -1f, 1f, 1f, 0.42f, 0.55f, 0.72f, 1f,
        -1f, 1f, -1f, 0.42f, 0.55f, 0.72f, 1f,
        // Right, ochre
        1f, -1f, 1f, 0.76f, 0.64f, 0.36f, 1f,
        1f, -1f, -1f, 0.76f, 0.64f, 0.36f, 1f,
        1f, 1f, -1f, 0.76f, 0.64f, 0.36f, 1f,
        1f, -1f, 1f, 0.76f, 0.64f, 0.36f, 1f,
        1f, 1f, -1f, 0.76f, 0.64f, 0.36f, 1f,
        1f, 1f, 1f, 0.76f, 0.64f, 0.36f, 1f,
        // Top, dusty lavender
        -1f, 1f, 1f, 0.61f, 0.50f, 0.72f, 1f,
        1f, 1f, 1f, 0.61f, 0.50f, 0.72f, 1f,
        1f, 1f, -1f, 0.61f, 0.50f, 0.72f, 1f,
        -1f, 1f, 1f, 0.61f, 0.50f, 0.72f, 1f,
        1f, 1f, -1f, 0.61f, 0.50f, 0.72f, 1f,
        -1f, 1f, -1f, 0.61f, 0.50f, 0.72f, 1f,
        // Bottom, soft teal
        -1f, -1f, -1f, 0.36f, 0.65f, 0.66f, 1f,
        1f, -1f, -1f, 0.36f, 0.65f, 0.66f, 1f,
        1f, -1f, 1f, 0.36f, 0.65f, 0.66f, 1f,
        -1f, -1f, -1f, 0.36f, 0.65f, 0.66f, 1f,
        1f, -1f, 1f, 0.36f, 0.65f, 0.66f, 1f,
        -1f, -1f, 1f, 0.36f, 0.65f, 0.66f, 1f
    )
}
