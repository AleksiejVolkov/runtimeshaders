package com.offmind.runtimeshaders.gl.scene

import android.opengl.GLES20
import android.opengl.Matrix
import com.offmind.runtimeshaders.gl.GlFrameInfo
import com.offmind.runtimeshaders.gl.GlRenderConstants
import com.offmind.runtimeshaders.gl.geometry.CubeGeometry
import com.offmind.runtimeshaders.gl.geometry.GlMesh
import com.offmind.runtimeshaders.gl.shader.GlProgram

class RotatingCubeScene(
    private val cubeAlpha: Float = DEFAULT_CUBE_ALPHA,
    private val rotationDegreesPerSecond: Float = DEFAULT_ROTATION_DEGREES_PER_SECOND
) : GlScene {
    private var program: GlProgram? = null
    private val mesh: GlMesh = CubeGeometry.createCalmColorCube()

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
            "RotatingCubeScene must be initialized before drawing."
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
            GlRenderConstants.DEFAULT_CAMERA_EYE_X,
            GlRenderConstants.DEFAULT_CAMERA_EYE_Y,
            GlRenderConstants.DEFAULT_CAMERA_EYE_Z,
            GlRenderConstants.DEFAULT_CAMERA_CENTER_X,
            GlRenderConstants.DEFAULT_CAMERA_CENTER_Y,
            GlRenderConstants.DEFAULT_CAMERA_CENTER_Z,
            GlRenderConstants.DEFAULT_CAMERA_UP_X,
            GlRenderConstants.DEFAULT_CAMERA_UP_Y,
            GlRenderConstants.DEFAULT_CAMERA_UP_Z
        )
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)

        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(
            model,
            0,
            frameInfo.timeSeconds * rotationDegreesPerSecond,
            ROTATION_AXIS_X,
            ROTATION_AXIS_Y,
            ROTATION_AXIS_Z
        )
        Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0)

        activeProgram.use()
        activeProgram.setMat4(UNIFORM_MVP, mvp)
        activeProgram.setFloat(UNIFORM_ALPHA, cubeAlpha)

        val positionHandle = activeProgram.getAttribute(ATTRIBUTE_POSITION)
        val colorHandle = activeProgram.getAttribute(ATTRIBUTE_COLOR)
        mesh.bindPosition(positionHandle)
        mesh.bindColor(colorHandle)
        mesh.draw()
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    override fun release() {
        program?.release()
        program = null
    }

    private companion object {
        private const val DEFAULT_CUBE_ALPHA = 0.72f
        private const val DEFAULT_ROTATION_DEGREES_PER_SECOND = 55f

        private const val ROTATION_AXIS_X = 0.6f
        private const val ROTATION_AXIS_Y = 1f
        private const val ROTATION_AXIS_Z = 0.25f

        private const val ATTRIBUTE_POSITION = "aPosition"
        private const val ATTRIBUTE_COLOR = "aColor"
        private const val UNIFORM_MVP = "uMvp"
        private const val UNIFORM_ALPHA = "uAlpha"

        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPosition;
            attribute vec4 aColor;
            varying vec4 vColor;

            void main() {
                vColor = aColor;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform float uAlpha;
            varying vec4 vColor;

            void main() {
                gl_FragColor = vec4(vColor.rgb, uAlpha);
            }
        """
    }
}
