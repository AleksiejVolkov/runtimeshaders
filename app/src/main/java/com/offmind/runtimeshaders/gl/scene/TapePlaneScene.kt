package com.offmind.runtimeshaders.gl.scene

import android.opengl.GLES20
import android.opengl.Matrix
import com.offmind.runtimeshaders.gl.GlFrameInfo
import com.offmind.runtimeshaders.gl.GlRenderConstants
import com.offmind.runtimeshaders.gl.geometry.GlMesh
import com.offmind.runtimeshaders.gl.geometry.PlaneGeometry
import com.offmind.runtimeshaders.gl.shader.GlProgram
import kotlin.math.cos
import kotlin.math.sin

class TapePlaneScene : GlScene {
    data class CameraControls(
        val yawRadians: Float,
        val elevationRadians: Float,
        val distance: Float
    )

    data class CurvePointControls(
        val index: Int,
        val x: Float,
        val y: Float,
        val z: Float,
        val width: Float
    )

    private data class SpringValue(
        val current: Float,
        val velocity: Float
    )

    private var program: GlProgram? = null
    private val curvePoints = ZERO_STATE.curvePoints.toMutableList()
    private val curveWidths = ZERO_STATE.curveWidths.toMutableList()
    private val pointMorphProgress = FloatArray(ZERO_STATE.curvePoints.size)
    private val pointMorphVelocity = FloatArray(ZERO_STATE.curvePoints.size)
    @Volatile
    private var mesh: GlMesh = createMesh(curvePoints, curveWidths)
    @Volatile
    private var orbitYawRadians = ZERO_STATE.camera.yawRadians
    @Volatile
    private var orbitElevationRadians = ZERO_STATE.camera.elevationRadians
    @Volatile
    private var cameraDistance = ZERO_STATE.camera.distance
    @Volatile
    private var targetMorphProgress = 0f
    private var lastFrameTimeNanos = 0L

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val mvp = FloatArray(16)

    override fun onSurfaceCreated() {
        program = GlProgram(
            vertexShaderSource = VERTEX_SHADER,
            fragmentShaderSource = FRAGMENT_SHADER
        )
    }

    override fun onDrawFrame(frameInfo: GlFrameInfo) {
        val activeProgram = checkNotNull(program) {
            "TapePlaneScene must be initialized before drawing."
        }
        updateMorph(frameInfo.frameTimeNanos)

        Matrix.perspectiveM(
            projection,
            0,
            GlRenderConstants.DEFAULT_FIELD_OF_VIEW_DEGREES,
            frameInfo.aspectRatio,
            GlRenderConstants.DEFAULT_NEAR_PLANE,
            GlRenderConstants.DEFAULT_FAR_PLANE
        )
        Matrix.setLookAtM(
            view,
            0,
            cameraEyeX(),
            cameraEyeY(),
            cameraEyeZ(),
            0f,
            0f,
            0f,
            0f,
            0f,
            1f
        )
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)

        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, -90f, 0f, 0f, 1f)
        Matrix.translateM(model, 0, 0f, -TAPE_HEIGHT * 0.5f, 0f)
        Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0)

        activeProgram.use()
        activeProgram.setMat4(UNIFORM_MVP, mvp)
        activeProgram.setFloat(UNIFORM_TAPE_WIDTH, TAPE_WIDTH)
        activeProgram.setFloat(UNIFORM_TAPE_HEIGHT, TAPE_HEIGHT)

        val positionHandle = activeProgram.getAttribute(ATTRIBUTE_POSITION)
        val normalHandle = activeProgram.getAttribute(ATTRIBUTE_NORMAL)
        val colorHandle = activeProgram.getAttribute(ATTRIBUTE_COLOR)
        mesh.bindPosition(positionHandle)
        mesh.bindNormal(normalHandle)
        mesh.bindColor(colorHandle)
        mesh.draw()
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    fun orbit(deltaYawRadians: Float, deltaElevationRadians: Float) {
        orbitYawRadians += deltaYawRadians
        orbitElevationRadians = (orbitElevationRadians + deltaElevationRadians)
            .coerceIn(MIN_ORBIT_ELEVATION_RADIANS, MAX_ORBIT_ELEVATION_RADIANS)
    }

    fun changeCameraDistance(delta: Float) {
        cameraDistance = (cameraDistance + delta).coerceIn(MIN_CAMERA_DISTANCE, MAX_CAMERA_DISTANCE)
    }

    fun movePoint2Height(delta: Float) {
        moveCurvePoint(index = POINT_2_INDEX, deltaY = 0f, deltaZ = delta)
    }

    fun moveCurvePoint(index: Int, deltaY: Float, deltaZ: Float) {
        synchronized(curvePoints) {
            val point = curvePoints[index]
            curvePoints[index] = point.copy(
                y = (point.y + deltaY).coerceIn(MIN_POINT_Y, MAX_POINT_Y),
                z = (point.z + deltaZ).coerceIn(MIN_POINT_HEIGHT, MAX_POINT_HEIGHT)
            )
            mesh = createMesh(curvePoints, curveWidths)
        }
    }

    fun changeCurvePointWidth(index: Int, delta: Float) {
        synchronized(curvePoints) {
            curveWidths[index] = (curveWidths[index] + delta).coerceIn(MIN_CURVE_WIDTH, MAX_CURVE_WIDTH)
            mesh = createMesh(curvePoints, curveWidths)
        }
    }

    fun setMorphProgress(progress: Float) {
        targetMorphProgress = progress.coerceIn(0f, 1f)
    }

    fun cameraControls(): CameraControls {
        return CameraControls(
            yawRadians = orbitYawRadians,
            elevationRadians = orbitElevationRadians,
            distance = cameraDistance
        )
    }

    fun curvePointControls(): List<CurvePointControls> {
        return synchronized(curvePoints) {
            curvePoints.mapIndexed { index, point ->
                CurvePointControls(
                    index = index + 1,
                    x = point.x,
                    y = point.y,
                    z = point.z,
                    width = curveWidths[index]
                )
            }
        }
    }

    override fun release() {
        program?.release()
        program = null
    }

    private fun cameraEyeX(): Float {
        return cameraDistance * cos(orbitElevationRadians) * sin(orbitYawRadians)
    }

    private fun cameraEyeY(): Float {
        return -cameraDistance * cos(orbitElevationRadians) * cos(orbitYawRadians)
    }

    private fun cameraEyeZ(): Float {
        return cameraDistance * sin(orbitElevationRadians)
    }

    private fun updateMorph(frameTimeNanos: Long) {
        if (lastFrameTimeNanos == 0L) {
            lastFrameTimeNanos = frameTimeNanos
            return
        }

        val deltaSeconds = ((frameTimeNanos - lastFrameTimeNanos) / NANOS_PER_SECOND)
            .coerceIn(0f, MAX_MORPH_DELTA_SECONDS)
        lastFrameTimeNanos = frameTimeNanos

        var changed = false
        for (index in pointMorphProgress.indices) {
            val pointSpring = stepSpring(
                current = pointMorphProgress[index],
                velocity = pointMorphVelocity[index],
                target = targetMorphProgress,
                stiffness = POINT_SPRING_STIFFNESS[index],
                damping = POINT_SPRING_DAMPING[index],
                deltaSeconds = deltaSeconds
            )
            pointMorphProgress[index] = pointSpring.current
            pointMorphVelocity[index] = pointSpring.velocity
            changed = changed || kotlin.math.abs(pointMorphVelocity[index]) > MORPH_EPSILON ||
                kotlin.math.abs(pointMorphProgress[index] - targetMorphProgress) > MORPH_EPSILON
        }

        if (changed) {
            applyInterpolatedTapeState()
        }
    }

    private fun applyInterpolatedTapeState() {
        synchronized(curvePoints) {
            curvePoints.clear()
            curvePoints += ZERO_STATE.curvePoints.indices.map { index ->
                interpolateCurvePoint(
                    start = ZERO_STATE.curvePoints[index],
                    end = FULL_STATE.curvePoints[index],
                    amount = pointMorphProgress[index]
                )
            }
            curveWidths.clear()
            curveWidths += ZERO_STATE.curveWidths.indices.map { index ->
                lerp(
                    start = ZERO_STATE.curveWidths[index],
                    end = FULL_STATE.curveWidths[index],
                    amount = pointMorphProgress[index]
                )
            }
            mesh = createMesh(curvePoints, curveWidths)
        }
    }

    private fun stepSpring(
        current: Float,
        velocity: Float,
        target: Float,
        stiffness: Float,
        damping: Float,
        deltaSeconds: Float
    ): SpringValue {
        val acceleration = (target - current) * stiffness - velocity * damping
        val nextVelocity = velocity + acceleration * deltaSeconds
        val nextCurrent = current + nextVelocity * deltaSeconds
        return SpringValue(nextCurrent, nextVelocity)
    }

    private companion object {
        private data class TapeState(
            val camera: CameraControls,
            val curvePoints: List<PlaneGeometry.CurvePoint>,
            val curveWidths: List<Float>
        )

        private const val TAPE_WIDTH = 0.804704f
        private const val TAPE_HEIGHT = 9.993885f
        private const val TAPE_THICKNESS = 0.06f
        private const val TAPE_SUBDIVISION_POWER = 2
        private const val MIN_CAMERA_DISTANCE = 1.6f
        private const val MAX_CAMERA_DISTANCE = 8f
        private const val POINT_2_INDEX = 1
        private const val MIN_POINT_Y = -1.5f
        private const val MAX_POINT_Y = TAPE_HEIGHT + 1.5f
        private const val MIN_POINT_HEIGHT = -2.4f
        private const val MAX_POINT_HEIGHT = 2.4f
        private const val MIN_CURVE_WIDTH = 0.1f
        private const val MAX_CURVE_WIDTH = 2.8f
        private const val MIN_ORBIT_ELEVATION_RADIANS = 0.17453292f
        private const val MAX_ORBIT_ELEVATION_RADIANS = 1.3962634f
        private const val NANOS_PER_SECOND = 1_000_000_000f
        private const val MAX_MORPH_DELTA_SECONDS = 0.033f
        private const val MORPH_EPSILON = 0.0005f
        private val POINT_SPRING_STIFFNESS = floatArrayOf(130f, 104f, 82f, 64f)
        private val POINT_SPRING_DAMPING = floatArrayOf(8.2f, 7.4f, 6.8f, 6.2f)

        private val ZERO_STATE = TapeState(
            camera = CameraControls(
                yawRadians = 0f,
                elevationRadians = 1.35f,
                distance = 7.65f
            ),
            curvePoints = listOf(
                PlaneGeometry.CurvePoint(0f, 0f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT / 3f - 1.2f, 2.4f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT * 2f / 3f - 3f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT - 4.68f, 0.18f)
            ),
            curveWidths = listOf(
                1f,
                1f,
                1.72f,
                0.1f
            )
        )

        private val FULL_STATE = TapeState(
            camera = CameraControls(
                yawRadians = 0f,
                elevationRadians = 0.96f,
                distance = 7.65f
            ),
            curvePoints = listOf(
                PlaneGeometry.CurvePoint(0f, 0f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT / 3f, -0.06f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT * 2f / 3f + 1.08f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT - 0.36f, 0.18f)
            ),
            curveWidths = listOf(
                1f,
                1f,
                1.72f,
                0.1f
            )
        )

        private fun interpolateCurvePoint(
            start: PlaneGeometry.CurvePoint,
            end: PlaneGeometry.CurvePoint,
            amount: Float
        ): PlaneGeometry.CurvePoint {
            return PlaneGeometry.CurvePoint(
                x = lerp(start.x, end.x, amount),
                y = lerp(start.y, end.y, amount),
                z = lerp(start.z, end.z, amount)
            )
        }

        private fun lerp(start: Float, end: Float, amount: Float): Float {
            return start + (end - start) * amount
        }

        private fun createMesh(
            curvePoints: List<PlaneGeometry.CurvePoint>,
            curveWidths: List<Float>
        ): GlMesh {
            return PlaneGeometry.createTapePlane(
                thickness = TAPE_THICKNESS,
                subdivisionPower = TAPE_SUBDIVISION_POWER,
                curvePoints = curvePoints.toList(),
                curveWidths = curveWidths.toList()
            )
        }

        private const val ATTRIBUTE_POSITION = "aPosition"
        private const val ATTRIBUTE_NORMAL = "aNormal"
        private const val ATTRIBUTE_COLOR = "aColor"
        private const val UNIFORM_MVP = "uMvp"
        private const val UNIFORM_TAPE_WIDTH = "uTapeWidth"
        private const val UNIFORM_TAPE_HEIGHT = "uTapeHeight"

        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec4 aColor;
            varying vec3 vObjectPosition;
            varying vec3 vNormal;
            varying vec4 vColor;

            void main() {
                vObjectPosition = aPosition;
                vNormal = aNormal;
                vColor = aColor;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform float uTapeWidth;
            uniform float uTapeHeight;
            varying vec3 vObjectPosition;
            varying vec3 vNormal;
            varying vec4 vColor;

            void main() {
                float centerDistance = abs(vObjectPosition.x) / (uTapeWidth * 0.5);
                float sideEdge = smoothstep(0.88, 0.98, centerDistance);
                float endEdge = max(
                    smoothstep(0.02, 0.0, vObjectPosition.y),
                    smoothstep(uTapeHeight - 0.02, uTapeHeight, vObjectPosition.y)
                );
                float centerHighlight = 1.0 - smoothstep(0.0, 0.68, centerDistance);
                float faceLight = 0.82 + abs(vNormal.z) * 0.18;

                vec3 baseColor = mix(vColor.rgb * faceLight, vec3(0.96, 0.99, 1.0), centerHighlight * 0.22);
                baseColor = mix(baseColor, vec3(0.28, 0.58, 0.78), sideEdge * 0.55);
                baseColor = mix(baseColor, vec3(0.25, 0.48, 0.66), endEdge * 0.7);

                gl_FragColor = vec4(baseColor, 1.);
            }
        """
    }
}
