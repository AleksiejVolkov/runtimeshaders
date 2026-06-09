package com.offmind.runtimeshaders.screens.effects.lighting

import androidx.compose.runtime.Stable
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

    /** Size of the lighting scope, used to normalize light falloff across the scene. */
    var sceneSize: Size by mutableStateOf(Size.Zero)

    fun register(id: Any, source: LightSource) {
        _lights[id] = source
    }

    fun unregister(id: Any) {
        _lights.remove(id)
    }
}
