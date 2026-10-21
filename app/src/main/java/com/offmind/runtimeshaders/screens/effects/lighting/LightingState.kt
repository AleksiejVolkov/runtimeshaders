package com.offmind.runtimeshaders.screens.effects.lighting

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

const val MAX_LIGHTS = 8

data class LightSource(
    val center: Offset,
    val color: Color,
    val intensity: Float,        // how strongly this light illuminates other elements
    val bloomIntensity: Float,   // how strongly this light draws its own visible halo
)

@Stable
class LightingState {
    private val _lights: SnapshotStateMap<Any, LightSource> =
        emptyList<Pair<Any, LightSource>>().toMutableStateMap()

    val lights: Collection<LightSource> get() = _lights.values

    // All receivers share one packed snapshot. Scrolling a receiver must not rebuild
    // the same arrays (or convert the same colors) for every layer on every frame.
    internal val uniforms: LightUniforms by derivedStateOf { LightUniforms(lights) }

    /** Size of the lighting scope, used to normalize light falloff across the scene. */
    var sceneSize: Size by mutableStateOf(Size.Zero)

    fun register(id: Any, source: LightSource) {
        _lights[id] = source
    }

    fun unregister(id: Any) {
        _lights.remove(id)
    }
}

internal class LightUniforms(lights: Collection<LightSource>) {
    val positions = FloatArray(MAX_LIGHTS * 2)
    val colors = FloatArray(MAX_LIGHTS * 3)
    val glareColors = FloatArray(MAX_LIGHTS * 3)
    val intensities = FloatArray(MAX_LIGHTS)
    val bloomIntensities = FloatArray(MAX_LIGHTS)
    val hasLighting: Boolean
    val hasBloom: Boolean

    init {
        lights.asSequence().take(MAX_LIGHTS).forEachIndexed { i, light ->
            positions[i * 2] = light.center.x
            positions[i * 2 + 1] = light.center.y
            colors[i * 3] = light.color.red
            colors[i * 3 + 1] = light.color.green
            colors[i * 3 + 2] = light.color.blue

            // RGB -> HSV, set V to 1, HSV -> RGB is RGB / max(R,G,B).
            // Preserve the original shader's achromatic black -> white case.
            val value = maxOf(light.color.red, light.color.green, light.color.blue)
            for (channel in 0..2) {
                glareColors[i * 3 + channel] =
                    if (value > 0f) colors[i * 3 + channel] / value else 1f
            }
            intensities[i] = light.intensity
            bloomIntensities[i] = light.bloomIntensity
        }
        hasLighting = intensities.any { it > 0f }
        hasBloom = bloomIntensities.any { it > 0f }
    }
}
