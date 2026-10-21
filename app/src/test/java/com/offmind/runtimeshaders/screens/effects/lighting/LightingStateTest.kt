package com.offmind.runtimeshaders.screens.effects.lighting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LightingStateTest {
    private val source = LightSource(Offset(20f, 40f), Color(0.2f, 0.4f, 0.8f), 0.5f, 0.3f)

    @Test
    fun receiversReuseUniformsUntilALightChanges() {
        val state = LightingState()
        state.register("lamp", source)
        val first = state.uniforms
        assertSame(first, state.uniforms)

        state.register("lamp", source.copy())
        assertSame(first, state.uniforms)

        state.register("lamp", source.copy(center = Offset(30f, 60f), intensity = 0.25f))
        val moved = state.uniforms
        assertNotSame(first, moved)
        assertEquals(30f, moved.positions[0], 0f)
        assertEquals(60f, moved.positions[1], 0f)
        assertEquals(0.25f, moved.intensities[0], 0f)
        // Already-issued snapshots remain valid for other receivers.
        assertEquals(20f, first.positions[0], 0f)
        assertEquals(0.5f, first.intensities[0], 0f)
    }

    @Test
    fun removingALightClearsItsUniformsAndDisablesBothPasses() {
        val state = LightingState()
        state.register("lamp", source)
        assertTrue(state.uniforms.hasLighting)
        assertTrue(state.uniforms.hasBloom)

        state.unregister("lamp")
        val empty = state.uniforms
        assertFalse(empty.hasLighting)
        assertFalse(empty.hasBloom)
        assertArrayEquals(FloatArray(MAX_LIGHTS * 2), empty.positions, 0f)
        assertArrayEquals(FloatArray(MAX_LIGHTS * 3), empty.colors, 0f)
        assertArrayEquals(FloatArray(MAX_LIGHTS), empty.intensities, 0f)
        assertArrayEquals(FloatArray(MAX_LIGHTS), empty.bloomIntensities, 0f)
    }

    @Test
    fun bloomAndSurfaceLightCanBeEnabledIndependently() {
        val bloomOnly = LightUniforms(listOf(source.copy(intensity = 0f)))
        assertFalse(bloomOnly.hasLighting)
        assertTrue(bloomOnly.hasBloom)

        val surfaceOnly = LightUniforms(listOf(source.copy(bloomIntensity = 0f)))
        assertTrue(surfaceOnly.hasLighting)
        assertFalse(surfaceOnly.hasBloom)
    }

    @Test
    fun lightsBeyondShaderCapacityDoNotEnableAPass() {
        val lights = List(MAX_LIGHTS) { source.copy(intensity = 0f, bloomIntensity = 0f) } + source
        val uniforms = LightUniforms(lights)
        assertFalse(uniforms.hasLighting)
        assertFalse(uniforms.hasBloom)
        assertEquals(MAX_LIGHTS * 2, uniforms.positions.size)
    }

    @Test
    fun glarePreservesHueAndSaturationAtFullValue() {
        val uniforms = LightUniforms(listOf(source, source.copy(color = Color.Black)))
        assertEquals(0.25f, uniforms.glareColors[0], 0.002f)
        assertEquals(0.5f, uniforms.glareColors[1], 0.002f)
        assertEquals(1f, uniforms.glareColors[2], 0f)
        // The original HSV shader also maps black (zero saturation) to white at V=1.
        assertArrayEquals(floatArrayOf(1f, 1f, 1f), uniforms.glareColors.sliceArray(3..5), 0f)
    }
}
