package com.offmind.runtimeshaders.gl.geometry

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GlMesh(
    vertices: FloatArray,
    val vertexLayout: VertexLayout,
    val drawMode: Int = GLES20.GL_TRIANGLES
) {
    val vertexCount: Int = vertices.size / vertexLayout.floatsPerVertex

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    fun bindPosition(attributeHandle: Int) {
        bindAttribute(
            attributeHandle = attributeHandle,
            componentCount = vertexLayout.positionComponents,
            offsetFloats = vertexLayout.positionOffsetFloats
        )
    }

    fun bindColor(attributeHandle: Int) {
        bindAttribute(
            attributeHandle = attributeHandle,
            componentCount = vertexLayout.colorComponents,
            offsetFloats = vertexLayout.colorOffsetFloats
        )
    }

    fun draw() {
        GLES20.glDrawArrays(drawMode, 0, vertexCount)
    }

    private fun bindAttribute(
        attributeHandle: Int,
        componentCount: Int,
        offsetFloats: Int
    ) {
        vertexBuffer.position(offsetFloats)
        GLES20.glVertexAttribPointer(
            attributeHandle,
            componentCount,
            GLES20.GL_FLOAT,
            false,
            vertexLayout.strideBytes,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(attributeHandle)
    }
}
