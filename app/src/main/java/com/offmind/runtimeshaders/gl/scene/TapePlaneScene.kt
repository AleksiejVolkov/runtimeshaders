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

class TapePlaneScene(
    private val gradientStartColor: FloatArray = DEFAULT_GRADIENT_START_COLOR,
    private val gradientEndColor: FloatArray = DEFAULT_GRADIENT_END_COLOR
) : GlScene {
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
    private var shadowProgram: GlProgram? = null
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
        shadowProgram = GlProgram(
            vertexShaderSource = SHADOW_VERTEX_SHADER,
            fragmentShaderSource = SHADOW_FRAGMENT_SHADER
        )
    }

    override fun onDrawFrame(frameInfo: GlFrameInfo) {
        val activeProgram = checkNotNull(program) {
            "TapePlaneScene must be initialized before drawing."
        }
        val activeShadowProgram = checkNotNull(shadowProgram) {
            "TapePlaneScene shadow program must be initialized before drawing."
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

        drawShadow(activeShadowProgram)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
        drawTape(activeProgram, frameInfo)
    }

    private fun drawShadow(activeProgram: GlProgram) {
        activeProgram.use()
        activeProgram.setMat4(UNIFORM_MVP, mvp)
        activeProgram.setFloat(UNIFORM_SHADOW_GROUND_Z, SHADOW_GROUND_Z)
        activeProgram.setFloat(UNIFORM_SHADOW_OFFSET_X, SHADOW_OFFSET_X)
        activeProgram.setFloat(UNIFORM_SHADOW_OFFSET_Y, SHADOW_OFFSET_Y)
        activeProgram.setFloat(UNIFORM_SHADOW_ALPHA, SHADOW_ALPHA)
        activeProgram.setFloat(UNIFORM_SHADOW_TAPE_HEIGHT, TAPE_HEIGHT)
        activeProgram.setFloat(UNIFORM_SHADOW_TAPE_WIDTH, TAPE_WIDTH)
        activeProgram.setFloat(UNIFORM_SHADOW_BASE_SOFTNESS, SHADOW_BASE_SOFTNESS)
        activeProgram.setFloat(UNIFORM_SHADOW_HEIGHT_SOFTNESS, SHADOW_HEIGHT_SOFTNESS)

        val positionHandle = activeProgram.getAttribute(ATTRIBUTE_POSITION)
        mesh.bindPosition(positionHandle)
        GLES20.glDepthMask(false)
        mesh.draw()
        GLES20.glDepthMask(true)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun drawTape(activeProgram: GlProgram, frameInfo: GlFrameInfo) {
        activeProgram.use()
        activeProgram.setMat4(UNIFORM_MVP, mvp)
        activeProgram.setFloat(UNIFORM_TAPE_HEIGHT, TAPE_HEIGHT)
        activeProgram.setFloat(
            UNIFORM_HAS_BACKGROUND_TEXTURE,
            if (frameInfo.isBackgroundTextureReady) 1f else 0f
        )
        activeProgram.setVec2(
            UNIFORM_RESOLUTION,
            frameInfo.width.toFloat(),
            frameInfo.height.toFloat()
        )
        activeProgram.setInt(UNIFORM_BACKGROUND_TEXTURE, BACKGROUND_TEXTURE_UNIT_INDEX)
        activeProgram.setVec3(
            UNIFORM_GRADIENT_START_COLOR,
            gradientStartColor[0],
            gradientStartColor[1],
            gradientStartColor[2]
        )
        activeProgram.setVec3(
            UNIFORM_GRADIENT_END_COLOR,
            gradientEndColor[0],
            gradientEndColor[1],
            gradientEndColor[2]
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + BACKGROUND_TEXTURE_UNIT_INDEX)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameInfo.backgroundTextureId)

        val positionHandle = activeProgram.getAttribute(ATTRIBUTE_POSITION)
        val normalHandle = activeProgram.getAttribute(ATTRIBUTE_NORMAL)
        mesh.bindPosition(positionHandle)
        mesh.bindNormal(normalHandle)
        mesh.draw()
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
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
        shadowProgram?.release()
        program = null
        shadowProgram = null
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
        private const val BACKGROUND_TEXTURE_UNIT_INDEX = 0
        private const val SHADOW_GROUND_Z = -0.18f
        private const val SHADOW_OFFSET_X = 0.02f
        private const val SHADOW_OFFSET_Y = -0.08f
        private const val SHADOW_ALPHA = 0.38f
        private const val SHADOW_BASE_SOFTNESS = 0.045f
        private const val SHADOW_HEIGHT_SOFTNESS = 0.10f
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
        private val POINT_SPRING_STIFFNESS = floatArrayOf(140f, 124f, 104f, 82f, 64f)
        private val POINT_SPRING_DAMPING = floatArrayOf(8.8f, 8.2f, 7.4f, 6.8f, 6.2f)

        private val ZERO_STATE = TapeState(
            camera = CameraControls(
                yawRadians = 0f,
                elevationRadians = 1.35f,
                distance = 7.8f
            ),
            curvePoints = listOf(
                PlaneGeometry.CurvePoint(0f, 0f, 0f),
                PlaneGeometry.CurvePoint(0f, 0.72f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT / 3f - 1.5f, 3.2f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT * 2f / 3f - 3.4f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT - 4.68f, 0.18f)
            ),
            curveWidths = listOf(
                0.65f,
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
                PlaneGeometry.CurvePoint(0f, 0.72f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT / 3f, -0.06f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT * 2f / 3f + 1.08f, 0f),
                PlaneGeometry.CurvePoint(0f, TAPE_HEIGHT - 0.36f, 0.18f)
            ),
            curveWidths = listOf(
                0.65f,
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
        private const val UNIFORM_MVP = "uMvp"
        private const val UNIFORM_TAPE_HEIGHT = "uTapeHeight"
        private const val UNIFORM_HAS_BACKGROUND_TEXTURE = "uHasBackgroundTexture"
        private const val UNIFORM_RESOLUTION = "uResolution"
        private const val UNIFORM_BACKGROUND_TEXTURE = "uBackgroundTexture"
        private const val UNIFORM_GRADIENT_START_COLOR = "uGradientStartColor"
        private const val UNIFORM_GRADIENT_END_COLOR = "uGradientEndColor"
        private const val UNIFORM_SHADOW_GROUND_Z = "uGroundZ"
        private const val UNIFORM_SHADOW_OFFSET_X = "uShadowOffsetX"
        private const val UNIFORM_SHADOW_OFFSET_Y = "uShadowOffsetY"
        private const val UNIFORM_SHADOW_ALPHA = "uShadowAlpha"
        private const val UNIFORM_SHADOW_TAPE_HEIGHT = "uTapeHeight"
        private const val UNIFORM_SHADOW_TAPE_WIDTH = "uTapeWidth"
        private const val UNIFORM_SHADOW_BASE_SOFTNESS = "uBaseSoftness"
        private const val UNIFORM_SHADOW_HEIGHT_SOFTNESS = "uHeightSoftness"
        private val DEFAULT_GRADIENT_START_COLOR = floatArrayOf(1.0f, 0.93f, 0.80f)
        private val DEFAULT_GRADIENT_END_COLOR = floatArrayOf(1.0f, 0.55f, 0.10f)

        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vObjectPosition;
            varying vec3 vNormal;

            void main() {
                vObjectPosition = aPosition;
                vNormal = aNormal;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uBackgroundTexture;
            uniform float uHasBackgroundTexture;
            uniform vec2 uResolution;
            uniform float uTapeHeight;
            uniform vec3 uGradientStartColor;
            uniform vec3 uGradientEndColor;
            varying vec3 vObjectPosition;
            varying vec3 vNormal;

            void main() {
                float gradientAmount = clamp(vObjectPosition.y / uTapeHeight, 0.0, 1.0);
                vec3 normal = normalize(vNormal);
                vec3 gradientColor = mix(uGradientStartColor, uGradientEndColor, gradientAmount);
                float sideShape = pow(abs(normal.x), 1.35);
                float depthShape = 1.0 - abs(normal.z);
                float frontFacing = smoothstep(0.12, 0.82, normal.z);
                float light = 0.92 + sideShape * 0.08 + frontFacing * 0.18 - depthShape * 0.035;
                vec3 materialColor = mix(gradientColor * light, vec3(1.0), frontFacing * 0.12);

                vec3 frostedColor = materialColor;
                if (uHasBackgroundTexture > 0.5) {
                    vec2 baseUv = vec2(
                        gl_FragCoord.x / uResolution.x,
                        1.0 - (gl_FragCoord.y / uResolution.y)
                    );
                    vec2 texel = vec2(1.0 / uResolution.x, 1.0 / uResolution.y);
                    vec2 blur1 = texel * 3.5;
                    vec2 blur2 = texel * 7.0;
                    vec3 blurred = texture2D(uBackgroundTexture, baseUv).rgb * 0.20;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(blur1.x, 0.0)).rgb * 0.08;
                    blurred += texture2D(uBackgroundTexture, baseUv - vec2(blur1.x, 0.0)).rgb * 0.08;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(0.0, blur1.y)).rgb * 0.08;
                    blurred += texture2D(uBackgroundTexture, baseUv - vec2(0.0, blur1.y)).rgb * 0.08;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(blur1.x, blur1.y)).rgb * 0.055;
                    blurred += texture2D(uBackgroundTexture, baseUv - vec2(blur1.x, blur1.y)).rgb * 0.055;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(blur1.x, -blur1.y)).rgb * 0.055;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(-blur1.x, blur1.y)).rgb * 0.055;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(blur2.x, 0.0)).rgb * 0.045;
                    blurred += texture2D(uBackgroundTexture, baseUv - vec2(blur2.x, 0.0)).rgb * 0.045;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(0.0, blur2.y)).rgb * 0.045;
                    blurred += texture2D(uBackgroundTexture, baseUv - vec2(0.0, blur2.y)).rgb * 0.045;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(blur2.x, blur2.y)).rgb * 0.025;
                    blurred += texture2D(uBackgroundTexture, baseUv - vec2(blur2.x, blur2.y)).rgb * 0.025;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(blur2.x, -blur2.y)).rgb * 0.025;
                    blurred += texture2D(uBackgroundTexture, baseUv + vec2(-blur2.x, blur2.y)).rgb * 0.025;

                    vec3 frostedBase = mix(materialColor, blurred, 0.1);
                    frostedColor = mix(frostedBase, vec3(1.0), 0.18);
                }

                gl_FragColor = vec4(frostedColor, 0.985);
            }
        """

        private const val SHADOW_VERTEX_SHADER = """
            precision mediump float;
            uniform mat4 uMvp;
            uniform float uGroundZ;
            uniform float uShadowOffsetX;
            uniform float uShadowOffsetY;
            uniform float uBaseSoftness;
            uniform float uHeightSoftness;
            attribute vec3 aPosition;
            varying vec3 vObjectPosition;
            varying float vHeightAboveGround;

            void main() {
                vObjectPosition = aPosition;
                vHeightAboveGround = max(aPosition.z - uGroundZ, 0.0);
                float spread = uBaseSoftness + vHeightAboveGround * uHeightSoftness;
                vec3 shadowPosition = vec3(
                    aPosition.x + sign(aPosition.x) * spread + uShadowOffsetX,
                    aPosition.y + uShadowOffsetY,
                    uGroundZ
                );
                gl_Position = uMvp * vec4(shadowPosition, 1.0);
            }
        """

        private const val SHADOW_FRAGMENT_SHADER = """
            precision mediump float;
            uniform float uShadowAlpha;
            uniform float uTapeHeight;
            uniform float uTapeWidth;
            uniform float uBaseSoftness;
            uniform float uHeightSoftness;
            varying vec3 vObjectPosition;
            varying float vHeightAboveGround;

            void main() {
                float halfWidth = uTapeWidth * 0.5;
                float edgeDistance = max(halfWidth - abs(vObjectPosition.x), 0.0);
                float softness = uBaseSoftness + vHeightAboveGround * uHeightSoftness;
                float edgeFade = smoothstep(0.0, softness, edgeDistance);
                float lengthFade = smoothstep(0.0, 0.12, vObjectPosition.y / uTapeHeight) *
                    (1.0 - smoothstep(0.88, 1.0, vObjectPosition.y / uTapeHeight));
                float heightFade = 1.0 - smoothstep(0.0, 2.4, vHeightAboveGround) * 0.34;
                gl_FragColor = vec4(
                    0.0,
                    0.0,
                    0.0,
                    uShadowAlpha * edgeFade * heightFade * (0.72 + lengthFade * 0.28)
                );
            }
        """
    }
}
