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
    private var program: GlProgram? = null
    private val curvePoints = createDefaultTapeCurvePoints().toMutableList()
    @Volatile
    private var mesh: GlMesh = createMesh(curvePoints)
    @Volatile
    private var orbitYawRadians = DEFAULT_ORBIT_YAW_RADIANS
    @Volatile
    private var orbitElevationRadians = DEFAULT_ORBIT_ELEVATION_RADIANS
    @Volatile
    private var cameraDistance = DEFAULT_CAMERA_DISTANCE

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
        activeProgram.setFloat(UNIFORM_SEGMENT_HEIGHT, TAPE_HEIGHT / TAPE_SEGMENTS)
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
        synchronized(curvePoints) {
            val point = curvePoints[POINT_2_INDEX]
            curvePoints[POINT_2_INDEX] = point.copy(
                z = (point.z + delta).coerceIn(MIN_POINT_HEIGHT, MAX_POINT_HEIGHT)
            )
            mesh = createMesh(curvePoints)
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

    private companion object {
        private const val TAPE_WIDTH = 0.804704f
        private const val TAPE_HEIGHT = 9.993885f
        private const val TAPE_SEGMENTS = 16f
        private const val DEFAULT_CAMERA_DISTANCE = 3.1f
        private const val MIN_CAMERA_DISTANCE = 1.6f
        private const val MAX_CAMERA_DISTANCE = 8f
        private const val POINT_2_INDEX = 1
        private const val MIN_POINT_HEIGHT = -2.4f
        private const val MAX_POINT_HEIGHT = 2.4f
        private const val DEFAULT_ORBIT_YAW_RADIANS = 0f
        private const val DEFAULT_ORBIT_ELEVATION_RADIANS = 0.7853982f
        private const val MIN_ORBIT_ELEVATION_RADIANS = 0.17453292f
        private const val MAX_ORBIT_ELEVATION_RADIANS = 1.3962634f

        private fun createDefaultTapeCurvePoints(): List<PlaneGeometry.CurvePoint> {
            return listOf(
                PlaneGeometry.CurvePoint(0f, 0f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT / 3f, 1.2f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT * 2f / 3f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT, 0f)
            )
        }

        private fun createMesh(curvePoints: List<PlaneGeometry.CurvePoint>): GlMesh {
            return PlaneGeometry.createTapePlane(curvePoints = curvePoints.toList())
        }

        private const val ATTRIBUTE_POSITION = "aPosition"
        private const val ATTRIBUTE_NORMAL = "aNormal"
        private const val ATTRIBUTE_COLOR = "aColor"
        private const val UNIFORM_MVP = "uMvp"
        private const val UNIFORM_SEGMENT_HEIGHT = "uSegmentHeight"
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
            uniform float uSegmentHeight;
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
                float segmentDistance = abs(fract(vObjectPosition.y / uSegmentHeight) - 0.5);
                float segmentLine = smoothstep(0.47, 0.5, segmentDistance);
                float centerHighlight = 1.0 - smoothstep(0.0, 0.68, centerDistance);
                float faceLight = 0.82 + abs(vNormal.z) * 0.18;

                vec3 baseColor = mix(vColor.rgb * faceLight, vec3(0.96, 0.99, 1.0), centerHighlight * 0.22);
                baseColor = mix(baseColor, vec3(0.28, 0.58, 0.78), sideEdge * 0.55);
                baseColor = mix(baseColor, vec3(1.0), segmentLine * 0.42);
                baseColor = mix(baseColor, vec3(0.25, 0.48, 0.66), endEdge * 0.7);

                gl_FragColor = vec4(baseColor, 0.92);
            }
        """
    }
}
