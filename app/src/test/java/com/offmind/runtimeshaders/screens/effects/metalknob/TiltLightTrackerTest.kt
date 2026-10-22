package com.offmind.runtimeshaders.screens.effects.metalknob

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class TiltLightTrackerTest {
    private var light = LightDirection(0f, 0f, 1f)
    private val tracker = TiltLightTracker { light = it }

    /** Rotation vector (x, y, z, w) for [angle] radians around a device axis. */
    private fun rotation(axis: Int, angle: Float) = FloatArray(4).also {
        it[axis] = sin(angle / 2)
        it[3] = cos(angle / 2)
    }

    private fun assertLight(expected: LightDirection, actual: LightDirection) {
        assertEquals(expected.x, actual.x, 1e-4f)
        assertEquals(expected.y, actual.y, 1e-4f)
        assertEquals(expected.z, actual.z, 1e-4f)
    }

    @Test
    fun `first orientation anchors the base light in every display rotation`() {
        for (displayRotation in 0..3) {
            tracker.displayRotation = displayRotation
            tracker.recenter()
            tracker.onRotationVector(rotation(axis = 0, angle = 0.7f))
            assertLight(LightDirection.Base, light)
        }
    }

    @Test
    fun `lowering the right edge moves a world-fixed light to the left`() {
        tracker.onRotationVector(rotation(axis = 1, angle = 0f))
        // Positive rotation around device y lowers the right edge.
        tracker.onRotationVector(rotation(axis = 1, angle = 0.3f))
        assertTrue(light.x < LightDirection.Base.x)
    }

    @Test
    fun `twisting the phone turns the light around the screen normal`() {
        tracker.onRotationVector(rotation(axis = 2, angle = 0f))
        tracker.onRotationVector(rotation(axis = 2, angle = 0.5f))
        // Twisting counter-clockwise turns a world-fixed light clockwise on screen.
        val c = cos(0.5f)
        val s = sin(0.5f)
        val base = LightDirection.Base
        assertLight(LightDirection(base.x * c - base.y * s, base.x * s + base.y * c, base.z), light)
    }

    @Test
    fun `gravity fallback anchors and follows tilt`() {
        tracker.onGravity(floatArrayOf(0f, 6.9f, 6.9f))
        assertLight(LightDirection.Base, light)
        repeat(60) { tracker.onGravity(floatArrayOf(-3f, 6.9f, 6.2f)) }
        assertTrue(light.x < LightDirection.Base.x)
    }
}
