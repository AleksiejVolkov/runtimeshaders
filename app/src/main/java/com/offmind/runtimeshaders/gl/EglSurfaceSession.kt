package com.offmind.runtimeshaders.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.view.Surface

class EglSurfaceSession(
    private val surface: Surface
) {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    fun start() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed: ${eglErrorString()}" }

        val version = IntArray(2)
        check(
            EGL14.eglInitialize(
                eglDisplay,
                version,
                GlRenderConstants.EGL_MAJOR_VERSION_INDEX,
                version,
                GlRenderConstants.EGL_MINOR_VERSION_INDEX
            )
        ) {
            "eglInitialize failed: ${eglErrorString()}"
        }

        val config = chooseAlphaConfig()
        eglContext = createContext(config)
        eglSurface = createWindowSurface(config)

        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            "eglMakeCurrent failed: ${eglErrorString()}"
        }
    }

    fun swapBuffers() {
        check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
            "eglSwapBuffers failed: ${eglErrorString()}"
        }
    }

    fun release() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return

        EGL14.eglMakeCurrent(
            eglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        if (eglContext != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            eglContext = EGL14.EGL_NO_CONTEXT
        }
        EGL14.eglTerminate(eglDisplay)
        eglDisplay = EGL14.EGL_NO_DISPLAY
    }

    private fun chooseAlphaConfig(): EGLConfig {
        val configAttributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)
        check(
            EGL14.eglChooseConfig(
                eglDisplay,
                configAttributes,
                0,
                configs,
                0,
                configs.size,
                configCount,
                0
            ) && configCount[0] > 0 && configs[0] != null
        ) {
            "No EGL config accepted EGL_ALPHA_SIZE = 8: ${eglErrorString()}"
        }
        return configs[0]!!
    }

    private fun createContext(config: EGLConfig): EGLContext {
        val contextAttributes = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION,
            GlRenderConstants.EGL_OPENGL_ES_VERSION,
            EGL14.EGL_NONE
        )
        val context = EGL14.eglCreateContext(
            eglDisplay,
            config,
            EGL14.EGL_NO_CONTEXT,
            contextAttributes,
            0
        )
        check(context != EGL14.EGL_NO_CONTEXT) {
            "eglCreateContext failed: ${eglErrorString()}"
        }
        return context
    }

    private fun createWindowSurface(config: EGLConfig): EGLSurface {
        val surfaceAttributes = intArrayOf(EGL14.EGL_NONE)
        val windowSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            config,
            surface,
            surfaceAttributes,
            0
        )
        check(windowSurface != EGL14.EGL_NO_SURFACE) {
            "eglCreateWindowSurface failed: ${eglErrorString()}"
        }
        return windowSurface
    }

    private fun eglErrorString(): String = "0x${EGL14.eglGetError().toString(16)}"
}
