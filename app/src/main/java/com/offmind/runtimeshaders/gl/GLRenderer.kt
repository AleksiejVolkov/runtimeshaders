package com.offmind.runtimeshaders.gl

import android.opengl.GLES20
import android.view.Surface
import com.offmind.runtimeshaders.gl.scene.GlScene
import kotlin.math.max

class GLRenderer(
    surface: Surface,
    initialWidth: Int,
    initialHeight: Int,
    private val scene: GlScene
) {
    private var width = max(initialWidth, 1)
    private var height = max(initialHeight, 1)
    private val eglSession = EglSurfaceSession(surface)

    fun start() {
        eglSession.start()
        configureGlState()
        scene.onSurfaceCreated()
    }

    fun resize(width: Int, height: Int) {
        this.width = max(width, 1)
        this.height = max(height, 1)
        scene.onSurfaceChanged(this.width, this.height)
    }

    fun render(frameTimeNanos: Long) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(
            GlRenderConstants.DEFAULT_CLEAR_RED,
            GlRenderConstants.DEFAULT_CLEAR_GREEN,
            GlRenderConstants.DEFAULT_CLEAR_BLUE,
            GlRenderConstants.DEFAULT_CLEAR_ALPHA
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        scene.onDrawFrame(
            GlFrameInfo(
                frameTimeNanos = frameTimeNanos,
                width = width,
                height = height
            )
        )
        eglSession.swapBuffers()
    }

    fun release() {
        scene.release()
        eglSession.release()
    }

    private fun configureGlState() {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glClearDepthf(1f)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }
}
