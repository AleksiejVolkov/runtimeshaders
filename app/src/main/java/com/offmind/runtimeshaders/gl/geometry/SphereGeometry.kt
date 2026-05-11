package com.offmind.runtimeshaders.gl.geometry

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object SphereGeometry {
    fun createGlassSphere(
        latitudeSegments: Int = DEFAULT_LATITUDE_SEGMENTS,
        longitudeSegments: Int = DEFAULT_LONGITUDE_SEGMENTS
    ): GlMesh {
        require(latitudeSegments >= 3) { "latitudeSegments must be at least 3." }
        require(longitudeSegments >= 3) { "longitudeSegments must be at least 3." }

        val vertices = mutableListOf<Float>()

        for (lat in 0 until latitudeSegments) {
            val theta0 = PI.toFloat() * lat / latitudeSegments
            val theta1 = PI.toFloat() * (lat + 1) / latitudeSegments

            for (lon in 0 until longitudeSegments) {
                val phi0 = TWO_PI * lon / longitudeSegments
                val phi1 = TWO_PI * (lon + 1) / longitudeSegments

                val p00 = spherePoint(theta0, phi0)
                val p10 = spherePoint(theta1, phi0)
                val p11 = spherePoint(theta1, phi1)
                val p01 = spherePoint(theta0, phi1)

                addVertex(vertices, p00)
                addVertex(vertices, p10)
                addVertex(vertices, p11)

                addVertex(vertices, p00)
                addVertex(vertices, p11)
                addVertex(vertices, p01)
            }
        }

        return GlMesh(
            vertices = vertices.toFloatArray(),
            vertexLayout = VertexLayout()
        )
    }

    private fun spherePoint(theta: Float, phi: Float): FloatArray {
        val sinTheta = sin(theta)
        return floatArrayOf(
            sinTheta * cos(phi),
            cos(theta),
            sinTheta * sin(phi)
        )
    }

    private fun addVertex(vertices: MutableList<Float>, point: FloatArray) {
        val colorMix = (point[Y_INDEX] + 1f) * 0.5f
        val color = floatArrayOf(
            lerp(0.52f, 0.82f, colorMix),
            lerp(0.74f, 0.94f, colorMix),
            1f,
            1f
        )

        vertices += point.toList()
        vertices += point.toList()
        vertices += color.toList()
    }

    private fun lerp(start: Float, end: Float, amount: Float): Float {
        return start + (end - start) * amount
    }

    private const val DEFAULT_LATITUDE_SEGMENTS = 32
    private const val DEFAULT_LONGITUDE_SEGMENTS = 48
    private const val TWO_PI = (PI * 2.0).toFloat()
    private const val Y_INDEX = 1
}
