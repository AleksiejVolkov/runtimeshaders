package com.offmind.runtimeshaders.gl.geometry

object PlaneGeometry {
    data class CurvePoint(
        val x: Float,
        val y: Float,
        val z: Float
    )

    fun createTapePlane(
        width: Float = DEFAULT_WIDTH,
        height: Float = DEFAULT_HEIGHT,
        lengthSegments: Int = DEFAULT_LENGTH_SEGMENTS,
        curvePoints: List<CurvePoint> = createStraightCurve(height)
    ): GlMesh {
        require(width > 0f) { "width must be greater than 0." }
        require(height > 0f) { "height must be greater than 0." }
        require(lengthSegments >= 1) { "lengthSegments must be at least 1." }
        require(curvePoints.size == CURVE_POINT_COUNT) {
            "curvePoints must contain exactly $CURVE_POINT_COUNT points."
        }

        val vertices = mutableListOf<Float>()
        val halfWidth = width * 0.5f
        val sections = List(lengthSegments + 1) { index ->
            val amount = index.toFloat() / lengthSegments
            val center = sampleCurve(curvePoints, amount)
            val tangent = sampleTangent(curvePoints, amount)
            TapeSection(
                left = floatArrayOf(center.x - halfWidth, center.y, center.z),
                right = floatArrayOf(center.x + halfWidth, center.y, center.z),
                normal = normalFromTangent(tangent),
                color = colorForSegment(amount)
            )
        }

        for (segment in 0 until lengthSegments) {
            val start = sections[segment]
            val end = sections[segment + 1]

            addVertex(vertices, start.left, start.normal, start.color)
            addVertex(vertices, start.right, start.normal, start.color)
            addVertex(vertices, end.right, end.normal, end.color)

            addVertex(vertices, start.left, start.normal, start.color)
            addVertex(vertices, end.right, end.normal, end.color)
            addVertex(vertices, end.left, end.normal, end.color)
        }

        return GlMesh(
            vertices = vertices.toFloatArray(),
            vertexLayout = VertexLayout()
        )
    }

    private data class TapeSection(
        val left: FloatArray,
        val right: FloatArray,
        val normal: FloatArray,
        val color: FloatArray
    )

    private fun addVertex(
        vertices: MutableList<Float>,
        position: FloatArray,
        normal: FloatArray,
        color: FloatArray
    ) {
        vertices += position.toList()
        vertices += normal.toList()
        vertices += color.toList()
    }

    private fun colorForSegment(amount: Float): FloatArray {
        return floatArrayOf(
            lerp(0.58f, 0.82f, amount),
            lerp(0.78f, 0.94f, amount),
            1f,
            1f
        )
    }

    private fun sampleCurve(points: List<CurvePoint>, amount: Float): CurvePoint {
        val scaledAmount = amount.coerceIn(0f, 1f) * (points.lastIndex)
        val segmentIndex = scaledAmount.toInt().coerceAtMost(points.lastIndex - 1)
        val localAmount = scaledAmount - segmentIndex
        return cubicHermite(
            start = points[segmentIndex],
            end = points[segmentIndex + 1],
            startTangent = tangentAt(points, segmentIndex),
            endTangent = tangentAt(points, segmentIndex + 1),
            amount = localAmount
        )
    }

    private fun sampleTangent(points: List<CurvePoint>, amount: Float): CurvePoint {
        val scaledAmount = amount.coerceIn(0f, 1f) * (points.lastIndex)
        val segmentIndex = scaledAmount.toInt().coerceAtMost(points.lastIndex - 1)
        val localAmount = scaledAmount - segmentIndex
        return cubicHermiteTangent(
            start = points[segmentIndex],
            end = points[segmentIndex + 1],
            startTangent = tangentAt(points, segmentIndex),
            endTangent = tangentAt(points, segmentIndex + 1),
            amount = localAmount
        )
    }

    private fun tangentAt(points: List<CurvePoint>, index: Int): CurvePoint {
        val previous = points[(index - 1).coerceAtLeast(0)]
        val next = points[(index + 1).coerceAtMost(points.lastIndex)]
        return CurvePoint(
            x = (next.x - previous.x) * 0.5f,
            y = (next.y - previous.y) * 0.5f,
            z = (next.z - previous.z) * 0.5f
        )
    }

    private fun cubicHermite(
        start: CurvePoint,
        end: CurvePoint,
        startTangent: CurvePoint,
        endTangent: CurvePoint,
        amount: Float
    ): CurvePoint {
        val t2 = amount * amount
        val t3 = t2 * amount
        val startWeight = 2f * t3 - 3f * t2 + 1f
        val startTangentWeight = t3 - 2f * t2 + amount
        val endWeight = -2f * t3 + 3f * t2
        val endTangentWeight = t3 - t2
        return CurvePoint(
            x = start.x * startWeight + startTangent.x * startTangentWeight +
                end.x * endWeight + endTangent.x * endTangentWeight,
            y = start.y * startWeight + startTangent.y * startTangentWeight +
                end.y * endWeight + endTangent.y * endTangentWeight,
            z = start.z * startWeight + startTangent.z * startTangentWeight +
                end.z * endWeight + endTangent.z * endTangentWeight
        )
    }

    private fun cubicHermiteTangent(
        start: CurvePoint,
        end: CurvePoint,
        startTangent: CurvePoint,
        endTangent: CurvePoint,
        amount: Float
    ): CurvePoint {
        val t2 = amount * amount
        val startWeight = 6f * t2 - 6f * amount
        val startTangentWeight = 3f * t2 - 4f * amount + 1f
        val endWeight = -6f * t2 + 6f * amount
        val endTangentWeight = 3f * t2 - 2f * amount
        return CurvePoint(
            x = start.x * startWeight + startTangent.x * startTangentWeight +
                end.x * endWeight + endTangent.x * endTangentWeight,
            y = start.y * startWeight + startTangent.y * startTangentWeight +
                end.y * endWeight + endTangent.y * endTangentWeight,
            z = start.z * startWeight + startTangent.z * startTangentWeight +
                end.z * endWeight + endTangent.z * endTangentWeight
        )
    }

    private fun normalFromTangent(tangent: CurvePoint): FloatArray {
        val normalY = tangent.z
        val normalZ = -tangent.y
        val length = kotlin.math.sqrt(normalY * normalY + normalZ * normalZ)
        if (length == 0f) return floatArrayOf(0f, 0f, -1f)
        return floatArrayOf(0f, normalY / length, normalZ / length)
    }

    private fun lerp(start: Float, end: Float, amount: Float): Float {
        return start + (end - start) * amount
    }

    private fun createStraightCurve(height: Float): List<CurvePoint> {
        return listOf(
            CurvePoint(0f, 0f, 0f),
            CurvePoint(0f, height / 3f, 0f),
            CurvePoint(0f, height * 2f / 3f, 0f),
            CurvePoint(0f, height, 0f)
        )
    }

    private const val DEFAULT_WIDTH = 0.804704f
    private const val DEFAULT_HEIGHT = 9.993885f
    private const val DEFAULT_LENGTH_SEGMENTS = 16
    private const val CURVE_POINT_COUNT = 4
}
