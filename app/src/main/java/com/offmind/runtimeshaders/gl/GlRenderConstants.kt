package com.offmind.runtimeshaders.gl

object GlRenderConstants {
    const val EGL_MAJOR_VERSION_INDEX = 0
    const val EGL_MINOR_VERSION_INDEX = 1
    const val EGL_OPENGL_ES_VERSION = 2

    const val COLOR_COMPONENTS = 4
    const val POSITION_COMPONENTS = 3
    const val FLOAT_BYTES = Float.SIZE_BYTES

    const val DEFAULT_CLEAR_RED = 0f
    const val DEFAULT_CLEAR_GREEN = 0f
    const val DEFAULT_CLEAR_BLUE = 0f
    const val DEFAULT_CLEAR_ALPHA = 0f

    const val DEFAULT_CAMERA_EYE_X = 0f
    const val DEFAULT_CAMERA_EYE_Y = 0f
    const val DEFAULT_CAMERA_EYE_Z = 5f
    const val DEFAULT_CAMERA_CENTER_X = 0f
    const val DEFAULT_CAMERA_CENTER_Y = 0f
    const val DEFAULT_CAMERA_CENTER_Z = 0f
    const val DEFAULT_CAMERA_UP_X = 0f
    const val DEFAULT_CAMERA_UP_Y = 1f
    const val DEFAULT_CAMERA_UP_Z = 0f

    const val DEFAULT_FIELD_OF_VIEW_DEGREES = 45f
    const val DEFAULT_NEAR_PLANE = 0.1f
    const val DEFAULT_FAR_PLANE = 100f
}
