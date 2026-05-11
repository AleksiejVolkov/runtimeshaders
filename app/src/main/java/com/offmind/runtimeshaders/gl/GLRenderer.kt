package com.offmind.runtimeshaders.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import com.offmind.runtimeshaders.gl.scene.GlScene
import kotlin.math.max

class GLRenderer(
    surface: Surface,
    initialWidth: Int,
    initialHeight: Int,
    private val scene: GlScene,
    private val backgroundCapture: BackgroundCapture? = null
) {
    private var width = max(initialWidth, 1)
    private var height = max(initialHeight, 1)
    private val eglSession = EglSurfaceSession(surface)
    private var backgroundTextureId = 0
    private var isBackgroundTextureReady = false

    fun start() {
        eglSession.start()
        configureGlState()
        backgroundTextureId = createBackgroundTexture()
        scene.onSurfaceCreated()
    }

    fun resize(width: Int, height: Int) {
        this.width = max(width, 1)
        this.height = max(height, 1)
        scene.onSurfaceChanged(this.width, this.height)
    }

    fun render(frameTimeNanos: Long) {
        backgroundCapture?.pollLatestBitmap()?.let(::uploadBackgroundBitmap)

        if (backgroundCapture?.shouldCapture() == true) {
            clearTransparent()
            eglSession.swapBuffers()
            backgroundCapture.requestCapture()
            return
        }

        if (backgroundCapture?.isCaptureInFlight() == true) {
            clearTransparent()
            eglSession.swapBuffers()
            return
        }

        clearTransparent()
        scene.onDrawFrame(
            GlFrameInfo(
                frameTimeNanos = frameTimeNanos,
                width = width,
                height = height,
                backgroundTextureId = backgroundTextureId,
                isBackgroundTextureReady = isBackgroundTextureReady
            )
        )
        eglSession.swapBuffers()
    }

    fun release() {
        scene.release()
        if (backgroundTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(backgroundTextureId), 0)
            backgroundTextureId = 0
        }
        backgroundCapture?.release()
        eglSession.release()
    }

    private fun clearTransparent() {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(
            GlRenderConstants.DEFAULT_CLEAR_RED,
            GlRenderConstants.DEFAULT_CLEAR_GREEN,
            GlRenderConstants.DEFAULT_CLEAR_BLUE,
            GlRenderConstants.DEFAULT_CLEAR_ALPHA
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    }

    private fun configureGlState() {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glClearDepthf(1f)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun createBackgroundTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return textureId
    }

    private fun uploadBackgroundBitmap(bitmap: Bitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        isBackgroundTextureReady = true
        bitmap.recycle()
    }
}
