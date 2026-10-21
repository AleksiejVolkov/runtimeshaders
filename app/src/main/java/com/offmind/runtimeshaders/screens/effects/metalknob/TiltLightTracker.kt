package com.offmind.runtimeshaders.screens.effects.metalknob

import kotlin.math.sqrt

/** Light direction in screen space: x right, y down, z toward the viewer. */
data class LightDirection(val x: Float, val y: Float, val z: Float) {
    companion object {
        val Base = LightDirection(-0.45f, -0.6f, 0.66f).normalized()
    }

    fun normalized(): LightDirection {
        val length = sqrt(x * x + y * y + z * z)
        return LightDirection(x / length, y / length, z / length)
    }
}

/**
 * Keeps a light fixed in the world while the phone moves underneath it.
 *
 * The first orientation sample anchors [LightDirection.Base] in the world; every later sample
 * expresses that same world direction in screen space, so tilting or twisting the phone moves the
 * reflections exactly like a lamp above real metal. A game rotation vector (gyroscope fused with
 * the accelerometer) gives smooth tilt and twist; gravity alone is a fallback that has no twist.
 */
class TiltLightTracker(private val onLight: (LightDirection) -> Unit) {
    private var anchorWorld: FloatArray? = null
    private var anchorUp: FloatArray? = null
    private var smoothedUp: FloatArray? = null

    /** Display rotation in quarter turns, matching `Surface.ROTATION_*`. */
    var displayRotation: Int = 0

    fun recenter() {
        anchorWorld = null
        anchorUp = null
        smoothedUp = null
        onLight(LightDirection.Base)
    }

    /** [vector] is the `SensorEvent.values` of a rotation vector sensor: x, y, z and optional w. */
    fun onRotationVector(vector: FloatArray) {
        val r = rotationMatrix(vector)
        val world = anchorWorld ?: multiply(r, toDevice(LightDirection.Base)).also { anchorWorld = it }
        emit(multiplyTransposed(r, world))
    }

    /** [acceleration] is gravity (or raw accelerometer) in device coordinates. */
    fun onGravity(acceleration: FloatArray) {
        val up = normalize(acceleration.copyOf(3))
        val smoothed = smoothedUp?.let { previous ->
            FloatArray(3) { previous[it] + (up[it] - previous[it]) * GravitySmoothing }
        } ?: up
        smoothedUp = smoothed
        val anchor = anchorUp ?: smoothed.also { anchorUp = it }
        // A light fixed above the phone moves in device space by as much as the up vector does.
        val base = toDevice(LightDirection.Base)
        emit(FloatArray(3) { base[it] + smoothed[it] - anchor[it] })
    }

    private fun emit(device: FloatArray) {
        val (x, yUp) = when (displayRotation and 3) {
            1 -> device[1] to -device[0]
            2 -> -device[0] to -device[1]
            3 -> -device[1] to device[0]
            else -> device[0] to device[1]
        }
        // Grazing or back-lit light makes the face black; keep the lamp slightly in front.
        onLight(LightDirection(x, -yUp, maxOf(device[2], MinLightZ)).normalized())
    }

    private fun toDevice(screen: LightDirection): FloatArray {
        val (x, yUp) = when (displayRotation and 3) {
            1 -> screen.y to screen.x
            2 -> -screen.x to screen.y
            3 -> -screen.y to -screen.x
            else -> screen.x to -screen.y
        }
        return floatArrayOf(x, yUp, screen.z)
    }

    private companion object {
        const val MinLightZ = 0.15f
        const val GravitySmoothing = 0.15f

        /** Row-major device-to-world matrix, the same as `SensorManager.getRotationMatrixFromVector`. */
        fun rotationMatrix(v: FloatArray): FloatArray {
            val x = v[0]
            val y = v[1]
            val z = v[2]
            val w = if (v.size > 3) v[3] else sqrt(maxOf(0f, 1f - x * x - y * y - z * z))
            return floatArrayOf(
                1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w),
                2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w),
                2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y),
            )
        }

        fun multiply(m: FloatArray, v: FloatArray) = FloatArray(3) { row ->
            m[row * 3] * v[0] + m[row * 3 + 1] * v[1] + m[row * 3 + 2] * v[2]
        }

        fun multiplyTransposed(m: FloatArray, v: FloatArray) = FloatArray(3) { column ->
            m[column] * v[0] + m[3 + column] * v[1] + m[6 + column] * v[2]
        }

        fun normalize(v: FloatArray): FloatArray {
            val length = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).takeIf { it > 0f } ?: 1f
            return FloatArray(3) { v[it] / length }
        }
    }
}
