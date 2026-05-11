package com.offmind.runtimeshaders.gl

data class GlFrameInfo(
    val frameTimeNanos: Long,
    val width: Int,
    val height: Int
) {
    val timeSeconds: Float
        get() = frameTimeNanos / 1_000_000_000f

    val aspectRatio: Float
        get() = width.toFloat() / height.toFloat()
}
