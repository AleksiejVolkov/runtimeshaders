package com.offmind.runtimeshaders.screens.effects.lighting

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

const val MAX_LIGHTS = 8
const val MAX_EXCLUSIONS = 8

data class LightSource(
    val center: Offset,
    val color: Color,
    val radiusPx: Float,       // how far receivers are influenced
    val bloomRadiusPx: Float,  // size of the visible halo in the global bloom pass
    val intensity: Float,
)

/**
 * A region (in root/window space) where the global bloom is masked out, so a
 * co-located [LightSource] appears to glow from under/around the element instead of
 * washing over its own pixels. [featherPx] softens the mask edge.
 */
data class ExclusionZone(
    val bounds: Rect,
    val featherPx: Float,
)

@Stable
class LightingState {
    private val _lights: SnapshotStateMap<Any, LightSource> =
        emptyList<Pair<Any, LightSource>>().toMutableStateMap()
    private val _exclusions: SnapshotStateMap<Any, ExclusionZone> =
        emptyList<Pair<Any, ExclusionZone>>().toMutableStateMap()

    val lights: Collection<LightSource> get() = _lights.values
    val exclusions: Collection<ExclusionZone> get() = _exclusions.values

    fun register(id: Any, source: LightSource) {
        _lights[id] = source
    }

    fun unregister(id: Any) {
        _lights.remove(id)
    }

    fun registerExclusion(id: Any, zone: ExclusionZone) {
        _exclusions[id] = zone
    }

    fun unregisterExclusion(id: Any) {
        _exclusions.remove(id)
    }
}
