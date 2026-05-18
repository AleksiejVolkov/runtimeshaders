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
        thickness: Float = DEFAULT_THICKNESS,
        lengthSegments: Int = DEFAULT_LENGTH_SEGMENTS,
        subdivisionPower: Int = DEFAULT_SUBDIVISION_POWER,
        curvePoints: List<CurvePoint> = createStraightCurve(height),
        curveWidths: List<Float> = List(curvePoints.size) { DEFAULT_CURVE_WIDTH }
    ): GlMesh {
        require(width > 0f) { "width must be greater than 0." }
        require(height > 0f) { "height must be greater than 0." }
        require(thickness >= 0f) { "thickness must be zero or greater." }
        require(lengthSegments >= 1) { "lengthSegments must be at least 1." }
        require(subdivisionPower >= 0) { "subdivisionPower must be zero or greater." }
        require(curvePoints.size >= MIN_CURVE_POINT_COUNT) {
            "curvePoints must contain at least $MIN_CURVE_POINT_COUNT points."
        }
        require(curveWidths.size == curvePoints.size) {
            "curveWidths must contain exactly one value per curve point."
        }

        val vertices = mutableListOf<Float>()
        val halfWidth = width * 0.5f
        val halfThickness = thickness * 0.5f
        val subdivisions = 1 shl subdivisionPower
        val geometrySegments = lengthSegments * subdivisions
        val firstBodySegment = subdivisions
        val lastBodySegmentExclusive = geometrySegments - subdivisions
        val capSegments = (DEFAULT_CAP_SEGMENTS * subdivisions).coerceAtLeast(DEFAULT_CAP_SEGMENTS)
        val sections = List(geometrySegments + 1) { index ->
            val amount = index.toFloat() / geometrySegments
            val center = sampleCurve(curvePoints, curveWidths, amount)
            val tangent = sampleTangent(curvePoints, curveWidths, amount)
            val surfaceNormal = normalFromTangent(tangent)
            val tangentUnit = tangentUnit(tangent)
            TapeSection(
                center = floatArrayOf(center.x, center.y, center.z),
                centerFront = offsetPosition(
                    position = floatArrayOf(center.x, center.y, center.z),
                    normal = surfaceNormal,
                    distance = halfThickness
                ),
                centerBack = offsetPosition(
                    position = floatArrayOf(center.x, center.y, center.z),
                    normal = surfaceNormal,
                    distance = -halfThickness
                ),
                leftFront = offsetPosition(
                    position = floatArrayOf(center.x - halfWidth, center.y, center.z),
                    normal = surfaceNormal,
                    distance = halfThickness
                ),
                rightFront = offsetPosition(
                    position = floatArrayOf(center.x + halfWidth, center.y, center.z),
                    normal = surfaceNormal,
                    distance = halfThickness
                ),
                leftBack = offsetPosition(
                    position = floatArrayOf(center.x - halfWidth, center.y, center.z),
                    normal = surfaceNormal,
                    distance = -halfThickness
                ),
                rightBack = offsetPosition(
                    position = floatArrayOf(center.x + halfWidth, center.y, center.z),
                    normal = surfaceNormal,
                    distance = -halfThickness
                ),
                frontNormal = surfaceNormal,
                backNormal = opposite(surfaceNormal),
                tangentUnit = tangentUnit,
                color = colorForSegment(amount)
            )
        }

        for (segment in firstBodySegment until lastBodySegmentExclusive) {
            val start = sections[segment]
            val end = sections[segment + 1]

            addQuad(
                vertices = vertices,
                bottomLeft = start.leftFront,
                bottomRight = start.rightFront,
                topRight = end.rightFront,
                topLeft = end.leftFront,
                bottomNormal = start.frontNormal,
                topNormal = end.frontNormal,
                bottomColor = start.color,
                topColor = end.color
            )
            addQuad(
                vertices = vertices,
                bottomLeft = start.rightBack,
                bottomRight = start.leftBack,
                topRight = end.leftBack,
                topLeft = end.rightBack,
                bottomNormal = start.backNormal,
                topNormal = end.backNormal,
                bottomColor = start.color,
                topColor = end.color
            )
            addQuad(
                vertices = vertices,
                bottomLeft = start.leftBack,
                bottomRight = start.leftFront,
                topRight = end.leftFront,
                topLeft = end.leftBack,
                bottomNormal = LEFT_SIDE_NORMAL,
                topNormal = LEFT_SIDE_NORMAL,
                bottomColor = start.color,
                topColor = end.color
            )
            addQuad(
                vertices = vertices,
                bottomLeft = start.rightFront,
                bottomRight = start.rightBack,
                topRight = end.rightBack,
                topLeft = end.rightFront,
                bottomNormal = RIGHT_SIDE_NORMAL,
                topNormal = RIGHT_SIDE_NORMAL,
                bottomColor = start.color,
                topColor = end.color
            )
        }

        addRoundCap(
            vertices = vertices,
            section = sections[firstBodySegment],
            halfWidth = halfWidth,
            capSegments = capSegments,
            isStart = true
        )
        addRoundCap(
            vertices = vertices,
            section = sections[lastBodySegmentExclusive],
            halfWidth = halfWidth,
            capSegments = capSegments,
            isStart = false
        )

        return GlMesh(
            vertices = vertices.toFloatArray(),
            vertexLayout = VertexLayout()
        )
    }

    private data class TapeSection(
        val center: FloatArray,
        val centerFront: FloatArray,
        val centerBack: FloatArray,
        val leftFront: FloatArray,
        val rightFront: FloatArray,
        val leftBack: FloatArray,
        val rightBack: FloatArray,
        val frontNormal: FloatArray,
        val backNormal: FloatArray,
        val tangentUnit: FloatArray,
        val color: FloatArray
    )

    private fun addQuad(
        vertices: MutableList<Float>,
        bottomLeft: FloatArray,
        bottomRight: FloatArray,
        topRight: FloatArray,
        topLeft: FloatArray,
        bottomNormal: FloatArray,
        topNormal: FloatArray,
        bottomColor: FloatArray,
        topColor: FloatArray
    ) {
        addVertex(vertices, bottomLeft, bottomNormal, bottomColor)
        addVertex(vertices, bottomRight, bottomNormal, bottomColor)
        addVertex(vertices, topRight, topNormal, topColor)

        addVertex(vertices, bottomLeft, bottomNormal, bottomColor)
        addVertex(vertices, topRight, topNormal, topColor)
        addVertex(vertices, topLeft, topNormal, topColor)
    }

    private fun addRoundCap(
        vertices: MutableList<Float>,
        section: TapeSection,
        halfWidth: Float,
        capSegments: Int,
        isStart: Boolean
    ) {
        val capCenterFront = section.centerFront
        val capCenterBack = section.centerBack
        val angles = List(capSegments + 1) { index ->
            val amount = index.toFloat() / capSegments
            if (isStart) {
                PI + PI * amount
            } else {
                PI * amount
            }
        }

        for (index in 0 until capSegments) {
            val angle0 = angles[index]
            val angle1 = angles[index + 1]
            val front0 = capPoint(capCenterFront, section.tangentUnit, halfWidth, angle0)
            val front1 = capPoint(capCenterFront, section.tangentUnit, halfWidth, angle1)
            val back0 = capPoint(capCenterBack, section.tangentUnit, halfWidth, angle0)
            val back1 = capPoint(capCenterBack, section.tangentUnit, halfWidth, angle1)
            val sideNormal0 = capSideNormal(section.tangentUnit, angle0)
            val sideNormal1 = capSideNormal(section.tangentUnit, angle1)

            addVertex(vertices, capCenterFront, section.frontNormal, section.color)
            addVertex(vertices, front0, section.frontNormal, section.color)
            addVertex(vertices, front1, section.frontNormal, section.color)

            addVertex(vertices, capCenterBack, section.backNormal, section.color)
            addVertex(vertices, back1, section.backNormal, section.color)
            addVertex(vertices, back0, section.backNormal, section.color)

            addVertex(vertices, back0, sideNormal0, section.color)
            addVertex(vertices, front0, sideNormal0, section.color)
            addVertex(vertices, front1, sideNormal1, section.color)

            addVertex(vertices, back0, sideNormal0, section.color)
            addVertex(vertices, front1, sideNormal1, section.color)
            addVertex(vertices, back1, sideNormal1, section.color)
        }
    }

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

    private fun sampleCurve(
        points: List<CurvePoint>,
        widths: List<Float>,
        amount: Float
    ): CurvePoint {
        val scaledAmount = amount.coerceIn(0f, 1f) * (points.lastIndex)
        val segmentIndex = scaledAmount.toInt().coerceAtMost(points.lastIndex - 1)
        val localAmount = scaledAmount - segmentIndex
        return cubicHermite(
            start = points[segmentIndex],
            end = points[segmentIndex + 1],
            startTangent = tangentAt(points, widths, segmentIndex),
            endTangent = tangentAt(points, widths, segmentIndex + 1),
            amount = localAmount
        )
    }

    private fun sampleTangent(
        points: List<CurvePoint>,
        widths: List<Float>,
        amount: Float
    ): CurvePoint {
        val scaledAmount = amount.coerceIn(0f, 1f) * (points.lastIndex)
        val segmentIndex = scaledAmount.toInt().coerceAtMost(points.lastIndex - 1)
        val localAmount = scaledAmount - segmentIndex
        return cubicHermiteTangent(
            start = points[segmentIndex],
            end = points[segmentIndex + 1],
            startTangent = tangentAt(points, widths, segmentIndex),
            endTangent = tangentAt(points, widths, segmentIndex + 1),
            amount = localAmount
        )
    }

    private fun tangentAt(
        points: List<CurvePoint>,
        widths: List<Float>,
        index: Int
    ): CurvePoint {
        val previous = points[(index - 1).coerceAtLeast(0)]
        val next = points[(index + 1).coerceAtMost(points.lastIndex)]
        val width = widths[index]
        return CurvePoint(
            x = (next.x - previous.x) * 0.5f * width,
            y = (next.y - previous.y) * 0.5f * width,
            z = (next.z - previous.z) * 0.5f * width
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

    private fun tangentUnit(tangent: CurvePoint): FloatArray {
        val length = kotlin.math.sqrt(tangent.y * tangent.y + tangent.z * tangent.z)
        if (length == 0f) return floatArrayOf(0f, 1f, 0f)
        return floatArrayOf(0f, tangent.y / length, tangent.z / length)
    }

    private fun capPoint(
        center: FloatArray,
        tangentUnit: FloatArray,
        halfWidth: Float,
        angle: Float
    ): FloatArray {
        val sideOffset = kotlin.math.cos(angle) * halfWidth
        val tangentOffset = kotlin.math.sin(angle) * halfWidth
        return floatArrayOf(
            center[0] + sideOffset,
            center[1] + tangentUnit[1] * tangentOffset,
            center[2] + tangentUnit[2] * tangentOffset
        )
    }

    private fun capSideNormal(
        tangentUnit: FloatArray,
        angle: Float
    ): FloatArray {
        val sideNormal = kotlin.math.cos(angle)
        val tangentNormal = kotlin.math.sin(angle)
        return floatArrayOf(
            sideNormal,
            tangentUnit[1] * tangentNormal,
            tangentUnit[2] * tangentNormal
        )
    }

    private fun offsetPosition(
        position: FloatArray,
        normal: FloatArray,
        distance: Float
    ): FloatArray {
        return floatArrayOf(
            position[0] + normal[0] * distance,
            position[1] + normal[1] * distance,
            position[2] + normal[2] * distance
        )
    }

    private fun opposite(normal: FloatArray): FloatArray {
        return floatArrayOf(-normal[0], -normal[1], -normal[2])
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
    private const val DEFAULT_THICKNESS = 0.16f
    private const val DEFAULT_LENGTH_SEGMENTS = 16
    private const val DEFAULT_SUBDIVISION_POWER = 2
    private const val DEFAULT_CAP_SEGMENTS = 8
    private const val DEFAULT_CURVE_WIDTH = 1f
    private const val MIN_CURVE_POINT_COUNT = 2
    private const val PI = kotlin.math.PI.toFloat()
    private val LEFT_SIDE_NORMAL = floatArrayOf(-1f, 0f, 0f)
    private val RIGHT_SIDE_NORMAL = floatArrayOf(1f, 0f, 0f)
}
