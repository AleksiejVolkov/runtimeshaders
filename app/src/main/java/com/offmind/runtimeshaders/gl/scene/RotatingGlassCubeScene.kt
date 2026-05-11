package com.offmind.runtimeshaders.gl.scene

import android.opengl.GLES20
import android.opengl.Matrix
import com.offmind.runtimeshaders.gl.GlFrameInfo
import com.offmind.runtimeshaders.gl.GlRenderConstants
import com.offmind.runtimeshaders.gl.geometry.CubeGeometry
import com.offmind.runtimeshaders.gl.geometry.GlMesh
import com.offmind.runtimeshaders.gl.shader.GlProgram

class RotatingGlassCubeScene(
    private val cubeAlpha: Float = DEFAULT_CUBE_ALPHA,
    private val refractionStrength: Float = DEFAULT_REFRACTION_STRENGTH,
    private val rotationDegreesPerSecond: Float = DEFAULT_ROTATION_DEGREES_PER_SECOND
) : GlScene {
    private var program: GlProgram? = null
    private val mesh: GlMesh = CubeGeometry.createGlassCube()

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
            "RotatingGlassCubeScene must be initialized before drawing."
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
        activeProgram.setMat4(UNIFORM_MODEL, model)
        activeProgram.setFloat(UNIFORM_ALPHA, cubeAlpha)
        activeProgram.setFloat(
            UNIFORM_HAS_BACKGROUND_TEXTURE,
            if (frameInfo.isBackgroundTextureReady) 1f else 0f
        )
        activeProgram.setFloat(UNIFORM_REFRACTION_STRENGTH, refractionStrength)
        activeProgram.setVec2(UNIFORM_RESOLUTION, frameInfo.width.toFloat(), frameInfo.height.toFloat())
        activeProgram.setInt(UNIFORM_BACKGROUND_TEXTURE, BACKGROUND_TEXTURE_UNIT_INDEX)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + BACKGROUND_TEXTURE_UNIT_INDEX)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameInfo.backgroundTextureId)

        val positionHandle = activeProgram.getAttribute(ATTRIBUTE_POSITION)
        val normalHandle = activeProgram.getAttribute(ATTRIBUTE_NORMAL)
        mesh.bindPosition(positionHandle)
        mesh.bindNormal(normalHandle)
        GLES20.glDepthMask(false)
        mesh.draw()
        GLES20.glDepthMask(true)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun release() {
        program?.release()
        program = null
    }

    private companion object {
        private const val DEFAULT_CUBE_ALPHA = 0.98f
        private const val DEFAULT_REFRACTION_STRENGTH = 0.045f
        private const val DEFAULT_ROTATION_DEGREES_PER_SECOND = 55f
        private const val BACKGROUND_TEXTURE_UNIT_INDEX = 0

        private const val ROTATION_AXIS_X = 0.6f
        private const val ROTATION_AXIS_Y = 1f
        private const val ROTATION_AXIS_Z = 0.25f

        private const val ATTRIBUTE_POSITION = "aPosition"
        private const val ATTRIBUTE_NORMAL = "aNormal"
        private const val UNIFORM_MVP = "uMvp"
        private const val UNIFORM_MODEL = "uModel"
        private const val UNIFORM_ALPHA = "uAlpha"
        private const val UNIFORM_HAS_BACKGROUND_TEXTURE = "uHasBackgroundTexture"
        private const val UNIFORM_REFRACTION_STRENGTH = "uRefractionStrength"
        private const val UNIFORM_RESOLUTION = "uResolution"
        private const val UNIFORM_BACKGROUND_TEXTURE = "uBackgroundTexture"

        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vObjectPosition;
            varying vec3 vViewNormal;

            void main() {
                vObjectPosition = aPosition;
                vViewNormal = normalize((uModel * vec4(aNormal, 0.0)).xyz);
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uBackgroundTexture;
            uniform float uAlpha;
            uniform float uHasBackgroundTexture;
            uniform float uRefractionStrength;
            uniform vec2 uResolution;
            varying vec3 vObjectPosition;
            varying vec3 vViewNormal;

            void main() {
                if (uHasBackgroundTexture < 0.5) {
                    gl_FragColor = vec4(0.0);
                    return;
                }

                vec2 baseUv = vec2(
                    gl_FragCoord.x / uResolution.x,
                    1.0 - (gl_FragCoord.y / uResolution.y)
                );
                vec2 refractedUv = clamp(
                    baseUv + normalize(vViewNormal).xy * uRefractionStrength,
                    vec2(0.0),
                    vec2(1.0)
                );
                vec4 background = texture2D(uBackgroundTexture, refractedUv);
                vec3 normal = normalize(vViewNormal);
                vec3 viewDir = vec3(0.0, 0.0, 1.0);
                vec3 lightDir = normalize(vec3(-0.38, 0.68, 0.62));
                vec3 halfDir = normalize(lightDir + viewDir);

                vec3 absPosition = abs(vObjectPosition);
                float faceEdge = max(
                    min(absPosition.x, absPosition.y),
                    max(min(absPosition.x, absPosition.z), min(absPosition.y, absPosition.z))
                );
                float edgeMask = smoothstep(0.76, 0.98, faceEdge);
                float glancing = pow(1.0 - abs(normal.z), 1.35);
                float frontFacing = smoothstep(-0.05, 0.55, normal.z);
                float backFacing = smoothstep(0.05, 0.75, -normal.z);
                float frontReflection = pow(max(dot(normal, halfDir), 0.0), 34.0) * frontFacing;
                float internalReflection = edgeMask * glancing * (0.45 + backFacing * 0.75);

                vec3 tint = vec3(0.52, 0.82, 1.0);
                vec3 frontTint = vec3(0.92, 0.98, 1.0);
                vec3 internalTint = vec3(0.62, 0.88, 1.0);
                vec3 color = mix(background.rgb, tint, 0.16);
                color += frontTint * frontReflection * 0.55;
                color += internalTint * internalReflection * 0.34;
                float alpha = clamp(uAlpha + internalReflection * 0.14 + frontReflection * 0.08, 0.0, 1.0);
                gl_FragColor = vec4(color, alpha);
            }
        """
    }
}
